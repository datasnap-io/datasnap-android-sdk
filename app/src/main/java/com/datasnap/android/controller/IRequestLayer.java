package com.datasnap.android.controller;

import com.datasnap.android.utils.IThreadedLayer;
import com.google.gson.JsonObject;

import java.util.List;

/**
 * Handles sending requests to the server end point
 */
public interface IRequestLayer extends IThreadedLayer {

    //
    // Callbacks
    //

    /**
     * Callback for the #flush method
     */
    public interface EventRequestCallback {
        /**
         * Called when a send request to the server is completed.
         *
         * @param success True for a successful request, false for not.
         */
        void onRequestCompleted(boolean success, int statusCode);
    }

    //
    // Methods
    //

    /**
     * Send events to the server.
     */
    void send(List<EventWrapper> batch, EventRequestCallback callback);

}
