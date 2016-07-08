# Intro
The Datasnap.io SDK is designed to make integrating your location-based app with the Datasnap analytics platform simple and painless. 
The simplest way to integrate is to include our library and write a single line of code, as detailed below. Your app should then track location events with no further configuration.
However, if you want to track events programmatically, that can be done as well. 
The SDK supports Android versions back to 16.

# Version
The current version is [2.0.0](releases/datasnapsdk-2.0.0.zip).  It simplifies the  process of integrating with third-party libraries and it now sends certain user information by default (see What Data is Sent by Default, below).   If your privacy consent with end users does not allow sharing this data with Neustar, you will need to override the default setting (see Identifying User Data, below)

### Version Archive
- [Version 1.0.2](releases/datasnapsdk-1.0.2.zip)
  - [Documentation](wiki/Documentation-for-Version-1.0.2)
- [Version 1.0.1](releases/datasnapsdk-1.0.1.zip)


# Setup / integration

#### Getting Started
- Download the latest SDK release here (link to zip)
- Uncompress Datasnap.framework-*.zip wherever you like to keep libraries

#### Generic Integration
This section illustrates the basic Datasnap integration. It does not include a specific vendor implementation. Therefore it omits the vendorProperties object which is used to configure and activate a specific vendor’s implementation. This integration would be used when you don’t want to use any of the supplied 3rd-party integrations; for example you might choose to capture all events yourself and send them manually to Datasnap (see “Sending Events Programatically”)

```java
String apiKeyId = "3F34FXD78NOINDS99IYW950W4”;                        // Your Datasnap API Key ID
String apiKeySecret = "KA0HdzrZzNjvPn8OnKQoxaReyUayZY0ckNYoMZURxK8";  // Your Datasnap API Key Secret
String organizationId = "19CYxNMSQvfnnKl1QS4b3Z";                     // Your Datasnap Organization ID
String projectId = "21864f8b-8341-4ef3-a6b8-ed0f8abc5186";            // Datasnap-Issued Project ID (you may have several)
Config config = new Config.Builder()
    .setApiKeyId(apiKeyId)
    .setApiKeySecret(apiKeySecret)
    .setOrganizationId(organizationId)
    .setProjectId(projectId)
    .build();
DataSnap.initialize(getApplicationContext(), config);
```

#### Vendor Integration (Gimbal example)
Most often you will allow the datasnap  SDK to automatically integrate with third-party beacon manufacturers’ libraries.  At the moment, only Gimbal is supported in this way.

If you have already integrated the vendor code with your own app, there is nothing further to install.

If you’re starting from scratch, however, you’ll need to install the [vendor SDK](https://docs.gimbal.com/android/v2/devguide.html) as well.

```java
VendorProperties vendorProperties = new VendorProperties();
vendorProperties.setGimbalApiKey("044e761a-0b9f-4472-b2aa-714625e83574");
vendorProperties.setVendor(VendorProperties.Vendor.GIMBAL);
Config config = new Config.Builder()
    .setApiKeyId(apiKeyId)
    .setApiKeySecret(apiKeySecret)
    .setOrganizationId(organizationId)
    .setProjectId(projectId)
    .setVendorProperties(vendorProperties)
    .build();
DataSnap.initialize(getApplicationContext(), config);
```

### Sending Events Programatically
Instead of allowing the Datasnap SDK to automatically send events, you can send them manually.  You might do this for testing purposes or if you needed to modify the event content in some way.

```java
Beacon beacon = new Beacon();

DeviceInfo deviceInfo = new DeviceInfo();
deviceInfo.setCreated(getTime());
Device device = new Device();
device.setIpAddress(getIpAddress());
device.setPlatform(android.os.Build.VERSION.SDK);
device.setOsVersion(System.getProperty("os.version"));
device.setModel(android.os.Build.MODEL);
device.setManufacturer(android.os.Build.MANUFACTURER);
device.setName(android.os.Build.DEVICE);
device.setVendorId(android.os.Build.BRAND);
TelephonyManager manager = (TelephonyManager) getApplicationContext().getSystemService(Context.TELEPHONY_SERVICE);
device.setCarrierName(manager.getNetworkOperatorName());
deviceInfo.setDevice(device);

User user = new User();
Id id = new Id();
id.setMobileDeviceGoogleAdvertisingId("sample id");
id.setMobileDeviceGoogleAdvertisingIdOptIn("true");
user.setId(id);

Event event = new BeaconEvent(
                        BeaconEventType.BEACON_SIGHTING,
                        DataSnap.getOrgId(),
                        DataSnap.getProjectId(),
                        null,
                        null,
                        null,
                        beacon,
                        user,
                        deviceInfo
                  );
DataSnap.trackEvent(event);
```

### Custom Events
It is possible to create and send custom-defined events.  At the moment, Datasnap does not do anything with these events, but does record them, so that if they are needed for calculations in the future they will be available.

```java

DeviceInfo deviceInfo = new DeviceInfo();
Device device = new Device();
deviceInfo.setCreated(getTime());
device.setIpAddress(getIpAddress());
device.setPlatform(android.os.Build.VERSION.SDK);
device.setOsVersion(System.getProperty("os.version"));
device.setModel(android.os.Build.MODEL);
device.setManufacturer(android.os.Build.MANUFACTURER);
device.setName(android.os.Build.DEVICE);
device.setVendorId(android.os.Build.BRAND);
TelephonyManager manager = (TelephonyManager) getSystemService(Context.TELEPHONY_SERVICE);
device.setCarrierName(manager.getNetworkOperatorName());
deviceInfo.setDevice(device);
DeviceInfo.initialize(deviceInfo);

Event event = new Event() {
};
event.setEventType(“my_custom_event_with_a_special_name”);
```


## Configuration

It is possible to configure which events are sent to the server.  Why would you want to do this? If you wanted to send events directly from your code, perform some preprocessing on them, or completely stop sending a certain type of event or even stop sending all events.

```java
DataSnap.setEventEnabled([String but where do we get the constant val], false);
```

The SDK doesn’t send events to the server as soon as they occur. To avoid making thousands of requests and to save battery life, events are stored in a buffer and only sent out when the buffer reaches a particular size or when a time threshold is met.  Set the size and time thresholds with *setFlushParams*

```java
DataSnap.setFlushParams(durationInMillis, maxElements);
```

## What Data is Sent by Default
By default, the SDK collects a variety of data about the device to aid in statistical analysis.

- IP address
- Android SDK version
- OS version
- Device model
- Device manufacturer
- Device name
- Device brand
- Carrier name
- Google Advertiser ID (dependent on user settings)

## Identifying User Data (Google Ad Id/email)
The Datasnap analytics platform can be more useful when events are linked to a unique user identifier.  We can use either the Google Ad Id or the user's email to uniquely identify that user.

The user can disable the use of the Google Ad Id by turning on “Opt out of interest-based ads" in Settings >> Accounts >> Google >> Ads >> Opt out of interest-based ads.  You, the developer, can also stop the Google Ad Id from being sent to the Datasnap servers by using `
setAllowGoogleAdId(false)` as you are building the Config object for initialization.  ex.:
```
Config config = new Config.Builder()
    .setAllowGoogleAdId(false)
    .build();
```

We will only use an identifier, whether email or Google Ad Id, as a means to tie together sequences of events.

# Sample App

There are two sample apps to show how Datasnap should be used in the sampleapp folder, DataSnapAllEventsActivity and DataSnapCustomEventActivity. In order to run a sample app make sure the activity you choose is the correct one being launched in the sampleapp/AndroidManifest file.

The first activity simply aims to show how to construct all the available Datasnap events. It has a list of buttons and each will trigger a Datasnap event upon click, e.g.:

```java
beaconSighting.setOnClickListener(new View.OnClickListener() {
    @Override
    public void onClick(View v) {
        Beacon beacon = new Beacon();
        beacon.setIdentifier("sample-identifier");
        beacon.setBatteryLevel("high");
        beacon.setRssi("sample rssi");
        beacon.setName("sample identifier");
        beacon.setBleVendorId("Gimbal");
        Event event = new BeaconEvent(EventType.BEACON_SIGHTING, DataSnap.getOrgId(), DataSnap.getProjectId(), null, null, null, null, user,
                beacon, deviceInfo, null);
        DataSnap.trackEvent(event);
    }
});

```

The second one shows how to create custom events in spite of datasnap's automatic events, creating a custom beacon sighting event.
A custom event is a class that extends the Event class like below:


```java
public class MyCustomBeaconEvent extends Event {
private String myCustomField;
private Beacon beacon;

public MyCustomBeaconEvent(String eventType, String organizationId,
                                                String projectId, String customerOrgId, String customerVenueOrgId, String venueOrgId, User user, String myCustomField,
                                                DeviceInfo deviceInfo, Beacon beacon, Map<String, Object> additionalProperties) {
        super(eventType, organizationId, projectId, customerOrgId, customerVenueOrgId, venueOrgId, user, deviceInfo, additionalProperties);
        this.myCustomField = myCustomField;
        this.beacon = beacon;
    }
}

```

In the example, the user creates the event to be sent in spite of the regular Datasnap beacon sighting event because he wants to add the information “myCustomField” which would be missing. So, along with creating the new event type he would have to:
send the new event types when a beacon sighting is detected:

```java
gimbalBeaconEventListener = new BeaconEventListener() {
    @Override
    public void onBeaconSighting(BeaconSighting sighting) {
        super.onBeaconSighting(sighting);
        String eventType = "my_custom_event_type";
        Beacon beacon = new Beacon();
        beacon.setIdentifier(sighting.getBeacon().getIdentifier());
        beacon.setBatteryLevel(sighting.getBeacon().getBatteryLevel().toString());
        beacon.setRssi(sighting.getBeacon().getUuid());
        beacon.setName(sighting.getBeacon().getName());
        beacon.setBleVendorId("Gimbal");
        Event event = new MyCustomBeaconEvent(eventType, DataSnap.getOrgId(), DataSnap.getProjectId(), null, null, null, User.getInstance(),
                "custom value", DeviceInfo.getInstance(), beacon, null);
        DataSnap.trackEvent(event);
    }
};

```

To turn off the automatically sent Datasnap beacon sightings so that the server won’t receive two versions of the same event:

`DataSnap.setEventEnabled(EventListener.GIMBAL_BEACON_SIGHTING, false);`

In order to show that the new event is working and is sending information over to the network a text view is used to track the amount of network requests made. Then two buttons are added to switch on and off the Datasnap regular event and the custom event, so that the user can see that turning the Datasnap event off actually stops the network communication and then turning the custom event on starts back the communication sending custom events.

Notice that the sampleapp gradle file shows how to include datasnap as a jar, but if you want to compile directly the app package and debug it you can do that by uncommenting the two lines:

```
// compile project(':app')
// compile fileTree(dir: 'libs', include: ['*.jar'], exclude: ['datasnap.jar'])
```

## Datasnap.io Backend 

#### Event API

http://docs.datasnapio.apiary.io/#

#### Status Page
We offer the ability to check on our server status at anytime. Also if you are a client and we have issued you an API key then that means that we will email you of any downtime as soon as it occurs:

http://status.datasnap.io/

## License

The Datasnap SDK is available under the Apache 2 license. See the LICENSE file for more info.
