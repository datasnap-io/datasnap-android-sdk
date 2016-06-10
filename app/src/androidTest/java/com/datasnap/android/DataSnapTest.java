package com.datasnap.android;

/**
 * Created by paolopelagatti on 5/9/16.
 */

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.wifi.WifiManager;
import android.provider.ContactsContract;
import android.support.test.runner.AndroidJUnit4;
import android.util.Pair;

import com.datasnap.android.controller.EventDatabase;
import com.datasnap.android.controller.EventWrapper;
import com.datasnap.android.controller.HTTPRequester;
import com.datasnap.android.eventproperties.Id;
import com.datasnap.android.eventproperties.User;
import com.datasnap.android.events.BeaconEvent;
import com.datasnap.android.events.Event;
import com.datasnap.android.events.InteractionEvent;
import com.datasnap.android.utils.DsConfig;
import com.datasnap.android.utils.HandlerTimer;
import com.google.gson.FieldNamingPolicy;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import org.apache.http.Header;
import org.apache.http.HeaderIterator;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.ProtocolVersion;
import org.apache.http.StatusLine;
import org.apache.http.params.HttpParams;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.*;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
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
  private Event sampleEvent;

  @Before
  public void setUp() throws Exception {
    wifiManager = (WifiManager) getTargetContext().getSystemService(Context.WIFI_SERVICE);
    //network requests are going to be mocked but in the case of lack of connectivity they won't be even attempted
    wifiManager.setWifiEnabled(true);
    ConnectivityManager connectivityManager
        = (ConnectivityManager) getTargetContext().getSystemService(Context.CONNECTIVITY_SERVICE);
    NetworkInfo activeNetworkInfo = connectivityManager.getActiveNetworkInfo();
    if (activeNetworkInfo == null || !activeNetworkInfo.isConnected()) {
      throw new IllegalStateException("Datasnap tests need to be run on a phone that is connected to the internet.");
    }
    database = EventDatabase.getInstance(getTargetContext());
    database.removeEvents();
    String apiKeyId = "3F34FXD78PCINFR99IYW950W4";
    String apiKeySecret = "KA0HdzrZzNjvUq8OnKQoxaReyUayZY0ckNYoMZURxK8";
    VendorProperties vendorProperties = new VendorProperties();
    vendorProperties.setGimbalApiKey("044e761a-0b9f-4472-b2bb-714625e83574");
    vendorProperties.addVendor(VendorProperties.Vendor.GIMBAL);
    DataSnap.initialize(getTargetContext(), apiKeyId, apiKeySecret, "19CYxNMSQvfnnMf1QS4b3Z", "21213f8b-8341-4ef3-a6b8-ed0f84945186", vendorProperties);
    sampleEvent = getSampleEvent();
  }

  @After
  public void tearDown() throws Exception {
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
    ConnectivityManager connectivityManager
        = (ConnectivityManager) getTargetContext().getSystemService(Context.CONNECTIVITY_SERVICE);
    NetworkInfo activeNetworkInfo = connectivityManager.getActiveNetworkInfo();
    if (activeNetworkInfo != null && activeNetworkInfo.isConnected()) {
      throw new IllegalStateException("This test needs to check Datasnap's behavior with lack of connectivity." +
          "Please make sure to use a device that won't be connected to the internet without using wifi.");
    }
    HTTPRequester.startRequestCount();
    DataSnap.trackEvent(sampleEvent);
    Thread.sleep(DsConfig.getInstance().getFlushAfter() + 3000);
    assertTrue(HTTPRequester.getRequestCount() == 0);
    wifiManager.setWifiEnabled(true);
    Thread.sleep(DsConfig.getInstance().getFlushAfter() + 3000);
    assertTrue(HTTPRequester.getRequestCount() == 1);
  }

  @Test
  public void shouldBatchEvents() throws InterruptedException {
    HTTPRequester.startRequestCount();
    for (int i = 0; i < 2 * DsConfig.getInstance().getFlushAt(); i++) {
      DataSnap.trackEvent(sampleEvent);
    }
    Thread.sleep(3000);
    HTTPRequester.stopRequestCount();
    assertTrue(HTTPRequester.getRequestCount() == 2);
    HTTPRequester.startRequestCount();
    for (int i = 0; i < DsConfig.getInstance().getFlushAt() / 2; i++) {
      DataSnap.trackEvent(sampleEvent);
    }
    Thread.sleep(DsConfig.getInstance().getFlushAfter() + 3000);
    HTTPRequester.stopRequestCount();
    assertTrue(HTTPRequester.getRequestCount() == 1);
  }

  @Test
  public void shouldFlushPeriodically() throws InterruptedException {
    //setting the max number of elements to a large number will make the flushing be driven by time
    //and not by the queue getting filled up
    int currentFlushTime = DsConfig.getInstance().getFlushAfter();
    DataSnap.setFlushParams(2000, 500);
    //make sure the new frequency kicked in:
    Thread.sleep(currentFlushTime);
    setMockedResponse(200);
    HandlerTimer timer = new HandlerTimer(300, eventClock);
    timer.start();
    HTTPRequester.startRequestCount();
    //the runnable will yield 3 events per second:
    Thread.sleep(10000);
    assertTrue(HTTPRequester.getRequestCount() >= 4 && HTTPRequester.getRequestCount() <= 6);
    //if we set also the flushing time to a large number we should see no more requests going out
    DataSnap.setFlushParams(30000, 500);
    //make sure the new frequency kicked in and there are no pending requests before resetting the requests count:
    Thread.sleep(3000);
    HTTPRequester.stopRequestCount();
    HTTPRequester.startRequestCount();
    //verify that stop counts flushes the requests
    assertTrue(HTTPRequester.getRequestCount() == 0);
    Thread.sleep(20000);
    assertTrue(HTTPRequester.getRequestCount() == 0);
    HTTPRequester.stopRequestCount();
  }

  @Test
  public void shouldFlushAutomaticallyIfDataAccumulated() throws InterruptedException {
    DataSnap.setFlushParams(300000, 50);
    GsonBuilder gsonBuilder = new GsonBuilder();
    gsonBuilder.setFieldNamingPolicy(FieldNamingPolicy.LOWER_CASE_WITH_UNDERSCORES);
    Gson gson = gsonBuilder.create();
    String json = gson.toJson(sampleEvent);
    EventWrapper eventWrapper = new EventWrapper(json);
    for (int i = 0; i < 400; i++) {
      database.addEvent(eventWrapper);
    }
    setMockedResponse(200);
    HTTPRequester.startRequestCount();
    DataSnap.trackEvent(sampleEvent);
    Thread.sleep(10000);
    HTTPRequester.stopRequestCount();
    assertTrue(HTTPRequester.getRequestCount() == 9);
    assertTrue(database.getEvents(10).size() == 0);
  }

  @Test
  public void shouldNotRetryToSend400Requests() throws InterruptedException {
    DataSnap.setFlushParams(300000, 50);
    GsonBuilder gsonBuilder = new GsonBuilder();
    gsonBuilder.setFieldNamingPolicy(FieldNamingPolicy.LOWER_CASE_WITH_UNDERSCORES);
    Gson gson = gsonBuilder.create();
    String json = gson.toJson(sampleEvent);
    EventWrapper eventWrapper = new EventWrapper(json);
    for (int i = 0; i < 30; i++) {
      database.addEvent(eventWrapper);
    }
    setMockedResponse(400);
    HTTPRequester.startRequestCount();
    DataSnap.trackEvent(sampleEvent);
    Thread.sleep(10000);
    HTTPRequester.stopRequestCount();
    assertTrue(HTTPRequester.getRequestCount() == 1);
    assertTrue(database.getEvents(10).size() == 0);
  }

  @Test
  public void shouldRetryToSendDifferentErrorRequests() throws InterruptedException {
    DataSnap.setFlushParams(300000, 50);

    GsonBuilder gsonBuilder = new GsonBuilder();
    gsonBuilder.setFieldNamingPolicy(FieldNamingPolicy.LOWER_CASE_WITH_UNDERSCORES);
    Gson gson = gsonBuilder.create();
    String json = gson.toJson(sampleEvent);
    EventWrapper eventWrapper = new EventWrapper(json);
    for (int i = 0; i < 30; i++) {
      database.addEvent(eventWrapper);
    }
    setMockedResponse(404);
    HTTPRequester.startRequestCount();
    DataSnap.trackEvent(sampleEvent);
    Thread.sleep(10000);
    HTTPRequester.stopRequestCount();
    assertTrue(database.getEvents(10).size() == 31);
  }

  private Event getSampleEvent(){
    User user = new User();
    Id id = new Id();
    id.setMobileDeviceGoogleAdvertisingId("sample id");
    id.setMobileDeviceGoogleAdvertisingIdOptIn("true");
    user.setId(id);
    String eventType = "app_installed";
    return new InteractionEvent(eventType, DataSnap.getOrgId(), DataSnap.getProjectId(), null, null, null, user, null);
  }

  private void setMockedResponse(final int statusCode){
    HTTPRequester.setMockedResponse(new HttpResponse() {
      @Override
      public StatusLine getStatusLine() {
        return new StatusLine() {
          @Override
          public ProtocolVersion getProtocolVersion() {
            return null;
          }

          @Override
          public int getStatusCode() {
            return statusCode;
          }

          @Override
          public String getReasonPhrase() {
            return null;
          }
        };
      }

      @Override
      public void setStatusLine(StatusLine statusLine) {

      }

      @Override
      public void setStatusLine(ProtocolVersion protocolVersion, int i) {

      }

      @Override
      public void setStatusLine(ProtocolVersion protocolVersion, int i, String s) {

      }

      @Override
      public void setStatusCode(int i) throws IllegalStateException {

      }

      @Override
      public void setReasonPhrase(String s) throws IllegalStateException {

      }

      @Override
      public HttpEntity getEntity() {
        return new HttpEntity() {
          @Override
          public boolean isRepeatable() {
            return false;
          }

          @Override
          public boolean isChunked() {
            return false;
          }

          @Override
          public long getContentLength() {
            return 0;
          }

          @Override
          public Header getContentType() {
            return null;
          }

          @Override
          public Header getContentEncoding() {
            return null;
          }

          @Override
          public InputStream getContent() throws IOException, IllegalStateException {
            return null;
          }

          @Override
          public void writeTo(OutputStream outputStream) throws IOException {

          }

          @Override
          public boolean isStreaming() {
            return false;
          }

          @Override
          public void consumeContent() throws IOException {

          }
        };
      }

      @Override
      public void setEntity(HttpEntity httpEntity) {

      }

      @Override
      public Locale getLocale() {
        return null;
      }

      @Override
      public void setLocale(Locale locale) {

      }

      @Override
      public ProtocolVersion getProtocolVersion() {
        return null;
      }

      @Override
      public boolean containsHeader(String s) {
        return false;
      }

      @Override
      public Header[] getHeaders(String s) {
        return new Header[0];
      }

      @Override
      public Header getFirstHeader(String s) {
        return null;
      }

      @Override
      public Header getLastHeader(String s) {
        return null;
      }

      @Override
      public Header[] getAllHeaders() {
        return new Header[0];
      }

      @Override
      public void addHeader(Header header) {

      }

      @Override
      public void addHeader(String s, String s1) {

      }

      @Override
      public void setHeader(Header header) {

      }

      @Override
      public void setHeader(String s, String s1) {

      }

      @Override
      public void setHeaders(Header[] headers) {

      }

      @Override
      public void removeHeader(Header header) {

      }

      @Override
      public void removeHeaders(String s) {

      }

      @Override
      public HeaderIterator headerIterator() {
        return null;
      }

      @Override
      public HeaderIterator headerIterator(String s) {
        return null;
      }

      @Override
      public HttpParams getParams() {
        return null;
      }

      @Override
      public void setParams(HttpParams httpParams) {

      }
    });
  }

  protected static Runnable eventClock = new Runnable() {
    @Override
    public void run() {
      User user = new User();
      Id id = new Id();
      id.setMobileDeviceGoogleAdvertisingId("sample id");
      id.setMobileDeviceGoogleAdvertisingIdOptIn("true");
      user.setId(id);
      String eventType = "app_installed";
      Event event = new InteractionEvent(eventType, DataSnap.getOrgId(), DataSnap.getProjectId(), null, null, null, user, null);
      DataSnap.trackEvent(event);
    }
  };

}

