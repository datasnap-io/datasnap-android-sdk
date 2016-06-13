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
import com.datasnap.android.events.Event;
import com.datasnap.android.events.EventListener;
import com.datasnap.android.events.EventType;
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
import com.datasnap.android.controller.FlushThread;
import com.datasnap.android.controller.FlushThread.BatchFactory;
import com.datasnap.android.controller.IFlushLayer;
import com.datasnap.android.controller.IFlushLayer.FlushCallback;
import com.datasnap.android.controller.IRequestLayer;
import com.datasnap.android.controller.RequestThread;
import com.datasnap.android.stats.AnalyticsStatistics;
import com.datasnap.android.utils.HandlerTimer;
import com.google.gson.GsonBuilder;
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
    private static boolean syncingLocked;

    public static void initialize(android.content.Context context, String apiKeyId, String apiKeySecret, String organizationId, String projectId, VendorProperties properties) {
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
        dsConfig.setOrgId(organizationId);
        dsConfig.setProjectId(projectId);
        initializeData();
    }

    public static String getOrgId() {
        if (dsConfig == null)
            isInitialized();
        return dsConfig.getOrgId();
    }

    public static String  getProjectId() {
        if (dsConfig == null)
            isInitialized();
        return dsConfig.getProjectId();
    }

    /**
     *
     * API Calls: trackEvent() for a single event
     */
    public static void trackEvent(Event event) {
        if(!isInitialized())
            return;
        event.setDataSnapVersion(BuildConfig.VERSION_NAME);
        if(!event.validate())
            throw new IllegalStateException("Mandatory event data missing. Please call DataSnap.initialize before using the library.");
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
        if(event.equals(EventListener.GIMBAL_BEACON_SIGHTING) || event.equals(EventListener.ALL_EVENTS)) {
            if(gimbalService == null)
                return;
            if (value) {
                gimbalService.addGimbalBeaconSightingListener();
                sharedPreferences.edit().putBoolean(EventListener.GIMBAL_BEACON_SIGHTING, true).commit();
            } else {
                gimbalService.releaseGimbalBeaconSightingListener();
                sharedPreferences.edit().putBoolean(EventListener.GIMBAL_BEACON_SIGHTING, false).commit();
            }
        } else if(event.equals(EventListener.GIMBAL_COMMUNICATION) || event.equals(EventListener.ALL_EVENTS)) {
            if(gimbalService == null)
                return;
            if(value){
                gimbalService.addGimbalCommunicationListener();
                sharedPreferences.edit().putBoolean(EventListener.GIMBAL_COMMUNICATION, true).commit();
            } else {
                gimbalService.releaseGimbalCommunicationListener();
                sharedPreferences.edit().putBoolean(EventListener.GIMBAL_COMMUNICATION, false).commit();
            }
        }
    }

    public static void setFlushParams(int durationInMillis, int maxElements){
        sharedPreferences.edit().putInt(INITIAL_FLUSH_PERIOD, durationInMillis).commit();
        sharedPreferences.edit().putInt(FLUSH_PERIOD, durationInMillis).commit();
        dsConfig.setFlushAfter(durationInMillis);
        sharedPreferences.edit().putInt(INITIAL_FLUSH_QUEUE, maxElements).commit();
        sharedPreferences.edit().putInt(FLUSH_QUEUE, maxElements).commit();
        dsConfig.setFlushAt(maxElements);
        DataSnap.flushTimer.setFrequencyMs(durationInMillis);
    }

    /**
     * Stop the threads, and reset the client
     */
    public static void close() {

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
        User.initialize(user);
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
                    id.setMobileDeviceGoogleAdvertisingId(adInfo.isLimitAdTrackingEnabled() ? adInfo.getId() : "");
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
        if(vendorProperties == null)
            return;
        for(VendorProperties.Vendor vendor : vendorProperties.getVendor()) {
            switch (vendor) {
                case GIMBAL:
                    attemptGimbalServiceConnection();
                    break;
                case ESTIMOTE:
                    intent = new Intent(dataSnapContext, EstimoteService.class);
                    intent.putExtra(Context.class.toString(), (Serializable) dataSnapContext);
                    dataSnapContext.startService(intent);
                    dataSnapContext.bindService(intent, estimoteServiceConnection, Context.BIND_AUTO_CREATE);
                    break;
            }
        }
        if(sharedPreferences.getBoolean(PREFERENCE_FIRST_RUN, true)){
            String eventType = "app_installed";
            Event event = new InteractionEvent(eventType, getOrgId(), getProjectId(), null, null, null, user, null);
            trackEvent(event);
            sharedPreferences.edit().putBoolean(PREFERENCE_FIRST_RUN, false).commit();
        }
    }

    private static void attemptGimbalServiceConnection(){
        ConnectivityManager connectivityManager
            = (ConnectivityManager) dataSnapContext.getSystemService(Context.CONNECTIVITY_SERVICE);
        final NetworkInfo activeNetworkInfo = connectivityManager.getActiveNetworkInfo();
        if(activeNetworkInfo != null && activeNetworkInfo.isConnected()) {
            Intent intent = new Intent(dataSnapContext, GimbalService.class);
            intent.putExtra("gimbalApiKey", vendorProperties.getGimbalApiKey());
            dataSnapContext.startService(intent);
            dataSnapContext.bindService(intent, gimbalServiceConnection, Context.BIND_AUTO_CREATE);
        } else {
            final Handler handler = new Handler();
            handler.postDelayed(new Runnable() {
                @Override
                public void run() {
                    if(activeNetworkInfo == null || !activeNetworkInfo.isConnected()){
                        attemptGimbalServiceConnection();
                    }
                }
            }, 10000);
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
            if(gimbalService == null)
                return;
            if(sharedPreferences.getBoolean(EventListener.GIMBAL_BEACON_SIGHTING, true)) {
                if(gimbalService != null)
                    gimbalService.addGimbalBeaconSightingListener();
            } else {
                gimbalService.releaseGimbalBeaconSightingListener();
            }
            if(sharedPreferences.getBoolean(EventListener.GIMBAL_COMMUNICATION, true)) {
                if(gimbalService != null)
                    gimbalService.addGimbalCommunicationListener();
            } else {
                gimbalService.releaseGimbalCommunicationListener();
            }
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
        if(!isInitialized() || syncingLocked)
            return;
        syncingLocked = true;
        final CountDownLatch latch = new CountDownLatch(1);
        ConnectivityManager connectivityManager
            = (ConnectivityManager) dataSnapContext.getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo activeNetworkInfo = connectivityManager.getActiveNetworkInfo();
        if(activeNetworkInfo == null || !activeNetworkInfo.isConnected())
            return;
        statistics.updateFlushAttempts(1);
        final long start = System.currentTimeMillis();
        flushLayer.flush(new FlushCallback() {
            @Override
            public void onFlushCompleted(boolean success, List<EventWrapper>batch, int statusCode) {
                latch.countDown();
                syncingLocked = false;
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
                if (rowCount >= DsConfig.getInstance().getFlushAt()) {
                    DataSnap.flush(true);
                }
            }
        });
    }

    private static boolean isInitialized() {
        return initialized && User.getInstance() != null && DeviceInfo.getInstance() != null && DsConfig.getInstance() != null;
    }
}
