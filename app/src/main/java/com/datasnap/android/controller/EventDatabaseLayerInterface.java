package com.datasnap.android.controller;

import com.datasnap.android.models.EventWrapper;
import com.datasnap.android.utils.IThreadedLayer;

import java.util.List;

/**
 * Handles communication with the database using its own thread to achieve SQL thread safety.
 */
public interface EventDatabaseLayerInterface extends IThreadedLayer {
    //
    // Callbacks
    //

    /**
     * Called when an enqueue completes.
     */
    public interface EnqueueCallback {
        /**
         * Called when an enqueue finishes.
         *
         * @param success  Whether the enqueue was successful.
         * @param rowCount The new database size
         */
        void onEnqueue(boolean success, long rowCount);
    }

    /**
     * Callback for when a database payload query returns
     *
     * @author ivolo
     */
    public interface PayloadCallback {
        void onPayload(long minId, long maxId, List<EventWrapper> payloads);
    }

    /**
     * Callback for when payloads are removed from the database
     *
     * @author ivolo
     */
    public interface RemoveCallback {
        void onRemoved(int removed);
    }

    //
    // Methods
    //

    /**
     * Adds a payload to the database
     */
    void enqueue(EventWrapper payload, EnqueueCallback callback);

    /**
     * Gets the next payloads from the database
     */
    void nextEvent(PayloadCallback callback);

    /**
     * Removes payloads from the database
     */
    void removePayloads(final long minId, long maxId, RemoveCallback callback);
}