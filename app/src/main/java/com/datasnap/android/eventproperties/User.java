package com.datasnap.android.eventproperties;

public class User {

    private Tags tags;
    private Id id;
    private Audience audience;
    private UserProperties userProperties;
    // private boolean optInLocation;
    //  private boolean optInPushNotifications;
    // private boolean optInVendor;

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
	
	
