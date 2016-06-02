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
     * @param eventType
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
        this.eventType = eventType;
        this.place = place;
        this.user = user;
        this.beacon = beacon;
        this.setDeviceInfo(deviceInfo);
        this.additionalProperties = additionalProperties;
        this.organizationIds[0] = organizationId;
        this.projectIds[0] = projectId;
        this.customerOrgId = customerOrgId;
        this.customerVenueOrgId = customerVenueOrgId;
        this.venueOrgId = venueOrgId;
    }

    public BeaconEvent(String eventType, String organizationId,
                       String projectId, String customerOrgId, String customerVenueOrgId, String venueOrgId, Beacon beacon,
                       User user, DeviceInfo deviceInfo) {
        this.eventType = eventType;
        this.beacon = beacon;
        this.user = user;
        this.setDeviceInfo(deviceInfo);
        this.organizationIds[0] = organizationId;
        this.projectIds[0] = projectId;
        this.customerOrgId = customerOrgId;
        this.customerVenueOrgId = customerVenueOrgId;
        this.venueOrgId = venueOrgId;
    }


    public BeaconEvent(String eventType, String organizationId,
                       String projectId, String customerOrgId, String customerVenueOrgId, String venueOrgId, Place place, User user, Beacon beacon,
                       DeviceInfo deviceInfo) {
        this.eventType = eventType;
        this.place = place;
        this.user = user;
        this.beacon = beacon;
        this.setDeviceInfo(deviceInfo);
        this.organizationIds[0] = organizationId;
        this.projectIds[0] = projectId;
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
