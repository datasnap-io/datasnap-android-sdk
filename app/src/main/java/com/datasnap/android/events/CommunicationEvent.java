package com.datasnap.android.events;

import com.datasnap.android.eventproperties.Campaign;
import com.datasnap.android.eventproperties.Communication;

public class CommunicationEvent extends Event {

  private Communication communication;
  private Campaign campaign;

  public CommunicationEvent(EventType eventType, Communication communication, Campaign campaign) {
    super(eventType);
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
