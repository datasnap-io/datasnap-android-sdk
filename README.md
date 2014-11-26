Datasnap Android SDK
====================
* Includes a sample app that integrates with Estimote beacon hardware
* Example includes a Beacon Sighting Event
* See details about event types here: http://datasnap-io.github.io/sendingdata/
* Events in java pojo format are in the events folders

Setup
=====
In order to integrate the Datasnap SDK with your application
* Add a datasnap.xml resources file to your project containing the following information (See more details in the sample app project - required fields : datasnap server, apiKey, organizationIds, projectIds):    
    ```xml
<string name="datasnap_server">https://api-events.givendatasnapaddress.com</string>     
       <string name="apiKey">QUM4VUJQSlRWWjp3ZHBjWWdOR2VheWxGUTBRZ1JKZ3RIaUhSdUZSK2lNR1JrWGVCbUNSRTNV</string>    
  <string-array name="organizationIds"> <item>56xj08dyrRFeaOOler4eYa</item> </string-array>    
    <string-array name="projectIds"> <item>90xj08dMrJKeaOOler4eYa</item> </string-array>    
```
* Initialize Datasnap and send an instance of IEvent to it, in order to send events to the datasnap server


