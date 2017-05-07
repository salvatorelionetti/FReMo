package org.giasalfeusi.android.fremo;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattService;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.SystemClock;
import android.util.Log;

import org.giasalfeusi.android.blen.Orchestrator;
import org.giasalfeusi.android.blen.ScanFailed;
import org.giasalfeusi.android.blen.Utils;

import java.util.ArrayList;
import java.util.List;
import java.util.Observable;
import java.util.Observer;

/**
 * Created by salvy on 26/03/17.
 */

public class PollingBroadcastReceiver extends /*Wakeful*/ BroadcastReceiver implements Observer {
    static final private String TAG = "PollBroadRx";
    private Context appContext = null;
    static BluetoothDevice blueDev;
    static private List<String> measures = new ArrayList<String>();
    static private int state = 0;
    static private final int STATE_NONE = 0;
    static private final int STATE_INITIALIZED = 1;
    static private final int STATE_BLUETOOTH_ENABLED = 2;
    static private final int STATE_SCANNING = 3;
    static private final int STATE_CONFIGURING = 4;
    static private final int STATE_POLLING = 5;

    static private boolean alarmStarted = false;

    static public final String confUuid = "f000aa02-0451-4000-b000-000000000000";
    static public final String tempUuid = "f000aa01-0451-4000-b000-000000000000";

    /* Pointer to PollBroadRec everytime changes */
    static private int pollCnt = 0;
    static private int consecutiveZeroValue = 0;
    static private int maxConsecutiveZeroValue = 20; // Measured 12

    static private PendingResult asyncResult = null;
    static private AsyncTask<PollingBroadcastReceiver, Void, Void> pollingAsyncThread = null;

    static private byte[] lastObjT = null;

    @Override
    public void onReceive(Context context, Intent intent)
    {
        if (appContext == null)
        {
            appContext = context.getApplicationContext();
        }

        Log.i(TAG, String.format("onReceive context %s %s %s", context, intent, Utils.describeIntentExtra(intent)));

        if (state == STATE_NONE)
        {
            Orchestrator.singletonInitialize(appContext);

            /* This is a static properties, so verified once */
            if (!Orchestrator.singleton().getDeviceHost().hasBluetoothLE(appContext)) {
                Log.e(TAG, "BLE not supported! Exiting");
                return;
            }

            state = STATE_INITIALIZED;
        }

        if (Orchestrator.singleton().getDeviceHost().isEnabled())
        {
            if (state == STATE_INITIALIZED)
            {
                state = STATE_BLUETOOTH_ENABLED;
            }
        }
        else
        {
            state = STATE_INITIALIZED;
            Log.e(TAG, "BLE not enabled!");
        }

        if (state == STATE_BLUETOOTH_ENABLED)
        {
            // Initial scanning
            Log.e(TAG, "onReceive startScan!!!");
            Orchestrator.singleton().getDeviceHost().addObserver(this);
            Orchestrator.singleton().startScan();
            state = STATE_SCANNING;
        }

        Log.i(TAG, String.format("onReceive schedule nextTick %d", Utils.getPrefLong(appContext, "nextTick")));

        /* Check we have a device to connect with */
        if (Utils.getPrefString(appContext, "devAddr") == null)
        {
            Log.e(TAG, "Device not yet found!");
            return;
        }

        if (state != STATE_POLLING)
        {
            return;
        }

        if (isAlarmIntent(intent))
        {
            setNextAlarm(appContext);
            startAsync();
        }
    }

    public void update(Observable o, Object arg)
    {
        Log.i(TAG, String.format("update: %s %s %s %s", o.getClass(), arg.getClass(), o, arg));

        if (arg instanceof BluetoothDevice)
        {
            BluetoothDevice bd = (BluetoothDevice) arg;

//            Log.i(TAG, String.format("update: Found new blueDev %s", blueDev.getName()));
            if (bd.getName()!=null && bd.getName().equals("CC2650 SensorTag"))
            {
                blueDev = bd;
                Utils.setPref(appContext, "devAddr", blueDev.getAddress());
                Log.i(TAG, String.format("update: Connecting!!!"));
                Orchestrator.singleton().stopScan();
                step1();
            }
        }
        else if (arg instanceof ScanFailed)
        {
            ScanFailed scanFailed = (ScanFailed) arg;
            Log.i(TAG, String.format("update: scanning failed with error is %d", scanFailed.getErrorCode()));
        }
        else if (arg instanceof BluetoothGattService)
        {
            BluetoothGattService gattService = (BluetoothGattService) arg;

            for (BluetoothGattCharacteristic gattChar : gattService.getCharacteristics())
            {
                String uuid = gattChar.getUuid().toString();
                if (gattChar.getUuid().toString().equals(confUuid))
                {
                    step2();
                }
            }
        }
        else if (arg instanceof BluetoothGattCharacteristic)
        {
            /* Read / Write a Characteristic */
            BluetoothGattCharacteristic gattCh = (BluetoothGattCharacteristic) arg;

            if (gattCh.getUuid().toString().equals(confUuid))
            {
                step34(gattCh);
            }
            else if (gattCh.getUuid().toString().equals(tempUuid))
            {
                step6(gattCh);
            }
        }
/*        else
        {
            Log.i(TAG, String.format("update: Unknown object %s", arg));
        }*/
    }

    private void step1()
    {
        // 1) Connect to the device
        Log.i(TAG, "1) Connecting to device");
        Orchestrator.singleton().connectToDevice(appContext, blueDev);
    }

    private void step2()
    {
        /* 2) Read the desired descriptor */
        Log.i(TAG, "2) Reading conf");
        Orchestrator.singleton().readCharacteristic(blueDev, confUuid);
    }

    private void step34(BluetoothGattCharacteristic gattCh)
    {
        byte[] val = gattCh.getValue();
        if (val[0] != 1) {
            val[0] = 1;
            state = STATE_CONFIGURING;
            Log.d(TAG, String.format("3) Writing conf %s -> %s", Utils.bytesToHex(gattCh.getValue()), Utils.bytesToHex(val)));
            Orchestrator.singleton().writeCharacteristic(blueDev, gattCh, val);
        } else {
            Log.i(TAG, "4) Start T polling ");
            pollCnt = 0;
            state = STATE_POLLING;
            Orchestrator.singleton().readCharacteristic(blueDev, tempUuid);
        }
    }

    public void step5()
    {
        pollCnt++;
//        state = STATE_POLLING;
        Log.i(TAG, String.format("4) Polling T %d %s ", pollCnt, this));
        Orchestrator.singleton().readCharacteristic(blueDev, tempUuid);
    }

    private void step6(BluetoothGattCharacteristic gattCh)
    {
        /* First T seem to be 0 */
        if ((gattCh.getValue()[0] == 0) && (gattCh.getValue()[1] == 0))
        {
            if (consecutiveZeroValue > maxConsecutiveZeroValue)
            {
                /* Stop reading, wait next 1min alarm */
                Log.e(TAG, String.format("step6: stop polling after reading %d consecutive zero value %d", maxConsecutiveZeroValue));
            }
            else
            {
                /* Start new read */
                consecutiveZeroValue++;
                Log.i(TAG, "step6: starting new read since value is 0");
                Orchestrator.singleton().readCharacteristic(blueDev, tempUuid);
            }
        }
        else
        {
            /* First non zero T value */
            String val = Utils.bytesToHex(gattCh.getValue());
            Log.i(TAG, String.format("step6: sendVal %s after %d 0 values", val, consecutiveZeroValue));
            consecutiveZeroValue = 0;
            lastObjT = gattCh.getValue();
            measures.add(val);

            if (Utils.getPrefBool(appContext, "thingspeak_switch"))
            {
                //String api_key = "RWHKOZVDLK7G5B6P";
                String api_key = Utils.getPrefString(appContext, "thingspeak_key");

                if (api_key.length()>0)
                {
                    new HttpReq().notifyMeasure(measures, api_key);
                    measures = new ArrayList<String>();
                }
            }

            if (!alarmStarted)
            {
                alarmStarted = true;
                setNextAlarm(appContext);
            }

            stopAsync();
        }
    }

    static public byte[] getLstObjT()
    {
        return lastObjT;
    }

    private boolean isBootIntent(Intent intent) {
        boolean ret = false;
        if (intent.getAction() != null && intent.getAction().equals("android.intent.action.BOOT_COMPLETED")) {
            ret = true;
        }
        return ret;
    }

    private boolean isAlarmIntent(Intent intent)
    {
        boolean ret = false;
        if (intent.hasExtra("android.intent.extra.ALARM_TARGET_TIME")) {
            ret = true;
        }
        return ret;
    }

    public void setNextAlarm(Context context) {
        AlarmManager alarmMgr;
        Intent intent;
        PendingIntent alarmIntent;
        long nextTick;

        intent = new Intent(context, PollingBroadcastReceiver.class);
        alarmIntent = PendingIntent.getBroadcast(context, 0, intent, 0);
        alarmMgr = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        nextTick = SystemClock.elapsedRealtime() + 60 * 1000;
        alarmMgr.setExact(AlarmManager.ELAPSED_REALTIME_WAKEUP, nextTick, alarmIntent);
        long nextTickKm1 = Utils.getPrefLong(context, "nextTick");
        Utils.setPref(context, "nextTick", nextTick);
        Log.w(TAG, String.format("nextTick %d <- %d, %d ticks", nextTick, nextTickKm1, (nextTick - nextTickKm1)));
    }

    public PendingResult getAsyncResult()
    {
        return asyncResult;
    }

    private void startAsync()
    {
        stopAsync();
        asyncResult = goAsync();
        Log.e(TAG, String.format("goAsync ret %s", asyncResult));
/*        pollingAsyncThread = new PollingAsyncThread();
        pollingAsyncThread.execute(this);*/
        step5();
    }

    public void stopAsync()
    {
        if (asyncResult != null)
        {
            Log.e(TAG, "Declaring Async finished!");
            asyncResult.finish();
            asyncResult = null;
        }

        if (pollingAsyncThread != null)
        {
            Log.i(TAG, "Deleting ref to PollAsyncThread");
            pollingAsyncThread = null;
        }
    }

/*    public void clearPollingAsyncThreadRef()
    {
        pollingAsyncThread = null;
    }*/
}
