package com.datasnap.android;

import android.os.Parcel;
import android.os.Parcelable;

import com.datasnap.android.events.IEvent;

import java.util.ArrayList;

/**
 * Created by brianferan on 11/18/14.
 */
public class EventStore implements Parcelable {
    private ArrayList<IEvent> eventStore;
    public ArrayList<IEvent> getEventStore() {
        return eventStore;
    }

    public void setEventStore(ArrayList<IEvent> eventStore) {
        this.eventStore = eventStore;
    }
    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeSerializable(eventStore);

    }
}
