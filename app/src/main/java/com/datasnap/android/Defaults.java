package com.datasnap.android;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

public class Defaults {
    public static final String PACKAGE_NAME = Defaults.class.getPackage().getName();

    public static class Database {

        /**
         * Version 1: uses payload.action
         * Version 2: uses payload.type
         */
        public static final int VERSION = 2;

        public static final String NAME = PACKAGE_NAME;

        public static class EventTable {

            public static final String NAME = "event_table";

            public static final String[] FIELD_NAMES = new String[]{
                    Fields.Id.NAME, Fields.Event.NAME
            };

            public static class Fields {

                public static class Id {

                    public static final String NAME = "id";

                    /**
                     * INTEGER PRIMARY KEY AUTOINCREMENT means index is monotonically
                     * increasing, regardless of removals
                     */
                    public static final String TYPE = "INTEGER PRIMARY KEY AUTOINCREMENT";
                }

                public static class Event {

                    public static final String NAME = "event";

                    public static final String TYPE = " TEXT";
                }
            }
        }
    }

    public class SharedPreferences {
        public static final String ANONYMOUS_ID_KEY = "anonymous.id";
        public static final String USER_ID_KEY = "user.id";
        public static final String GROUP_ID_KEY = "group.id";
    }

    public static final boolean DEBUG = false;

    public static final String HOST = "https://api-events.datasnap.io/v1.0/";

    public static final int FLUSH_AT = 20;
    public static final int FLUSH_AFTER = (int) TimeUnit.SECONDS.toMillis(10);

    @SuppressWarnings("serial")
    public static final Map<String, String> ENDPOINTS = new HashMap<String, String>() {
        {
            this.put("trackEvent", "/v1/trackEvent");
            this.put("import", "/v1/import");
        }
    };

    public static String getSettingsEndpoint(String writeKey) {
        return "/project/" + writeKey + "/settings";
    }

    public static final int MAX_QUEUE_SIZE = 10000;

    // cache the settings for 1 hour before reloading
    public static final int SETTINGS_CACHE_EXPIRY = 1000 * 60 * 60;

    // try to send the location by default
    public static final boolean SEND_LOCATION = true;


}
