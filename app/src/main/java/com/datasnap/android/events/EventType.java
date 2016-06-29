package com.datasnap.android.events;

import com.google.gson.annotations.SerializedName;

public enum EventType {
  @SerializedName("beacon_arrive")
  BEACON_ARRIVED,
  @SerializedName("beacon_sighting")
  BEACON_SIGHTING,
  @SerializedName("beacon_depart")
  BEACON_DEPART,
  @SerializedName("ds_communication_open")
  COMMUNICATION_OPEN,
  @SerializedName("ds_communication_sent")
  COMMUNICATION_SENT,
  @SerializedName("geofence_depart")
  GEOFENCE_DEPART,
  @SerializedName("global_position_sighting")
  GLOBAL_POSITION_SIGHTING,
  @SerializedName("opt_in_vendor")
  OPT_IN_VENDOR,
  @SerializedName("opt_in_location")
  OPT_IN_LOCATION,
  @SerializedName("opt_in_push_notifications")
  OPT_IN_PUSH_NOTIFICATIONS,
  @SerializedName("app_installed")
  APP_INSTALLED,
  @SerializedName("status_ping")
  STATUS_PING,
  @SerializedName("all_events")
  ALL_EVENTS;
}
