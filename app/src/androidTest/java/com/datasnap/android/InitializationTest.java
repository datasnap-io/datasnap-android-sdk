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
  public void shouldNotCrashIfNotConnected() {
    wifiManager = (WifiManager) getTargetContext().getSystemService(Context.WIFI_SERVICE);
    //network requests are going to be mocked but in the case of lack of connectivity they won't be even attempted
    wifiManager.setWifiEnabled(false);
    ConnectivityManager connectivityManager
        = (ConnectivityManager) getTargetContext().getSystemService(Context.CONNECTIVITY_SERVICE);
    NetworkInfo activeNetworkInfo = connectivityManager.getActiveNetworkInfo();
    if (activeNetworkInfo != null && activeNetworkInfo.isConnected()) {
      throw new IllegalStateException("This test needs to test airplane mode. Please make sure your device is disconnected from the internet.");
    }
    database = EventDatabase.getInstance(getTargetContext());
    database.removeEvents();
    String apiKeyId = "3F34FXD78PCINFR99IYW950W4";
    String apiKeySecret = "KA0HdzrZzNjvUq8OnKQoxaReyUayZY0ckNYoMZURxK8";
    VendorProperties vendorProperties = new VendorProperties();
    vendorProperties.setGimbalApiKey("044e761a-0b9f-4472-b2bb-714625e83574");
    vendorProperties.addVendor(VendorProperties.Vendor.GIMBAL);
    DataSnap.initialize(getTargetContext(), apiKeyId, apiKeySecret, "19CYxNMSQvfnnMf1QS4b3Z", "21213f8b-8341-4ef3-a6b8-ed0f84945186", vendorProperties);
    DataSnap.trackEvent(getSampleEvent());
  }

  //verifies that initialization sets up the shared preferences correctly
  @Test
  public void shouldSetUpSharedPreferencesCorrectly() {
    String apiKeyId = "3F34FXD78PCINFR99IYW950W4";
    String apiKeySecret = "KA0HdzrZzNjvUq8OnKQoxaReyUayZY0ckNYoMZURxK8";
    VendorProperties vendorProperties = new VendorProperties();
    vendorProperties.setGimbalApiKey("044e761a-0b9f-4472-b2bb-714625e83574");
    vendorProperties.addVendor(VendorProperties.Vendor.GIMBAL);
    DataSnap.initialize(getTargetContext(), apiKeyId, apiKeySecret, "19CYxNMSQvfnnMf1QS4b3Z", "21213f8b-8341-4ef3-a6b8-ed0f84945186", vendorProperties);
    SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(getTargetContext());
    assertTrue(sharedPreferences.getBoolean(EventType.COMMUNICATION_OPEN, true));
    assertTrue(sharedPreferences.getBoolean(EventType.COMMUNICATION_SENT, true));
    assertTrue(sharedPreferences.getBoolean(EventType.BEACON_SIGHTING, true));
    assertTrue(sharedPreferences.getBoolean(EventType.GEOFENCE_DEPART, true));
  }

  private Event getSampleEvent(){
    User user = new User();
    Id id = new Id();
    id.setMobileDeviceGoogleAdvertisingId("sample id");
    id.setMobileDeviceGoogleAdvertisingIdOptIn("true");
    user.setId(id);
    return new InteractionEvent(EventType.BEACON_SIGHTING, DataSnap.getOrgId(), DataSnap.getProjectId(), null, null, null, user, null, null);
  }
}
