package com.datasnap.android.controller;

import android.os.Handler;
import com.datasnap.android.DataSnap;
import com.datasnap.android.utils.Logger;
import com.datasnap.android.models.EventListContainer;
import com.datasnap.android.utils.LooperThreadWithHandler;
import java.io.IOException;
import org.apache.http.HttpResponse;
import org.apache.http.ParseException;
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

    handler.post(new Runnable() {
      @Override
      public void run() {
        long start = System.currentTimeMillis();
        // send the actual request
        HttpResponse response = requester.send(eventListContainer);
        long duration = System.currentTimeMillis() - start;
        DataSnap.getStatistics().updateRequestTime(duration);

        boolean success = false;

        if (response == null) {
          // there's been an error
          Logger.w("Failed to make request to the server.");
        } else if (response.getStatusLine().getStatusCode() != 200) {
          try {
            // there's been a server error
            Logger.e("Received a failed response from the server. %s",
                EntityUtils.toString(response.getEntity()));
          } catch (ParseException e) {
            Logger.w(e, "Failed to parse the response from the server.");
          } catch (IOException e) {
            Logger.w(e, "Failed to read the response from the server.");
          }
        } else {

          Logger.d("Successfully sent a eventListContainer to the server");

          success = true;
        }

        if (callback != null) callback.onRequestCompleted(success);
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