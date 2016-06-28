package com.datasnap.android.eventproperties;

import android.content.Context;
import android.provider.Settings;
import com.google.android.gms.ads.identifier.AdvertisingIdClient;
import android.os.Handler;

public class User {

    private Tags tags;
    private Id id;
    private Audience audience;
    private UserProperties userProperties;
    private static User instance;

    public static User getInstance(){
        return instance;
    }

    public static void initialize(Handler handler, Runnable runnable, final Context context){
        instance = new User(handler, runnable, context);
    }

    private User(final Handler handler, final Runnable runnable, final Context context) {
        String android_id = Settings.Secure.getString(context.getContentResolver(),
            Settings.Secure.ANDROID_ID);
        id = new Id();
        id.setGlobalDistinctId(android_id);
        new Thread(new Runnable() {
            public void run() {
                try {
                    AdvertisingIdClient.Info adInfo = AdvertisingIdClient.getAdvertisingIdInfo(context);
                    id.setMobileDeviceGoogleAdvertisingId(adInfo.isLimitAdTrackingEnabled() ? adInfo.getId() : "");
                    id.setMobileDeviceGoogleAdvertisingIdOptIn("" + adInfo.isLimitAdTrackingEnabled());
                    instance.setId(id);
                    handler.post(runnable);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }).start();
    }

    public Tags getTags() {
        return tags;
    }

    public void setTags(Tags tags) {
        this.tags = tags;
    }

    public Id getId() {
        return id;
    }

    public void setId(Id id) {
        this.id = id;
    }

    public Audience getAudience() {
        return audience;
    }

    public void setAudience(Audience audience) {
        this.audience = audience;
    }

    public UserProperties getUserProperties() {
        return userProperties;
    }

    public void setUserProperties(UserProperties userProperties) {
        this.userProperties = userProperties;
    }
    
 /*   public boolean getOptInLocation() {
        return optInLocation;
	}

	public void setOptInLocation(boolean b) {
		this.optInLocation = b;
	}

	public void setOptInPushNotifications(boolean optInPushNotifications) {
		this.optInPushNotifications = optInPushNotifications;
	}

	public boolean getOptInVendor() {
		return optInVendor;
	}

	public void setOptInVendor(boolean optInVendor) {
		this.optInVendor = optInVendor;
	}*/


}
	
	
