## Datasnap Android SDK
====================
* Includes a sample app that integrates with Estimote beacon hardware
* Example includes a Beacon Sighting Event
* See details about event types here: http://docs.datasnapio.apiary.io/
* Events in java pojo format are in the events folders
* The SDK stores events in the local SDK database before flushing to the Datasnap server
* The max size for caching is currently configured to be 10,000 events 
* The DB size will only grow if there is no network connectivity and bluetooth is switched on
* If network connectivity is restored the database will gradually be flushed until empty

## Setup
=====
In order to integrate the Datasnap SDK with your application
* Add a datasnap.xml resources file to your project containing the following information (See more details in the sample app project - required fields : datasnap server, apiKey, organizationIds, projectIds):    
```xml  
<?xml version="1.0" encoding="utf-8"?>

<resources>
    <!-- Datasnap Server-->
    <string name="datasnap_server">https://api-events.datasnap.io/v1.0/events</string>
    <!-- Api Key-->
    <string name="datasnap_apiKey">MjFBQTNNVFhFRE1XTTJZSEJKMjFRUFQ1Ujo3Wnl0cjdzblFEaGlPM2E5SGwwaUFwZEhZQktUdGVYa05LTGNhQzlTSHcw</string>
    <!-- Organization Ids -->
    <string-array name="datasnap_organizationIds">
        <item>21zzFMqYZitUC6Km4oXLBC</item>
    </string-array>
    <!-- Project Ids -->
    <string-array name="datasnap_projectIds">
        <item>21zzFMqYZitUC6Km4oXLBC</item>
    </string-array>
    <bool name="datasnap_logging">true</bool>

</resources>
```
* Initialize Datasnap and send an instance of IEvent to it, in order to send events to the datasnap server




## Android Quick Start

Integrating Datasnap.io with an Android application? Check out the [Datasnap.io Android Sample App](https://github.com/datasnap-io/datasnap-android-estimote-sample
) to get started, then check back here for more detailed documentation.

## Installation

### Gradle

Android Studio:



### Minimal Sample App

the /sampleapp folder is also included so you can easily run a sample app form within the latest source.



## Permissions This SDK needs

```

<!-- Required for sending events via HTTPS to our REST API. -->
<uses-permission android:name="android.permission.INTERNET"/>
<uses-permission android:name="android.permission.ACCESS_NETWORK_STATE"/>

<!-- Required for dumping events to SD to handle queuing of events as needed. -->
<uses-permission android:name="android.permission.WRITE_EXTERNAL_STORAGE" />

<!-- Allow you to get LOGCAT information when a crash occurs. -->
<uses-permission android:name="android.permission.READ_LOGS"/>

<!-- Allow you to see which activity was active when a crash occurs. -->
<uses-permission android:name="android.permission.GET_TASKS"/>


```

## Data that could be sent to us and what we send automatically


You will notice that our SDK is pretty dumb and does NOT try to itself pull information about the device.

We do however define these OPTIONAL properties in our API and we can easily integrate those analytic type properties into
our reporting but we really try to let the app developer manage hwo to get and set those.

Here is some sample code in our sample app showing how you could make the appropriate android API calls and set our properties:



```
    private User getUserInfo() {
        User user = new User();
        Id id = new Id();
        String android_id = Settings.Secure.getString(getApplicationContext().getContentResolver(),
                Settings.Secure.ANDROID_ID);
        id.setGlobalDistinctId(android_id);
        id.setMobileDeviceGoogleAdvertisingId(advertisingId);
        id.setMobileDeviceGoogleAdvertisingIdOptIn(""+advertisingIdOptIn);
        user.setId(id);
        return user;
    }

    private Device getDeviceInfo() {
        Device device = new Device();
        device.setIpAddress(getIpAddress());
        device.setPlatform(android.os.Build.VERSION.SDK);
        device.setOsVersion(System.getProperty("os.version"));
        device.setModel(android.os.Build.MODEL);
        device.setManufacturer(android.os.Build.MANUFACTURER);
        device.setName(android.os.Build.DEVICE);
        device.setVendorId(android.os.Build.BRAND);
        device.setCarrierName(carrierName);
        return device;
    }

```


## Sample event.

Here is a sample method using some hardcoded values and some live data beinf retrieve via the Estimote SDK.

This method will also send the event to us:

```
 /*  Creates an example datasnap event - for more event types see
    http://datasnap-io.github.io/sendingdata/
    */
    public void createBeaconSightingEvent(com.estimote.sdk.Beacon estimoteBeacon) {
        String eventType = "beacon_sighting";
        Beacon beacon = new Beacon();
        User user = getUserInfo();
        beacon.setIdentifier(estimoteBeacon.getProximityUUID() + "" + estimoteBeacon.getMajor() + "" + estimoteBeacon.getMinor());
        beacon.setHardware(estimoteBeacon.getMacAddress());
        beacon.setBatteryLevel("" + estimoteBeacon.getMeasuredPower());
        beacon.setRssi("" + estimoteBeacon.getRssi());
        beacon.setName(estimoteBeacon.getName());
        beacon.setBleVendorId("Estimote");
        DeviceInfo deviceInfo = new DeviceInfo();
        deviceInfo.setCreated(Utils.getTime());
        deviceInfo.setDevice(device);
        IEvent event = new BeaconEvent(eventType, organizationIds, projectIds, beacon, user,
                deviceInfo);
        dispatchEvent(event);
    }
```



