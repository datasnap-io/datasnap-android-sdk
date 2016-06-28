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
import com.datasnap.android.eventproperties.Id;
import com.datasnap.android.eventproperties.User;
import com.datasnap.android.events.Event;
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
    private static Context dataSnapContext;
    private static final String PREFERENCE_FIRST_RUN = "first_run";
    private static final String FLUSH_PERIOD = "flush_period";
    private static final String FLUSH_QUEUE = "flush_queue";
    private static final String INITIAL_FLUSH_PERIOD = "initial_flush_period";
    private static final String INITIAL_FLUSH_QUEUE = "initial_flush_queue";
    private static GimbalService gimbalService;
    private static EstimoteService estimoteService;
    private static SharedPreferences sharedPreferences;
    private static VendorProperties vendorProperties;
    private static boolean syncingLocked;

    public static void initialize(Config config) {
        String errorPrefix = "DataSnap client must be initialized with a valid ";
        if (config.context == null) throw new IllegalArgumentException(errorPrefix + "android context.");
        if (initialized) return;
        DsConfig.initialize(config.apiKeyId+":"+config.apiKeySecret);
        dataSnapContext = config.context;
        vendorProperties = config.vendorProperties;
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
        database = EventDatabase.getInstance(config.context);
        // now we need to create our singleton thread-safe database thread
        DataSnap.databaseLayer = new EventDatabaseThread(database);
        DataSnap.databaseLayer.start();

        HTTPRequester requester = new HTTPRequester();
        // and a single request thread
        DataSnap.requestLayer = new RequestThread(requester,  dataSnapContext);
        DataSnap.requestLayer.start();
        // start the flush thread
        DataSnap.flushLayer = new FlushThread(DataSnap.databaseLayer, DataSnap.requestLayer, dataSnapContext);
        DataSnap.flushTimer = new HandlerTimer(dsConfig.getFlushAfter(), flushClock);
        initialized = true;
        // start the other threads
        DataSnap.flushTimer.start();
        DataSnap.flushLayer.start();

        // intitialise json builder
        GsonBuilder gsonBuilder = new GsonBuilder();
        gsonBuilder.setFieldNamingPolicy(FieldNamingPolicy.LOWER_CASE_WITH_UNDERSCORES);
        dsConfig.setOrgId(config.organizationId);
        dsConfig.setProjectId(config.projectId);
        initializeData();
    }

    /**
     *
     * API Calls: trackEvent() for a single event
     */
    public static void trackEvent(Event event) {
        if(!isInitialized())
            return;
        if(!event.validate())
            throw new IllegalStateException("Mandatory event data missing. Please call DataSnap.initialize before using the library.");
        GsonBuilder gsonBuilder = new GsonBuilder();
        gsonBuilder.setFieldNamingPolicy(FieldNamingPolicy.LOWER_CASE_WITH_UNDERSCORES);
        Gson gson = gsonBuilder.create();
        String json = gson.toJson(event);
        if (isNullOrEmpty(json)) {
            throw new IllegalArgumentException(
                    "analytics-android #trackEvent must be initialized with a valid event name.");
        }
        EventWrapper eventWrapper = new EventWrapper(json);
        enqueue(eventWrapper);
        statistics.updateTracks(1);
    }

    /**
     *  this method turns on and off automatic events sending from Datasnap.
     *  @param event: the event type, e.g. BEACON_SIGHTING
     *  @param value: true/false to turn on and off
     */
    public static void setEventEnabled(String event, boolean value) {
        if(event.equals(EventType.BEACON_SIGHTING) || event.equals(EventType.ALL_EVENTS)) {
            if(gimbalService == null)
                return;
            if (value) {
                gimbalService.addGimbalBeaconSightingListener();
                sharedPreferences.edit().putBoolean(EventType.BEACON_SIGHTING, true).commit();
            } else {
                gimbalService.releaseGimbalBeaconSightingListener();
                sharedPreferences.edit().putBoolean(EventType.BEACON_SIGHTING, false).commit();
            }
        }
        if(event.equals(EventType.COMMUNICATION_SENT) || event.equals(EventType.ALL_EVENTS)) {
            if(gimbalService == null)
                return;
            if(value){
                gimbalService.addGimbalCommunicationSentListener();
                sharedPreferences.edit().putBoolean(EventType.COMMUNICATION_SENT, true).commit();
            } else {
                gimbalService.releaseGimbalCommunicationSentListener();
                sharedPreferences.edit().putBoolean(EventType.COMMUNICATION_SENT, false).commit();
            }
        }
        if(event.equals(EventType.COMMUNICATION_OPEN) || event.equals(EventType.ALL_EVENTS)) {
            if(gimbalService == null)
                return;
            if(value){
                gimbalService.addGimbalCommunicationOpenListener();
                sharedPreferences.edit().putBoolean(EventType.COMMUNICATION_OPEN, true).commit();
            } else {
                gimbalService.releaseGimbalCommunicationOpenListener();
                sharedPreferences.edit().putBoolean(EventType.COMMUNICATION_OPEN, false).commit();
            }
        }
        if(event.equals(EventType.GEOFENCE_DEPART) || event.equals(EventType.ALL_EVENTS)) {
            if(gimbalService == null)
                return;
            if(value){
                gimbalService.addGimbalGeofenceDepartListener();
                sharedPreferences.edit().putBoolean(EventType.GEOFENCE_DEPART, true).commit();
            } else {
                gimbalService.releaseGimbalGeofenceDepartListener();
                sharedPreferences.edit().putBoolean(EventType.GEOFENCE_DEPART, false).commit();
            }
        }
    }

    /**
     *  this method changes the parameters to determine when to sync with the server.
     *  @param durationInMillis: time to be waited for periodic sync
     *  @param maxElements: queue size for queue driven sync. e.g. 30 --> sync is triggered after 30 elements are added
     */
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
        if(flushTimer != null)
            flushTimer.quit();
        if(flushLayer != null)
            flushLayer.quit();
        if(databaseLayer != null)
            databaseLayer.quit();
        if(requestLayer != null)
            requestLayer.quit();

        // close database and set intialized parameters to null
        database.close();
        dsConfig = null;
        initialized = false;
    }

    private static void initializeData() {
        Device.initialize(dataSnapContext);
        final Handler mainHandler = new Handler(dataSnapContext.getMainLooper());
        final Runnable mainRunnable = new Runnable() {
            @Override
            public void run() {
                initializeVendorServices();
            }
        };
        User.initialize(mainHandler, mainRunnable, dataSnapContext);
    }

    private static void initializeVendorServices(){
        Intent intent;
        if(vendorProperties == null)
            return;
        for(VendorProperties.Vendor vendor : vendorProperties.getVendor()) {
            switch (vendor) {
                case GIMBAL:
                    // Gimbal requires to pull some data from the network in order to start correctly otherwise
                    // it just fails to start. Accordingly we need to make sure there is connectivity before
                    // attempting a connection to the service.
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
            Event event = new InteractionEvent(EventType.APP_INSTALLED);
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
            if(sharedPreferences.getBoolean(EventType.BEACON_SIGHTING, true)) {
                if(gimbalService != null)
                    gimbalService.addGimbalBeaconSightingListener();
            } else {
                gimbalService.releaseGimbalBeaconSightingListener();
            }
            if(sharedPreferences.getBoolean(EventType.COMMUNICATION_SENT, true)) {
                if(gimbalService != null)
                    gimbalService.addGimbalCommunicationSentListener();
            } else {
                gimbalService.releaseGimbalCommunicationSentListener();
            }
            if(sharedPreferences.getBoolean(EventType.COMMUNICATION_OPEN, true)) {
                if(gimbalService != null)
                    gimbalService.addGimbalCommunicationOpenListener();
            } else {
                gimbalService.releaseGimbalCommunicationOpenListener();
            }
            if(sharedPreferences.getBoolean(EventType.GEOFENCE_DEPART, true)) {
                if(gimbalService != null)
                    gimbalService.addGimbalGeofenceDepartListener();
            } else {
                gimbalService.releaseGimbalGeofenceDepartListener();
            }
        }
    };

    // Flushes on a clock timer
    private static Runnable flushClock = new Runnable() {
        @Override
        public void run() {
            DataSnap.flush(true);
        }
    };

    /**
     * Flush data to the server.
     *
     * @param async True to block until the data is flushed
     */
    private static void flush(boolean async) {
        if(!isInitialized() || syncingLocked)
            return;
        final CountDownLatch latch = new CountDownLatch(1);
        ConnectivityManager connectivityManager
            = (ConnectivityManager) dataSnapContext.getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo activeNetworkInfo = connectivityManager.getActiveNetworkInfo();
        if(activeNetworkInfo == null || !activeNetworkInfo.isConnected())
            return;
        statistics.updateFlushAttempts(1);
        final long start = System.currentTimeMillis();
        syncingLocked = true;
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
        return initialized && User.getInstance() != null && DsConfig.getInstance() != null;
    }
}
