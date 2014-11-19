

package com.datasnap.android.controller;

import android.annotation.SuppressLint;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteException;
import android.database.sqlite.SQLiteOpenHelper;
import android.database.sqlite.SQLiteStatement;
import android.util.Pair;
import com.datasnap.android.DataSnap;
import com.datasnap.android.Defaults;
import com.datasnap.android.models.EventWrapper;
import com.datasnap.android.models.EventWrapperSuper;
import com.datasnap.android.utils.Logger;

import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;

public class EventDatabase extends SQLiteOpenHelper {

  //
  // Singleton
  //

  private static EventDatabase instance;

  public static EventDatabase getInstance(Context context) {
    if (instance == null) {
      instance = new EventDatabase(context);
    }

    return instance;
  }

  //
  // Instance
  //

  /**
   * Caches the count of the database without requiring SQL count to be
   * called every time. This will allow us to quickly determine whether
   * our database is full and we shouldn't add anymore
   */
  private AtomicLong count;
  private boolean initialCount;

  private EventSerializerInterface serializer = new JsonPayloadSerializer();

  private EventDatabase(Context context) {
    super(context, Defaults.Database.NAME, null, Defaults.Database.VERSION);

    this.count = new AtomicLong();
  }

  @Override
  public void onCreate(SQLiteDatabase db) {

    String sql = String.format("CREATE TABLE IF NOT EXISTS %s (%s %s, %s %s);",
        Defaults.Database.EventTable.NAME,  // eventtablename

            Defaults.Database.EventTable.Fields.Id.NAME,   // eventtablefieldsidname
            Defaults.Database.EventTable.Fields.Id.TYPE, // "INTEGER PRIMARY KEY AUTOINCREMENT"

            Defaults.Database.EventTable.Fields.Event.NAME,  // eventdbdataname
            Defaults.Database.EventTable.Fields.Event.TYPE);  // TEXT
    try {
      db.execSQL(sql);
    } catch (SQLException e) {
      Logger.e(e, "Failed to create Segment SQL lite database");
    }
  }

  @Override
  public void onOpen(SQLiteDatabase db) {
    super.onOpen(db);
  }

  /**
   * Counts the size of the current database and sets the cached counter
   *
   * This shouldn't be called onOpen() or onCreate() because it will cause
   * a recursive database get.
   */
  private void ensureCount() {
    if (!initialCount) {
      count.set(countRows());
      initialCount = true;
    }
  }

  @Override
  public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
    // migration from db version, 1->2, we want to drop all the existing items to prevent crashes
    if (oldVersion == 1) removeEvents();
  }

  /**
   * Adds a payload to the database
   */
  public boolean addEvent(EventWrapper payload) {

    ensureCount();

    long rowCount = getRowCount();
    final int maxQueueSize = DataSnap.getOptions().getMaxQueueSize();
    if (rowCount >= maxQueueSize) {
      Logger.w("Cant add action, the database is larger than max queue size (%d).", maxQueueSize);
      return false;
    }

    boolean success = false;

    String json = payload.getEventStr();
    //serializer.serialize(payload);

    synchronized (this) {

      SQLiteDatabase db = null;

      try {

        db = getWritableDatabase();
        ContentValues contentValues = new ContentValues();
          contentValues.put(Defaults.Database.EventTable.Fields.Event.NAME, json);
          long result = db.insert(Defaults.Database.EventTable.NAME, null, contentValues);

          if (result == -1) {
          Logger.w("Database insert failed. Result: %s", result);
        } else {
          success = true;
          // increase the row count
          count.addAndGet(1);
        }
      } catch (SQLiteException e) {
        Logger.e(e, "Failed to open or write to DataSnap Event db");
      } finally {
        if (db != null) db.close();
      }

      return success;
    }
  }

  /**
   * Fetches the total amount of rows in the database
   */
  private long countRows() {

    String sql = String.format("SELECT COUNT(*) FROM %s", Defaults.Database.EventTable.NAME);

    long numberRows = 0;

    SQLiteDatabase db = null;

    synchronized (this) {

      try {
        db = getWritableDatabase();
        SQLiteStatement statement = db.compileStatement(sql);
        numberRows = statement.simpleQueryForLong();
      } catch (SQLiteException e) {
        Logger.e(e, "Failed to ensure row count in the Segment payload db");
      } finally {
        if (db != null) db.close();
      }
    }

    return numberRows;
  }

  /**
   * Fetches the total amount of rows in the database without
   * an actual database query, using a cached counter.
   */
  public long getRowCount() {
    if (!initialCount) ensureCount();
    return count.get();
  }

  /**
   * Get the next (limit) events from the database
   */
  public List<Pair<Long, EventWrapper>> getEvents(int limit) {

    List<Pair<Long, EventWrapper>> result = new LinkedList<Pair<Long, EventWrapper>>();

    SQLiteDatabase db = null;
    Cursor cursor = null;

    synchronized (this) {

      try {

        db = getWritableDatabase();

        String table = Defaults.Database.EventTable.NAME;
        String[] columns = Defaults.Database.EventTable.FIELD_NAMES;
        String selection = null;
        String selectionArgs[] = null;
        String groupBy = null;
        String having = null;
        String orderBy = Defaults.Database.EventTable.Fields.Id.NAME + " ASC";
        String limitBy = "" + limit;

        cursor =
            db.query(table, columns, selection, selectionArgs, groupBy, having, orderBy, limitBy);

        while (cursor.moveToNext()) {
          long id = cursor.getLong(0);
          String json = cursor.getString(1);

          EventWrapper payload = serializer.deserialize(json);

          if (payload != null)
              result.add(new Pair<Long, EventWrapper>(id, payload));
        }
      } catch (SQLiteException e) {
        Logger.e(e, "Failed to open or read from the Segment payload db");
      } finally {
        try {
          if (cursor != null) cursor.close();
          if (db != null) db.close();
        } catch (SQLiteException e) {
          Logger.e(e, "Failed to close db cursor");
        }
      }
    }

    return result;
  }

  /**
   * Remove these events from the database
   */
  @SuppressLint("DefaultLocale")
  public int removeEvents(long minId, long maxId) {
    ensureCount();

    SQLiteDatabase db = null;

    String idFieldName = Defaults.Database.EventTable.Fields.Id.NAME;

    String filter = String.format("%s >= %d AND %s <= %d", idFieldName, minId, idFieldName, maxId);

    int deleted = -1;

    synchronized (this) {
      try {
        db = getWritableDatabase();
        deleted = db.delete(Defaults.Database.EventTable.NAME, filter, null);
        // decrement the row counter
        count.addAndGet(-deleted);
      } catch (SQLiteException e) {
        Logger.e(e, "Failed to remove items from the Segment payload db");
      } finally {
        if (db != null) db.close();
      }
    }

    return deleted;
  }

  /**
   * Remove all events from the database
   */
  @SuppressLint("DefaultLocale")
  public int removeEvents() {
    ensureCount();
    SQLiteDatabase db = null;
    int deleted = -1;

    synchronized (this) {
      try {
        db = getWritableDatabase();
        deleted = db.delete(Defaults.Database.EventTable.NAME, null, null);
        // decrement the row counter
        count.addAndGet(-deleted);
      } catch (SQLiteException e) {
        Logger.e(e, "Failed to remove all items from the Segment payload db");
      } finally {
        if (db != null) db.close();
      }
    }

    Logger.d("Removed all %d event items from the Segment payload db.", deleted);
    return deleted;
  }
}