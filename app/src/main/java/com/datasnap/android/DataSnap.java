package com.datasnap.android;

import android.app.Activity;
import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;

import com.datasnap.android.controller.EventDatabase;
import com.datasnap.android.controller.EventDatabaseLayerInterface;
import com.datasnap.android.controller.EventDatabaseThread;
import com.datasnap.android.controller.HTTPRequester;
import com.datasnap.android.controller.EventWrapper;
import com.datasnap.android.utils.DsConfig;
import com.datasnap.android.utils.Logger;
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

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;

import static com.datasnap.android.utils.Utils.isNullOrEmpty;

public final class DataSnap {

    private static AnalyticsStatistics statistics;
    private static String apiKey;
    private static DsConfig dsConfig;
    private static HandlerTimer flushTimer;
    private static EventDatabase database;
    private static EventDatabaseLayerInterface databaseLayer;
    private static IRequestLayer requestLayer;
    private static IFlushLayer flushLayer;
    private static volatile boolean initialized;
    private static volatile boolean optedOut;
    public static boolean networkAvailable;
    private static Gson gson;

    private DataSnap() {
        throw new AssertionError("No instances allowed");
    }


    public static void initialize(android.content.Context context) {
        String errorPrefix = "DataSnap client must be initialized with a valid ";
        if (context == null) throw new IllegalArgumentException(errorPrefix + "android context.");
        if (initialized) return;
        DataSnap.statistics = new AnalyticsStatistics();
        dsConfig = DsConfig.getInstance(context);
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
        gson = gsonBuilder.create();

    }

    /**
     *
     * API Calls: trackEvent() for a single vvent or trackEvents() for multiple
     */

    public static void trackEvent(IEvent event) {
        if (dsConfig == null)
            dsConfig = new DsConfig();
        checkInitialized();
        GsonBuilder gsonBuilder = new GsonBuilder();
        gsonBuilder.setFieldNamingPolicy(FieldNamingPolicy.LOWER_CASE_WITH_UNDERSCORES);
        Gson gson = gsonBuilder.create();
        String json = gson.toJson(event);
        if (optedOut) return;
        if (isNullOrEmpty(json)) {
            throw new IllegalArgumentException(
                    "analytics-android #trackEvent must be initialized with a valid event name.");
        }
        EventWrapper eventWrapper = new EventWrapper(json);
        enqueue(eventWrapper);
        statistics.updateTracks(1);
    }


    public static void trackEvents(ArrayList<IEvent> eventArrayList) {
        for (IEvent event : eventArrayList) {
            checkInitialized();
            String json = gson.toJson(event);
            if (optedOut) return;
            if (isNullOrEmpty(json)) {
                throw new IllegalArgumentException(
                        "Event failed to be created- due to null or empty data");
            }
            if (dsConfig == null)
                dsConfig = new DsConfig();
            EventWrapper eventWrapper = new EventWrapper(json);
            enqueue(eventWrapper);
            statistics.updateTracks(1);
        }
    }

    /**
     * Flush data to the server.
     *
     * @param async True to block until the data is flushed
     */
    public static void flush(boolean async) {
        checkInitialized();
        statistics.updateFlushAttempts(1);
        final long start = System.currentTimeMillis();
        final CountDownLatch latch = new CountDownLatch(1);

        flushLayer.flush(new FlushCallback() {
            @Override
            public void onFlushCompleted(boolean success, List<EventWrapper>batch) {
                latch.countDown();
                if (success) {
                    long duration = System.currentTimeMillis() - start;
                    statistics.updateFlushTime(duration);
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
     * Enqueues a an event
     */
    public static void enqueue(final EventWrapper payload) {
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
        if (!initialized) {
            throw new IllegalStateException("Please call DataSnap.initialize before using the library.");
        }
    }

    /**
     *
     * @param optOut true to stop sending any more events.
     */
    public static void optOut(boolean optOut) {
        DataSnap.optedOut = optOut;
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
        apiKey = null;
        initialized = false;
    }


    /**
     * Returns whether the client is initialized
     */
    public static boolean isInitialized() {
        return initialized;
    }

    /**
     * Gets the Datasnap.io client options
     */
    public static DsConfig getDsConfig() {
        if (dsConfig == null)
            checkInitialized();
        return dsConfig;
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
     * Gets the client statistics
     */
    public static AnalyticsStatistics getStatistics() {
        if (statistics == null) checkInitialized();
        return statistics;
    }


}
