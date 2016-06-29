package com.datasnap.android.events;

import com.datasnap.android.eventproperties.Device;
import com.datasnap.android.eventproperties.Place;
import com.datasnap.android.eventproperties.User;
import com.datasnap.android.eventproperties.Geofence;

import java.util.Map;

public class GeoFenceEvent extends Event {

  private Place place;
  private Geofence geofence;

  public GeoFenceEvent(EventType eventType, Place place, Geofence geofence) {
    super(eventType);
    this.place = place;
    this.geofence = geofence;
  }

  public Place getPlace() {
    return place;
  }

  public void setPlace(Place place) {
    this.place = place;
  }

  public Geofence getGeofence() {
    return geofence;
  }

  public void setGeofence(Geofence geofence) {
    this.geofence = geofence;
  }

}
