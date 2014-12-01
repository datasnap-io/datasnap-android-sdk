package com.datasnap.android.sampleapp;

import android.os.Parcel;
import android.os.Parcelable;

import com.datasnap.android.events.IEvent;

import java.util.ArrayList;
import java.util.HashMap;

/**
 * Created by brianferan on 11/18/14.
 */
public class BeaconStore implements Parcelable {

    private HashMap<String, String> beaconStore;

    public HashMap<String, String>  getBeaconStore() {
        return beaconStore;
    }

    public BeaconStore(HashMap<String, String> beaconStore){
        this.beaconStore = beaconStore;
    }


    private BeaconStore(Parcel in) {
        beaconStore = (HashMap<String, String>) in.readSerializable();
    }

    public void setBeaconStore(HashMap<String, String> beaconStore) {
        this.beaconStore = beaconStore;
    }
    @Override
    public int describeContents() {
        return 0;
    }

    public static final Parcelable.Creator<BeaconStore> CREATOR =
            new Parcelable.Creator<BeaconStore>() {
                public BeaconStore createFromParcel(Parcel in) {
                    return new BeaconStore(in);
                }

                @Override
                public BeaconStore[] newArray(int size) {
                    return new BeaconStore[size];
                }
            };

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeSerializable(beaconStore);

    }
}
