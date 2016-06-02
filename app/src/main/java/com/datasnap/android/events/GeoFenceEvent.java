package com.datasnap.android.events;

import com.datasnap.android.eventproperties.Place;
import com.datasnap.android.eventproperties.User;
import com.datasnap.android.eventproperties.Geofence;

import java.util.Map;

public class GeoFenceEvent extends Event {

    private Place place;
    private Geofence geofence;

    public GeoFenceEvent(String eventType, String organizationId,
                         String projectId, String customerOrgId, String customerVenueOrgId, String venueOrgId, Place place, Geofence geofence, User user, Map<String, Object> additionalProperties) {
        super();
        this.eventType = eventType;
        this.organizationIds[0] = organizationId;
        this.projectIds[0] = projectId;
        this.place = place;
        this.geofence = geofence;
        this.user = user;
        this.additionalProperties = additionalProperties;
        this.customerOrgId = customerOrgId;
        this.customerVenueOrgId = customerVenueOrgId;
        this.venueOrgId = venueOrgId;

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
