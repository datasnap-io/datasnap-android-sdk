package com.datasnap.android;

/**
 * Created by paolopelagatti on 5/9/16.
 */
import static android.support.test.InstrumentationRegistry.getTargetContext;

import android.support.test.InstrumentationRegistry;
import android.support.test.runner.AndroidJUnit4;
import android.util.Pair;

import com.datasnap.android.controller.EventDatabase;
import com.datasnap.android.controller.EventWrapper;
import com.datasnap.android.eventproperties.Beacon;
import com.datasnap.android.eventproperties.Device;
import com.datasnap.android.eventproperties.DeviceInfo;
import com.datasnap.android.eventproperties.Id;
import com.datasnap.android.eventproperties.User;
import com.datasnap.android.events.BeaconEvent;
import com.datasnap.android.events.Event;
import com.google.gson.FieldNamingPolicy;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.List;

import static com.datasnap.android.utils.Utils.isNullOrEmpty;
import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;

@RunWith(AndroidJUnit4.class)
public class EventDatabaseTest {

  private EventDatabase database;
  private Beacon beacon1;
  private Beacon beacon2;
  DeviceInfo deviceInfo;
  User user;
  Gson gson;

  @Before
  public void setUp() throws Exception {
    getTargetContext().deleteDatabase(Defaults.Database.NAME);
    database = EventDatabase.getInstance(getTargetContext());
    beacon1 = new Beacon();
    beacon1.setIdentifier("sample identifier 1");
    beacon1.setName("sample name 1");
    beacon2 = new Beacon();
    beacon2.setIdentifier("sample identifier 2");
    beacon2.setName("sample name 2");
    deviceInfo = new DeviceInfo();
    deviceInfo.setDevice(new Device());

    user = new User();
    Id id = new Id();
    String android_id = "sample id";
    id.setGlobalDistinctId(android_id);
    id.setMobileDeviceGoogleAdvertisingId("sample ad id");
    id.setMobileDeviceGoogleAdvertisingIdOptIn("true");
    user.setId(id);
    DataSnap.initialize(InstrumentationRegistry.getContext(), "", "", "", "", null);
    GsonBuilder gsonBuilder = new GsonBuilder();
    gsonBuilder.setFieldNamingPolicy(FieldNamingPolicy.LOWER_CASE_WITH_UNDERSCORES);
    gson = gsonBuilder.create();
  }

  @After
  public void tearDown() throws Exception {
    database.close();
  }

  @Test
  public void shouldAddEvent() throws Exception {
    String eventType = "beacon_sighting";
    Event event = new BeaconEvent(eventType, DataSnap.getOrgId(), DataSnap.getProjectId(), null, null, null, beacon1, user,
        deviceInfo);
    String json = gson.toJson(event);
    EventWrapper eventWrapper = new EventWrapper(json);
    database.addEvent(eventWrapper);
    List<Pair<Long, EventWrapper>> beaconEvents = database.getEvents(10);
    assertThat(beaconEvents.size(), is(1));
  }

  @Test
  public void shouldGetEvents() throws Exception {
    String eventType = "beacon_sighting";
    Event event1 = new BeaconEvent(eventType, DataSnap.getOrgId(), DataSnap.getProjectId(), null, null, null, beacon1, user,
        deviceInfo);
    String json = gson.toJson(event1);
    EventWrapper eventWrapper1 = new EventWrapper(json);
    database.addEvent(eventWrapper1);
    Event event2 = new BeaconEvent(eventType, DataSnap.getOrgId(), DataSnap.getProjectId(), null, null, null, beacon1, user,
        deviceInfo);
    json = gson.toJson(event2);
    EventWrapper eventWrapper2 = new EventWrapper(json);
    database.addEvent(eventWrapper2);
    List<Pair<Long, EventWrapper>> beaconEvents = database.getEvents(10);
    assertThat(beaconEvents.size(), is(2));
    assertThat(beaconEvents.get(0).second.toString(), is(eventWrapper1.toString()));
    assertThat(beaconEvents.get(1).second.toString(), is(eventWrapper2.toString()));
  }

  @Test
  public void shouldRemoveEvents() throws Exception {
    String eventType = "beacon_sighting";
    Event event = new BeaconEvent(eventType, DataSnap.getOrgId(), DataSnap.getProjectId(), null, null, null, beacon1, user,
        deviceInfo);
    String json = gson.toJson(event);
    EventWrapper eventWrapper = new EventWrapper(json);
    database.addEvent(eventWrapper);
    List<Pair<Long, EventWrapper>> beaconEvents = database.getEvents(10);
    assertThat(beaconEvents.size(), is(1));
    database.removeEvents();
    beaconEvents = database.getEvents(10);
    assertThat(beaconEvents.size(), is(0));
  }
}