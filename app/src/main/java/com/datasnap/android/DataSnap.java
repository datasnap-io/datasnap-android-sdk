package com.datasnap.android;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Handler;
import android.os.IBinder;
import android.preference.PreferenceManager;

import com.datasnap.android.controller.EventDatabase;
import com.datasnap.android.controller.EventDatabaseLayerInterface;
import com.datasnap.android.controller.EventDatabaseLayerInterface.EnqueueCallback;
import com.datasnap.android.controller.EventDatabaseThread;
import com.datasnap.android.controller.EventWrapper;
import com.datasnap.android.controller.FlushThread;
import com.datasnap.android.controller.HTTPRequester;
import com.datasnap.android.controller.IFlushLayer;
import com.datasnap.android.controller.IFlushLayer.FlushCallback;
import com.datasnap.android.controller.IRequestLayer;
import com.datasnap.android.controller.RequestThread;
import com.datasnap.android.eventproperties.Device;
import com.datasnap.android.eventproperties.User;
import com.datasnap.android.events.Event;
import com.datasnap.android.events.EventType;
import com.datasnap.android.events.InteractionEvent;
import com.datasnap.android.services.EstimoteService;
import com.datasnap.android.services.GimbalService;
import com.datasnap.android.services.ServiceManager;
import com.datasnap.android.stats.AnalyticsStatistics;
import com.datasnap.android.utils.DsConfig;
import com.datasnap.android.utils.HandlerTimer;
import com.datasnap.android.utils.Logger;
import com.google.gson.FieldNamingPolicy;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import java.io.Serializable;
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
    private static Context dataSnapContext;
    private static final String PREFERENCE_FIRST_RUN = "com.datasnap.android.first_run";
    private static final String FLUSH_PERIOD = "com.datasnap.android.flush_period";
    private static final String FLUSH_QUEUE = "com.datasnap.android.flush_queue";
    private static final String INITIAL_FLUSH_PERIOD = "com.datasnap.android.initial_flush_period";
    private static final String INITIAL_FLUSH_QUEUE = "com.datasnap.android.initial_flush_queue";
    private static GimbalService gimbalService;
    private static EstimoteService estimoteService;
    private static SharedPreferences sharedPreferences;
    private static VendorProperties vendorProperties;
    private static boolean syncingLocked;
    private static Config datasnapConfig;

    public static void initialize(Context context, Config config) {
        // cleanup all the active processes in case Datasnap has been already initialized
        close();
        if (context == null) {
            Logger.e("DataSnap client must be initialized with a valid android context.");
            return;
        }
        dataSnapContext = context;
        datasnapConfig = config;
        initializeDsConfig();
        // set logging based on the debug mode
        Logger.setLog(dsConfig.isDebug());
        vendorProperties = config.getVendorProperties();
        sharedPreferences = PreferenceManager.getDefaultSharedPreferences(dataSnapContext);
        DataSnap.statistics = AnalyticsStatistics.getInstance();
        initializeDatabaseLayer();
        initializeRequestLayer();
        initializeFlushingLayer();
        initializeGsonBuilder();
        initializeData();
    }

    /**
     *
     * API Calls: trackEvent() for a single event
     */
    public static void trackEvent(Event event) {
        //TODO add logs instead of returning/throwing exceptions
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
    public static void setEventEnabled(EventType event, boolean value) {
        ServiceManager.setEventEnabled(event, value, dataSnapContext);
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
    private static void close() {

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
        if(database != null)
            database.close();
        dsConfig = null;
    }

    private static void initializeDsConfig(){
        DsConfig.initialize(datasnapConfig.getApiKeyId()+":"+datasnapConfig.getApiKeySecret());
        dsConfig = DsConfig.getInstance();
        dsConfig.setOrgId(datasnapConfig.getOrganizationId());
        dsConfig.setProjectId(datasnapConfig.getProjectId());
    }

    private static void initializeGsonBuilder(){
        GsonBuilder gsonBuilder = new GsonBuilder();
        gsonBuilder.setFieldNamingPolicy(FieldNamingPolicy.LOWER_CASE_WITH_UNDERSCORES);
    }

    private static void initializeDatabaseLayer(){
        database = EventDatabase.getInstance(dataSnapContext);
        // now we need to create our singleton thread-safe database thread
        DataSnap.databaseLayer = new EventDatabaseThread(database);
        DataSnap.databaseLayer.start();
    }

    private static void initializeRequestLayer(){
        HTTPRequester requester = new HTTPRequester();
        DataSnap.requestLayer = new RequestThread(requester,  dataSnapContext);
        DataSnap.requestLayer.start();
    }

    private static void initializeFlushingLayer(){
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
        // start the flush thread
        DataSnap.flushLayer = new FlushThread(DataSnap.databaseLayer, DataSnap.requestLayer, dataSnapContext);
        DataSnap.flushTimer = new HandlerTimer(dsConfig.getFlushAfter(), flushClock);
        // start the other threads
        DataSnap.flushTimer.start();
        DataSnap.flushLayer.start();
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
        //TODO move to service manager
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
            Event event = new InteractionEvent(EventType.APP_INSTALLED);
            trackEvent(event);
            sharedPreferences.edit().putBoolean(PREFERENCE_FIRST_RUN, false).commit();
        }
    }

    /* Gimbal requires to pull some data from the network in order to start correctly otherwise
     * it just fails to start. Accordingly we need to make sure there is connectivity before
     * attempting a connection to the service.
     */
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
            ServiceManager.initializeService(estimoteService);
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
            ServiceManager.initializeService(gimbalService);
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
        return User.getInstance() != null && Device.getInstance() != null && DsConfig.getInstance() != null;
    }
}
