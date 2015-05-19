package com.datasnap.android.controller;

import com.datasnap.android.utils.IThreadedLayer;

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
    public interface RequestCallback {
        /**
         * Called when a request to the server is completed.
         *
         * @param success True for a successful request, false for not.
         */
        void onRequestCompleted(boolean success);
    }

    //
    // Methods
    //

    /**
     * Send events to the server.
     */
    void send(List<EventWrapper> batch, RequestCallback callback);
}
