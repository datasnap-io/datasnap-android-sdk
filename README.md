Datasnap Android SDK
====================
* Includes a sample app that integrates with Estimote beacon hardware
* Example includes a Beacon Sighting Event
* See details about event types here: http://datasnap-io.github.io/sendingdata/
* Events in java pojo format are in the events folders
* The SDK stores events in the local SDK database before flushing to the Datasnap server
* The max size for caching is currently configured to be 10,000 events 
* The DB size will only grow if there is no network connectivity and bluetooth is switched on
* If network connectivity is restored the database will gradually be flushed until empty

Setup
=====
In order to integrate the Datasnap SDK with your application
* Add a datasnap.xml resources file to your project containing the following information (See more details in the sample app project - required fields : datasnap server, apiKey, organizationIds, projectIds):    
```xml  
    <!-- Datasnap Server-->    
        <string name="datasnap_server">https://api-events-staging.datasnap.io/v1.0/events</string>
        <!-- Api Key-->
        <string name="apiKey">K8xBSjg0S1RYWDRDV1hBQUM4VUJQSlRWWjp3ZHBjWWdOR2VheWxGUTBRZ1JKZ3RIaUhSdUZSK2lNR1JrWGVCb      UNSRTNV</string>
        <!-- Organization Ids -->
        <string-array name="organizationIds">
            <item>56xj08dMrRFeaOOler4eYa</item>
        </string-array>
        <!-- Project Ids -->
        <string-array name="projectIds">
            <item>56xj08dMrRFeaOOler4eYa</item>
        </string-array>   
```
* Initialize Datasnap and send an instance of IEvent to it, in order to send events to the datasnap server


