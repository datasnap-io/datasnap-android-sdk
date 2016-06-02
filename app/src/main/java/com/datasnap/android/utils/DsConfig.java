package com.datasnap.android.utils;

import android.content.Context;
import android.util.Base64;
import com.datasnap.android.Defaults;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

import static com.datasnap.android.utils.Utils.isNullOrEmpty;


public class DsConfig {

    static final String DATASNAP_STAGING_SERVER_URL = "https://api-events-staging.datasnap.io/v1.0/events";
    static final String DATASNAP_API_SERVER_URL = "https://api-events.datasnap.io/v1.0/events";
    static final String ORGANIZATION_ENDPOINT = "https://entities-loadtest.datasnap.io/v1.0/organization";
    static final String STAGING_ORGANIZATION_ENDPOINT = "TODO";
    static final boolean LOGGING = true;
    private static DsConfig instance;
    private String orgId;
    private String projectId;
    private String venue;

    /**
     * Caches the count of the database without requiring SQL count to be
     * called every time. This will allow us to quickly determine whether
     * our database is full and we shouldn't add anymore
     */
    private AtomicLong count;
    private boolean initialCount;

    // cache the settings for 1 hour before reloading
    public static final int SETTINGS_CACHE_EXPIRY = 1000 * 60 * 60;

    /**
     * Whether or not debug logging is enabled to ADT logcat
     */
    private boolean debug;

    /**
     * The REST API endpoint (with scheme)
     */
    private String host;

    /**
     * The Organization API endpoint (with scheme)
     */
    private String organizationHost;

    /**
     * The API Key
     */
    private String apiKey;


    /**
     * Flush after these many messages are added to the queue
     */
    private int flushAt = 20;

    /**
     * Flush after this many milliseconds have passed without a flush
     */
    private int flushAfter= (int) TimeUnit.SECONDS.toMillis(10);;

    /**
     * Stop accepting messages after the queue reaches this capacity
     */
    private int maxQueueSize= 10000;

    /**
     * Reload the provider settings from the Segment.io after this
     * amount of time
     */
    private int settingsCacheExpiry;

    /**
     * Send the location in the options object.
     */
    private boolean sendLocation;

    public static void initialize(String apiKey){
        instance = new DsConfig(apiKey);
    }

    public static DsConfig getInstance() {
        return instance;
    }

    private DsConfig(String apiKey) {
        String encodedBytes = Base64.encodeToString(apiKey.getBytes(), Base64.URL_SAFE|Base64.NO_WRAP);
        this.apiKey = encodedBytes;
        host = DATASNAP_STAGING_SERVER_URL;
        organizationHost = ORGANIZATION_ENDPOINT;
        debug = LOGGING;
    }

    /**
     * Get the identifier for the resource with a given type and key.
     */
    private static int getIdentifier(Context context, String type, String key) {
        return context.getResources().getIdentifier(key, type, context.getPackageName());
    }

    /**
     * Creates a default options
     */
    public DsConfig() {
        this(Defaults.DEBUG, Defaults.HOST, Defaults.FLUSH_AT, Defaults.FLUSH_AFTER,
                Defaults.MAX_QUEUE_SIZE,  Defaults.SEND_LOCATION);
    }

    /**
     * Creates an option with the provided settings
     */
    DsConfig(boolean debug, String host, int flushAt, int flushAfter, int maxQueueSize,boolean sendLocation) {

        setDebug(debug);
        setHost(host);
        setFlushAt(flushAt);
        setFlushAfter(flushAfter);
        setMaxQueueSize(maxQueueSize);
        setSendLocation(sendLocation);
    }

    public String getApiKey() {
        return apiKey;
    }

    public void setApiKey(String apiKey) {
        this.apiKey = apiKey;
    }


    public boolean isDebug() {
        return debug;
    }

    public int getFlushAt() {
        return flushAt;
    }

    public int getFlushAfter() {
        return flushAfter;
    }

    public String getHost() {
        return host;
    }

    public String getOrganizationsHost() {
        return organizationHost;
    }

    public int getMaxQueueSize() {
        return maxQueueSize;
    }

    public int getSettingsCacheExpiry() {
        return settingsCacheExpiry;
    }

    public boolean shouldSendLocation() {
        return sendLocation;
    }

    /**
     * Sets the amount of messages that need to be in the queue before it is
     * flushed
     */
    public DsConfig setFlushAt(int flushAt) {

        if (flushAt <= 0) {
            throw new IllegalArgumentException("DataSnap Options #flushAt must be greater than 0.");
        }

        this.flushAt = flushAt;
        return this;
    }

    /**
     * Sets the maximum amount of time to queue before invoking a flush (in
     * milliseconds)
     */
    public DsConfig setFlushAfter(int flushAfter) {

        if (flushAfter <= 50) {
            throw new IllegalArgumentException("DataSnap Options #flushAfter must be greater than 50.");
        }

        this.flushAfter = flushAfter;
        return this;
    }

    /**
     * Sets the maximum queue capacity, which is an emergency pressure relief
     * valve. If we're unable to flush messages fast enough, the queue will stop
     * accepting messages after this capacity is reached.
     */
    public DsConfig setMaxQueueSize(int maxQueueSize) {

        if (flushAfter <= 0) {
            throw new IllegalArgumentException("DataSnap Options #flushAfter must be greater than 0.");
        }

        this.maxQueueSize = maxQueueSize;
        return this;
    }

    /**
     * Sets the REST API endpoint
     */
    public DsConfig setHost(String host) {

        if (isNullOrEmpty(host)) {
            throw new IllegalArgumentException("DataSnap Options #host must be non-null or empty.");
        }

        this.host = host;
        return this;
    }


    /**
     * Sets whether debug logging to LogCat is enabled
     *
     * @param debug True to enable debug logging
     */
    public DsConfig setDebug(boolean debug) {
        this.debug = debug;
        return this;
    }

    /**
     * Sets whether the library sends the location attributes.
     *
     * @param sendLocation True to send location information
     */
    public DsConfig setSendLocation(boolean sendLocation) {
        this.sendLocation = sendLocation;
        return this;
    }

    public void setOrgId(String orgId) {
        this.orgId = orgId;
    }

    public void setProjectId(String projectId) {
        this.projectId = projectId;
    }

    public String getProjectId() {
        return projectId;
    }

    public String getOrgId() {
        return orgId;
    }

    public String getVenue() {
        return venue;
    }

    public void setVenue(String venue) {
        this.venue = venue;
    }
}