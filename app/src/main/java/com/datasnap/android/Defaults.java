package com.datasnap.android;

import java.util.concurrent.TimeUnit;

public class Defaults {
  public static final String PACKAGE_NAME = Defaults.class.getPackage().getName();
  public static final boolean DEBUG = false;
  public static final String HOST = "https://api-events.datasnap.io/v1.0/";
  public static final int FLUSH_AT = 20;
  public static final int FLUSH_AFTER = (int) TimeUnit.SECONDS.toMillis(10);
  public static final int MAX_QUEUE_SIZE = 10000;
  // try to send the location by default
  public static final boolean SEND_LOCATION = true;


  public static class Database {
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
          public static final String TYPE = "TEXT";
        }

        public static class AttemptCount {
          public static final String NAME = "attempts";
          public static final String TYPE = "INTEGER";
        }
      }
    }
  }


}
