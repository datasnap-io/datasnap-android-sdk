package com.datasnap.android.services;

import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.Binder;
import android.os.IBinder;
import android.provider.Settings;
import android.telephony.TelephonyManager;

import com.datasnap.android.DataSnap;
import com.datasnap.android.eventproperties.Beacon;
import com.datasnap.android.eventproperties.Device;
import com.datasnap.android.eventproperties.Id;
import com.datasnap.android.eventproperties.User;
import com.datasnap.android.events.BeaconEvent;
import com.datasnap.android.utils.DsConfig;
import com.gimbal.android.BeaconEventListener;
import com.gimbal.android.BeaconManager;
import com.gimbal.android.BeaconSighting;
import com.gimbal.android.Gimbal;
import com.google.android.gms.ads.identifier.AdvertisingIdClient;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;

/**
 * Created by paolopelagatti on 5/16/16.
 */
public class BaseService extends Service {

  protected User user;
  protected String organizationId;
  protected String projectId;
  protected boolean classesLoadingFailed;

  @Override
  public IBinder onBind(Intent intent) {
    return null;
  }

  @Override
  public void onRebind(Intent intent) {
    super.onRebind(intent);
  }

  @Override
  public boolean onUnbind(Intent intent) {
    return true;
  }

  @Override
  public int onStartCommand(Intent intent, int flags, int startId) {
    super.onStartCommand(intent, flags, startId);
    user = User.getInstance();
    organizationId = DsConfig.getInstance().getOrgId();
    projectId = DsConfig.getInstance().getProjectId();
    return START_STICKY;
  }

  @Override
  public void onDestroy() {
    super.onDestroy();
  }

  public void addBeaconSightingListener() {
  }

  public void releaseBeaconSightingListener() {
  }

  public void addCommunicationSentListener() {
  }

  public void releaseCommunicationSentListener() {
  }

  public void addCommunicationOpenListener() {
  }

  public void addGeofenceDepartListener() {
  }

  public void releaseGeofenceDepartListener() {
  }

  public void releaseCommunicationOpenListener() {
  }
}
