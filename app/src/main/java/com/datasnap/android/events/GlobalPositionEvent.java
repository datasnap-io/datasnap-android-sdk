package com.datasnap.android.events;

//import org.codehaus.jackson.annotate.JsonIgnore;
//import org.codehaus.jackson.map.annotate.JsonSerialize;
//import com.github.datasnap.propertiesandevents.Beacon;
// import com.github.datasnap.propertiesandevents.DeviceInfo;
// import com.github.datasnap.propertiesandevents.Place;
// import com.github.datasnap.propertiesandevents.User;

import com.datasnap.android.eventproperties.DeviceInfo;
import com.datasnap.android.eventproperties.User;
import com.datasnap.android.eventproperties.GlobalPosition;

import java.util.Map;

//@JsonSerialize(include = JsonSerialize.Inclusion.NON_NULL)

public class GlobalPositionEvent extends Event {

    private String eventType;
    private String[] organizationIds;
    private String[] projectIds;
    private String customerOrgId;
    private String customerVenueOrgId;
    private String venueOrgId;
    private User user;
    private GlobalPosition GlobalPosition;
    private DeviceInfo DeviceInfo;
    //	@JsonIgnore
    private Map<String, Object> additionalProperties;

    public GlobalPositionEvent(String eventType, String[] organizationIds,
                               String[] projectIds, String customerOrgId, String customerVenueOrgId, String venueOrgId, User user, GlobalPosition globalPosition,
                               DeviceInfo deviceInfo, Map<String, Object> additionalProperties) {
        super();
        this.eventType = eventType;
        this.organizationIds = organizationIds;
        this.projectIds = projectIds;
        this.user = user;
        GlobalPosition = globalPosition;
        DeviceInfo = deviceInfo;
        this.additionalProperties = additionalProperties;
        this.customerOrgId = customerOrgId;
        this.customerVenueOrgId = customerVenueOrgId;
        this.venueOrgId = venueOrgId;

    }

    public Map<String, Object> getAdditionalProperties() {
        return this.additionalProperties;
    }

    public void setAdditionalProperties(Map<String, Object> additionalProperties) {
        this.additionalProperties = additionalProperties;
    }

    public String getEvent_type() {
        return eventType;
    }

    public void setEvent_type(String eventType) {
        this.eventType = eventType;
    }

    public String[] getOrganizationIds() {
        return organizationIds;
    }

    public void setOrganizationIds(String[] organizationIds) {
        this.organizationIds = organizationIds;
    }

    public String[] getProjectIds() {
        return projectIds;
    }

    public void setProjectIds(String[] projectIds) {
        this.projectIds = projectIds;
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


    public GlobalPosition getGlobalPosition() {
        return GlobalPosition;
    }

    public void setGlobalPosition(GlobalPosition GlobalPosition) {
        this.GlobalPosition = GlobalPosition;
    }

    //	@JsonProperty("datasnap")
    public DeviceInfo getDeviceInfo() {
        return DeviceInfo;
    }

    public void setDeviceInfo(DeviceInfo DeviceInfo) {
        this.DeviceInfo = DeviceInfo;
    }

    public User getUser() {
        return user;
    }

    public void setUser(User user) {
        this.user = user;
    }

}
