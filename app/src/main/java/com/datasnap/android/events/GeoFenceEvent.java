package com.datasnap.android.events;

import com.datasnap.android.eventproperties.DeviceInfo;
import com.datasnap.android.eventproperties.Place;
import com.datasnap.android.eventproperties.User;
import com.datasnap.android.eventproperties.Geofence;

import java.util.Map;

public class GeoFenceEvent extends Event {

    private Place place;
    private Geofence geofence;

    public GeoFenceEvent(String eventType, String organizationId,
                         String projectId, String customerOrgId, String customerVenueOrgId, String venueOrgId, Place place, Geofence geofence, User user, Map<String, Object> additionalProperties, DeviceInfo deviceInfo) {
        super(eventType, organizationId, projectId, customerOrgId, customerVenueOrgId, venueOrgId, user, deviceInfo, additionalProperties);
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
