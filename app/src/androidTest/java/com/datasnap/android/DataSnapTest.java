package com.datasnap.android;

/**
 * Created by paolopelagatti on 5/9/16.
 */

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

import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.protocol.HTTP;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import static org.mockito.Mockito.*;

import java.io.Serializable;
import java.io.UnsupportedEncodingException;
import java.util.List;
import java.util.concurrent.TimeUnit;

import static android.support.test.InstrumentationRegistry.getTargetContext;
import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;

@RunWith(AndroidJUnit4.class)
public class DataSnapTest {

  private EventDatabase database;
  private String sampleEventJson = "{\"data_snap_version\":\"1.0.2\",\"event_type\":\"app_installed\",\"organization_ids\":[\"19CYxNMSQvfnnMf1QS4b3Z\"],\"project_ids\":[\"MyDatasnap_test-org-052516\"],\"user\":{\"id\":{\"mobile_device_google_advertising_id\":\"sample id\",\"mobile_device_google_advertising_id_opt_in\":\"true\"}}}";

  @Before
  public void setUp() throws Exception {
    database = EventDatabase.getInstance(getTargetContext());
    String apiKeyId = "3F34FXD78PCINFR99IYW950W4";
    String apiKeySecret = "KA0HdzrZzNjvUq8OnKQoxaReyUayZY0ckNYoMZURxK8";
    VendorProperties vendorProperties = new VendorProperties();
    vendorProperties.setGimbalApiKey("044e761a-0b9f-4472-b2bb-714625e83574");
    vendorProperties.setVendor(VendorProperties.Vendor.GIMBAL);
    DataSnap.initialize(getTargetContext(), apiKeyId, apiKeySecret, vendorProperties);
  }

  @After
  public void tearDown() throws Exception {
    database.close();
  }

  @Test
  public void shouldInitializeData() throws Exception {
    //wait for organization request to complete..
    Thread.sleep(5000);
    assertThat(DataSnap.getOrgIds()[0], is("19CYxNMSQvfnnMf1QS4b3Z"));
    assertThat(DataSnap.getProjectIds()[0], is("MyDatasnap_GrioTestVenue"));
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
    Event event = new InteractionEvent(eventType, DataSnap.getOrgIds(), DataSnap.getProjectIds(), null, null, null, user, null);
    DataSnap.trackEvent(event);
    Thread.sleep(2000);
    List<Pair<Long, EventWrapper>> events = database.getEvents(10);
    assertThat(events.get(0).second.toString(), is(sampleEventJson));
  }

  @Test
  public void shouldSendEventToServer(){
    EventWrapper eventWrapper = new EventWrapper(sampleEventJson);
    database.addEvent(eventWrapper);
    StringBuilder builder = new StringBuilder();
    String finalStr = "["+sampleEventJson+"]";
    HttpPost post = new HttpPost(DsConfig.getInstance().getHost());
    StringEntity se = null;
    try {
      se = new StringEntity(finalStr, HTTP.UTF_8);
      se.setContentType("application/json");
      post.setHeader("Content-Type", "application/json");
      post.setHeader("Accept", "application/json");
      post.setHeader("Authorization",
          "Basic " + DsConfig.getInstance().getApiKey());
      post.setEntity(se);
    } catch (UnsupportedEncodingException e) {
      e.printStackTrace();
    }
    HTTPRequester httpRequester = mock(HTTPRequester.class);
    verify(httpRequester, times(1)).send(post);
  }

}