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
import com.datasnap.android.eventproperties.Id;
import com.datasnap.android.eventproperties.User;
import com.datasnap.android.events.BeaconEvent;
import com.datasnap.android.events.Event;
import com.datasnap.android.events.EventType;
import com.datasnap.android.utils.DsConfig;
import com.datasnap.android.utils.Logger;
import com.google.gson.FieldNamingPolicy;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import org.junit.After;
import org.junit.Before;
import org.junit.Ignore;
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

    String apiKeyId = "API KEY";
    String apiKeySecret = "API SECRET";
    Config config = new Config();
    config.context = getTargetContext();
    config.apiKeyId = apiKeyId;
    config.apiKeySecret = apiKeySecret;
    config.organizationId = "MY_ORGANIZATION";
    config.projectId = "MY_PROJECT";
    DataSnap.initialize(config);
    GsonBuilder gsonBuilder = new GsonBuilder();
    gsonBuilder.setFieldNamingPolicy(FieldNamingPolicy.LOWER_CASE_WITH_UNDERSCORES);
    gson = gsonBuilder.create();
    database.removeEvents();
  }

  @After
  public void tearDown() throws Exception {
    database.close();
  }
  
  @Test
  public void shouldAddEvent() throws Exception {
    Event event = new BeaconEvent(EventType.BEACON_SIGHTING, null, beacon1);
    String json = gson.toJson(event);
    EventWrapper eventWrapper = new EventWrapper(json);
    List<Pair<Long, EventWrapper>> beaconEvents = database.getEvents(10);
    assertThat(beaconEvents.size(), is(0));
    database.addEvent(eventWrapper);
    beaconEvents = database.getEvents(10);
    assertThat(beaconEvents.size(), is(1));
  }

  @Test
  public void shouldGetEvents() throws Exception {
    Event event1 = new BeaconEvent(EventType.BEACON_SIGHTING, null, beacon1);
    String json = gson.toJson(event1);
    EventWrapper eventWrapper1 = new EventWrapper(json);
    database.addEvent(eventWrapper1);
    Event event2 = new BeaconEvent(EventType.BEACON_SIGHTING, null, beacon2);
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
    Event event = new BeaconEvent(EventType.BEACON_SIGHTING, null, beacon1);
    String json = gson.toJson(event);
    EventWrapper eventWrapper = new EventWrapper(json);
    database.addEvent(eventWrapper);
    List<Pair<Long, EventWrapper>> beaconEvents = database.getEvents(10);
    assertTrue(beaconEvents.size() == 1);
    database.removeEvents();
    beaconEvents = EventDatabase.getInstance(getTargetContext()).getEvents(10);
    assertTrue(beaconEvents.size() == 0);
  }
}