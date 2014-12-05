package com.datasnap.android.controller;

import android.os.Handler;

import com.datasnap.android.DataSnap;
import com.datasnap.android.utils.DsConfig;
import com.datasnap.android.utils.Logger;
import com.datasnap.android.utils.LooperThreadWithHandler;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import org.apache.http.HttpResponse;
import org.apache.http.ParseException;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.protocol.HTTP;
import org.apache.http.util.EntityUtils;

/**
 * A Looper/Handler backed request thread
 */
public class RequestThread extends LooperThreadWithHandler implements IRequestLayer {

    private HTTPRequester requester;

    public RequestThread(HTTPRequester requester) {
        this.requester = requester;
    }
    /**
     * Performs the request to the server.
     *
     */
    public void send(final List<EventWrapper> batch, final RequestCallback callback) {
        Handler handler = handler();

        final DsConfig ds = DataSnap.getDsConfig();

        handler.post(new Runnable() {
            @Override
            public void run() {
                long start = System.currentTimeMillis();
                String url = ds.getHost();
                LinkedList<EventWrapper> list = (LinkedList<EventWrapper>) batch;
                ArrayList<String> stringArrayList = new ArrayList<String>();
                StringBuilder builder = new StringBuilder();
                for (EventWrapper event : list) {
                    if (builder.length() != 0) {
                        builder.append(",");
                    }
                    builder.append(event.getEventStr());
                    stringArrayList.add(event.getEventStr());
                }
                builder.insert(0,"[");
                builder.append("]");
                String finalStr = builder.toString();
                HttpPost post = new HttpPost(url);
                StringEntity se = null;
                    HttpResponse response = null;
                    try {
                        se = new StringEntity(finalStr, HTTP.UTF_8);
                        se.setContentType("application/json");
                        post.setHeader("Content-Type", "application/json");
                        post.setHeader("Accept", "application/json");
                        post.setHeader("Authorization",
                                "Basic " + ds.getApiKey());
                        post.setEntity(se);
                        response = HTTPRequester.send(post);
                    } catch (UnsupportedEncodingException e) {
                        e.printStackTrace();
                    }
                    long duration = System.currentTimeMillis() - start;
                    DataSnap.getStatistics().updateRequestTime(duration);
                    boolean success = false;
                    if (response == null) {
                        // there's been an error
                        Logger.w("Failed to make request to the server.");
                        if(!DataSnap.networkAvailable)
                        success = true;
                    } else if (response.getStatusLine().getStatusCode() == 200 || response.getStatusLine().getStatusCode() == 201  ) {
                        Logger.d("Successfully sent events to the server"+list.size());
                        success = true;
                    } else {
                        try {
                            // there's been a server error
                            Logger.e("Received a failed response from the server. %s",
                                    EntityUtils.toString(response.getEntity()));
                            // there's been a server error
                            Logger.e("Received a failed response from the server. %s",
                                    EntityUtils.toString(response.getEntity()));
                        } catch (ParseException e) {
                            Logger.w(e, "Failed to parse the response from the server.");
                        } catch (IOException e) {
                            Logger.w(e, "Failed to read the response from the server.");
                        }
                    }
                    if (callback != null) callback.onRequestCompleted(success);
                }
        });
    }

}