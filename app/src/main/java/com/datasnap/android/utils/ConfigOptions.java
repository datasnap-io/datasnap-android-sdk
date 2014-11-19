package com.datasnap.android.utils;

import com.datasnap.android.Defaults;

import static com.datasnap.android.utils.Utils.isNullOrEmpty;

// bf: put in 'DEFAULTS' data here
/**
 * Client configuration
 */
public class ConfigOptions {

  /**
   * Whether or not debug logging is enabled to ADT logcat
   */
  private boolean debug;

  /**
   * The REST API endpoint (with scheme)
   */
  private String host;

  /**
   * Flush after these many messages are added to the queue
   */
  private int flushAt;

  /**
   * Flush after this many milliseconds have passed without a flush
   */
  private int flushAfter;

  /**
   * Stop accepting messages after the queue reaches this capacity
   */
  private int maxQueueSize;

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
  public ConfigOptions() {
    this(Defaults.DEBUG, Defaults.HOST, Defaults.FLUSH_AT, Defaults.FLUSH_AFTER,
            Defaults.MAX_QUEUE_SIZE, Defaults.SETTINGS_CACHE_EXPIRY, Defaults.SEND_LOCATION);
  }

  /**
   * Creates an option with the provided settings
   */
  ConfigOptions(boolean debug, String host, int flushAt, int flushAfter, int maxQueueSize,
                int settingsCacheExpiry, boolean sendLocation) {

    setDebug(debug);
    setHost(host);
    setFlushAt(flushAt);
    setFlushAfter(flushAfter);
    setMaxQueueSize(maxQueueSize);
    setSettingsCacheExpiry(settingsCacheExpiry);
    setSendLocation(sendLocation);
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
  public ConfigOptions setFlushAt(int flushAt) {

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
  public ConfigOptions setFlushAfter(int flushAfter) {

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
  public ConfigOptions setMaxQueueSize(int maxQueueSize) {

    if (flushAfter <= 0) {
      throw new IllegalArgumentException("DataSnap Options #flushAfter must be greater than 0.");
    }

    this.maxQueueSize = maxQueueSize;
    return this;
  }

  /**
   * Sets the REST API endpoint
   */
  public ConfigOptions setHost(String host) {

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
  public ConfigOptions setSettingsCacheExpiry(int milliseconds) {

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
  public ConfigOptions setDebug(boolean debug) {
    this.debug = debug;
    return this;
  }

  /**
   * Sets whether the library sends the location attributes.
   *
   * @param sendLocation True to send location information
   */
  public ConfigOptions setSendLocation(boolean sendLocation) {
    this.sendLocation = sendLocation;
    return this;
  }
}