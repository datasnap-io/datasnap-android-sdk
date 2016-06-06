package com.datasnap.android;

/**
 * Created by paolopelagatti on 5/9/16.
 */

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.wifi.WifiManager;
import android.provider.ContactsContract;
import android.support.test.runner.AndroidJUnit4;
import android.util.Pair;

import com.datasnap.android.controller.EventDatabase;
import com.datasnap.android.controller.EventWrapper;
import com.datasnap.android.controller.HTTPRequester;
import com.datasnap.android.eventproperties.Id;
import com.datasnap.android.eventproperties.User;
import com.datasnap.android.events.Event;
import com.datasnap.android.events.InteractionEvent;
import com.datasnap.android.utils.DsConfig;

import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.*;

import java.io.Serializable;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.TimeUnit;

import static android.support.test.InstrumentationRegistry.getTargetContext;
import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;

@RunWith(AndroidJUnit4.class)
public class DataSnapTest {

  private EventDatabase database;
  private String sampleEventJson = "{\"data_snap_version\":\"1.0.2\",\"event_type\":\"app_installed\",\"organization_ids\":[\"19CYxNMSQvfnnMf1QS4b3Z\"],\"project_ids\":[\"21213f8b-8341-4ef3-a6b8-ed0f84945186\"],\"user\":{\"id\":{\"mobile_device_google_advertising_id\":\"sample id\",\"mobile_device_google_advertising_id_opt_in\":\"true\"}}}";
  private WifiManager wifiManager;

  @Before
  public void setUp() throws Exception {
    wifiManager = (WifiManager) getTargetContext().getSystemService(Context.WIFI_SERVICE);
    wifiManager.setWifiEnabled(true);
    database = EventDatabase.getInstance(getTargetContext());
    database.removeEvents();
    String apiKeyId = "3F34FXD78PCINFR99IYW950W4";
    String apiKeySecret = "KA0HdzrZzNjvUq8OnKQoxaReyUayZY0ckNYoMZURxK8";
    VendorProperties vendorProperties = new VendorProperties();
    vendorProperties.setGimbalApiKey("044e761a-0b9f-4472-b2bb-714625e83574");
    vendorProperties.setVendor(VendorProperties.Vendor.GIMBAL);
    DataSnap.initialize(getTargetContext(), apiKeyId, apiKeySecret, "19CYxNMSQvfnnMf1QS4b3Z", "21213f8b-8341-4ef3-a6b8-ed0f84945186", vendorProperties);
  }

  @After
  public void tearDown() throws Exception {
    database.close();
  }

  @Test
  public void trackEventShouldAddEventsToTheDatabase() throws Exception {
    //wait for organization request to complete..
    Thread.sleep(5000);
    User user = new User();
    Id id = new Id();
    id.setMobileDeviceGoogleAdvertisingId("sample id");
    id.setMobileDeviceGoogleAdvertisingIdOptIn("true");
    user.setId(id);
    String eventType = "app_installed";
    Event event = new InteractionEvent(eventType, DataSnap.getOrgId(), DataSnap.getProjectId(), null, null, null, user, null);
    DataSnap.trackEvent(event);
    Thread.sleep(2000);
    List<Pair<Long, EventWrapper>> events = database.getEvents(10);
    assertTrue(events.get(0).second.toString().equals(sampleEventJson));
  }

  @Test
  public void shouldSendDatabaseEventsToServer() throws InterruptedException {
    Thread.sleep(DsConfig.getInstance().getFlushAfter() + 3000);
    HTTPRequester.startRequestCount();
    EventWrapper eventWrapper = new EventWrapper(sampleEventJson);
    database.addEvent(eventWrapper);
    Thread.sleep(3000);
    assertTrue(HTTPRequester.getRequestCount() == 1);
  }

  @Test
  public void shouldHandleLackOfConnectivity() throws InterruptedException {
    wifiManager.setWifiEnabled(false);
    HTTPRequester.startRequestCount();
    User user = new User();
    Id id = new Id();
    id.setMobileDeviceGoogleAdvertisingId("sample id");
    id.setMobileDeviceGoogleAdvertisingIdOptIn("true");
    user.setId(id);
    String eventType = "app_installed";
    Event event = new InteractionEvent(eventType, DataSnap.getOrgId(), DataSnap.getProjectId(), null, null, null, user, null);
    DataSnap.trackEvent(event);
    Thread.sleep(DsConfig.getInstance().getFlushAfter() + 3000);
    assertTrue(HTTPRequester.getRequestCount() == 0);
    wifiManager.setWifiEnabled(true);
    Thread.sleep(DsConfig.getInstance().getFlushAfter() + 3000);
    assertTrue(HTTPRequester.getRequestCount() == 1);
  }

  @Test
  public void shouldBatchEvents() throws InterruptedException {
    HTTPRequester.startRequestCount();
    User user = new User();
    Id id = new Id();
    id.setMobileDeviceGoogleAdvertisingId("sample id");
    id.setMobileDeviceGoogleAdvertisingIdOptIn("true");
    user.setId(id);
    String eventType = "app_installed";
    Event event = new InteractionEvent(eventType, DataSnap.getOrgId(), DataSnap.getProjectId(), null, null, null, user, null);
    for(int i = 0; i < 2 * DsConfig.getInstance().getFlushAt(); i++){
      DataSnap.trackEvent(event);
    }
    Thread.sleep(3000);
    HTTPRequester.stopRequestCount();
    assertTrue(HTTPRequester.getRequestCount() == 2);
    HTTPRequester.startRequestCount();
    for(int i = 0; i < DsConfig.getInstance().getFlushAt()/2; i++){
      DataSnap.trackEvent(event);
    }
    Thread.sleep(DsConfig.getInstance().getFlushAfter() + 3000);
    HTTPRequester.stopRequestCount();
    assertTrue(HTTPRequester.getRequestCount() == 1);
  }
}