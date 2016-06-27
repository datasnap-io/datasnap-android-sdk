package com.datasnap.android.sampleapp;

import android.app.Activity;
import android.app.Notification;
import android.os.Bundle;
import android.text.method.ScrollingMovementMethod;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.TextView;
import android.widget.Toast;

import com.datasnap.android.Config;
import com.datasnap.android.DataSnap;
import com.datasnap.android.VendorProperties;
import com.gimbal.android.BeaconEventListener;
import com.gimbal.android.BeaconManager;
import com.gimbal.android.BeaconSighting;
import com.gimbal.android.Gimbal;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;



public class DataSnapGimbalActivity extends Activity {

    private TextView textView;
    private List<String> beaconList = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        textView = (TextView) findViewById(R.id.log_text);
        textView.setMovementMethod(new ScrollingMovementMethod());
        String apiKeyId = "MY_API_KEY";
        String apiKeySecret = "MY_API_SECRET";
        VendorProperties vendorProperties = new VendorProperties();
        vendorProperties.setGimbalApiKey("MY_GIMBAL_API_KEY");
        vendorProperties.addVendor(VendorProperties.Vendor.GIMBAL);
        Config config = new Config();
        config.context = getApplicationContext();
        config.apiKeyId = apiKeyId;
        config.apiKeySecret = apiKeySecret;
        config.organizationId = "MY_ORGANIZATION";
        config.projectId = "MY_PROJECT";
        config.vendorProperties = vendorProperties;
        DataSnap.initialize(config);
        DataSnap.setFlushParams(100000, 50);
        Gimbal.setApiKey(this.getApplication(), "MY_GIMBAL_API_KEY");
        BeaconManager gimbalBeaconManager = new com.gimbal.android.BeaconManager();
        BeaconEventListener gimbalBeaconEventListener = new BeaconEventListener() {
            @Override
            public void onBeaconSighting(BeaconSighting sighting) {
                super.onBeaconSighting(sighting);
                setBeaconListener(sighting.getBeacon());
            }
        };
        gimbalBeaconManager.addListener(gimbalBeaconEventListener);
        gimbalBeaconManager.startListening();
        getActionBar().setDisplayHomeAsUpEnabled(true);
    }

    public void setBeaconListener(com.gimbal.android.Beacon beacon){
        if(textView!=null && !beaconList.contains(beacon.getIdentifier())){
            beaconList.add(beacon.getIdentifier());
            textView.append("Time: " + Utils.getTime() + System.getProperty("line.separator")
                + "Event Type: Beacon Sighting" + System.getProperty("line.separator")
                + "Beacon name: " + beacon.getName()
                + System.getProperty("line.separator") +
                "(mac address:" + beacon.getIdentifier() + ")"
                + System.getProperty("line.separator") + "Result: " + "Dispatched to Datasnap for data analysis"
                + System.getProperty("line.separator")
                + "*******************************" + System.getProperty("line.separator"));
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            finish();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }
}
