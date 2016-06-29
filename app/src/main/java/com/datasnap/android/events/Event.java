package com.datasnap.android.events;

import com.datasnap.android.BuildConfig;
import com.datasnap.android.eventproperties.Device;
import com.datasnap.android.eventproperties.User;
import com.datasnap.android.utils.DsConfig;
import com.google.gson.annotations.SerializedName;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Map;

//TODO documentation
public abstract class Event {

    protected EventType eventType;
    protected String[] organizationIds = new String[1];
    protected String[] projectIds = new String[1];
    protected String customerOrgId;
    protected String customerVenueOrgId;
    protected String venueOrgId;
    protected User user;
    protected Map<String, Object> additionalProperties;
    protected Datasnap datasnap;

    public boolean validate(){
        return this.organizationIds.length > 0 && this.organizationIds[0].length() > 0
            && this.projectIds.length > 0 && projectIds[0].length() > 0 && this.user != null;
    }

    //TODO documentation
    public Event(EventType eventType){
        this.eventType = eventType;
        this.user = User.getInstance();
        this.datasnap = new Datasnap(BuildConfig.VERSION_NAME, Device.getInstance());
        this.organizationIds[0] = DsConfig.getInstance().getOrgId();
        this.projectIds[0] = DsConfig.getInstance().getProjectId();
    }

    public Event(EventType eventType, String organizationId,
                 String projectId, String customerOrgId, String customerVenueOrgId, String venueOrgId, User user,
                 Device device, Map<String, Object> additionalProperties){
        this.eventType = eventType;
        this.user = user;
        this.additionalProperties = additionalProperties;
        this.organizationIds[0] = organizationId;
        this.projectIds[0] = projectId;
        this.customerOrgId = customerOrgId;
        this.customerVenueOrgId = customerVenueOrgId;
        this.venueOrgId = venueOrgId;
        this.datasnap = new Datasnap(BuildConfig.VERSION_NAME, device);
    }

    public Datasnap getDatasnap() {
        return datasnap;
    }

    public void setDatasnap(Datasnap datasnap) {
        this.datasnap = datasnap;
    }

    public void setAdditionalProperties(Map<String, Object> additionalProperties) {
        this.additionalProperties = additionalProperties;
    }

    public Map<String, Object> getAdditionalProperties(){
        return additionalProperties;
    }


    public EventType getEventType() {
        return eventType;
    }

    public void setEventType(EventType eventType) {
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

    public Device getDevice() {
        return datasnap.getDevice();
    }

    public void setDevice(Device device) {
        this.datasnap.setDevice(device);
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
        Device device;

        public Datasnap(String version, Device device){
            this.created = getTime();
            this.version = version;
            this.device = device;
        }

        public Device getDevice() {
            return device;
        }

        public void setDevice(Device device) {
            this.device = device;
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
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssZZ");
            return sdf.format(d);
        }
    }

}
