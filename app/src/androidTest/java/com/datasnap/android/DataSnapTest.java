package com.datasnap.android;

/**
 * Created by paolopelagatti on 5/9/16.
 */

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.provider.ContactsContract;
import android.provider.Settings;
import android.support.test.runner.AndroidJUnit4;
import android.telephony.TelephonyManager;
import android.util.Pair;

import com.datasnap.android.controller.EventDatabase;
import com.datasnap.android.controller.EventWrapper;
import com.datasnap.android.controller.FlushThread;
import com.datasnap.android.controller.HTTPRequester;
import com.datasnap.android.eventproperties.Beacon;
import com.datasnap.android.eventproperties.Device;
import com.datasnap.android.eventproperties.DeviceInfo;
import com.datasnap.android.eventproperties.Id;
import com.datasnap.android.eventproperties.User;
import com.datasnap.android.events.BeaconEvent;
import com.datasnap.android.events.Event;
import com.datasnap.android.events.EventType;
import com.datasnap.android.events.InteractionEvent;
import com.datasnap.android.utils.DsConfig;
import com.datasnap.android.utils.HandlerTimer;
import com.datasnap.android.utils.Logger;
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
import org.junit.Ignore;
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
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.TimeUnit;

import static android.support.test.InstrumentationRegistry.getTargetContext;
import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;

@RunWith(AndroidJUnit4.class)
public class DataSnapTest {

  private EventDatabase database;
  private String sampleEventJson = "{\"beacon\":{\"battery_level\":\"high\",\"ble_vendor_id\":\"Gimbal\",\"identifier\":\"sample-identifier\",\"name\":\"sample identifier\",\"rssi\":\"sample rssi\"},\"datasnap\":{\"created\":\"created date\",\"device\":{\"carrier_name\":\"sample carrier name\",\"ip_address\":\"sample ip\",\"model\":\"model\",\"os_version\":\"os.version\",\"platform\":\"sample platform\"},\"version\":\"sample version\"},\"event_type\":\"beacon_sighting\",\"organization_ids\":[\"ORGANIZATION ID\"],\"project_ids\":[\"PROJECT ID\"],\"user\":{\"id\":{\"global_distinct_id\":\"sample android id\",\"mobile_device_google_advertising_id\":\"sample id\",\"mobile_device_google_advertising_id_opt_in\":\"true\"}}}";
  private WifiManager wifiManager;
  private Event sampleEvent;

  @Before
  public void setUp() throws Exception {
    wifiManager = (WifiManager) getTargetContext().getSystemService(Context.WIFI_SERVICE);
    //network requests are going to be mocked but in the case of lack of connectivity they won't be even attempted
    wifiManager.setWifiEnabled(true);
    Thread.sleep(4000);
    ConnectivityManager connectivityManager
        = (ConnectivityManager) getTargetContext().getSystemService(Context.CONNECTIVITY_SERVICE);
    NetworkInfo activeNetworkInfo = connectivityManager.getActiveNetworkInfo();
    if (activeNetworkInfo == null || !activeNetworkInfo.isConnected()) {
      throw new IllegalStateException("Datasnap tests need to be run on a phone that is connected to the internet.");
    }
    database = EventDatabase.getInstance(getTargetContext());
    database.removeEvents();
    String apiKeyId = "API KEY";
    String apiKeySecret = "API SECRET";
    VendorProperties vendorProperties = new VendorProperties();
    vendorProperties.addVendor(VendorProperties.Vendor.GIMBAL);
    DataSnap.initialize(getTargetContext(), apiKeyId, apiKeySecret, "ORGANIZATION ID", "PROJECT ID", vendorProperties);
    sampleEvent = getSampleEvent();
  }

  @After
  public void tearDown() throws Exception {
    DataSnap.close();
  }

  //verifies that tracking an event adds the correct value to the database
  @Test
  public void trackEventShouldAddEventsToTheDatabase() throws Exception {
    final User user = new User();
    final Id id = new Id();
    DeviceInfo deviceInfo = new DeviceInfo();
    Device device = new Device();
    id.setGlobalDistinctId("sample android id");
    device.setIpAddress("sample ip");
    device.setPlatform("sample platform");
    device.setOsVersion("os.version");
    device.setModel("model");
    device.setCarrierName("sample carrier name");
    deviceInfo.setDevice(device);
    id.setMobileDeviceGoogleAdvertisingId("sample id");
    id.setMobileDeviceGoogleAdvertisingIdOptIn("true");
    user.setId(id);
    Beacon beacon = new Beacon();
    beacon.setIdentifier("sample-identifier");
    beacon.setBatteryLevel("high");
    beacon.setRssi("sample rssi");
    beacon.setName("sample identifier");
    beacon.setBleVendorId("Gimbal");
    Event event = new BeaconEvent(EventType.BEACON_SIGHTING, DsConfig.getInstance().getOrgId(), DsConfig.getInstance().getProjectId(), null, null, null, null, user,
        beacon, deviceInfo, null);
    event.getDatasnap().setVersion("sample version");
    event.getDatasnap().setCreated("created date");
    final ConnectivityManager connectivityManager = Mockito.mock( ConnectivityManager.class );
    final NetworkInfo networkInfo = Mockito.mock( NetworkInfo.class );
    Mockito.when( connectivityManager.getActiveNetworkInfo()).thenReturn( networkInfo );
    Mockito.when( networkInfo.isConnected() ).thenReturn( false );
    DataSnap.trackEvent(event);
    Thread.sleep(1000);
    List<Pair<Long, EventWrapper>> events = database.getEvents(10);
    assertTrue(events.get(0).second.toString().equals(sampleEventJson));
  }

  //verifies that adding an event to the database will ultimately trigger a network call to the server
  @Test
  public void shouldSendDatabaseEventsToServer() throws Exception {
    long wait = DsConfig.getInstance().getFlushAfter();
    DataSnap.setFlushParams(1000, 1);
    Thread.sleep(wait);
    setMockedResponse(200);
    //wait for the new settings to be active and start monitoring the communication with the server:
    Thread.sleep(DsConfig.getInstance().getFlushAfter());
    HTTPRequester.startRequestCount();
    EventWrapper eventWrapper = new EventWrapper(sampleEventJson);
    database.addEvent(eventWrapper);
    Thread.sleep(2000);
    assertTrue(HTTPRequester.getRequestCount() == 1);
  }

  //verifies that if an event is tracked and the connectivity is lacking, the event is being sent as
  //soon as the connectivity gets back
  @Test
  public void shouldHandleLackOfConnectivity() throws InterruptedException {
    DataSnap.setFlushParams(2000, 10);
    wifiManager.setWifiEnabled(false);
    Thread.sleep(1000);
    ConnectivityManager connectivityManager
        = (ConnectivityManager) getTargetContext().getSystemService(Context.CONNECTIVITY_SERVICE);
    NetworkInfo activeNetworkInfo = connectivityManager.getActiveNetworkInfo();
    if (activeNetworkInfo != null && activeNetworkInfo.isConnected()) {
      // WARNING: this ends the test because it is not possible to remove connectivity on the emulator
      // used on Circle CI, but still we don't want the build to fail for that reason. It should still
      // be run locally though.
      return;
    }
    HTTPRequester.startRequestCount();
    DataSnap.trackEvent(sampleEvent);
    //we need to wait long enough in order to prove that requests weren't attempted at all and it wouldn't
    //be a matter of waiting
    Thread.sleep(DsConfig.getInstance().getFlushAfter() + 3000);
    assertTrue(HTTPRequester.getRequestCount() == 0);
    wifiManager.setWifiEnabled(true);
    int timeSlots = 0;
    while (activeNetworkInfo == null || !activeNetworkInfo.isConnected()) {
      if(timeSlots > 10)
        throw new IllegalStateException("Datasnap tests need to be run on a phone that is connected to the internet.");
      Thread.sleep(3000);
      activeNetworkInfo = connectivityManager.getActiveNetworkInfo();
      timeSlots++;
    }
    Thread.sleep(DsConfig.getInstance().getFlushAfter());
    assertTrue(HTTPRequester.getRequestCount() == 1);
  }

  //verifies that if an event is tracked and the connectivity is lacking, the event is being sent as
  //soon as the connectivity gets back. This test however doesn't check on the actual connectivity, but
  //bases itself on a mock of the connectivity manager. This test is needed in order to be run on Circle CI
  //where the previous test can't be run.
  @Test
  public void shouldHandleLackOfConnectivityMockingNetworkInfo() throws InterruptedException {
    DataSnap.setFlushParams(2000, 10);
    Thread.sleep(1000);
    final ConnectivityManager connectivityManager = Mockito.mock( ConnectivityManager.class );
    final NetworkInfo networkInfo = Mockito.mock( NetworkInfo.class );
    Mockito.when( connectivityManager.getActiveNetworkInfo()).thenReturn( networkInfo );
    Mockito.when( networkInfo.isConnected() ).thenReturn( false );
    HTTPRequester.startRequestCount();
    DataSnap.trackEvent(sampleEvent);
    //we need to wait long enough in order to prove that requests weren't attempted at all and it wouldn't
    //be a matter of waiting
    Thread.sleep(DsConfig.getInstance().getFlushAfter() + 3000);
    assertTrue(HTTPRequester.getRequestCount() == 0);
    Mockito.when( networkInfo.isConnected() ).thenReturn( true );
    Thread.sleep(DsConfig.getInstance().getFlushAfter() + 10000);
    assertTrue(HTTPRequester.getRequestCount() == 1);
  }

  //verifies that the time flush parameter works properly
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
    //we can't be exactly sure of the amount of requests being sent as that also depends on the overhead
    //time to submit them, but we can assert on their approximate number:
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
    timer.quit();
  }

  //when data accumulates offline it gets flushed automatically without waiting for the flushing timer
  //to trigger. This test verifies that requests are enqueued properly, don't send the same items
  //and do send all items
  @Test
  public void shouldFlushAutomaticallyIfDataAccumulated() throws Exception {
    //setting the time to a large number will make the flushing be driven by the queue
    //and not by the queue getting filled up
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
    FlushThread.startTrackingRanges();
    DataSnap.trackEvent(sampleEvent);
    Thread.sleep(20000);
    HTTPRequester.stopRequestCount();
    FlushThread.stopTrackingRanges();
    int last = 0;
    for(Pair<Integer, Integer> range: FlushThread.getRanges()){
      if(last > 0)
        assertTrue(range.first == last + 1);

      if(!FlushThread.getRanges().get(FlushThread.getRanges().size() - 1).equals(range)) {
//        throw new Exception("range first is: " + range.second + " and rage second is " + range.second + " and count is "+ );
        assertTrue(range.second == range.first + DsConfig.getInstance().getFlushAt() - 1);
      }
      last = range.second;
    }
    //exactly 9 requests were sent:
    assertTrue(HTTPRequester.getRequestCount() == 9);
    //all items were sent:
    assertTrue(database.getEvents(10).size() == 0);
  }

  //verifies that requests with bad data are not reattempted clogging the stream of data going to the
  //server
  @Test
  public void shouldNotRetryToSend400Requests() throws Exception {
    DataSnap.setFlushParams(5000, 50);
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

  //verifies that if the server is down or there is an error that is not related to the data the
  //queue of events is not purged
  @Test
  public void shouldRetryToSendDifferentErrorRequests() throws Exception {
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
    assertTrue(database.getEvents(10).size() > 5);
  }

  private Event getSampleEvent(){
    User user = new User();
    Id id = new Id();
    id.setMobileDeviceGoogleAdvertisingId("sample id");
    id.setMobileDeviceGoogleAdvertisingIdOptIn("true");
    user.setId(id);
    return new InteractionEvent(EventType.BEACON_SIGHTING, DsConfig.getInstance().getOrgId(), DsConfig.getInstance().getProjectId(), null, null, null, user, null, null);
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
      Event event = new InteractionEvent(EventType.OPT_IN_VENDOR, DsConfig.getInstance().getOrgId(), DsConfig.getInstance().getProjectId(), null, null, null, user, null, null);
      DataSnap.trackEvent(event);
    }
  };
}

