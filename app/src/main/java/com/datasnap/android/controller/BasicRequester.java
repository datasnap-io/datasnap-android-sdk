package com.datasnap.android.controller;

import android.util.Base64;
import android.util.Log;

import com.datasnap.android.DataSnap;
import com.datasnap.android.utils.Logger;

import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.DefaultHttpClient;

public class BasicRequester implements IRequester {

    @Override
    public HttpResponse send(HttpPost httpPost) {
        try {
            HttpClient httpclient = new DefaultHttpClient();
            HttpResponse response = httpclient.execute(httpPost);

            return response;
        } catch (Exception e) {
            Logger.w(e, "Failed to send request.");
            Log.d("BasicRequester", "Falied ot Send Request" + e);
            return null;
        }
    }


    private String basicAuthHeader() {
        return "Basic " + Base64.encodeToString((DataSnap.getDsConfig().getApiKey() + ":").getBytes(),
                Base64.NO_WRAP);
    }
}
