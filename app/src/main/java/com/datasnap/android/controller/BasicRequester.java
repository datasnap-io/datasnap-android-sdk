package com.datasnap.android.controller;

import android.util.Base64;

import com.datasnap.android.models.EventWrapper;
import com.datasnap.android.utils.ConfigOptions;
import com.datasnap.android.DataSnap;
import com.datasnap.android.utils.Logger;
import com.datasnap.android.models.EventListContainer;

import java.util.LinkedList;

import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.ResponseHandler;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.BasicResponseHandler;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.message.BasicHeader;
import org.apache.http.protocol.HTTP;

public class BasicRequester implements IRequester {

  @Override
  public HttpResponse send(HttpPost httpPost) {
      try {
          HttpClient httpclient = new DefaultHttpClient();
          HttpResponse response = httpclient.execute(httpPost);
          return response;
      } catch (Exception e) {
          Logger.w(e, "Failed to send request.");
          return null;
      }
  }


  private String basicAuthHeader() {
    return "Basic " + Base64.encodeToString((DataSnap.getApiKey() + ":").getBytes(),
        Base64.NO_WRAP);
  }
}
