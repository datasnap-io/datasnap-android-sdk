package com.datasnap.android.services;

import android.app.Notification;
import android.content.Context;
import android.content.Intent;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.Binder;
import android.os.Handler;
import android.os.IBinder;
import android.widget.Toast;

import com.datasnap.android.DataSnap;
import com.datasnap.android.eventproperties.Beacon;
import com.datasnap.android.eventproperties.Campaign;
import com.datasnap.android.eventproperties.Device;
import com.datasnap.android.eventproperties.DeviceInfo;
import com.datasnap.android.eventproperties.Geofence;
import com.datasnap.android.eventproperties.Id;
import com.datasnap.android.eventproperties.Place;
import com.datasnap.android.eventproperties.User;
import com.datasnap.android.events.BeaconEvent;
import com.datasnap.android.events.CommunicationEvent;
import com.datasnap.android.events.Event;
import com.datasnap.android.events.EventType;
import com.datasnap.android.events.GeoFenceEvent;
import com.gimbal.android.BeaconEventListener;
import com.gimbal.android.BeaconManager;
import com.gimbal.android.BeaconSighting;
import com.gimbal.android.Communication;
import com.gimbal.android.CommunicationListener;
import com.gimbal.android.CommunicationManager;
import com.gimbal.android.Gimbal;
import com.gimbal.android.GimbalDebugger;
import com.gimbal.android.PlaceEventListener;
import com.gimbal.android.PlaceManager;
import com.gimbal.android.Push;
import com.gimbal.android.Visit;
import com.google.android.gms.ads.identifier.AdvertisingIdClient;
import com.datasnap.android.utils.Logger;

import java.util.Collection;
import java.util.List;

public class GimbalService extends BaseService {

  private IBinder mBinder = new DataSnapBinder();
  private BeaconManager gimbalBeaconManager;
  private BeaconEventListener gimbalBeaconEventListener;
  private CommunicationListener communicationListener;
  private PlaceEventListener placeEventListener;
  private boolean communicationSentListenerActive;
  private boolean communicationOpenListenerActive;
  private boolean geofenceDepartListenerActive;

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

  public void addGimbalBeaconSightingListener() {
    if(gimbalBeaconEventListener!=null)
      gimbalBeaconManager.removeListener(gimbalBeaconEventListener);
    gimbalBeaconEventListener = new BeaconEventListener() {
      @Override
      public void onBeaconSighting(BeaconSighting sighting) {
        super.onBeaconSighting(sighting);
        com.datasnap.android.utils.Logger.d("Received sighting with RSSI %s", sighting.getRSSI());
        Beacon beacon = new Beacon();
        beacon.setIdentifier(sighting.getBeacon().getIdentifier());
        beacon.setBatteryLevel(sighting.getBeacon().getBatteryLevel().toString());
        beacon.setRssi(sighting.getBeacon().getUuid());
        beacon.setName(sighting.getBeacon().getName());
        beacon.setBleVendorId("Gimbal");
        Event event = new BeaconEvent(EventType.BEACON_SIGHTING, organizationId, projectId, null, null, null, null, user, beacon,
            deviceInfo, null);
        DataSnap.trackEvent(event);
      }
    };
    final Handler mainHandler = new Handler(this.getMainLooper());
    final Runnable mainRunnable = new Runnable() {
      @Override
      public void run() {
        gimbalBeaconManager.addListener(gimbalBeaconEventListener);
        gimbalBeaconManager.startListening();
      }
    };
    mainHandler.post(mainRunnable);
  }

  public void addGimbalCommunicationSentListener(){
    communicationSentListenerActive = true;
  }

  public void addGimbalCommunicationOpenListener(){
    communicationOpenListenerActive = true;
  }

  public void addGimbalGeofenceDepartListener(){
    geofenceDepartListenerActive = true;
  }

  public void releaseGimbalBeaconSightingListener(){
    gimbalBeaconManager.stopListening();
    gimbalBeaconManager.removeListener(gimbalBeaconEventListener);
  }

  public void releaseGimbalCommunicationSentListener(){
    communicationSentListenerActive = false;
  }

  public void releaseGimbalCommunicationOpenListener(){
    communicationOpenListenerActive = false;
  }

  public void releaseGimbalGeofenceDepartListener(){
    geofenceDepartListenerActive = false;
  }

  @Override
  public int onStartCommand(Intent intent, int flags, int startId) {
    super.onStartCommand(intent, flags, startId);
    String apiKey = intent.getStringExtra("gimbalApiKey");
    try {
      Gimbal.setApiKey(this.getApplication(), apiKey);
      GimbalDebugger.enableBeaconSightingsLogging();
      gimbalBeaconManager = new com.gimbal.android.BeaconManager();
      initGimbalCommunicationListener();
      initGimbalPlaceListener();
    } catch (NoClassDefFoundError e) {
      Logger.e("Gimbal sdk can't be found, please add it to your project's dependencies");
      classesLoadingFailed = true;
    }
    return START_STICKY;
  }

  @Override
  public void onDestroy() {
    super.onDestroy();
  }

  public class DataSnapBinder extends Binder {
    public GimbalService getService() {
      if(classesLoadingFailed)
        return null;
      return GimbalService.this;
    }
  }


  private Collection<Communication> presentNotificationForCommunicationsListener(Collection<Communication> communications, Visit visit) {
    if (communicationSentListenerActive) {
      for (Communication communication : communications) {
        com.datasnap.android.eventproperties.Communication dataSnapCommunication = new com.datasnap.android.eventproperties.Communication();
        dataSnapCommunication.setIdentifier(communication.getIdentifier());
        dataSnapCommunication.setName(communication.getTitle());
        dataSnapCommunication.setDescription(communication.getDescription());
        Campaign campaign = new Campaign();
        campaign.setIdentifier(projectId);
        campaign.setCommunicationIds(communication.getIdentifier());
        String venueId = visit.getVisitID();
        Event event = new CommunicationEvent(EventType.COMMUNICATION_SENT, organizationId, projectId, null, venueId, venueId, user,
            dataSnapCommunication, campaign, null, deviceInfo);
        DataSnap.trackEvent(event);
      }
    }
    return communications;
  }

  private Collection<Communication> presentNotificationForCommunicationsListener(Collection<Communication> communications, Push push) {
    if (communicationSentListenerActive) {
      for (Communication communication : communications) {
        com.datasnap.android.eventproperties.Communication dataSnapCommunication = new com.datasnap.android.eventproperties.Communication();
        dataSnapCommunication.setIdentifier(communication.getIdentifier());
        dataSnapCommunication.setName(communication.getTitle());
        dataSnapCommunication.setDescription(communication.getDescription());
        Campaign campaign = new Campaign();
        campaign.setIdentifier(projectId);
        campaign.setCommunicationIds(communication.getIdentifier());
        push.getPushType();
        Event event = new CommunicationEvent(EventType.COMMUNICATION_SENT, organizationId, projectId, null, null, null, user,
            dataSnapCommunication, campaign, null, deviceInfo);
        DataSnap.trackEvent(event);
      }
    }
    return communications;
  }

  private void onNotificationClickedListener(List<Communication> communications) {
    if (communicationOpenListenerActive) {
      for (Communication communication : communications) {
        com.datasnap.android.eventproperties.Communication dataSnapCommunication = new com.datasnap.android.eventproperties.Communication();
        dataSnapCommunication.setIdentifier(communication.getIdentifier());
        dataSnapCommunication.setName(communication.getTitle());
        dataSnapCommunication.setDescription(communication.getDescription());
        Campaign campaign = new Campaign();
        campaign.setIdentifier(projectId);
        campaign.setCommunicationIds(communication.getIdentifier());
        Event event = new CommunicationEvent(EventType.COMMUNICATION_OPEN, organizationId, projectId, null, null, null, user,
            dataSnapCommunication, campaign, null, deviceInfo);
        DataSnap.trackEvent(event);
      }
    }
  }

  private void onVisitEndListener(Visit visit) {
    if(geofenceDepartListenerActive) {
      com.gimbal.android.Place gimbalPlace = visit.getPlace();
      Geofence geofence = new Geofence();
      geofence.setIdentifier(gimbalPlace.getIdentifier());
      geofence.setName(gimbalPlace.getName());
      Event event = new GeoFenceEvent(EventType.GEOFENCE_DEPART, DataSnap.getOrgId(), DataSnap.getProjectId(), null, null, null, null,
          geofence, user, null, deviceInfo);
      DataSnap.trackEvent(event);
    }
  }

  private void initGimbalCommunicationListener(){
    communicationListener = new CommunicationListener(){
      @Override
      public Collection<Communication> presentNotificationForCommunications(Collection<Communication> communications, Visit visit) {
        return presentNotificationForCommunicationsListener(communications, visit);
      }

      @Override
      public Collection<Communication> presentNotificationForCommunications(Collection<Communication> communications, Push push) {
        return presentNotificationForCommunicationsListener(communications, push);
      }

      @Override
      public void onNotificationClicked(List<Communication> communications) {
        onNotificationClickedListener(communications);
      }
    };
    CommunicationManager.getInstance().addListener(communicationListener);
    CommunicationManager.getInstance().startReceivingCommunications();
    PlaceManager.getInstance().startMonitoring();
  }

  private void initGimbalPlaceListener(){
    placeEventListener = new PlaceEventListener() {
      @Override
      public void onVisitStart(Visit visit) {
        super.onVisitStart(visit);
      }

      @Override
      public void onVisitEnd(Visit visit) {
        super.onVisitEnd(visit);
        onVisitEndListener(visit);
      }
    };
    PlaceManager.getInstance().addListener(placeEventListener);
    if(!PlaceManager.getInstance().isMonitoring()){
      PlaceManager.getInstance().startMonitoring();
    }
  }

}
