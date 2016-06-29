package com.datasnap.android.events;

public class UpdateEvent extends Event {

    // different types of updates- might link to other event types....

    public UpdateEvent(EventType eventType) {
        super(eventType);
    }
}
