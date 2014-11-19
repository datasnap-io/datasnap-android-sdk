

package com.datasnap.android.models;

import java.util.Calendar;
import java.util.List;
import java.util.UUID;
import org.json.JSONArray;

public class EventListContainer {

    private final static String WRITE_KEY = "writeKey";
    private final static String BATCH_KEY = "batch";
    private final static String MESSAGE_ID_KEY = "messageId";
    private final static String SENT_AT_KEY = "sentAt";

    private List<EventWrapper> batch;


    public List<EventWrapper> getBatch() {
        return batch;
    }

    public void setBatch(List<EventWrapper> batch) {
        this.batch = batch;
    }

    public EventListContainer(String writeKey, List<EventWrapper> batch) {
     this.batch = batch;
    }
}

