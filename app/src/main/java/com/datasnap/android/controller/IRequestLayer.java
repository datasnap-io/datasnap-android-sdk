package com.datasnap.android.controller;

import com.datasnap.android.models.EventListContainer;
import com.datasnap.android.utils.IThreadedLayer;

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
     * Send an action eventListContainer to the server.
     */
    void send(EventListContainer eventListContainer, RequestCallback callback);
}
