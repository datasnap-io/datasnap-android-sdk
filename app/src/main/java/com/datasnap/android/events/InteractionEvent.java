package com.datasnap.android.events;

import com.datasnap.android.eventproperties.User;

import java.util.Map;

public class InteractionEvent extends Event {

    // opt-in stuff needed
    public InteractionEvent(String eventType, String[] organizationIds,
                            String[] projectIds, String customerOrgId, String customerVenueOrgId, String venueOrgId, User user, Map<String, Object> additionalProperties) {
        super();
        this.eventType = eventType;
        this.organizationIds = organizationIds;
        this.projectIds = projectIds;
        this.user = user;
        this.additionalProperties = additionalProperties;
        this.customerOrgId = customerOrgId;
        this.customerVenueOrgId = customerVenueOrgId;
        this.venueOrgId = venueOrgId;

    }
}
