package com.datasnap.android.events;

import com.datasnap.android.eventproperties.User;

import java.util.Map;

public class InteractionEvent extends Event {

    // opt-in stuff needed
    public InteractionEvent(String eventType, String organizationId,
                            String projectId, String customerOrgId, String customerVenueOrgId, String venueOrgId, User user, Map<String, Object> additionalProperties) {
        super();
        this.eventType = eventType;
        this.organizationIds[0] = organizationId;
        this.projectIds[0] = projectId;
        this.user = user;
        this.additionalProperties = additionalProperties;
        this.customerOrgId = customerOrgId;
        this.customerVenueOrgId = customerVenueOrgId;
        this.venueOrgId = venueOrgId;

    }
}
