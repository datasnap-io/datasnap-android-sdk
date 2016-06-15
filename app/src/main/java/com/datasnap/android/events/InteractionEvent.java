package com.datasnap.android.events;

import com.datasnap.android.eventproperties.DeviceInfo;
import com.datasnap.android.eventproperties.User;

import java.util.Map;

public class InteractionEvent extends Event {

    // opt-in stuff needed
    public InteractionEvent(String eventType, String organizationId,
                            String projectId, String customerOrgId, String customerVenueOrgId, String venueOrgId, User user, Map<String, Object> additionalProperties, DeviceInfo deviceInfo) {
        super(eventType, organizationId, projectId, customerOrgId, customerVenueOrgId, venueOrgId, user, deviceInfo, additionalProperties);
    }
}
