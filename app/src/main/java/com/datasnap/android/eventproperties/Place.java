package com.datasnap.android.eventproperties;

import java.util.ArrayList;

public class Place {
  private String id;
  private String name;
  private Tags tags;
  private Address address;
  private String lastPlace;
  private ArrayList<String> geofences;
  private ArrayList<String> beacons;

  public String getId() {
    return id;
  }

  public void setId(String id) {
    this.id = id;
  }

  public String getName() {
    return name;
  }

  public void setName(String name) {
    this.name = name;
  }

  public Tags getTags() {
    return tags;
  }

  public void setTags(Tags tags) {
    this.tags = tags;
  }

  public Address getAddress() {
    return address;
  }

  public void setAddress(Address address) {
    this.address = address;
  }

  public String getLastPlace() {
    return lastPlace;
  }

  public void setLastPlace(String lastPlace) {
    this.lastPlace = lastPlace;
  }

  public ArrayList<String> getGeofences() {
    return geofences;
  }

  public void setGeofences(ArrayList<String> geofences) {
    this.geofences = geofences;
  }

  public ArrayList<String> getBeacons() {
    return beacons;
  }

  public void setBeacons(ArrayList<String> beacons) {
    this.beacons = beacons;
  }

}
