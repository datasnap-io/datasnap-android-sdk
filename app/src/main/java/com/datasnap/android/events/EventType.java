package com.datasnap.android.events;

public class EventType {
  public static String BEACON_ARRIVED = "beacon_arrive";
  public static String BEACON_SIGHTING = "beacon_sighting";
  public static String BEACON_DEPART = "beacon_depart";
  public static String COMMUNICATION_OPEN = "ds_communication_open";
  public static String COMMUNICATION_SENT = "ds_communication_sent";
  public static String GEOFENCE_DEPART = "geofence_depart";
  public static String GLOBAL_POSITION_SIGHTING = "global_position_sighting";
  public static String OPT_IN_VENDOR = "opt_in_vendor";
  public static String OPT_IN_LOCATION = "opt_in_location";
  public static String OPT_IN_PUSH_NOTIFICATIONS = "opt_in_push_notifications";
  public static String APP_INSTALLED = "app_installed";
  public static String STATUS_PING = "status_ping";
  public static String ALL_EVENTS = "all_events";
}
