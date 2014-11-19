package com.datasnap.android.models;

import com.datasnap.android.events.IEvent;

import org.json.JSONObject;

public class EventWrapper  {

    public IEvent getEvent() {
        return event;
    }

    public void setEvent(IEvent event) {
        this.event = event;
    }

    public IEvent event;
  public final static String TYPE = "trackEvent";

  private final static String USER_ID_KEY = "userId";
  private static final String EVENT_KEY = "beacon test event";
  private static final String PROPERTIES_KEY = "properties";

    public String getEventStr() {
        return eventStr;
    }

    public void setEventStr(String eventStr) {
        this.eventStr = eventStr;
    }

    private String eventStr;

  public EventWrapper(JSONObject obj, IEvent event) {

      this.event = event;
  }


  public EventWrapper(String userId, String eventStr, Props properties,
                      Options options) {
      this.eventStr = eventStr;

 //   setProperties(properties);
  }



}
