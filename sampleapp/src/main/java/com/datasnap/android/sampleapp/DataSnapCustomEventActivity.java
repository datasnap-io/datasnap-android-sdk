package com.datasnap.android.sampleapp;

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.provider.ContactsContract;
import android.telephony.TelephonyManager;
import android.text.method.ScrollingMovementMethod;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import com.datasnap.android.DataSnap;
import com.datasnap.android.VendorProperties;
import com.datasnap.android.controller.HTTPRequester;
import com.datasnap.android.eventproperties.Beacon;
import com.datasnap.android.eventproperties.Device;
import com.datasnap.android.eventproperties.DeviceInfo;
import com.datasnap.android.eventproperties.Place;
import com.datasnap.android.eventproperties.User;
import com.datasnap.android.events.BeaconEvent;
import com.datasnap.android.events.Event;
import com.datasnap.android.events.EventListener;
import com.datasnap.android.events.EventType;
import com.datasnap.android.services.BaseService;
import com.datasnap.android.utils.HandlerTimer;
import com.gimbal.android.BeaconEventListener;
import com.gimbal.android.BeaconManager;
import com.gimbal.android.BeaconSighting;
import com.gimbal.android.CommunicationManager;
import com.gimbal.android.Gimbal;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Map;

//this is a sample activity that shows how to create custom events in spite of datasnap's automatic
//events. A text view is used to track the amount of network requests made. If all the listeners are
//turned off the communication should stop, by switching them back on the communication should start
//over. A custom beacon event is created to show how to manage custom events.
public class DataSnapCustomEventActivity extends Activity {

    private TextView textView;
    private Button datasnapSightings;
    private Button customSightings;
    private static SharedPreferences sharedPreferences;
    private BeaconEventListener gimbalBeaconEventListener;
    BeaconManager gimbalBeaconManager;
    boolean customSightingListenerActive;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_custom_event);
        textView = (TextView) findViewById(R.id.requests_count);
        textView.setMovementMethod(new ScrollingMovementMethod());
        String apiKeyId = "3F34FXD78PCINFR99IYW950W4";
        String apiKeySecret = "KA0HdzrZzNjvUq8OnKQoxaReyUayZY0ckNYoMZURxK8";
        VendorProperties vendorProperties = new VendorProperties();
        vendorProperties.setGimbalApiKey("044e761a-0b9f-4472-b2bb-714625e83574");
        vendorProperties.addVendor(VendorProperties.Vendor.GIMBAL);
        DataSnap.initialize(getApplicationContext(), apiKeyId, apiKeySecret, "19CYxNMSQvfnnMf1QS4b3Z", "21213f8b-8341-4ef3-a6b8-ed0f84945186",  vendorProperties);
        DataSnap.setFlushParams(100000, 20);
        Gimbal.setApiKey(this.getApplication(), "044e761a-0b9f-4472-b2bb-714625e83574");

        DeviceInfo deviceInfo = new DeviceInfo();
        Device device = new Device();
        deviceInfo.setCreated(getTime());
        device.setIpAddress(getIpAddress());
        device.setPlatform(android.os.Build.VERSION.SDK);
        device.setOsVersion(System.getProperty("os.version"));
        device.setModel(android.os.Build.MODEL);
        device.setManufacturer(android.os.Build.MANUFACTURER);
        device.setName(android.os.Build.DEVICE);
        device.setVendorId(android.os.Build.BRAND);
        TelephonyManager manager = (TelephonyManager) getSystemService(Context.TELEPHONY_SERVICE);
        device.setCarrierName(manager.getNetworkOperatorName());
        deviceInfo.setDevice(device);
        DeviceInfo.initialize(deviceInfo);
        getActionBar().setDisplayHomeAsUpEnabled(true);
        gimbalBeaconManager = new BeaconManager();
        gimbalBeaconEventListener = new BeaconEventListener() {
            @Override
            public void onBeaconSighting(BeaconSighting sighting) {
                super.onBeaconSighting(sighting);
                String eventType = "my_custom_event_type";
                Beacon beacon = new Beacon();
                beacon.setIdentifier(sighting.getBeacon().getIdentifier());
                beacon.setBatteryLevel(sighting.getBeacon().getBatteryLevel().toString());
                beacon.setRssi(sighting.getBeacon().getUuid());
                beacon.setName(sighting.getBeacon().getName());
                beacon.setBleVendorId("Gimbal");
                Event event = new MyCustomBeaconEvent(eventType, DataSnap.getOrgId(), DataSnap.getProjectId(), null, null, null, User.getInstance(),
                    "custom value", DeviceInfo.getInstance(), beacon, null);
                DataSnap.trackEvent(event);
            }
        };
        HTTPRequester.startRequestCount();
        datasnapSightings = (Button) findViewById(R.id.datasnap_sightings);
        sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);
        if(!sharedPreferences.getBoolean(EventListener.GIMBAL_BEACON_SIGHTING, true)){
            datasnapSightings.setText("Turn on datasnap sightings");
        }
        //turn on and off datasnap's sent beacon sightings:
        datasnapSightings.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                boolean activeListener = sharedPreferences.getBoolean(EventListener.GIMBAL_BEACON_SIGHTING, true);
                if(activeListener) {
                    DataSnap.setEventEnabled(EventListener.GIMBAL_BEACON_SIGHTING, false);
                    datasnapSightings.setText("Turn on datasnap sightings");
                } else {
                    DataSnap.setEventEnabled(EventListener.GIMBAL_BEACON_SIGHTING, true);
                    datasnapSightings.setText("Turn off datasnap sightings");
                }
            }
        });
        customSightings = (Button) findViewById(R.id.custom_sightings);
        //add custom beacon listener:
        customSightings.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                switchBeaconListener();
                if(customSightingListenerActive)
                    customSightings.setText("Turn on custom sightings");
                else
                    customSightings.setText("Turn off custom sightings");
                customSightingListenerActive = !customSightingListenerActive;
            }
        });
        HandlerTimer timer = new HandlerTimer(1000, eventClock);
        timer.start();
    }

    private Runnable eventClock = new Runnable() {
        @Override
        public void run() {
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    if (textView != null) {
                        textView.setText("Requests count: " + HTTPRequester.getRequestCount());
                    }
                }
            });
        }
    };

    private void switchBeaconListener(){
        if(customSightingListenerActive){
            gimbalBeaconManager.removeListener(gimbalBeaconEventListener);
            gimbalBeaconManager.stopListening();
        } else {
            gimbalBeaconManager.addListener(gimbalBeaconEventListener);
            gimbalBeaconManager.startListening();
        }
    }

    private String getIpAddress() {
        WifiManager wifiMan = (WifiManager) getSystemService(Context.WIFI_SERVICE);
        WifiInfo wifiInf = wifiMan.getConnectionInfo();
        int ipAddress = wifiInf.getIpAddress();
        String ip = String.format("%d.%d.%d.%d", (ipAddress & 0xff), (ipAddress >> 8 & 0xff), (ipAddress >> 16 & 0xff), (ipAddress >> 24 & 0xff));
        return ip;
    }

    private String getTime() {
        Calendar c = Calendar.getInstance();
        Date d = c.getTime();
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd hh:mm:ss ZZ");
        return sdf.format(d);
    }

    public class MyCustomBeaconEvent extends Event {

        private String myCustomField;
        private Beacon beacon;

        public MyCustomBeaconEvent(String eventType, String organizationId,
                                   String projectId, String customerOrgId, String customerVenueOrgId, String venueOrgId, User user, String myCustomField,
                                   DeviceInfo deviceInfo, Beacon beacon, Map<String, Object> additionalProperties) {
            super(eventType, organizationId, projectId, customerOrgId, customerVenueOrgId, venueOrgId, user, deviceInfo, additionalProperties);
            this.myCustomField = myCustomField;
            this.beacon = beacon;
        }

    }

}
