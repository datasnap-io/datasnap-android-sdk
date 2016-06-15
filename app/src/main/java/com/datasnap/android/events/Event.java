package com.datasnap.android.events;

import com.datasnap.android.BuildConfig;
import com.datasnap.android.eventproperties.DeviceInfo;
import com.datasnap.android.eventproperties.User;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
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
    protected Datasnap datasnap;

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

    public Event(String eventType, String organizationId,
                 String projectId, String customerOrgId, String customerVenueOrgId, String venueOrgId, User user,
                 DeviceInfo deviceInfo, Map<String, Object> additionalProperties){
        this.eventType = eventType;
        this.user = user;
        this.setDeviceInfo(deviceInfo);
        this.additionalProperties = additionalProperties;
        this.organizationIds[0] = organizationId;
        this.projectIds[0] = projectId;
        this.customerOrgId = customerOrgId;
        this.customerVenueOrgId = customerVenueOrgId;
        this.venueOrgId = venueOrgId;
        this.datasnap = new Datasnap(BuildConfig.VERSION_NAME);
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

    public class Datasnap {
        String created;
        String version;

        public Datasnap(String version){
            this.created = getTime();
            this.version = version;
        }

        public String getCreated() {
            return created;
        }

        public void setCreated(String created) {
            this.created = created;
        }

        public String getVersion() {
            return version;
        }

        public void setVersion(String version) {
            this.version = version;
        }

        private String getTime() {
            Calendar c = Calendar.getInstance();
            Date d = c.getTime();
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd hh:mm:ss ZZ");
            return sdf.format(d);
        }
    }

}
