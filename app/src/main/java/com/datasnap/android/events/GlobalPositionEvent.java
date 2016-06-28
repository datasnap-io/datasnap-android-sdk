package com.datasnap.android.events;

import com.datasnap.android.eventproperties.GlobalPosition;
import com.google.gson.annotations.SerializedName;

import java.util.Map;

public class GlobalPositionEvent extends Event {

    @SerializedName("global-position")
    private GlobalPosition globalPosition;

    public GlobalPositionEvent(String eventType, GlobalPosition globalPosition) {
        super(eventType);
        this.globalPosition = globalPosition;
    }

    public GlobalPosition getGlobalPosition() {
        return globalPosition;
    }

    public void setGlobalPosition(GlobalPosition globalPosition) {
        this.globalPosition = globalPosition;
    }



}
