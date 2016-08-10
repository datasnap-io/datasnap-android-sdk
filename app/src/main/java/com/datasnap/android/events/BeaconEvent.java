package com.datasnap.android.events;

import com.datasnap.android.eventproperties.Beacon;
import com.datasnap.android.eventproperties.Place;
import com.datasnap.android.eventproperties.User;

import java.util.Map;

public class BeaconEvent extends Event {

  private Place place;
  private Beacon beacon;

  /**
   * @param place
   * @param beacon
   */
  public BeaconEvent(EventType eventType, Place place, Beacon beacon) {
    super(eventType);
    this.place = place;
    this.beacon = beacon;
  }

  public Place getPlace() {
    return place;
  }

  public void setPlace(Place place) {
    this.place = place;
  }

  @Override
  public User getUser() {
    return user;
  }

  @Override
  public void setUser(User user) {
    this.user = user;
  }

  public Beacon getBeacon() {
    return beacon;
  }

  public void setBeacon(Beacon beacon) {
    this.beacon = beacon;
  }


}
