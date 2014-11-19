package com.datasnap.android;

import android.app.Activity;

import com.datasnap.android.controller.EventDatabase;
import com.datasnap.android.controller.EventDatabaseLayerInterface;
import com.datasnap.android.controller.EventDatabaseThread;
import com.datasnap.android.models.EventWrapper;
import com.datasnap.android.models.EventWrapperSuper;
//import com.datasnap.android.models.SuperOfEventWrapperSuper;
import com.datasnap.android.utils.ConfigOptions;
import com.datasnap.android.utils.Logger;
import com.datasnap.android.utils.ResourceConfig;
import com.google.gson.Gson;
import com.datasnap.android.utils.AnonymousIdCache;
import com.datasnap.android.utils.SimpleStringCache;
import com.datasnap.android.controller.EventDatabaseLayerInterface.EnqueueCallback;
import com.datasnap.android.events.IEvent;
import com.datasnap.android.controller.FlushThread;
import com.datasnap.android.controller.FlushThread.BatchFactory;
import com.datasnap.android.controller.IFlushLayer;
import com.datasnap.android.controller.IFlushLayer.FlushCallback;
import com.datasnap.android.models.EventListContainer;
import com.datasnap.android.models.Context;
import com.datasnap.android.models.Options;
import com.datasnap.android.models.Props;
import com.datasnap.android.controller.BasicRequester;
import com.datasnap.android.controller.IRequestLayer;
import com.datasnap.android.controller.IRequester;
import com.datasnap.android.controller.RequestThread;
import com.datasnap.android.stats.AnalyticsStatistics;
import com.datasnap.android.utils.HandlerTimer;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;

import static com.datasnap.android.utils.Utils.isNullOrEmpty;

public final class DataSnap {
  private static AnalyticsStatistics statistics;

  private static Context globalContext;
 // private static SuperOfEventWrapperSuper bundledIntegrations;
  private static String apiKey;
  private static ConfigOptions options;
  private static HandlerTimer flushTimer;
  private static EventDatabase database;
  private static EventDatabaseLayerInterface databaseLayer;
  private static IRequestLayer requestLayer;
  private static IFlushLayer flushLayer;

  private static volatile boolean initialized;
  private static volatile boolean optedOut;

  private static SimpleStringCache anonymousIdCache;
  private static SimpleStringCache userIdCache;
  private static SimpleStringCache groupIdCache;
  //private static SettingsCache settingsCache;

  private DataSnap() {
    throw new AssertionError("No instances allowed");
  }

  public static void onCreate(android.content.Context context) {
    DataSnap.initialize(context);
  }

  public static void onCreate(android.content.Context context, String apiKey) {
    DataSnap.initialize(context, apiKey);
  }

    // pass in config dynamically
  public static void onCreate(android.content.Context context, String apiKey, ConfigOptions options) {
    DataSnap.initialize(context, apiKey, options);
  }

  public static void activityStart(Activity activity) {
    DataSnap.initialize(activity);
  }

  public static void activityStart(Activity activity, String apiKey) {
    DataSnap.initialize(activity, apiKey);
  }

  public static void activityStart(Activity activity, String apiKey, ConfigOptions options) {
    DataSnap.initialize(activity, apiKey, options);
    if (optedOut) return;
  }

  /**
   * Called when the activity has been resumed
   *
   * @param activity Your Android Activity
   */
  public static void activityResume(Activity activity) {
    DataSnap.initialize(activity);
    if (optedOut) return;
  }

  /**
   * Called when the activity has been stopped
   *
   * @param activity Your Android Activity
   */
  public static void activityStop(Activity activity) {
    DataSnap.initialize(activity);
    if (optedOut) return;
  }

  /**
   * Called when the activity has been paused
   *
   * @param activity Your Android Activity
   */
  public static void activityPause(Activity activity) {
    DataSnap.initialize(activity);
    if (optedOut) return;
  }


    // main handler....

  public static void initialize(android.content.Context context) {
    if (initialized) return;
    String apiKey = ResourceConfig.getApiKey(context);
    ConfigOptions options = ResourceConfig.getOptions(context);
    initialize(context, apiKey, options);
  }

  public static void initialize(android.content.Context context, String apiKey) {
    if (initialized) return;
    // read options from analytics.xml
    ConfigOptions options = ResourceConfig.getOptions(context);
    initialize(context, apiKey, options);
  }


    // initialize statistics...
    // initialize logging...
    // initialize database..
    // initialize database thread..
    // initialize requester
    // initialize requester thread..
    // initialize and start threads...
    // caches too...
  public static void initialize(android.content.Context context, String apiKey, ConfigOptions options) {
    String errorPrefix = "DataSnap client must be initialized with a valid ";
    if (context == null) throw new IllegalArgumentException(errorPrefix + "android context.");
   if (isNullOrEmpty(apiKey)) {
     throw new IllegalArgumentException(errorPrefix + "apiKey.");
    }
    if (options == null) throw new IllegalArgumentException(errorPrefix + "options.");
    if (initialized) return;

    DataSnap.statistics = new AnalyticsStatistics();
    DataSnap.apiKey = apiKey;
    DataSnap.options = options;
    // set logging based on the debug mode
    Logger.setLog(options.isDebug());

    // create the database using the activity context
    database = EventDatabase.getInstance(context);
    // knows how to create global context about this android device
  //  infoManager = new InfoManager(options);

    anonymousIdCache = new AnonymousIdCache(context);
    groupIdCache = new SimpleStringCache(context, Defaults.SharedPreferences.GROUP_ID_KEY);
    userIdCache = new SimpleStringCache(context, Defaults.SharedPreferences.USER_ID_KEY);

    // now we need to create our singleton thread-safe database thread
    DataSnap.databaseLayer = new EventDatabaseThread(database);
    DataSnap.databaseLayer.start();

    IRequester requester = new BasicRequester();

    // and a single request thread
    DataSnap.requestLayer = new RequestThread(requester);
    DataSnap.requestLayer.start();

    // start the flush thread
    DataSnap.flushLayer =
        new FlushThread(DataSnap.databaseLayer, batchFactory, DataSnap.requestLayer);

    DataSnap.flushTimer = new HandlerTimer(options.getFlushAfter(), flushClock);

 //   DataSnap.refreshSettingsTimer =
  //      new HandlerTimer(options.getSettingsCacheExpiry() + 1000, refreshSettingsClock);

   // DataSnap.settingsLayer = new SettingsThread(requester);

   // settingsCache = new SettingsCache(context, settingsLayer, options.getSettingsCacheExpiry());
    // globalContext = generateContext(context);

    initialized = true;

    // start the other threads
    DataSnap.flushTimer.start();
  //  DataSnap.refreshSettingsTimer.start();
    DataSnap.flushLayer.start();
   // DataSnap.settingsLayer.start();

    // tell the server to look for settings right now
  //  DataSnap.refreshSettingsTimer.scheduleNow();
  }

/*  private static Context generateContext(android.content.Context context) {
    return new Context(infoManager.build(context));
  }*/

  /**
   * Factory that creates batches from action payloads.
   *
   * Inserts system information into global batches
   */
  private static BatchFactory batchFactory = new BatchFactory() {

    @Override
    public EventListContainer create(List<EventWrapper> payloads) {
      return new EventListContainer(apiKey, payloads);
    }
  };

  /**
   * Flushes on a clock timer
   */
  private static Runnable flushClock = new Runnable() {
    @Override
    public void run() {
      DataSnap.flush(true);
    }
  };

  //
  // API Calls
  //

  public static void trackEvent(IEvent event) {
      trackEvent(event, null, null);
  }

  public static void trackEvent(IEvent event, Options options) {
      trackEvent(event, null, options);
  }

  public static void trackEvent(IEvent event, Props properties) {
      trackEvent(event, properties, null);
  }

  /**
   *
   *
   * EventWrapper will use an automatically generated userId unless one has been
   * provided by identify(..).
   *
   * @param event describes what this user just did. It's a human readable
   * description like "Played a Song", "Printed a Report" or
   * "Updated Status".
   * @param properties a dictionary with items that describe the event in more
   * detail. This argument is optional, but highly
   * recommended. You'll find these properties extremely useful
   * later.
   * @param options options allows you to set a timestamp, an anonymousId, a context,
   * and select specific integrations.
   */
  public static void trackEvent(IEvent event, Props properties, Options options) {
    checkInitialized();

      Gson gson = new Gson(); // Or use new GsonBuilder().create();
      String json = gson.toJson(event); // serializes target to Json
        if (optedOut) return;

    String eventUserId = getOrSetEventUserId(null);

    if (isNullOrEmpty(eventUserId)) {
      throw new IllegalArgumentException(
          "Datasnap #trackEvent must be initialized with a valid event user id.");
    }

    if (isNullOrEmpty(json)) {
      throw new IllegalArgumentException(
          "analytics-android #trackEvent must be initialized with a valid event name.");
    }

    if (properties == null) properties = new Props();
    if (options == null) options = new Options();

    EventWrapper eventWrapper = new EventWrapper(eventUserId, json, properties, options);
    enqueue(eventWrapper);
  //  integrationManager.trackEvent(trackEvent);
    statistics.updateTracks(1);
  }



    public static void trackEvents(ArrayList<IEvent> eventArrayList, Props properties, Options options) {

        for (IEvent event : eventArrayList) {
            checkInitialized();
            Gson gson = new Gson(); // Or use new GsonBuilder().create();
            String json = gson.toJson(event); // serializes target to Json
            if (optedOut) return;

            String eventUserId = getOrSetEventUserId(null);

            if (isNullOrEmpty(eventUserId)) {
                throw new IllegalArgumentException(
                        "Datasnap #trackEvent must be initialized with a valid event user id.");
            }

            if (isNullOrEmpty(json)) {
                throw new IllegalArgumentException(
                        "analytics-android #trackEvent must be initialized with a valid event name.");
            }

            if (properties == null)
                properties = new Props();
            if (options == null)
                options = new Options();

            EventWrapper eventWrapper = new EventWrapper(eventUserId, json, properties,  options);
            enqueue(eventWrapper);
            statistics.updateTracks(1);
        }
    }

    //
  // Internal
  //

  /**
   * Gets or sets the current userId. If the provided userId
   * is null, then we'll send the sessionId. If the userId
   * is not null, then it will be set in the userId cache and will
   * be returned.
   */
  private static String getOrSetEventUserId(String userId) {
    if (isNullOrEmpty(userId)) {
      // no user id provided, lets try to see if we have it saved
      userId = userIdCache.get();
      if (isNullOrEmpty(userId)) {
        // we have no user Id, let's use the sessionId
        userId = anonymousIdCache.get();
      }
    } else {
      // we were passed a user Id so let's save it
      userIdCache.set(userId);
    }

    return userId;
  }


  /**
   * Enqueues a an event
   * {@link com.datasnap.android.models.EventWrapper}, alias,
   * or any action of type {@link com.datasnap.android.models.EventWrapperSuper}
   */
  public static void enqueue(final EventWrapper payload) {
    statistics.updateInsertAttempts(1);
    // merge the global context into this payload's context
  //  payload.getContext().merge(globalContext);
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
        if (rowCount >= options.getFlushAt()) {
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

  //
  // Opt out
  //

  /**
   * Turns on opt out, opting out of any analytics sent from this point forward.
   */
  public static void optOut() {
    optOut(true);
  }

  /**
   * Toggle opt out
   *
   * @param optOut true to stop sending any more analytics.
   */
  public static void optOut(boolean optOut) {
    boolean toggled = DataSnap.optedOut != optOut;
    DataSnap.optedOut = optOut;
  //  if (toggled) integrationManager.toggleOptOut(optOut);
  }

  //
  // Actions
  //

  /**
   * Triggers a flush of data to the server.
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
      public void onFlushCompleted(boolean success, EventListContainer eventListContainer) {
        latch.countDown();
        if (success) {
          long duration = System.currentTimeMillis() - start;
          statistics.updateFlushTime(duration);
        }
      }
    });

    // flush all the integrations as well
   // integrationManager.flush();

    if (!async) {
      try {
        latch.await();
      } catch (InterruptedException e) {
        Logger.e("Interrupted while waiting for a blocking flush.");
      }
    }
  }

  /**
   * Resets the cached userId. Should be used when the user logs out.
   */
  public static void reset() {
    if (initialized) {
      userIdCache.reset();
      groupIdCache.reset();

      // reset all the integrations
    }
  }
  /**
   * Stops the analytics client threads, and resets the client
   */
  public static void close() {
    checkInitialized();
    // stops the looper on the timer, flush, request, and database thread
    flushTimer.quit();
    flushLayer.quit();
    databaseLayer.quit();
    requestLayer.quit();
    // closes the database
    database.close();
    options = null;
    apiKey = null;
    initialized = false;
  }

  //
  // Getters and Setters
  //

  /**
   * Gets the unique anonymous ID generated for this user
   * until a userId is provided.
   *
   * Use this ID as the "from" ID to alias your new
   * userId if the user was just created.
   */
  public static String getAnonymousId() {
    checkInitialized();
    return anonymousIdCache.get();
  }

  /**
   * Allows you to set your own anonymousId. Used mostly for testing.
   */
  public static void setAnonymousId(String anonymousId) {
    checkInitialized();
    anonymousIdCache.set(anonymousId);
  }

  /**
   * Gets the userId thats currently saved for this
   * application. If none has been entered yet,
   * this will return null.
   */
  public static String getUserId() {
    checkInitialized();
    return userIdCache.get();
  }

  /**
   * Returns whether the client is initialized
   */
  public static boolean isInitialized() {
    return initialized;
  }

  /**
   * Gets the current Datasnap.io API apiKey
   */
  public static String getApiKey() {
    if (apiKey == null) checkInitialized();
    return apiKey;
  }

  public static void setApiKey(String apiKey) {
    DataSnap.apiKey = apiKey;
  }



  /**
   * Gets the Datasnap.io client options
   */
  public static ConfigOptions getOptions() {
    if (options == null) checkInitialized();
    return options;
  }

  /**
   * Gets the client statistics
   */
  public static AnalyticsStatistics getStatistics() {
    if (statistics == null) checkInitialized();
    return statistics;
  }

  /**
   * Allow custom {link {@link com.datasnap.android.controller.IRequester} for counting requests,
   * bandwidth, or testing.
   */
  public static void setRequester(IRequester requester) {
    if (requestLayer instanceof RequestThread) {
      ((RequestThread) requestLayer).setRequester(requester);
    }
  }


}
