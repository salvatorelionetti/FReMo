package org.giasalfeusi.android.fremo;

import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.widget.VideoView;

import org.giasalfeusi.android.blen.Orchestrator;
import org.giasalfeusi.android.blen.Utils;

import java.util.Observable;
import java.util.Observer;

public class ScanActivity extends AppCompatActivity implements Observer {

    static private final String TAG = "ScanAct";
    static private final int REQUEST_ENABLE_BT = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_scan);

        Log.i(TAG, String.format("onCreate %s", this));

        final VideoView videoView = (VideoView) findViewById(R.id.videoView);

        videoView.requestFocus();
        //we also set an setOnPreparedListener in order to know when the video file is ready for playback
        videoView.setOnPreparedListener(new MediaPlayer.OnPreparedListener() {
            public void onPrepared(MediaPlayer mediaPlayer) {
                //if we have a position on savedInstanceState, the video playback should start from here
                mediaPlayer.setLooping(true);
                videoView.start();
            }
        });
        videoView.setVideoURI(Uri.parse("android.resource://" + getPackageName() + "/" + R.raw.make_disco));
    }

    @Override
    protected void onResume()
    {
        super.onResume();
        Log.i(TAG, String.format("onResume %s", this));
        Orchestrator.singleton().getDeviceHost().addObserver(this);
        nextStep();
    }

    @Override
    protected void onPause()
    {
        super.onPause();
        Log.i(TAG, String.format("onPause %s", this));
        nextStep();
        Orchestrator.singleton().getDeviceHost().delObserver(this);
    }

    private void nextStep()
    {
        if (Orchestrator.singleton().isConnected(Utils.getPrefString(this, "devAddr")))
        {
            finish();
        }
    }

    public void update(Observable o, Object arg)
    {
        if (arg instanceof Integer)
        {
            Log.i(TAG, String.format("update: Device connection state now %d", arg));
            /* BroadReceiver Connects only with recognized devices */
            nextStep();
        }
    }
}