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

    public interface OrganizationRequestCallback {
        /**
         * Called when an organization request to the server is completed.
         *
         * @param response
         */
        void onRequestCompleted(String response);
    }

    //
    // Methods
    //

    /**
     * Send events to the server.
     */
    void send(List<EventWrapper> batch, EventRequestCallback callback);

    void getOrganization(OrganizationRequestCallback callback);
}
