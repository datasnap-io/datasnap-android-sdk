package com.datasnap.android.services;

import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;

import com.datasnap.android.events.EventType;

/**
 * Created by paolopelagatti on 6/28/16.
 */
public class ServiceManager {

  private static BaseService service;

  public static void initializeService(BaseService targetService){
    service = targetService;
    SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(service);
    if(service == null)
      return;
    if(sharedPreferences.getBoolean(EventType.BEACON_SIGHTING.name(), true)) {
      if(service != null)
        service.addBeaconSightingListener();
    } else {
      service.releaseBeaconSightingListener();
    }
    if(sharedPreferences.getBoolean(EventType.COMMUNICATION_SENT.name(), true)) {
      if(service != null)
        service.addCommunicationSentListener();
    } else {
      service.releaseCommunicationSentListener();
    }
    if(sharedPreferences.getBoolean(EventType.COMMUNICATION_OPEN.name(), true)) {
      if(service != null)
        service.addCommunicationOpenListener();
    } else {
      service.releaseCommunicationOpenListener();
    }
    if(sharedPreferences.getBoolean(EventType.GEOFENCE_DEPART.name(), true)) {
      if(service != null)
        service.addGeofenceDepartListener();
    } else {
      service.releaseGeofenceDepartListener();
    }
  }
  
  public static void setEventEnabled(EventType event, boolean value, Context context) {
    SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context);
    if(event.equals(EventType.BEACON_SIGHTING) || event.equals(EventType.ALL_EVENTS)) {
      if(service == null)
        return;
      if (value) {
        service.addBeaconSightingListener();
        sharedPreferences.edit().putBoolean(EventType.BEACON_SIGHTING.name(), true).commit();
      } else {
        service.releaseBeaconSightingListener();
        sharedPreferences.edit().putBoolean(EventType.BEACON_SIGHTING.name(), false).commit();
      }
    }
    if(event.equals(EventType.COMMUNICATION_SENT) || event.equals(EventType.ALL_EVENTS)) {
      if(service == null)
        return;
      if(value){
        service.addCommunicationSentListener();
        sharedPreferences.edit().putBoolean(EventType.COMMUNICATION_SENT.name(), true).commit();
      } else {
        service.releaseCommunicationSentListener();
        sharedPreferences.edit().putBoolean(EventType.COMMUNICATION_SENT.name(), false).commit();
      }
    }
    if(event.equals(EventType.COMMUNICATION_OPEN) || event.equals(EventType.ALL_EVENTS)) {
      if(service == null)
        return;
      if(value){
        service.addCommunicationOpenListener();
        sharedPreferences.edit().putBoolean(EventType.COMMUNICATION_OPEN.name(), true).commit();
      } else {
        service.releaseCommunicationOpenListener();
        sharedPreferences.edit().putBoolean(EventType.COMMUNICATION_OPEN.name(), false).commit();
      }
    }
    if(event.equals(EventType.GEOFENCE_DEPART) || event.equals(EventType.ALL_EVENTS)) {
      if(service == null)
        return;
      if(value){
        service.addGeofenceDepartListener();
        sharedPreferences.edit().putBoolean(EventType.GEOFENCE_DEPART.name(), true).commit();
      } else {
        service.releaseGeofenceDepartListener();
        sharedPreferences.edit().putBoolean(EventType.GEOFENCE_DEPART.name(), false).commit();
      }
    }
  }
}
