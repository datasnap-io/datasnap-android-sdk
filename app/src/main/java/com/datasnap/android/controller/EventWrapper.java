package com.datasnap.android.controller;

public class EventWrapper {

    private String eventStr;

    public String getEventStr() {
        return eventStr;
    }

    public void setEventStr(String eventStr) {
        this.eventStr = eventStr;
    }

    public EventWrapper(String eventStr) {
        this.eventStr = eventStr;
    }
    public EventWrapper() {
    }

    @Override
    public String toString() {
        return eventStr;
    }
}
