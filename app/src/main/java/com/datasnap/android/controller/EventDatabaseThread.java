package com.datasnap.android.controller;

import android.os.Handler;
import android.util.Pair;

import com.datasnap.android.Defaults;
import com.datasnap.android.utils.DsConfig;
import com.datasnap.android.utils.LooperThreadWithHandler;

import java.util.LinkedList;
import java.util.List;

/**
 * The DatabaseThread is a singleton handler thread that is
 * statically created to assure a single entry point into the
 * database to achieve SQLLite thread safety.
 */
public class EventDatabaseThread extends LooperThreadWithHandler
    implements EventDatabaseLayerInterface {

  private EventDatabase database;

  public EventDatabaseThread(EventDatabase database) {
    this.database = database;
  }

  public void enqueue(final EventWrapper payload, final EnqueueCallback callback) {
    Handler handler = handler();
    handler.post(new Runnable() {
      @Override
      public void run() {
        boolean success = false;
        try {
          success = database.addEvent(payload);
        } catch (Exception e) {
          e.printStackTrace();
        }
        long rowCount = database.getRowCount();

        if (callback != null) callback.onEnqueue(success, rowCount);
      }
    });
  }

  public void nextEvent(final PayloadCallback callback) {
    Handler handler = handler();
    handler.post(new Runnable() {
      @Override
      public void run() {
        List<Pair<Long, EventWrapper>> pairs = database.getEvents(DsConfig.getInstance().getFlushAt());
        long minId = 0;
        long maxId = 0;

        List<EventWrapper> payloads = new LinkedList<EventWrapper>();
        if (pairs.size() > 0) {
          minId = pairs.get(0).first;
          maxId = pairs.get(pairs.size() - 1).first;
          for (Pair<Long, EventWrapper> pair : pairs) {
            payloads.add(pair.second);
          }
        }
        if (callback != null) callback.onPayload(minId, maxId, payloads);
      }
    });
  }

  public void removePayloads(final long minId, final long maxId, final RemoveCallback callback) {
    Handler handler = handler();
    handler.post(new Runnable() {
      @Override
      public void run() {
        int removed = database.removeEvents(minId, maxId);
        if (callback != null) callback.onRemoved(removed);
      }
    });
  }
}