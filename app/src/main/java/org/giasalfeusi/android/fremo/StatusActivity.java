package org.giasalfeusi.android.fremo;

import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothGattCharacteristic;
import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import org.giasalfeusi.android.blen.Orchestrator;
import org.giasalfeusi.android.blen.Rssi;
import org.giasalfeusi.android.blen.Utils;

import java.util.Observable;
import java.util.Observer;

public class StatusActivity extends AppCompatActivity implements Observer
{
    private static final String TAG = "StatusAct";

    private static final int REQUEST_ENABLE_BT = 0;
    private static final int REQUEST_SCAN_BT = 1;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        /*StrictMode.setVmPolicy(new StrictMode.VmPolicy.Builder()
                .detectAll()
                .penaltyLog()
                .penaltyDeath()
                .build());*/
        super.onCreate(savedInstanceState);
        Log.i(TAG, "onCreate");
        /* Required only once. Could be safely called multiple times */
        Orchestrator.singletonInitialize(this);

        ensureBlePresent();
        ensureBleEnabled();

        setContentView(R.layout.activity_status);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        sendBroadcast(new Intent("org.giasalfeusi.android.fremo.gui.started"));
    }

    @Override
    protected void onResume()
    {
        super.onResume();
        Log.i(TAG, String.format("onResume %s", this));
        Orchestrator.singleton().getDeviceHost().addObserver(this);
        /* Device already connected, polling with semaphore */
        Orchestrator.singleton().readRssi(Utils.getPrefString(this, "devAddr"));
        updateT(Orchestrator.singleton().readValue(Utils.getPrefString(this, "devAddr"), PollingBroadcastReceiver.tempUuid));
        nextStep();
    }

    @Override
    protected void onPause()
    {
        super.onPause();
        Log.i(TAG, String.format("onPause %s", this));
        Orchestrator.singleton().getDeviceHost().delObserver(this);
    }

    private void nextStep()
    {

        if (!Orchestrator.singleton().getDeviceHost().isEnabled())
        {
            /* On Marshmallow activity continue running */
            Log.e(TAG, "nextStep: BLE not enabled!");
        }
        else if (!Orchestrator.singleton().isConnected(Utils.getPrefString(this, "devAddr")))
        {
            startActivity(new Intent(this, ScanActivity.class));
        }
    }

    private void ensureBlePresent()
    {
        if (!Orchestrator.singleton().getDeviceHost().hasBluetoothLE(this))
        {
            Toast.makeText(this, "This phone does not support BLE!",
                    Toast.LENGTH_SHORT).show();
            finish();
        }
    }

    private void ensureBleEnabled()
    {
        if (!Orchestrator.singleton().getDeviceHost().isEnabled())
        {
            Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            this.startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT);
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {

        if (requestCode == REQUEST_ENABLE_BT)
        {
            if (resultCode != Activity.RESULT_OK)
            {
                // User don't activate Bluetooth
                finish();
                return;
            }
            else
            {
                /* BT is active */
                sendBroadcast(new Intent("org.giasalfeusi.android.fremo.gui.started"));
                nextStep();
            }
        }
        else if (requestCode == REQUEST_SCAN_BT)
        {
        }
        super.onActivityResult(requestCode, resultCode, data);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.menu_status, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item)
    {
        switch (item.getItemId()) {
            case R.id.action_settings:
                startActivity(new Intent(this, SettingsActivity.class));
                break;
            default:
                break;
        }
        return true;
    }

    private boolean isProvisionedAddress(String mac)
    {
        String  _mac = Utils.getPrefString(this, "devAddr");
        return _mac!=null && _mac.equals(mac);
    }

    public void update(Observable o, Object arg) {
        Log.i(TAG, String.format("update: %s %s %s %s", o.getClass(), arg.getClass(), o, arg));

        final Object _arg = arg;
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                updateUI(_arg);
            }
        });
    }

    public void updateUI(Object arg)
    {
        if (arg instanceof Rssi)
        {
            Rssi rssiObj = (Rssi) arg;

            ProgressBar rssiProgressBar = (ProgressBar) findViewById(R.id.rssiProgressBarId);
            Log.i(TAG, "Updateing RSSI!!!");
            rssiProgressBar.setIndeterminate(false);
            rssiProgressBar.setMax(127);
            rssiProgressBar.setProgress(127+rssiObj.getValue());
        }
        else if (arg instanceof BluetoothGattCharacteristic)
        {
            BluetoothGattCharacteristic gattCh = (BluetoothGattCharacteristic) arg;
            if (gattCh.getUuid().toString().equals(PollingBroadcastReceiver.tempUuid))
            {
                updateT(gattCh.getValue());
            }
        }
    }

    private void updateT(byte [] T_obj_bytes) {
        /* Temperature read */
        String text = "Not available";
        TextView tempTextView = (TextView) findViewById(R.id.tempTextViewId);

        if (T_obj_bytes != null)
        {
            float T_obj = ((T_obj_bytes[1] << 8) + T_obj_bytes[0]) / 128.0f;
            Log.i(TAG, String.format("Updateing Temperature object %f!!!", T_obj));
            text = String.format("%.2f °C", T_obj);
        }

        tempTextView.setText(text);
        tempTextView.invalidate();
    }
}
