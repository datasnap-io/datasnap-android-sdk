package com.datasnap.android.controller;

import android.os.Handler;
import android.util.Pair;
import android.util.Range;

import com.datasnap.android.DataSnap;
import com.datasnap.android.utils.DsConfig;
import com.datasnap.android.utils.Logger;
import com.datasnap.android.controller.EventDatabaseLayerInterface.PayloadCallback;
import com.datasnap.android.controller.EventDatabaseLayerInterface.RemoveCallback;
import com.datasnap.android.controller.IRequestLayer.EventRequestCallback;
import com.datasnap.android.stats.AnalyticsStatistics;
import com.datasnap.android.utils.LooperThreadWithHandler;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;

/**
 * A Looper/Handler backed flushing thread
 */
public class FlushThread extends LooperThreadWithHandler implements IFlushLayer {

    /**
     * A factory to create a batch around a list of payload actions
     */
    public interface BatchFactory {
        List<EventWrapper> create(List<EventWrapper> payloads);
    }

    private IRequestLayer requestLayer;
    private EventDatabaseLayerInterface databaseLayer;
    private BatchFactory batchFactory;
    private static ArrayList<Pair<Integer, Integer>> ranges = new ArrayList<>();
    private static boolean trackRanges;

    public FlushThread(EventDatabaseLayerInterface databaseLayer, BatchFactory batchFactory,
                       IRequestLayer requestLayer) {

        this.requestLayer = requestLayer;
        this.batchFactory = batchFactory;
        this.databaseLayer = databaseLayer;
    }

    public static void startTrackingRanges(){
      trackRanges = true;
      ranges = new ArrayList<>();
    }

    public static void stopTrackingRanges(){
      trackRanges = false;
    }

    public static void addRange(Pair range){
      ranges.add(range);
    }

    public static ArrayList<Pair<Integer,Integer>> getRanges(){
      return ranges;
    }

    /**
     * Request a flush of data to the server. This method does not block, but asks for
     * a flush to happen.
     * <p/>
     * A flush consists of several operations - specifically getting messages from the database,
     * making a request to the server, and deleting the sent messages from the local database.
     * <p/>
     * If there's anything left in the queue, another flush will be asked for.
     */
    public void flush(final FlushCallback callback) {
        Handler handler = handler();
        handler.post(new Runnable() {
            @Override
            public void run() {
                // we're on the context of the flushing thread
                // we want to wait until the flush completes
                final CountDownLatch latch = new CountDownLatch(1);
                Logger.d("Starting flush operation ..");
                flushNextEvent(new FlushCallback() {
                    @Override
                    public void onFlushCompleted(boolean success, List<EventWrapper> batch, int statusCode) {
                        if  ((success || statusCode == 400) && EventDatabase.getInstance().getRowCount() > DsConfig.getInstance().getFlushAt()) {
                          // we successfully sent a eventListContainer to the server, and we might
                          // have more to send, so lets trigger another flush
                          if (!DataSnap.networkAvailable) {
                            return;
                          } else {
                            flush(null);
                          }
                        }
                        // we're done with this flush operation
                        latch.countDown();
                        if (success) {
                            Logger.d("Flush op success. [%d items]", batch.size());
                        } else {
                            Logger.w("Flush op failed. [%d items]", batch.size());
                        }
                        if (callback != null)
                            callback.onFlushCompleted(success, batch, statusCode);
                    }
                });

                // okay, lets block until the flush completed. this is important because
                // we don't want two flushes happening simultaneously
                try {
                    latch.await();
                } catch (InterruptedException e) {
                    Logger.w(e, "Failed to wait for flush to finish.");
                }
            }
        });
    }

    /**
     * Flushes the next payload asynchronously.
     */
    private void flushNextEvent(final FlushCallback callback) {
        // ask the database for the next payload batch
        databaseLayer.nextEvent(new PayloadCallback() {
            @Override
            public void onPayload(final long minId, final long maxId, final List<EventWrapper> batch) {

                // we are currently executing on the database
                // thread so we're still technically locking the
                // database
                final String range = "[" + minId + " - " + maxId + "]";
                if(trackRanges) {
                  Integer min = (new Long(minId)).intValue();
                  Integer max = (new Long(maxId)).intValue();
                  ranges.add(new Pair<Integer, Integer>(min, max));
                }
                if (batch.size() == 0) {
                    // there is nothing to flush, we're done
                    if (callback != null)
                        callback.onFlushCompleted(true,  batch, 0);
                } else {
                    Logger.d("Sending events to the servers .. %s", range);
                    // now let's make a request on the flushing thread
                    requestLayer.send(batch, new IRequestLayer.EventRequestCallback() {

                        @Override
                        public void onRequestCompleted(boolean success, final int statusCode) {
                            // we are now executing in the context of the request thread
                            if (!success) {
                                Logger.w("Failed to send events to the servers .. %s", range);
                                if(statusCode == 400) {
                                  databaseLayer.removePayloads(minId, maxId, new RemoveCallback() {
                                    @Override
                                    public void onRemoved(int removed) {
                                      // we are again executing in the context of the database thread
                                      AnalyticsStatistics statistics = AnalyticsStatistics.getInstance();

                                      if (removed == -1) {

                                        for (int i = 0; i < removed; i += 1)
                                          statistics.updateFailed(1);

                                        Logger.e("We failed to remove payload from the database. %s", range);

                                        if (callback != null)
                                          callback.onFlushCompleted(false, batch, statusCode);
                                      } else if (removed == 0) {

                                        for (int i = 0; i < removed; i += 1)
                                          statistics.updateFailed(1);

                                        Logger.e("We didn't end up removing anything from the database. %s", range);

                                        if (callback != null)
                                          callback.onFlushCompleted(false, batch, statusCode);
                                      } else {

                                        for (int i = 0; i < removed; i += 1)
                                          statistics.updateSuccessful(1);

                                        Logger.d("Successfully removed items from the flush db. %s", range);

                                        if (callback != null)
                                          callback.onFlushCompleted(true, batch, statusCode);
                                      }
                                    }
                                  });
                                } else if (callback != null)
                                    callback.onFlushCompleted(false, batch, statusCode);
                            } else {
                                Logger.d("Successfully sent eventListContainer to the server. %s", range);
                                Logger.d("Removing flushed items from the db  .. %s", range);
                                // TODO: remove BF test
                                // if we were successful, we need to first delete the old items from the
                                // database, and then continue flushing
                                databaseLayer.removePayloads(minId, maxId, new RemoveCallback() {

                                    @Override
                                    public void onRemoved(int removed) {
                                        // we are again executing in the context of the database thread
                                        AnalyticsStatistics statistics = AnalyticsStatistics.getInstance();

                                        if (removed == -1) {

                                            for (int i = 0; i < removed; i += 1)
                                                statistics.updateFailed(1);

                                            Logger.e("We failed to remove payload from the database. %s", range);

                                            if (callback != null)
                                                callback.onFlushCompleted(false, batch, statusCode);
                                        } else if (removed == 0) {

                                            for (int i = 0; i < removed; i += 1)
                                                statistics.updateFailed(1);

                                            Logger.e("We didn't end up removing anything from the database. %s", range);

                                            if (callback != null)
                                                callback.onFlushCompleted(false, batch, statusCode);
                                        } else {

                                            for (int i = 0; i < removed; i += 1)
                                                statistics.updateSuccessful(1);

                                            Logger.d("Successfully removed items from the flush db. %s", range);

                                            if (callback != null)
                                                callback.onFlushCompleted(true, batch, statusCode);
                                        }
                                    }
                                });
                            }
                        }
                    });
                }
            }
        });
    }
}