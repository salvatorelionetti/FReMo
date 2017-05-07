package org.giasalfeusi.android.fremo;


import android.app.IntentService;
import android.content.Context;
import android.content.Intent;
import android.support.v4.content.WakefulBroadcastReceiver;
import android.util.Log;

import org.giasalfeusi.android.blen.Utils;

/**
 * Created by salvy on 28/03/17.
 */

public class PollingIntentService extends IntentService {
    final static private String TAG = "PollIntSrv";
    // Must create a default constructor
    public PollingIntentService() {
        // Used to name the worker thread, important only for debugging.
        super("PollingIntentService");
        Log.i(TAG, "Constructor");
    }

    @Override
    public void onCreate() {
        super.onCreate(); // if you override onCreate(), make sure to call super().
        // If a Context object is needed, call getApplicationContext() here.
        Log.i(TAG, "onCreate");
    }

    @Override
    protected void onHandleIntent(Intent intent) {
        WakefulBroadcastReceiver.completeWakefulIntent(intent);
        // This describes what will happen when service is triggered
        Log.i(TAG, String.format("onHandleIntent %s, extra %s", intent, Utils.describeIntentExtra(intent)));
    }

    private void notifyMeasure(Context context, String measure)
    {
        //
    }
}
