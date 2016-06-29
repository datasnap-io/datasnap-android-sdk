package com.datasnap.android.eventproperties;

public class Geofence {

  private String identifier;
  private String name;
  private String visibility;
  private Tags tags;
  private GeofenceCircle geofenceCircle;

  public String getIdentifier() {
    return identifier;
  }

  public void setIdentifier(String identifier) {
    this.identifier = identifier;
  }

  public String getName() {
    return name;
  }

  public void setName(String name) {
    this.name = name;
  }

  public String getVisibility() {
    return visibility;
  }

  public void setVisibility(String visibility) {
    this.visibility = visibility;
  }

  public Tags getPropTags() {
    return tags;
  }

  public void setPropTags(Tags tags) {
    this.tags = tags;
  }

  public GeofenceCircle getGeofenceCircle() {
    return geofenceCircle;
  }

  public void setGeofenceCircle(GeofenceCircle geofenceCircle) {
    this.geofenceCircle = geofenceCircle;
  }

}
