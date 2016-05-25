package com.datasnap.android.events;

import org.json.JSONObject;

import java.util.Map;

public abstract class Event {

    private String dataSnapVersion;

    abstract Map<String, Object> getAdditionalProperties();

    public String getDataSnapVersion() {
        return dataSnapVersion;
    }

    public void setDataSnapVersion(String dataSnapVersion) {
        this.dataSnapVersion = dataSnapVersion;
    }

}
