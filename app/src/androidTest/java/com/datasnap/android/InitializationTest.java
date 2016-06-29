package com.datasnap.android;

import android.content.Context;
import android.content.SharedPreferences;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.wifi.WifiManager;
import android.preference.PreferenceManager;
import android.provider.Settings;
import android.support.test.runner.AndroidJUnit4;

import com.datasnap.android.controller.EventDatabase;
import com.datasnap.android.eventproperties.Id;
import com.datasnap.android.eventproperties.User;
import com.datasnap.android.events.Event;
import com.datasnap.android.events.EventType;
import com.datasnap.android.events.InteractionEvent;
import com.datasnap.android.utils.DsConfig;

import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;

import static android.support.test.InstrumentationRegistry.getTargetContext;
import static org.junit.Assert.assertTrue;

/**
 * Created by paolopelagatti on 6/10/16.
 */
@RunWith(AndroidJUnit4.class)
public class InitializationTest {

  private EventDatabase database;
  private WifiManager wifiManager;

  //verifies that initialization goes through fine without connectivity
  @Test
  public void shouldNotCrashIfNotConnected() throws InterruptedException {
    wifiManager = (WifiManager) getTargetContext().getSystemService(Context.WIFI_SERVICE);
    //network requests are going to be mocked but in the case of lack of connectivity they won't be even attempted
    wifiManager.setWifiEnabled(false);
    Thread.sleep(4000);
    ConnectivityManager connectivityManager
        = (ConnectivityManager) getTargetContext().getSystemService(Context.CONNECTIVITY_SERVICE);
    NetworkInfo activeNetworkInfo = connectivityManager.getActiveNetworkInfo();
    if (activeNetworkInfo != null && activeNetworkInfo.isConnected()) {
      // WARNING: this ends the test because it is not possible to remove connectivity on the emulator
      // used on Circle CI, but still we don't want the build to fail for that reason. It should still
      // be run locally though.
      return;
    }
    database = EventDatabase.getInstance(getTargetContext());
    database.removeEvents();
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
    DataSnap.initialize(getTargetContext(), config);
    DataSnap.trackEvent(getSampleEvent());
  }

  //verifies that initialization sets up the shared preferences correctly
  @Test
  public void shouldSetUpSharedPreferencesCorrectly() {
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
    DataSnap.initialize(getTargetContext(), config);
    SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(getTargetContext());
    assertTrue(sharedPreferences.getBoolean(EventType.COMMUNICATION_OPEN.name(), true));
    assertTrue(sharedPreferences.getBoolean(EventType.COMMUNICATION_SENT.name(), true));
    assertTrue(sharedPreferences.getBoolean(EventType.BEACON_SIGHTING.name(), true));
    assertTrue(sharedPreferences.getBoolean(EventType.GEOFENCE_DEPART.name(), true));
  }

  private Event getSampleEvent() {
    return new InteractionEvent(EventType.BEACON_SIGHTING);
  }
}
