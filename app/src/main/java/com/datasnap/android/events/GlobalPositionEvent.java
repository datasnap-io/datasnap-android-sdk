package com.datasnap.android.events;

import com.datasnap.android.eventproperties.DeviceInfo;
import com.datasnap.android.eventproperties.User;
import com.datasnap.android.eventproperties.GlobalPosition;

import java.util.Map;

public class GlobalPositionEvent extends Event {

    private GlobalPosition globalPosition;

    public GlobalPositionEvent(String eventType, String[] organizationIds,
                               String[] projectIds, String customerOrgId, String customerVenueOrgId, String venueOrgId, User user, GlobalPosition globalPosition,
                               DeviceInfo deviceInfo, Map<String, Object> additionalProperties) {
        super();
        this.eventType = eventType;
        this.organizationIds = organizationIds;
        this.projectIds = projectIds;
        this.user = user;
        this.globalPosition = globalPosition;
        this.deviceInfo = deviceInfo;
        this.additionalProperties = additionalProperties;
        this.customerOrgId = customerOrgId;
        this.customerVenueOrgId = customerVenueOrgId;
        this.venueOrgId = venueOrgId;

    }

    public GlobalPosition getGlobalPosition() {
        return globalPosition;
    }

    public void setGlobalPosition(GlobalPosition globalPosition) {
        this.globalPosition = globalPosition;
    }



}
