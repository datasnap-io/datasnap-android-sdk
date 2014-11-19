package com.datasnap.android.sampleapp;

import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.content.Context;
import android.content.Intent;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.location.LocationManager;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.os.RemoteException;
import android.text.method.ScrollingMovementMethod;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.TextView;
import android.widget.Toast;

import com.datasnap.android.DataSnap;
import com.datasnap.android.eventproperties.Beacon;
import com.datasnap.android.events.BeaconEvent;
import com.datasnap.android.events.IEvent;
import com.datasnap.android.eventproperties.Defaults;
import com.datasnap.android.eventproperties.Device;
import com.datasnap.android.eventproperties.DeviceInfo;
import com.datasnap.android.eventproperties.Place;
import com.datasnap.android.eventproperties.PropId;
import com.datasnap.android.eventproperties.User;
import com.estimote.sdk.BeaconManager;
import com.estimote.sdk.Region;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import com.google.android.gms.location.LocationListener;


public class DataSnapEstimoteActivity extends Activity  {

    private static final String TAG = DataSnapEstimoteActivity.class.getSimpleName();
    private static final int REQUEST_ENABLE_BT = 1234;
    private static final Region ALL_ESTIMOTE_BEACONS_REGION = new Region("rid", null, null, null);
    private Address address;
    private String addressString;
    private BeaconManager beaconManager;
    private HashMap<String, com.estimote.sdk.Beacon> beaconDictionary;
    private TextView textView;
    private Device device;
    private String[] organizationIds = {Defaults.ORGANISATION_ID};
    private String[] projectIds = {Defaults.PROJECT_ID};
    private ArrayList<IEvent> eventStore;
    private LocationManager mgr;
    private Location location;
    private LocationListener locationListener;



    // Estimote Beaconmanager intiliazed & monitors for beacons
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        DataSnap.initialize(getApplicationContext(), Config.API_KEY);  // add config here as well...
        setContentView(R.layout.activity_main);
        getActionBar().setDisplayHomeAsUpEnabled(true);

        // Acquire a reference to the system Location Manager
         mgr = (LocationManager) this.getSystemService(Context.LOCATION_SERVICE);
        // Define a listener that responds to location updates
        location = mgr.getLastKnownLocation(LocationManager.NETWORK_PROVIDER);
        locationListener = new LocationListener() {
            public void onLocationChanged(Location location) {
                float accuracy=location.getAccuracy();
                // Called when a new location is found by the network location provider.
                makeUseOfNewLocation(location);
            }
        };

        addressString =  getAddress(location);
        beaconManager = new BeaconManager(this);
        textView = (TextView) findViewById(R.id.log_text);
        textView.setMovementMethod(new ScrollingMovementMethod());
        beaconDictionary = new HashMap<String, com.estimote.sdk.Beacon>();
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
                        for (com.estimote.sdk.Beacon beacon : beacons) {
                            if (!beaconDictionary.containsKey(beacon.getMacAddress())) {
                                beaconDictionary.put(beacon.getMacAddress(), beacon);
                                appendToLog(beacon);
                                createBeaconSightingEvent();
                            }
                        }
                        getActionBar().setSubtitle("Found beacons: " + beacons.size());
                        sendAllEvents();
                    }
                });
            }
        });
    }

     /*  Creates an example datasnap event - for more event types see
     http://datasnap-io.github.io/sendingdata/
     */
    public void createBeaconSightingEvent(){
        String eventType = "beacon_sighting";
        User user = new User();
        PropId propId = new PropId();
        propId.setMobileDeviceIosIdfa("1a847de9f24b18eee3fac634b833b7887b32dea3");
        propId.setGlobalDistinctId("userid1234");
        user.setId(propId);
        Place majorPlace = new Place();
        majorPlace.setName("major");

        Place minorPlace = new Place();
        minorPlace.setName("major");
       // minorPlace.setAddress();

        Beacon beacon = new Beacon();
        DeviceInfo deviceInfo = new DeviceInfo();
        deviceInfo.setCreated(Utils.getTime());
        deviceInfo.setDevice(device);
        Beacon beacon2 = new Beacon();
        String beaconid2 = "SHDG-test";
        beacon2.setIdentifier(beaconid2);

        // add optional additional properties to an event
        Map<String, Object> additionalProperties = new HashMap<String, Object>();
        additionalProperties.put("beacontest", beacon2);
        additionalProperties.put("beacontest2", beacon2);
        IEvent event = new BeaconEvent(eventType, organizationIds, projectIds, majorPlace, minorPlace, user, beacon,
                deviceInfo, additionalProperties);
        addToEventStore(event);
    }

    public void sendAllEvents(){
        DataSnap.trackEvents(eventStore, null);


    }



    public void addToEventStore(IEvent event){
        eventStore.add(event) ;
     //   dispatchEvent(event);
    }

    public void dispatchEvent(IEvent event){
        DataSnap.trackEvent(event);
    }

/*
    outputs event to sample app UI display
*/
    private void appendToLog(com.estimote.sdk.Beacon beacon){
        textView.append("Time: " +Utils.getTime() + System.getProperty("line.separator")
                + "Event Type: Beacon Sighting" + System.getProperty("line.separator")
                + "Beacon name: " + beacon.getName()
                + System.getProperty("line.separator") +
                "(mac address:" + beacon.getMacAddress() + ")"
                + System.getProperty("line.separator") + "Result: "+ "Dispatched to Datasnap for data analysis"
                + System.getProperty("line.separator")
        + "*******************************" + System.getProperty("line.separator"));
    }

    private Device getDeviceInfo() {
        Device device = new Device();
        //  device.setUserAgent("Mozilla/5.0 (Linux; U; Android 4.0.3; ko-kr; LG-L160L Build/IML74K) AppleWebkit/534.30 (KHTML, like Gecko) Version/4.0 Mobile Safari/534.30");
        device.setIpAddress(getIpAddress());
        device.setPlatform(android.os.Build.VERSION.SDK);
        device.setOsVersion(System.getProperty("os.version"));
        device.setModel(android.os.Build.MODEL);
        device.setManufacturer(android.os.Build.MANUFACTURER);
        device.setName(android.os.Build.DEVICE);
        device.setVendorId("63A7355F-5AF2-4E20-BE55-C3E80D0305B1");
        device.setCarrierName("Verizon");
        device.setCountryCode("1");
        device.setNetworkCode("327");
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

    public void makeUseOfNewLocation(Location location){
        this.location = location;
    }


/*
    optional address resolution using geocoding
*/
    public String getAddress(Location location) {
        Geocoder geocoder = new Geocoder(getApplicationContext(), Locale.getDefault());
        List<Address> addresses = null;
        // Try to get an address for the current location. Catch IO or network problems.
        try {
                /*
                 * Call the synchronous getFromLocation() method with the latitude and
                 * longitude of the current location. Return at most 1 address.
                 */
            addresses = geocoder.getFromLocation(location.getLatitude(),
                    location.getLongitude(), 1
            );

            // Catch network or other I/O problems.
        } catch (IOException exception1) {
            // Log an error and return an error message
            Log.e(Utils.APPTAG, "IO_Exception_getFromLocation");
            // print the stack trace
            exception1.printStackTrace();
            // Return an error message
            return ("IO_Exception_getFromLocation");
            // Catch incorrect latitude or longitude values
        } catch (IllegalArgumentException exception2) {
            // Construct a message containing the invalid arguments
            String errorString =
                    "illegal_argument_exception" +
                            location.getLatitude() +
                            location.getLongitude();

            // Log the error and print the stack trace
            Log.e(Utils.APPTAG, errorString);
            exception2.printStackTrace();
            return errorString;
        }
        // If the reverse geocode returned an address
        if (addresses != null && addresses.size() > 0) {
            // Get the first address
            address = addresses.get(0);
            return address.toString();

        }
        return "nothing";
    }

}