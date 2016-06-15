package com.datasnap.android.events;

import com.datasnap.android.eventproperties.Beacon;
import com.datasnap.android.eventproperties.DeviceInfo;
import com.datasnap.android.eventproperties.Place;
import com.datasnap.android.eventproperties.User;

import java.util.Map;

public class BeaconEvent extends Event {

    private Place place;
    private Beacon beacon;

    /**
     * @param organizationId
     * @param projectId
     * @param customerOrgId
     * @param customerVenueOrgId
     * @param customerVenueOrgId
     * @param place
     * @param user
     * @param beacon
     * @param deviceInfo
     * @param additionalProperties
     */
    public BeaconEvent(String eventType, String organizationId,
                       String projectId, String customerOrgId, String customerVenueOrgId, String venueOrgId, Place place, User user, Beacon beacon,
                       DeviceInfo deviceInfo, Map<String, Object> additionalProperties) {
        super(eventType, organizationId, projectId, customerOrgId, customerVenueOrgId, venueOrgId, user, deviceInfo, additionalProperties);
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
