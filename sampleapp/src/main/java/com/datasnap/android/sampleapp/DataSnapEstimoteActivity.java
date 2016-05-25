package com.datasnap.android.sampleapp;

import android.app.Activity;
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

import com.datasnap.android.DataSnap;
import com.datasnap.android.VendorProperties;
import com.datasnap.android.eventproperties.Device;
import com.datasnap.android.events.Event;
import com.estimote.sdk.BeaconManager;
import com.estimote.sdk.Region;
import com.google.android.gms.location.LocationListener;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;


public class DataSnapEstimoteActivity extends Activity {

  private static final String TAG = DataSnapEstimoteActivity.class.getSimpleName();
  private static final int REQUEST_ENABLE_BT = 1234;
  private HashMap<String, String> beaconDictionary;
  private TextView textView;

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
    setContentView(R.layout.activity_main);
    textView = (TextView) findViewById(R.id.log_text);
    textView.setMovementMethod(new ScrollingMovementMethod());
    String apiKeyId = "3F34FXD78PCINFR99IYW950W4";
    String apiKeySecret = "KA0HdzrZzNjvUq8OnKQoxaReyUayZY0ckNYoMZURxK8";
    VendorProperties vendorProperties = new VendorProperties();
    vendorProperties.setVendor(VendorProperties.Vendor.ESTIMOTE);
    DataSnap.initialize(getApplicationContext(), apiKeyId, apiKeySecret, vendorProperties);
    beaconDictionary = (savedInstanceState != null) ?
        ((BeaconStore) savedInstanceState.getParcelable("beaconStore")).getBeaconStore() : new HashMap<String, String>();
  }

  public void onBeaconsDiscovered(Region region, final List<com.estimote.sdk.Beacon> beacons) {
    getActionBar().setSubtitle("Beacons Found: " + beacons.size() + " (ordered by approx dist)");
    textView.setText("");
    for (com.estimote.sdk.Beacon beacon : beacons) {
      appendNewBeaconsToLog(beacon);
      if (!beaconDictionary.containsKey(beacon.getMacAddress())) {
        beaconDictionary.put(beacon.getMacAddress(), beacon.getName());
        appendNewBeaconsToLog(beacon);
      }
    }
  }

  public void onServiceException() {

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
  protected void onStart() {
    super.onStart();

    // Check if device supports Bluetooth Low Energy.
//        if (!beaconManager.hasBluetooth()) {
//            Toast.makeText(this, "Device does not have Bluetooth Low Energy", Toast.LENGTH_LONG).show();
//            return;
//        }
//
//        // If Bluetooth is not enabled, let user enable it.
//        if (!beaconManager.isBluetoothEnabled()) {
//            Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
//            startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT);
//        } else {
//            connectToService();
//        }
  }

  @Override
  protected void onStop() {
//        try {
//            beaconManager.stopRanging(ALL_ESTIMOTE_BEACONS_REGION);
//        } catch (RemoteException e) {
//            Log.d(TAG, "Error while stopping ranging", e);
//        }
    super.onStop();
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
//        connectToService();
      } else {
        Toast.makeText(this, "Bluetooth not enabled", Toast.LENGTH_LONG).show();
        getActionBar().setSubtitle("Bluetooth not enabled");
      }
      super.onActivityResult(requestCode, resultCode, intent);
    }
  }


}
