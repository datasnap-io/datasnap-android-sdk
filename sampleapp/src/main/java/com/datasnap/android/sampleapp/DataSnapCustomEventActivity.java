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

import com.datasnap.android.Config;
import com.datasnap.android.DataSnap;
import com.datasnap.android.VendorProperties;
import com.datasnap.android.controller.HTTPRequester;
import com.datasnap.android.eventproperties.Beacon;
import com.datasnap.android.eventproperties.Device;
import com.datasnap.android.eventproperties.Place;
import com.datasnap.android.eventproperties.User;
import com.datasnap.android.events.BeaconEvent;
import com.datasnap.android.events.Event;
import com.datasnap.android.events.EventType;
import com.datasnap.android.services.BaseService;
import com.datasnap.android.utils.DsConfig;
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
        String apiKeyId = "MY_API_KEY";
        String apiKeySecret = "MY_API_SECRET";
        VendorProperties vendorProperties = new VendorProperties();
        vendorProperties.setGimbalApiKey("MY_GIMBAL_API_KEY");
        vendorProperties.addVendor(VendorProperties.Vendor.GIMBAL);
        Config config = new Config.Builder()
            .setApiKeyId(apiKeyId)
            .setApiKeySecret(apiKeySecret)
            .setOrganizationId("MY_ORGANIZATION")
            .setProjectId("MY_PROJECT")
            .setVendorProperties(vendorProperties)
            .build();
        DataSnap.initialize(getApplicationContext(), config);
        DataSnap.setFlushParams(100000, 20);
        Gimbal.setApiKey(this.getApplication(), "MY_GIMBAL_API_KEY");

        Device device = Device.getInstance();
        device.setIpAddress(getIpAddress());
        device.setPlatform(android.os.Build.VERSION.SDK);
        device.setOsVersion(System.getProperty("os.version"));
        device.setModel(android.os.Build.MODEL);
        device.setManufacturer(android.os.Build.MANUFACTURER);
        device.setName(android.os.Build.DEVICE);
        device.setVendorId(android.os.Build.BRAND);
        TelephonyManager manager = (TelephonyManager) getSystemService(Context.TELEPHONY_SERVICE);
        device.setCarrierName(manager.getNetworkOperatorName());
        getActionBar().setDisplayHomeAsUpEnabled(true);
        gimbalBeaconManager = new BeaconManager();
        gimbalBeaconEventListener = new BeaconEventListener() {
            @Override
            public void onBeaconSighting(BeaconSighting sighting) {
                super.onBeaconSighting(sighting);
                Beacon beacon = new Beacon();
                beacon.setIdentifier(sighting.getBeacon().getIdentifier());
                beacon.setBatteryLevel(sighting.getBeacon().getBatteryLevel().toString());
                beacon.setRssi(sighting.getBeacon().getUuid());
                beacon.setName(sighting.getBeacon().getName());
                beacon.setBleVendorId("Gimbal");
                Event event = new MyCustomBeaconEvent(EventType.BEACON_SIGHTING, "custom value", beacon);
                DataSnap.trackEvent(event);
            }
        };
        HTTPRequester.startRequestCount();
        datasnapSightings = (Button) findViewById(R.id.datasnap_sightings);
        sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);
        if(!sharedPreferences.getBoolean(EventType.BEACON_SIGHTING.name(), true)){
            datasnapSightings.setText("Turn on datasnap sightings");
        }
        //turn on and off datasnap's sent beacon sightings:
        datasnapSightings.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                boolean activeListener = sharedPreferences.getBoolean(EventType.BEACON_SIGHTING.name(), true);
                if(activeListener) {
                    DataSnap.setEventEnabled(EventType.BEACON_SIGHTING, false);
                    datasnapSightings.setText("Turn on datasnap sightings");
                } else {
                    DataSnap.setEventEnabled(EventType.BEACON_SIGHTING, true);
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
        HandlerTimer timer = new HandlerTimer(1000, checkRequestsClock);
        timer.start();
    }

    private Runnable checkRequestsClock = new Runnable() {
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

        public MyCustomBeaconEvent(EventType eventType, String myCustomField, Beacon beacon) {
            super(eventType);
            this.myCustomField = myCustomField;
            this.beacon = beacon;
        }

    }

}
