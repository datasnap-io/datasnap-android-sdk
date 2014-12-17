package com.datasnap.android.sampleapp;

import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.content.Context;
import android.content.Intent;
import android.location.Location;
import android.location.LocationManager;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.os.RemoteException;
import android.provider.Settings;
import android.telephony.TelephonyManager;
import android.text.method.ScrollingMovementMethod;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.ads.identifier.AdvertisingIdClient;
import com.google.android.gms.ads.identifier.AdvertisingIdClient.Info;

import com.datasnap.android.DataSnap;
import com.datasnap.android.eventproperties.Beacon;
import com.datasnap.android.eventproperties.Id;
import com.datasnap.android.eventproperties.User;
import com.datasnap.android.events.BeaconEvent;
import com.datasnap.android.events.IEvent;
import com.datasnap.android.eventproperties.Device;
import com.datasnap.android.eventproperties.DeviceInfo;
import com.estimote.sdk.BeaconManager;
import com.estimote.sdk.Region;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import com.google.android.gms.location.LocationListener;


public class DataSnapEstimoteActivity extends Activity {

    private static final String TAG = DataSnapEstimoteActivity.class.getSimpleName();
    private static final int REQUEST_ENABLE_BT = 1234;
    private static final Region ALL_ESTIMOTE_BEACONS_REGION = new Region("rid", null, null, null);
    private String advertisingId;
    private boolean advertisingIdOptIn;
    private BeaconManager beaconManager;
    private HashMap<String, String> beaconDictionary;
    private TextView textView;
    private Device device;
    private String[] organizationIds;
    private String[] projectIds;
    private ArrayList<IEvent> eventStore;

    private LocationManager mgr;
    private Location location;
    private LocationListener locationListener;

    private String carrierName;


    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        BeaconStore beaconStore = new BeaconStore(beaconDictionary);
        outState.putParcelable("beaconStore", beaconStore);

    }

    // Estimote Beaconmanager intiliazed & monitors for beacons
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        beaconDictionary = (savedInstanceState != null) ?
                ((BeaconStore) savedInstanceState.getParcelable("beaconStore")).getBeaconStore() : new HashMap<String, String>();

        TelephonyManager manager = (TelephonyManager) getApplicationContext().getSystemService(Context.TELEPHONY_SERVICE);
        carrierName = manager.getNetworkOperatorName();

        DataSnap.initialize(getApplicationContext());
        organizationIds = DataSnap.getOrgIds();
        projectIds = DataSnap.getProjectIds();
        setContentView(R.layout.activity_main);
        getActionBar().setDisplayHomeAsUpEnabled(true);
        new Thread(new Runnable() {
            public void run() {
                try {
                    Info adInfo = AdvertisingIdClient.getAdvertisingIdInfo(getApplicationContext());
                    advertisingId = adInfo.getId();
                    advertisingIdOptIn = adInfo.isLimitAdTrackingEnabled();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }).start();

        beaconManager = new BeaconManager(this);
        textView = (TextView) findViewById(R.id.log_text);
        textView.setMovementMethod(new ScrollingMovementMethod());

        device = getDeviceInfo();
        // create event store
        eventStore = new ArrayList<IEvent>();
        beaconManager.setRangingListener(new BeaconManager.RangingListener() {
            @Override
            public void onBeaconsDiscovered(Region region, final List<com.estimote.sdk.Beacon> beacons) {
                // Note that results are not delivered on UI thread.
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        getActionBar().setSubtitle("Beacons Found: " + beacons.size() + " (ordered by approx dist)");
                        textView.setText("");
                        for (com.estimote.sdk.Beacon beacon : beacons) {
                            appendNewBeaconsToLog(beacon);
                            createBeaconSightingEvent(beacon);
                            if (!beaconDictionary.containsKey(beacon.getMacAddress())) {
                                beaconDictionary.put(beacon.getMacAddress(), beacon.getName());
                                appendNewBeaconsToLog(beacon);
                            }
                        }
                    }
                });
            }
        })
        ;
    }

    /*  Creates an example datasnap event - for more event types see
    http://datasnap-io.github.io/sendingdata/
    */
    public void createBeaconSightingEvent(com.estimote.sdk.Beacon estimoteBeacon) {
        String eventType = "beacon_sighting";
        Beacon beacon = new Beacon();
        User user = getUserInfo();
        beacon.setIdentifier(estimoteBeacon.getProximityUUID() + "" + estimoteBeacon.getMajor() + "" + estimoteBeacon.getMinor());
        beacon.setHardware(estimoteBeacon.getMacAddress());
        beacon.setBatteryLevel("" + estimoteBeacon.getMeasuredPower());
        beacon.setRssi("" + estimoteBeacon.getRssi());
        beacon.setName(estimoteBeacon.getName());
        beacon.setBleVendorId("Estimote");
        DeviceInfo deviceInfo = new DeviceInfo();
        deviceInfo.setCreated(Utils.getTime());
        deviceInfo.setDevice(device);
        IEvent event = new BeaconEvent(eventType, organizationIds, projectIds, beacon, user,
                deviceInfo);
        dispatchEvent(event);
    }


    // optionally switch on sending multiple events simultaneously to the SDK for processing
    public void sendAllEvents() {
        DataSnap.trackEvents(eventStore);


    }

    public void addToEventStore(IEvent event) {
        eventStore.add(event);
        //   dispatchEvent(event);
    }

    public void dispatchEvent(IEvent event) {
        DataSnap.trackEvent(event);
    }

    /*
        outputs event to sample app UI display
    */
    private void appendNewBeaconsToLog(com.estimote.sdk.Beacon beacon) {
        textView.append("Time: " + Utils.getTime() + System.getProperty("line.separator")
                + "Event Type: Beacon Sighting" + System.getProperty("line.separator")
                + "Beacon name: " + beacon.getName()
                + System.getProperty("line.separator") +
                "(mac address:" + beacon.getMacAddress() + ")"
                + System.getProperty("line.separator") + "Result: " + "Dispatched to Datasnap for data analysis"
                + System.getProperty("line.separator")
                + "*******************************" + System.getProperty("line.separator"));
    }

    // optionally use this to turn off beacon sightings for same beacon
    private void appendToSeenLog(com.estimote.sdk.Beacon beacon) {
        textView.append("Beacons currently in range:" + System.getProperty("line.separator")
                + "Beacon name: " + beacon.getName()
                + System.getProperty("line.separator") +
                "(mac address:" + beacon.getMacAddress() + ")"
                + System.getProperty("line.separator") + "This beacon sighting has already been dispatched to Datasnap for data analysis"
                + System.getProperty("line.separator")
                + "*******************************" + System.getProperty("line.separator"));
    }

    private User getUserInfo() {
        User user = new User();
        Id id = new Id();
        String android_id = Settings.Secure.getString(getApplicationContext().getContentResolver(),
                Settings.Secure.ANDROID_ID);
        id.setGlobalDistinctId(android_id);
        id.setMobileDeviceGoogleAdvertisingId(advertisingId);
        id.setMobileDeviceGoogleAdvertisingIdOptIn(""+advertisingIdOptIn);
        user.setId(id);
        return user;
    }

    private Device getDeviceInfo() {
        Device device = new Device();
        device.setIpAddress(getIpAddress());
        device.setPlatform(android.os.Build.VERSION.SDK);
        device.setOsVersion(System.getProperty("os.version"));
        device.setModel(android.os.Build.MODEL);
        device.setManufacturer(android.os.Build.MANUFACTURER);
        device.setName(android.os.Build.DEVICE);
        device.setVendorId(android.os.Build.BRAND);
        device.setCarrierName(carrierName);
        return device;
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

    @Override
    protected void onDestroy() {
        beaconManager.disconnect();
        super.onDestroy();
    }

    @Override
    protected void onStart() {
        super.onStart();

        // Check if device supports Bluetooth Low Energy.
        if (!beaconManager.hasBluetooth()) {
            Toast.makeText(this, "Device does not have Bluetooth Low Energy", Toast.LENGTH_LONG).show();
            return;
        }

        // If Bluetooth is not enabled, let user enable it.
        if (!beaconManager.isBluetoothEnabled()) {
            Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT);
        } else {
            connectToService();
        }
    }

    @Override
    protected void onStop() {
        try {
            beaconManager.stopRanging(ALL_ESTIMOTE_BEACONS_REGION);
        } catch (RemoteException e) {
            Log.d(TAG, "Error while stopping ranging", e);
        }
      super.onStop();
    }

    public String getIpAddress(){
        WifiManager wifiMan = (WifiManager) getApplicationContext().getSystemService(Context.WIFI_SERVICE);
        WifiInfo wifiInf = wifiMan.getConnectionInfo();
        int ipAddress = wifiInf.getIpAddress();
        String ip = String.format("%d.%d.%d.%d", (ipAddress & 0xff),(ipAddress >> 8 & 0xff),(ipAddress >> 16 & 0xff),(ipAddress >> 24 & 0xff));
        return ip;
    }

    private void connectToService() {
        getActionBar().setSubtitle("Scanning...");
        beaconManager.connect(new BeaconManager.ServiceReadyCallback() {
            @Override
            public void onServiceReady() {
                try {
                    beaconManager.startRanging(ALL_ESTIMOTE_BEACONS_REGION);
                } catch (RemoteException e) {
                    Toast.makeText(DataSnapEstimoteActivity.this, "Cannot start ranging, something terrible happened",
                            Toast.LENGTH_LONG).show();
                    Log.e(TAG, "Cannot start ranging", e);
                }
            }
        });
    }

    /*
 * Called when the system detects that this Activity is now visible.
 */
    @Override
    public void onResume() {
        super.onResume();

    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent intent) {

        if (requestCode == REQUEST_ENABLE_BT) {
            if (resultCode == Activity.RESULT_OK) {
                connectToService();
            } else {
                Toast.makeText(this, "Bluetooth not enabled", Toast.LENGTH_LONG).show();
                getActionBar().setSubtitle("Bluetooth not enabled");
            }
            super.onActivityResult(requestCode, resultCode, intent);
        }
    }



}
