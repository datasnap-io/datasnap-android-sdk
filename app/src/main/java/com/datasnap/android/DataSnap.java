package com.datasnap.android;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.Handler;
import android.os.IBinder;
import android.preference.PreferenceManager;
import android.provider.Settings;
import android.telephony.TelephonyManager;

import com.datasnap.android.controller.EventDatabase;
import com.datasnap.android.controller.EventDatabaseLayerInterface;
import com.datasnap.android.controller.EventDatabaseThread;
import com.datasnap.android.controller.HTTPRequester;
import com.datasnap.android.controller.EventWrapper;
import com.datasnap.android.eventproperties.Device;
import com.datasnap.android.eventproperties.DeviceInfo;
import com.datasnap.android.eventproperties.Id;
import com.datasnap.android.eventproperties.User;
import com.datasnap.android.events.InteractionEvent;
import com.datasnap.android.services.EstimoteService;
import com.datasnap.android.services.GimbalService;
import com.datasnap.android.utils.DsConfig;
import com.datasnap.android.utils.Logger;
import com.google.android.gms.ads.identifier.AdvertisingIdClient;
import com.google.android.gms.drive.realtime.internal.event.ObjectChangedDetails;
import com.google.gson.FieldNamingPolicy;
import com.google.gson.Gson;
import com.datasnap.android.controller.EventDatabaseLayerInterface.EnqueueCallback;
import com.datasnap.android.events.IEvent;
import com.datasnap.android.controller.FlushThread;
import com.datasnap.android.controller.FlushThread.BatchFactory;
import com.datasnap.android.controller.IFlushLayer;
import com.datasnap.android.controller.IFlushLayer.FlushCallback;
import com.datasnap.android.controller.IRequestLayer;
import com.datasnap.android.controller.RequestThread;
import com.datasnap.android.stats.AnalyticsStatistics;
import com.datasnap.android.utils.HandlerTimer;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.Serializable;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.concurrent.CountDownLatch;

import static com.datasnap.android.utils.Utils.isNullOrEmpty;

public final class DataSnap {

    private static AnalyticsStatistics statistics;
    private static DsConfig dsConfig;
    private static HandlerTimer flushTimer;
    private static EventDatabase database;
    private static EventDatabaseLayerInterface databaseLayer;
    private static IRequestLayer requestLayer;
    private static IFlushLayer flushLayer;
    private static volatile boolean initialized;
    public static boolean networkAvailable;
    private static Context dataSnapContext;
    private static final String PREFERENCE_FIRST_RUN = "first_run";
    private static final String BEACON_SIGHTING_SWITCH = "beacon_sighting";
    private static final String COMMUNICATION_SIGHTING_SWITCH = "communication_sighting";
    private static final String GEOFENCE_DEPART_SWITCH = "geofence_depart";
    private static final String FLUSH_PERIOD = "flush_period";
    private static final String FLUSH_QUEUE = "flush_queue";
    private static final String INITIAL_FLUSH_PERIOD = "initial_flush_period";
    private static final String INITIAL_FLUSH_QUEUE = "initial_flush_queue";
    private static final User user = new User();
    private static final Id id = new Id();
    private static DeviceInfo deviceInfo = new DeviceInfo();
    private static GimbalService gimbalService;
    private static EstimoteService estimoteService;
    private static SharedPreferences sharedPreferences;
    private static VendorProperties vendorProperties;

    public static void initialize(android.content.Context context, String apiKeyId, String apiKeySecret, VendorProperties properties) {
        String errorPrefix = "DataSnap client must be initialized with a valid ";
        if (context == null) throw new IllegalArgumentException(errorPrefix + "android context.");
        if (initialized) return;
        DsConfig.initialize(apiKeyId+":"+apiKeySecret);
        dataSnapContext = context;
        vendorProperties = properties;
        sharedPreferences = PreferenceManager.getDefaultSharedPreferences(dataSnapContext);
        DataSnap.statistics = AnalyticsStatistics.getInstance();
        dsConfig = DsConfig.getInstance();
        if(sharedPreferences.getInt(FLUSH_PERIOD, 0) == 0) {
            if(sharedPreferences.getInt(INITIAL_FLUSH_PERIOD, 0) == 0) {
                sharedPreferences.edit().putInt(FLUSH_PERIOD, dsConfig.getFlushAfter()).commit();
                sharedPreferences.edit().putInt(INITIAL_FLUSH_PERIOD, dsConfig.getFlushAfter()).commit();
            } else
                sharedPreferences.edit().putInt(FLUSH_PERIOD, sharedPreferences.getInt(INITIAL_FLUSH_PERIOD, 0)).commit();
        }
        if(sharedPreferences.getInt(FLUSH_QUEUE, 0) == 0) {
            if(sharedPreferences.getInt(INITIAL_FLUSH_QUEUE, 0) == 0) {
                sharedPreferences.edit().putInt(FLUSH_QUEUE, dsConfig.getFlushAfter()).commit();
                sharedPreferences.edit().putInt(INITIAL_FLUSH_QUEUE, dsConfig.getFlushAfter()).commit();
            } else
                sharedPreferences.edit().putInt(FLUSH_QUEUE, sharedPreferences.getInt(INITIAL_FLUSH_QUEUE, 0)).commit();
        }
        // set logging based on the debug mode
        Logger.setLog(dsConfig.isDebug());
        networkAvailable = isNetworkAvailable(context);
        database = EventDatabase.getInstance(context);
        // now we need to create our singleton thread-safe database thread
        DataSnap.databaseLayer = new EventDatabaseThread(database);
        DataSnap.databaseLayer.start();

        HTTPRequester requester = new HTTPRequester();
        // and a single request thread
        DataSnap.requestLayer = new RequestThread(requester);
        DataSnap.requestLayer.start();
        // start the flush thread
        DataSnap.flushLayer = new FlushThread(DataSnap.databaseLayer, batchFactory, DataSnap.requestLayer);
        DataSnap.flushTimer = new HandlerTimer(dsConfig.getFlushAfter(), flushClock);
        initialized = true;
        // start the other threads
        DataSnap.flushTimer.start();
        DataSnap.flushLayer.start();

        // intitialise json builder
        GsonBuilder gsonBuilder = new GsonBuilder();
        gsonBuilder.setFieldNamingPolicy(FieldNamingPolicy.LOWER_CASE_WITH_UNDERSCORES);
        initializeOrganization();
    }

    public static void initializeOrganization() {
        DataSnap.requestLayer.getOrganization(new IRequestLayer.OrganizationRequestCallback() {
            @Override
            public void onRequestCompleted(String response) {
                JSONArray organizationData;
                try {
                    organizationData = new JSONArray(response);
                    for(int i=0; i < organizationData.length(); i++){
                        JSONObject object = (JSONObject) organizationData.get(i);
                        if("true".equals(object.getString("isvenue"))){
                            String venue = object.getString("id");
                            dsConfig.setVenue(venue);
                        } else if("null".equals(object.getString("organization_id"))){
                            String[] projects = new String[1];
                            projects[0] = ((JSONObject)organizationData.get(0)).getString("name");
                            dsConfig.setProjectIds(projects);
                        } else{
                            String[] organizations = new String[1];
                            organizations[0] = ((JSONObject)organizationData.get(1)).getString("id");
                            dsConfig.setOrgIds(organizations);
                        }
                    }
                    initializeData();
                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }
        });
    }

    public static String[] getOrgIds() {
        if (dsConfig == null)
            checkInitialized();
        return dsConfig.getOrgIds();
    }

    public static String[]  getProjectIds() {
        if (dsConfig == null)
            checkInitialized();
        return dsConfig.getProjectIds();
    }

    /**
     *
     * API Calls: trackEvent() for a single event
     */
    public static void trackEvent(IEvent event) {
        checkInitialized();
        GsonBuilder gsonBuilder = new GsonBuilder();
        gsonBuilder.setFieldNamingPolicy(FieldNamingPolicy.LOWER_CASE_WITH_UNDERSCORES);
        Gson gson = gsonBuilder.create();
        String json = gson.toJson(event);
        json = json.replace("\"global_position\"", "\"global-position\"");
        if (isNullOrEmpty(json)) {
            throw new IllegalArgumentException(
                    "analytics-android #trackEvent must be initialized with a valid event name.");
        }
        EventWrapper eventWrapper = new EventWrapper(json);
        enqueue(eventWrapper);
        statistics.updateTracks(1);
    }

    /**
     *
     */
    public static void setEventEnabled(String event, boolean value) {
        switch (event){
            case BEACON_SIGHTING_SWITCH:
                if(value){
                    gimbalService.addGimbalBeaconSightingListener();
                    sharedPreferences.edit().putBoolean(BEACON_SIGHTING_SWITCH, true).commit();
                } else {
                    gimbalService.releaseGimbalBeaconSightingListener();
                    sharedPreferences.edit().putBoolean(BEACON_SIGHTING_SWITCH, false).commit();
                }
                break;
            case COMMUNICATION_SIGHTING_SWITCH:
                if(value){
                    gimbalService.addGimbalCommunicationListener();
                    sharedPreferences.edit().putBoolean(COMMUNICATION_SIGHTING_SWITCH, true).commit();
                } else {
                    gimbalService.releaseGimbalCommunicationListener();
                    sharedPreferences.edit().putBoolean(COMMUNICATION_SIGHTING_SWITCH, false).commit();
                }
                break;
        }
    }

    public static void setFlushParams(int duration, int maxElements){
        sharedPreferences.edit().putInt(INITIAL_FLUSH_PERIOD, duration).commit();
        sharedPreferences.edit().putInt(FLUSH_PERIOD, duration).commit();
        dsConfig.setFlushAfter(duration);
        sharedPreferences.edit().putInt(INITIAL_FLUSH_QUEUE, maxElements).commit();
        sharedPreferences.edit().putInt(FLUSH_QUEUE, maxElements).commit();
        dsConfig.setFlushAt(maxElements);
    }

    /**
     * Stop the threads, and reset the client
     */
    public static void close() {
        checkInitialized();

        // stop the looper on the timer, & the flush, request, and database threads
        flushTimer.quit();
        flushLayer.quit();
        databaseLayer.quit();
        requestLayer.quit();

        // close database and set intialized parameters to null
        database.close();
        dsConfig = null;
        initialized = false;
    }

    private static void initializeData() {
        String android_id = Settings.Secure.getString(dataSnapContext.getContentResolver(),
            Settings.Secure.ANDROID_ID);
        id.setGlobalDistinctId(android_id);
        Device device = new Device();
        deviceInfo.setCreated(getTime());
        device.setIpAddress(getIpAddress());
        device.setPlatform(android.os.Build.VERSION.SDK);
        device.setOsVersion(System.getProperty("os.version"));
        device.setModel(android.os.Build.MODEL);
        device.setManufacturer(android.os.Build.MANUFACTURER);
        device.setName(android.os.Build.DEVICE);
        device.setVendorId(android.os.Build.BRAND);
        TelephonyManager manager = (TelephonyManager) dataSnapContext.getSystemService(Context.TELEPHONY_SERVICE);
        device.setCarrierName(manager.getNetworkOperatorName());
        deviceInfo.setDevice(device);
        DeviceInfo.initialize(deviceInfo);
        final Handler mainHandler = new Handler(dataSnapContext.getMainLooper());
        final Runnable mainRunnable = new Runnable() {
            @Override
            public void run() {
                onDataInitialized();
            }
        };
        new Thread(new Runnable() {
            public void run() {
                try {
                    AdvertisingIdClient.Info adInfo = AdvertisingIdClient.getAdvertisingIdInfo(dataSnapContext);
                    id.setMobileDeviceGoogleAdvertisingId(adInfo.getId());
                    id.setMobileDeviceGoogleAdvertisingIdOptIn("" + adInfo.isLimitAdTrackingEnabled());
                    user.setId(id);
                    User.initialize(user);
                    mainHandler.post(mainRunnable);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }).start();
    }

    private static void onDataInitialized(){
        Intent intent;
        switch (vendorProperties.getVendor()) {
            case GIMBAL:
                intent = new Intent(dataSnapContext, GimbalService.class);
                intent.putExtra("gimbalApiKey", vendorProperties.getGimbalApiKey());
                dataSnapContext.startService(intent);
                dataSnapContext.bindService(intent, gimbalServiceConnection, Context.BIND_AUTO_CREATE);
                break;
            case ESTIMOTE:
                intent = new Intent(dataSnapContext, EstimoteService.class);
                intent.putExtra(Context.class.toString(), (Serializable) dataSnapContext);
                dataSnapContext.startService(intent);
                dataSnapContext.bindService(intent, estimoteServiceConnection, Context.BIND_AUTO_CREATE);
                break;
        }
        if(sharedPreferences.getBoolean(PREFERENCE_FIRST_RUN, true)){
            String eventType = "app_installed";
            IEvent event = new InteractionEvent(eventType, getOrgIds(), getProjectIds(), null, null, null, user, null);
            trackEvent(event);
            sharedPreferences.edit().putBoolean(PREFERENCE_FIRST_RUN, false).commit();
        }
        if(sharedPreferences.getBoolean(BEACON_SIGHTING_SWITCH, true)) {
            gimbalService.addGimbalBeaconSightingListener();
        } else {
            gimbalService.releaseGimbalBeaconSightingListener();
        }
        if(sharedPreferences.getBoolean(COMMUNICATION_SIGHTING_SWITCH, true)) {
            gimbalService.addGimbalCommunicationListener();
        } else {
            gimbalService.releaseGimbalCommunicationListener();
        }
    }

    private static ServiceConnection estimoteServiceConnection = new ServiceConnection() {
        @Override
        public void onServiceDisconnected(ComponentName name) {
        }

        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            EstimoteService.DataSnapBinder estimoteBinder = (EstimoteService.DataSnapBinder) service;
            estimoteService = estimoteBinder.getService();
        }
    };

    private static ServiceConnection gimbalServiceConnection = new ServiceConnection() {
        @Override
        public void onServiceDisconnected(ComponentName name) {
        }

        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            GimbalService.DataSnapBinder gimbalBinder = (GimbalService.DataSnapBinder) service;
            gimbalService = gimbalBinder.getService();
        }
    };

    private static String getIpAddress() {
        WifiManager wifiMan = (WifiManager) dataSnapContext.getSystemService(Context.WIFI_SERVICE);
        WifiInfo wifiInf = wifiMan.getConnectionInfo();
        int ipAddress = wifiInf.getIpAddress();
        String ip = String.format("%d.%d.%d.%d", (ipAddress & 0xff), (ipAddress >> 8 & 0xff), (ipAddress >> 16 & 0xff), (ipAddress >> 24 & 0xff));
        return ip;
    }

    private static String getTime() {
        Calendar c = Calendar.getInstance();
        Date d = c.getTime();
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd hh:mm:ss ZZ");
        return sdf.format(d);
    }

    // Factory that creates batches from event payloads.
    private static BatchFactory batchFactory = new BatchFactory() {
        @Override
        public List<EventWrapper>  create(List<EventWrapper> payloads) {
            return new ArrayList<EventWrapper> (payloads);
        }
    };

    // Flushes on a clock timer
    private static Runnable flushClock = new Runnable() {
        @Override
        public void run() {
            DataSnap.flush(true);
        }
    };


    private static boolean isNetworkAvailable(Context context) {
        ConnectivityManager connectivityManager
            = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo activeNetworkInfo = connectivityManager.getActiveNetworkInfo();
        return activeNetworkInfo != null && activeNetworkInfo.isConnected();
    }

    /**
     * Flush data to the server.
     *
     * @param async True to block until the data is flushed
     */
    private static void flush(boolean async) {
        checkInitialized();
        statistics.updateFlushAttempts(1);
        final long start = System.currentTimeMillis();
        final CountDownLatch latch = new CountDownLatch(1);

        flushLayer.flush(new FlushCallback() {
            @Override
            public void onFlushCompleted(boolean success, List<EventWrapper>batch, int statusCode) {
                latch.countDown();
                if (success) {
                    long duration = System.currentTimeMillis() - start;
                    statistics.updateFlushTime(duration);
                    sharedPreferences.edit().putInt(FLUSH_PERIOD, sharedPreferences.getInt(INITIAL_FLUSH_PERIOD, 0)).commit();
                } else {
                    if(statusCode != 400) {
                        dsConfig.setFlushAfter(((int)(sharedPreferences.getInt(FLUSH_PERIOD, 0) * 1.5)));
                        sharedPreferences.edit().putInt(FLUSH_PERIOD, dsConfig.getFlushAfter()).commit();

                    }
                }
            }
        });

        if (!async) {
            try {
                latch.await();
            } catch (InterruptedException e) {
                Logger.e("Interrupted while waiting for a blocking flush.");
            }
        }
    }

    /**
     * Enqueues a an event
     */
    private static void enqueue(final EventWrapper payload) {
        statistics.updateInsertAttempts(1);
        final long start = System.currentTimeMillis();
        databaseLayer.enqueue(payload, new EnqueueCallback() {
            @Override
            public void onEnqueue(boolean success, long rowCount) {
                long duration = System.currentTimeMillis() - start;
                statistics.updateInsertTime(duration);
                if (success) {
                    Logger.d("Item %s successfully enqueued.", "description string");
                } else {
                    Logger.w("Item %s failed to be enqueued.", "description string");
                }
                //   flushes depending on rowcount
                if (rowCount >= dsConfig.getFlushAt()) {
                    DataSnap.flush(true);
                }
            }
        });
    }

    private static void checkInitialized() {
        if (!initialized || User.getInstance() == null || DeviceInfo.getInstance() == null || DsConfig.getInstance() == null) {
            throw new IllegalStateException("Please call DataSnap.initialize before using the library.");
        }
    }
}
