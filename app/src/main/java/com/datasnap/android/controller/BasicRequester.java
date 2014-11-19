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
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.message.BasicHeader;
import org.apache.http.protocol.HTTP;

public class BasicRequester implements IRequester {

  @Override
  public HttpResponse send(EventListContainer eventListContainer) {
   // eventListContainer.setSentAt(Calendar.getInstance());

    ConfigOptions options = DataSnap.getConfigOptions();

  // String url = options.getHost() + Def.ENDPOINTS.get("import");
   String url = "https://private-anon-f5491adb9-datasnapio.apiary-mock.com/v1.0/events/?api_key=$E9NZuB6A91e2J03PKA2g7wx0629czel8&data=$%2520WERF%2520&redirect=$http%3A%2F%2Fwww.apple.com" ;
      LinkedList<EventWrapper> list = (LinkedList<EventWrapper>) eventListContainer.getBatch();
      EventWrapper bp = list.getFirst();
      String event =       bp.getEventStr();

    HttpClient httpclient = new DefaultHttpClient();
    HttpPost post = new HttpPost(url);
    post.setHeader("Content-Type", "application/json");
    post.addHeader("Authorization",
              "Basic MUVNNTNIVDg1OTdDQzdRNVFQMFU4RE43MzpDY2R1eWFrUnNaOEFRL0hMZFhFUjJFanNDT2xmMjlDVEZWay9CY3RGbVFN");
    try {
      StringEntity se = new StringEntity(event);
      se.setContentEncoding(new BasicHeader(HTTP.CONTENT_TYPE, "application/json"));
      post.setEntity(se);
      return httpclient.execute(post);
    } catch (Exception e) {
      Logger.w(e, "Failed to send request.");
    }

    return null;
  }



  private String basicAuthHeader() {
    return "Basic " + Base64.encodeToString((DataSnap.getApiKey() + ":").getBytes(),
        Base64.NO_WRAP);
  }
}
