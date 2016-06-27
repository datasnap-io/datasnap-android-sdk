package com.datasnap.android.sampleapp;

import android.app.Activity;
import android.content.Context;
import android.location.LocationListener;
import android.location.LocationManager;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.provider.Settings;
import android.telephony.TelephonyManager;
import android.text.method.ScrollingMovementMethod;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.datasnap.android.Config;
import com.datasnap.android.DataSnap;
import com.datasnap.android.VendorProperties;
import com.datasnap.android.eventproperties.Beacon;
import com.datasnap.android.eventproperties.Campaign;
import com.datasnap.android.eventproperties.Communication;
import com.datasnap.android.eventproperties.Device;
import com.datasnap.android.eventproperties.DeviceInfo;
import com.datasnap.android.eventproperties.Geofence;
import com.datasnap.android.eventproperties.GlobalPosition;
import com.datasnap.android.eventproperties.Id;
import com.datasnap.android.eventproperties.Location;
import com.datasnap.android.eventproperties.Place;
import com.datasnap.android.eventproperties.User;
import com.datasnap.android.events.BeaconEvent;
import com.datasnap.android.events.CommunicationEvent;
import com.datasnap.android.events.EventType;
import com.datasnap.android.events.GeoFenceEvent;
import com.datasnap.android.events.GlobalPositionEvent;
import com.datasnap.android.events.Event;
import com.datasnap.android.events.InteractionEvent;
import com.datasnap.android.utils.DsConfig;

import java.math.BigDecimal;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;


public class DataSnapAllEventsActivity extends Activity {


    protected final User user = new User();
    protected final Id id = new Id();
    protected DeviceInfo deviceInfo = new DeviceInfo();
    protected Device device = new Device();

    private Button beaconSighting;
    private Button beaconDepart;
    private Button beaconArrive;
    private Button geofenceDepart;
    private Button globalPositionSighting;
    private Button communicationOpen;
    private Button communicationSent;
    private Button appInstalled;
    private Button optInVendor;
    private Button optIntPushNotification;
    private Button optInLocation;
    private Button statusPing;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_all_events);
        String apiKeyId = "MY_API_KEY";
        String apiKeySecret = "MY_API_SECRET";
        Config config = new Config();
        config.context = getApplicationContext();
        config.apiKeyId = apiKeyId;
        config.apiKeySecret = apiKeySecret;
        config.organizationId = "MY_ORGANIZATION";
        config.projectId = "MY_PROJECT";
        DataSnap.initialize(config);
        beaconSighting = (Button) findViewById(R.id.beacon_sighting);
        beaconDepart = (Button) findViewById(R.id.beacon_depart);
        beaconArrive = (Button) findViewById(R.id.beacon_arrive);
        geofenceDepart = (Button) findViewById(R.id.geofence_depart);
        globalPositionSighting = (Button) findViewById(R.id.global_position_sighting);
        communicationOpen = (Button) findViewById(R.id.ds_communication_open);
        communicationSent = (Button) findViewById(R.id.ds_communication_sent);
        appInstalled = (Button) findViewById(R.id.app_installed);
        optInVendor = (Button) findViewById(R.id.opt_in_vendor);
        optIntPushNotification = (Button) findViewById(R.id.opt_in_push_notifications);
        optInLocation = (Button) findViewById(R.id.opt_in_location);
        statusPing = (Button) findViewById(R.id.status_ping);


        String android_id = Settings.Secure.getString(getApplicationContext().getContentResolver(),
            Settings.Secure.ANDROID_ID);
        id.setGlobalDistinctId(android_id);
        deviceInfo.setCreated(getTime());
        device.setIpAddress(getIpAddress());
        device.setPlatform(android.os.Build.VERSION.SDK);
        device.setOsVersion(System.getProperty("os.version"));
        device.setModel(android.os.Build.MODEL);
        device.setManufacturer(android.os.Build.MANUFACTURER);
        device.setName(android.os.Build.DEVICE);
        device.setVendorId(android.os.Build.BRAND);
        TelephonyManager manager = (TelephonyManager) getApplicationContext().getSystemService(Context.TELEPHONY_SERVICE);
        device.setCarrierName(manager.getNetworkOperatorName());
        deviceInfo.setDevice(device);
        new Thread(new Runnable() {
            public void run() {
                try {
                    com.google.android.gms.ads.identifier.AdvertisingIdClient.Info adInfo = com.google.android.gms.ads.identifier.AdvertisingIdClient.getAdvertisingIdInfo(getApplicationContext());
                    id.setMobileDeviceGoogleAdvertisingId(adInfo.getId());
                    id.setMobileDeviceGoogleAdvertisingIdOptIn("" + adInfo.isLimitAdTrackingEnabled());
                    user.setId(id);
                } catch (Exception e) {
                    id.setMobileDeviceGoogleAdvertisingId("sample id");
                    id.setMobileDeviceGoogleAdvertisingIdOptIn("true");
                    user.setId(id);
                    e.printStackTrace();
                }
            }
        }).start();
        beaconSighting.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Beacon beacon = new Beacon();
                beacon.setIdentifier("sample-identifier");
                beacon.setBatteryLevel("high");
                beacon.setRssi("sample rssi");
                beacon.setName("sample identifier");
                beacon.setBleVendorId("Gimbal");
                Event event = new BeaconEvent(EventType.BEACON_SIGHTING, DsConfig.getInstance().getOrgId(), DsConfig.getInstance().getProjectId(), null, null, null, null, user,
                    beacon, deviceInfo, null);
                DataSnap.trackEvent(event);
            }
        });
        beaconDepart.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String eventType = "beacon_depart";
                Beacon beacon = new Beacon();
                beacon.setIdentifier("sample-identifier");
                beacon.setBatteryLevel("high");
                beacon.setRssi("sample rssi");
                beacon.setName("sample identifier");
                beacon.setBleVendorId("Gimbal");
                Event event = new BeaconEvent(EventType.BEACON_DEPART, DsConfig.getInstance().getOrgId(), DsConfig.getInstance().getProjectId(), null, null, null, null, user,
                    beacon, deviceInfo, null);
                DataSnap.trackEvent(event);
            }
        });
        beaconArrive.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Beacon beacon = new Beacon();
                beacon.setIdentifier("sample-identifier");
                beacon.setBatteryLevel("high");
                beacon.setRssi("sample rssi");
                beacon.setName("sample identifier");
                beacon.setBleVendorId("Gimbal");
                Event event = new BeaconEvent(EventType.BEACON_ARRIVED, DsConfig.getInstance().getOrgId(), DsConfig.getInstance().getProjectId(), null, null, null, null, user,
                    beacon, deviceInfo, null);
                DataSnap.trackEvent(event);
            }
        });
        geofenceDepart.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Place place = new Place();
                Geofence geofence = new Geofence();
                Event event = new GeoFenceEvent(EventType.GEOFENCE_DEPART, DsConfig.getInstance().getOrgId(), DsConfig.getInstance().getProjectId(), null, null, null, place,
                    geofence, user, null, deviceInfo);
                DataSnap.trackEvent(event);
            }
        });
        final Context self = this;
        globalPositionSighting.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                final String eventType = "global_position_sighting";
                final GlobalPosition globalPosition = new GlobalPosition();
                final BigDecimal[] coordinates = new BigDecimal[2];
                LocationManager locationManager = (LocationManager)
                    getSystemService(Context.LOCATION_SERVICE);
                LocationListener locationListener = new LocationListener() {
                    @Override
                    public void onLocationChanged(android.location.Location location) {
                        coordinates[0] = new BigDecimal(location.getLatitude());
                        coordinates[1] = new BigDecimal(location.getLongitude());
                        Location loc = new Location(coordinates);
                        globalPosition.setLocation(loc);
                        Event event = new GlobalPositionEvent(EventType.GLOBAL_POSITION_SIGHTING, DsConfig.getInstance().getOrgId(), DsConfig.getInstance().getProjectId(), null, null, null, user,
                            globalPosition, deviceInfo, null);
                        DataSnap.trackEvent(event);
                    }

                    @Override
                    public void onStatusChanged(String provider, int status, Bundle extras) {

                    }

                    @Override
                    public void onProviderEnabled(String provider) {

                    }

                    @Override
                    public void onProviderDisabled(String provider) {

                    }
                };
                locationManager.requestLocationUpdates(
                    LocationManager.GPS_PROVIDER, 5000, 10, locationListener);
            }
        });
        communicationOpen.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Communication communication = new Communication();
                communication.setIdentifier("sample communication identifier");
                communication.setCommunicationVendorId(DsConfig.getInstance().getOrgId());
                Campaign campaign = new Campaign();
                campaign.setCommunicationIds(communication.getIdentifier());
                campaign.setIdentifier("sample campaign identifier");
                campaign.setName("sample campaign name");
                communication.setName("sample communication name");
                String venueId = "MegaStadium01";

                Event event = new CommunicationEvent(EventType.COMMUNICATION_OPEN, DsConfig.getInstance().getOrgId(), DsConfig.getInstance().getProjectId(), null, venueId, venueId, user,
                    communication, campaign, null, deviceInfo);
                DataSnap.trackEvent(event);
            }
        });
        communicationSent.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Communication communication = new Communication();
                communication.setIdentifier("sample communication identifier");
                communication.setCommunicationVendorId(DsConfig.getInstance().getOrgId());
                Campaign campaign = new Campaign();
                campaign.setCommunicationIds(communication.getIdentifier());
                campaign.setIdentifier("sample campaign identifier");
                campaign.setName("sample campaign name");
                communication.setName("sample communication name");
                String venueId = "MegaStadium01";

                Event event = new CommunicationEvent(EventType.COMMUNICATION_SENT, DsConfig.getInstance().getOrgId(), DsConfig.getInstance().getProjectId(), null, venueId, venueId, user,
                    communication, campaign, null, deviceInfo);
                DataSnap.trackEvent(event);
            }
        });
        appInstalled.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String eventType = "app_installed";
                Event event = new InteractionEvent(EventType.APP_INSTALLED, DsConfig.getInstance().getOrgId(), DsConfig.getInstance().getProjectId(), null, null, null, user, null, deviceInfo);
                DataSnap.trackEvent(event);
            }
        });
        optInVendor.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String eventType = "opt_in_vendor";
                Event event = new InteractionEvent(EventType.OPT_IN_VENDOR, DsConfig.getInstance().getOrgId(), DsConfig.getInstance().getProjectId(), null, null, null, user, null, deviceInfo);
                DataSnap.trackEvent(event);
            }
        });
        optIntPushNotification.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String eventType = "opt_in_push_notifications";
                Event event = new InteractionEvent(EventType.OPT_IN_PUSH_NOTIFICATIONS, DsConfig.getInstance().getOrgId(), DsConfig.getInstance().getProjectId(), null, null, null, user, null, deviceInfo);
                DataSnap.trackEvent(event);
            }
        });
        optInLocation.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Event event = new InteractionEvent(EventType.OPT_IN_LOCATION, DsConfig.getInstance().getOrgId(), DsConfig.getInstance().getProjectId(), null, null, null, user, null, deviceInfo);
                DataSnap.trackEvent(event);
            }
        });
        statusPing.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String eventType = "status_ping";
                Toast.makeText(self, "Not implemented yet", Toast.LENGTH_SHORT).show();
            }
        });
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            finish();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    public String getIpAddress() {
        WifiManager wifiMan = (WifiManager) getApplicationContext().getSystemService(Context.WIFI_SERVICE);
        WifiInfo wifiInf = wifiMan.getConnectionInfo();
        int ipAddress = wifiInf.getIpAddress();
        String ip = String.format("%d.%d.%d.%d", (ipAddress & 0xff), (ipAddress >> 8 & 0xff), (ipAddress >> 16 & 0xff), (ipAddress >> 24 & 0xff));
        return ip;
    }

    public String getTime() {
        Calendar c = Calendar.getInstance();
        Date d = c.getTime();
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd hh:mm:ss ZZ");
        return sdf.format(d);
    }
}
