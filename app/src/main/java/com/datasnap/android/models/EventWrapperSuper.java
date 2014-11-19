package com.datasnap.android.models;

import java.util.Calendar;
import java.util.UUID;
import org.json.JSONObject;

public class EventWrapperSuper {

    private final static String TYPE_KEY = "type";
    private final static String CONTEXT_KEY = "context";
    private final static String ANONYMOUS_ID_KEY = "anonymousId";
    private final static String TIMESTAMP_KEY = "timestamp";
    private final static String MESSAGE_ID_KEY = "messageId";


    public EventWrapperSuper(String type, Options options) {
        if (options == null) options = new Options();


    }



}
