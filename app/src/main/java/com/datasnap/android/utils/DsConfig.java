package com.datasnap.android.utils;

import android.content.Context;
import android.content.res.Resources;

import com.datasnap.android.Defaults;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

import static com.datasnap.android.utils.Utils.isNullOrEmpty;


public class DsConfig {

    static final String API_KEY_RESOURCE_IDENTIFIER = "apiKey";
    static final String ORGANIZATION_IDS_RESOURCE_IDENTIFIER = "organizationIds";
    static final String PROJECT_IDS_RESOURCE_IDENTIFIER = "projectIds";
    static final String DATASNAP_SERVER_RESOURCE_IDENTIFIER = "datasnap_server";

    private static DsConfig instance;

    public static DsConfig getInstance(Context context) {
        if (instance == null) {
            instance = new DsConfig(context);
        }

        return instance;
    }

    /**
     * Caches the count of the database without requiring SQL count to be
     * called every time. This will allow us to quickly determine whether
     * our database is full and we shouldn't add anymore
     */
    private AtomicLong count;
    private boolean initialCount;
    private String[] orgIds;
    private String[] projectIds;

    private DsConfig(Context context) {
        apiKey = getResourceString(context, API_KEY_RESOURCE_IDENTIFIER);
        orgIds = getResourceStringArray(context, ORGANIZATION_IDS_RESOURCE_IDENTIFIER);
        projectIds = getResourceStringArray(context, PROJECT_IDS_RESOURCE_IDENTIFIER);
        host = getResourceString(context, DATASNAP_SERVER_RESOURCE_IDENTIFIER);
    }

    //** Get the string resource for the given key. Returns null if not found.

    /**
     * Get the identifier for the resource with a given type and key.
     */
    private static int getIdentifier(Context context, String type, String key) {
        return context.getResources().getIdentifier(key, type, context.getPackageName());
    }

    static String getResourceString(Context context, String key) {
        int id = getIdentifier(context, "string", key);
        if (id != 0) {
            return context.getResources().getString(id);
        } else {
            return null;
        }
    }

    static String[] getResourceStringArray(Context context, String key) {
        int id = getIdentifier(context, "array", key);
        if (id != 0) {
            return context.getResources().getStringArray(id);
        } else {
            return null;
        }
    }


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

    /**
     * Creates a default options
     */
    public DsConfig() {
        this(Defaults.DEBUG, Defaults.HOST, Defaults.FLUSH_AT, Defaults.FLUSH_AFTER,
                Defaults.MAX_QUEUE_SIZE, Defaults.SETTINGS_CACHE_EXPIRY, Defaults.SEND_LOCATION);
    }

    /**
     * Creates an option with the provided settings
     */
    DsConfig(boolean debug, String host, int flushAt, int flushAfter, int maxQueueSize,
             int settingsCacheExpiry, boolean sendLocation) {

        setDebug(debug);
        setHost(host);
        setFlushAt(flushAt);
        setFlushAfter(flushAfter);
        setMaxQueueSize(maxQueueSize);
        setSettingsCacheExpiry(settingsCacheExpiry);
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
     * Sets the amount of time the Segment.io integration settings
     * are cached before being reloaded. This time in milliseconds
     * represents the maximum amount of time your settings for a provider
     * won't reload.
     *
     * @param milliseconds Settings cache time
     */
    public DsConfig setSettingsCacheExpiry(int milliseconds) {

        if (milliseconds < 1000 || milliseconds > 999999999) {
            throw new IllegalArgumentException(
                    "DataSnap Options #settingsCacheExpiry must be between 1000 and 999999999.");
        }

        this.settingsCacheExpiry = milliseconds;
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

    public void setOrgIds(String[] orgIds) {
        this.orgIds = orgIds;
    }

    public void setProjectIds(String[] projectIds) {
        this.projectIds = projectIds;
    }

    public String[] getProjectIds() {
        return projectIds;
    }

    public String[] getOrgIds() {
        return orgIds;
    }
}