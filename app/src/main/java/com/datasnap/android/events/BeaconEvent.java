package com.datasnap.android.events;

import com.datasnap.android.eventproperties.Beacon;
import com.datasnap.android.eventproperties.DeviceInfo;
import com.datasnap.android.eventproperties.Place;
import com.datasnap.android.eventproperties.User;

import java.util.Map;

public class BeaconEvent implements IEvent {

    private String eventType;
    private String[] organizationIds;
    private String[] projectIds;
    private Place place;
    private User user;
    private Beacon beacon;
    private DeviceInfo deviceInfo; // for now

    /**
     * @param eventType
     * @param organizationIds
     * @param projectIds
     * @param place
     * @param user
     * @param beacon
     * @param deviceInfo
     * @param additionalProperties
     */
    public BeaconEvent(String eventType, String[] organizationIds,
                       String[] projectIds, Place place, User user, Beacon beacon,
                       DeviceInfo deviceInfo, Map<String, Object> additionalProperties) {
        this.eventType = eventType;
        this.place = place;
        this.user = user;
        this.beacon = beacon;
        this.setDeviceInfo(deviceInfo);
        this.additionalProperties = additionalProperties;
        this.organizationIds = organizationIds;
        this.projectIds = projectIds;
    }

    public BeaconEvent(String eventType, String[] organizationIds,
                       String[] projectIds, Beacon beacon,
                       User user, DeviceInfo deviceInfo) {
        this.eventType = eventType;
        this.beacon = beacon;
        this.user = user;
        this.setDeviceInfo(deviceInfo);
        this.organizationIds = organizationIds;
        this.projectIds = projectIds;
    }


    public BeaconEvent(String eventType, String[] organizationIds,
                       String[] projectIds, Place place, User user, Beacon beacon,
                       DeviceInfo deviceInfo) {
        this.eventType = eventType;
        this.place = place;
        this.user = user;
        this.beacon = beacon;
        this.setDeviceInfo(deviceInfo);
        this.organizationIds = organizationIds;
        this.projectIds = projectIds;
    }

    //@JsonIgnore
    private Map<String, Object> additionalProperties;

    /* (non-Javadoc)
     * @see com.github.datasnap.events.IEvent#getAdditionalProperties()
     */
    public Map<String, Object> getAdditionalProperties() {
        return this.additionalProperties;
    }

    /**
     * @param additionalProperties
     */
    public void setAdditionalProperties(Map<String, Object> additionalProperties) {
        this.additionalProperties = additionalProperties;
    }

    /**
     * @return
     */
    public String getEvent_type() {
        return eventType;
    }

    /**
     * @param eventType
     */
    public void setEvent_type(String eventType) {
        this.eventType = eventType;
    }

    /**
     * @return
     */
    public String[] getOrganizationIds() {
        return organizationIds;
    }

    /**
     * @param organizationIds
     */
    public void setOrganizationIds(String[] organizationIds) {
        this.organizationIds = organizationIds;
    }

    /**
     * @return
     */
    public String[] getProjectIds() {
        return projectIds;
    }

    /**
     * @param projectIds
     */
    public void setProjectIds(String[] projectIds) {
        this.projectIds = projectIds;
    }

    /**
     * @return
     */
    public Beacon getBeacon() {
        return beacon;
    }

    /**
     * @param beacon
     */
    public void setBeacon(Beacon beacon) {
        this.beacon = beacon;
    }

    /**
     * @return
     */
    public Place getPlace() {
        return place;
    }

    /**
     * @param place
     */
    public void setPlace(Place place) {
        this.place = place;
    }


    /**
     * @return
     */
    public User getUser() {
        return user;
    }

    /**
     * @param user
     */
    public void setUser(User user) {
        this.user = user;
    }

    public DeviceInfo getDeviceInfo() {
        return deviceInfo;
    }

    public void setDeviceInfo(DeviceInfo deviceInfo) {
        this.deviceInfo = deviceInfo;
    }

}
