package com.datasnap.android.events;

import com.datasnap.android.eventproperties.DeviceInfo;
import com.datasnap.android.eventproperties.User;
import com.datasnap.android.eventproperties.GlobalPosition;

import java.util.Map;

public class GlobalPositionEvent extends Event {

    private GlobalPosition globalPosition;

    public GlobalPositionEvent(String eventType, String organizationId,
                               String projectId, String customerOrgId, String customerVenueOrgId, String venueOrgId, User user, GlobalPosition globalPosition,
                               DeviceInfo deviceInfo, Map<String, Object> additionalProperties) {
        super(eventType, organizationId, projectId, customerOrgId, customerVenueOrgId, venueOrgId, user, deviceInfo, additionalProperties);
        this.globalPosition = globalPosition;
    }

    public GlobalPosition getGlobalPosition() {
        return globalPosition;
    }

    public void setGlobalPosition(GlobalPosition globalPosition) {
        this.globalPosition = globalPosition;
    }



}
