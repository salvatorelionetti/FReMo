package org.giasalfeusi.android.fremo;

import android.os.AsyncTask;

/**
 * Created by salvy on 28/04/17.
 */
class PollingAsyncThread extends AsyncTask<PollingBroadcastReceiver, Void, Void> {
    @Override
    protected Void doInBackground(PollingBroadcastReceiver... params) {
        params[0].step5();

        return null;
    }
}
