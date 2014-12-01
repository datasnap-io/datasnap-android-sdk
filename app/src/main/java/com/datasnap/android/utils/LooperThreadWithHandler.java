package com.datasnap.android.utils;

import android.os.Handler;
import android.os.Looper;

public class LooperThreadWithHandler extends Thread implements IThreadedLayer {

    private Handler handler;

    private void waitForReady() {
        while (this.handler == null) {
            synchronized (this) {
                try {
                    wait();
                } catch (InterruptedException e) {
                    Logger.e(e, "Failed while waiting for singleton thread ready.");
                }
            }
        }
    }

    @Override
    public void run() {
        Looper.prepare();
        synchronized (this) {
            handler = new Handler();
            notifyAll();
        }
        Looper.loop();
    }

    /**
     * Gets this thread's handler
     */
    public Handler handler() {
        waitForReady();
        return handler;
    }

    /**
     * Quits the current looping thread
     */
    public void quit() {
        Looper.myLooper().quit();
    }
}
