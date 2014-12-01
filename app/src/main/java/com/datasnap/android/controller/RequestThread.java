package com.datasnap.android.controller;

import android.os.Handler;
import android.util.Log;

import com.datasnap.android.DataSnap;
import com.datasnap.android.models.EventWrapper;
import com.datasnap.android.utils.DsConfig;
import com.datasnap.android.utils.Logger;
import com.datasnap.android.models.EventListContainer;
import com.datasnap.android.utils.LooperThreadWithHandler;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.LinkedList;

import org.apache.http.HttpResponse;
import org.apache.http.ParseException;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.protocol.HTTP;
import org.apache.http.util.EntityUtils;


/**
 * A Looper/Handler backed request thread
 */
public class RequestThread extends LooperThreadWithHandler implements IRequestLayer {

    private IRequester requester;

    public RequestThread(IRequester requester) {
        this.requester = requester;
    }

    /**
     * Performs the request to the server.
     *
     * @param eventListContainer the action eventListContainer to send
     */
    public void send(final EventListContainer eventListContainer, final RequestCallback callback) {
        Handler handler = handler();

        final DsConfig ds = DataSnap.getDsConfig();
        Log.i("RequestThread.send", "Sending messages to Datasnap" + ds.getApiKey());

        handler.post(new Runnable() {
            @Override
            public void run() {
                long start = System.currentTimeMillis();
                String url = ds.getHost();
                LinkedList<EventWrapper> list = (LinkedList<EventWrapper>) eventListContainer.getBatch();
                HttpPost post = new HttpPost(url);

                for (EventWrapper event : list) {
                    StringEntity se = null;
                    HttpResponse response = null;
                    try {
                        String finalStr = '[' + event.getEventStr() + ']';
                        se = new StringEntity(finalStr, HTTP.UTF_8);
                        se.setContentType("application/json");
                        post.setHeader("Content-Type", "application/json");
                        post.setHeader("Accept", "application/json");
                        post.setHeader("Authorization",
                                "Basic " + ds.getApiKey());
                        post.setEntity(se);
                        HttpClient httpclient = new DefaultHttpClient();
                        response = httpclient.execute(post);
                    } catch (UnsupportedEncodingException e) {
                        e.printStackTrace();
                    } catch (ClientProtocolException e) {
                        e.printStackTrace();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }

                    long duration = System.currentTimeMillis() - start;
                    DataSnap.getStatistics().updateRequestTime(duration);

                    boolean success = false;

                    if (response == null) {
                        // there's been an error
                        Logger.w("Failed to make request to the server.");
                        Log.i("RequestThread.send", "Failed to make request to the server");
                    } else if (response.getStatusLine().getStatusCode() != 200) {
                        try {
                            // there's been a server error
                            Log.i("RequestThread.send", "Failed to make request to the server. " + response.getStatusLine().getStatusCode());
                            Logger.e("Received a failed response from the server. %s",
                                    EntityUtils.toString(response.getEntity()));

                        } catch (ParseException e) {
                            Logger.w(e, "Failed to parse the response from the server.");
                        } catch (IOException e) {
                            Logger.w(e, "Failed to read the response from the server.");
                        }
                    } else {

                        Logger.d("Successfully sent an event to the server");

                        success = true;
                    }

                    if (callback != null) callback.onRequestCompleted(success);
                }
            }
        });
    }


    /**
     * Allow custom {link {@link IRequester} for testing.
     */
    public void setRequester(IRequester requester) {
        this.requester = requester;
    }
}