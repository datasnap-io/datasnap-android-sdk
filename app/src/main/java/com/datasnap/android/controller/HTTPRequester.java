package com.datasnap.android.controller;

import android.util.Log;

import com.datasnap.android.utils.Logger;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.util.EntityUtils;

public class HTTPRequester {

    private static HTTPRequester instance = new HTTPRequester();
    private static int requestCounter;
    private static boolean requestCounterEnabled;
    private static HttpResponse mockedResponse;

    public static HttpResponse getMockedResponse() {
        return mockedResponse;
    }

    public static void setMockedResponse(HttpResponse response) {
        HTTPRequester.mockedResponse = response;
    }

    public static void startRequestCount(){
        requestCounter = 0;
        requestCounterEnabled = true;
    }

    public static void stopRequestCount(){
        requestCounterEnabled = false;
    }

    public static int getRequestCount(){
        return requestCounter;
    }

    public static HttpResponse send(HttpPost httpPost) {
        try {
            HttpClient httpclient = new DefaultHttpClient();
            HttpResponse response;
            if(mockedResponse != null)
                response = mockedResponse;
            else
                response = httpclient.execute(httpPost);
            if(requestCounterEnabled)
                ++requestCounter;
            return response;
        } catch (Exception e) {
            Logger.w(e, "Failed to send request.");
            Log.d("BasicRequester", "Falied ot Send Request" + e);
            return null;
        }
    }
}
