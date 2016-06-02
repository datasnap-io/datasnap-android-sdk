package com.datasnap.android.events;

import com.datasnap.android.eventproperties.DeviceInfo;
import com.datasnap.android.eventproperties.User;

import java.util.Map;

public abstract class Event {

    protected String dataSnapVersion;
    protected String eventType;
    protected String[] organizationIds = new String[1];
    protected String[] projectIds = new String[1];
    protected String customerOrgId;
    protected String customerVenueOrgId;
    protected String venueOrgId;
    protected User user;
    protected DeviceInfo deviceInfo;
    protected Map<String, Object> additionalProperties;

    public boolean validate(){
        return this.organizationIds.length > 0 && this.organizationIds[0].length() > 0
            && this.projectIds.length > 0 && projectIds[0].length() > 0 && this.user != null;
    }

    public void setAdditionalProperties(Map<String, Object> additionalProperties) {
        this.additionalProperties = additionalProperties;
    }

    public Map<String, Object> getAdditionalProperties(){
        return additionalProperties;
    }

    public String getDataSnapVersion() {
        return dataSnapVersion;
    }

    public void setDataSnapVersion(String dataSnapVersion) {
        this.dataSnapVersion = dataSnapVersion;
    }

    public String getEventType() {
        return eventType;
    }

    public void setEventType(String eventType) {
        this.eventType = eventType;
    }

    public String getOrganizationId() {
        if(organizationIds.length == 0)
            return null;
        return organizationIds[0];
    }

    public void setOrganizationId(String organizationId) {
        this.organizationIds[0] = organizationId;
    }

    public String getProjectId() {
        if(projectIds.length == 0)
            return null;
        return projectIds[0];
    }

    public void setProjectId(String projectId) {
        this.projectIds[0] = projectId;
    }

    public String getCustomerOrgId() {
        return customerOrgId;
    }

    public void setCustomerOrgId(String customerOrgId) {
        this.customerOrgId = customerOrgId;
    }

    public String getCustomerVenueOrgId() {
        return customerVenueOrgId;
    }

    public void setCustomerVenueOrgId(String customerVenueOrgId) {
        this.customerVenueOrgId = customerVenueOrgId;
    }

    public String getVenueOrgId() {
        return venueOrgId;
    }

    public void setVenueOrgId(String venueOrgId) {
        this.venueOrgId = venueOrgId;
    }

    public DeviceInfo getDeviceInfo() {
        return deviceInfo;
    }

    public void setDeviceInfo(DeviceInfo deviceInfo) {
        this.deviceInfo = deviceInfo;
    }

    public User getUser() {
        return user;
    }

    public void setUser(User user) {
        this.user = user;
    }

}
