package com.datasnap.android.events;

import com.datasnap.android.eventproperties.Beacon;
import com.datasnap.android.eventproperties.Place;
import com.datasnap.android.eventproperties.User;

import java.util.Map;

public class UpdateEvent extends Event {

    // different types of updates- might link to other event types....

    private com.datasnap.android.eventproperties.Beacon Beacon;
    private Place place;

    public UpdateEvent(String eventType, String organizationId,
                       String projectId, String customerOrgId, String customerVenueOrgId, String venueOrgId, com.datasnap.android.eventproperties.Beacon beacon, Map<String, Object> additionalProperties) {
        super();
        this.eventType = eventType;
        this.organizationIds[0] = organizationId;
        this.projectIds[0] = projectId;
        Beacon = beacon;
        this.additionalProperties = additionalProperties;
        this.customerOrgId = customerOrgId;
        this.customerVenueOrgId = customerVenueOrgId;
        this.venueOrgId = venueOrgId;
    }

    public com.datasnap.android.eventproperties.Beacon getBeacon() {
        return Beacon;
    }

    public void setBeacon(com.datasnap.android.eventproperties.Beacon beacon) {
        Beacon = beacon;
    }



}
