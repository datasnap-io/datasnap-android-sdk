package com.datasnap.android.events;

import com.datasnap.android.eventproperties.Campaign;
import com.datasnap.android.eventproperties.Communication;
import com.datasnap.android.eventproperties.DeviceInfo;
import com.datasnap.android.eventproperties.User;

import java.util.Map;

public class CommunicationEvent extends Event {

    private Communication communication;
    private Campaign campaign;

    public CommunicationEvent(String eventType, String organizationId,
                              String projectId, String customerOrgId, String customerVenueOrgId, String venueOrgId, User user, Communication communication,
                              Campaign campaign, Map<String, Object> additionalProperties, DeviceInfo deviceInfo) {
        super(eventType, organizationId, projectId, customerOrgId, customerVenueOrgId, venueOrgId, user, deviceInfo, additionalProperties);
        this.communication = communication;
        this.campaign = campaign;
    }

    public Communication getCommunication() {
        return communication;
    }

    public void setCommunication(Communication communication) {
        this.communication = communication;
    }

    public Campaign getCampaign() {
        return campaign;
    }

    public void setCampaign(Campaign campaign) {
        this.campaign = campaign;
    }

}
