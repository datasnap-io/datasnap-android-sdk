package com.datasnap.android.services;

import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.os.Binder;
import android.os.IBinder;
import android.os.RemoteException;
import android.provider.Settings;
import android.util.Log;
import android.widget.Toast;

import com.datasnap.android.DataSnap;
import com.datasnap.android.eventproperties.Device;
import com.datasnap.android.eventproperties.DeviceInfo;
import com.datasnap.android.eventproperties.Id;
import com.datasnap.android.eventproperties.User;
import com.datasnap.android.events.BeaconEvent;
import com.datasnap.android.events.Event;
import com.estimote.sdk.Beacon;
import com.estimote.sdk.Region;
import com.gimbal.android.BeaconEventListener;
import com.gimbal.android.BeaconManager;
import com.gimbal.android.BeaconSighting;
import com.gimbal.android.Gimbal;
import com.google.android.gms.ads.identifier.AdvertisingIdClient;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

public class EstimoteService extends BaseService {

  private IBinder mBinder = new DataSnapBinder();
  private com.estimote.sdk.BeaconManager beaconManager;
  private static final Region ALL_ESTIMOTE_BEACONS_REGION = new Region("rid", null, null, null);

  @Override
  public void onCreate() {
    super.onCreate();
  }

  @Override
  public IBinder onBind(Intent intent) {
    return mBinder;
  }

  @Override
  public void onRebind(Intent intent) {
    super.onRebind(intent);
  }

  @Override
  public boolean onUnbind(Intent intent) {
    return true;
  }

  private void setEstimoteBeaconSightingListener() {
    beaconManager.setRangingListener(new com.estimote.sdk.BeaconManager.RangingListener() {
      @Override
      public void onBeaconsDiscovered(Region region, final List<Beacon> beacons) {
        for (com.estimote.sdk.Beacon estimoteBeacon : beacons) {
          String eventType = "beacon_sighting";
          com.datasnap.android.eventproperties.Beacon beacon = new com.datasnap.android.eventproperties.Beacon();
          beacon.setIdentifier(estimoteBeacon.getProximityUUID() + "" + estimoteBeacon.getMajor() + "" + estimoteBeacon.getMinor());
          beacon.setHardware(estimoteBeacon.getMacAddress());
          beacon.setBatteryLevel("" + estimoteBeacon.getMeasuredPower());
          beacon.setRssi("" + estimoteBeacon.getRssi());
          beacon.setName(estimoteBeacon.getName());
          beacon.setBleVendorId("Estimote");
          Event event = new BeaconEvent(eventType, organizationId, projectId, null, null, null, beacon, user,
              deviceInfo);
          DataSnap.trackEvent(event);
        }
      }
    });
  }

  @Override
  public int onStartCommand(Intent intent, int flags, int startId) {
    super.onStartCommand(intent, flags, startId);
    beaconManager = new com.estimote.sdk.BeaconManager(this);
    beaconManager.connect(new com.estimote.sdk.BeaconManager.ServiceReadyCallback() {
      @Override
      public void onServiceReady() {
        try {
          beaconManager.startRanging(ALL_ESTIMOTE_BEACONS_REGION);
        } catch (RemoteException e) {
        }
      }
    });
    return START_STICKY;
  }

  @Override
  public void onDestroy() {
    super.onDestroy();
  }

  public class DataSnapBinder extends Binder {
    public EstimoteService getService() {
      return EstimoteService.this;
    }
  }

}
