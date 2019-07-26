package com.innovavn.bluetoothutils;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothClass;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothHeadset;
import android.bluetooth.BluetoothProfile;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.util.Log;

import java.util.List;

public class BluetoothHandsfreeUtils implements BluetoothClassicUtils {
    private static final String TAG = "BluetoothHandsfreeUtils";
    public static final boolean DEBUG = true;

    private Context mContext;

    private BluetoothAdapter mBluetoothAdapter;
    private BluetoothHeadset mBluetoothHeadset;
    private BluetoothDevice mConnectedHeadset;

    private static boolean mIsOnHeadsetOutput = true;

    public BluetoothHandsfreeUtils(Context mContext) {
        this.mContext = mContext;
        mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
    }

    @Override
    public boolean isOnOutput() {
        return mIsOnHeadsetOutput;
    }

    public void setOnHeadsetOutput(boolean on) {
        mIsOnHeadsetOutput = on;
    }

    @Override
    public boolean startBluetooth() {
        if (DEBUG) {
            Log.d(TAG, "startBluetooth");
        }

        if (mBluetoothAdapter != null) {
            if (mBluetoothAdapter.getProfileProxy(mContext, mHeadsetProfileListener, BluetoothProfile.HEADSET)) {
                return true;
            }
        }
        return false;
    }

    @Override
    public void stopBluetooth() {
        Log.d(TAG, "stopBluetooth");

        if (mBluetoothHeadset != null) {
            // Need to call stopVoiceRecognition here when the app change the orientation or close
            // with headset still turns on.
            mBluetoothHeadset.stopVoiceRecognition(mConnectedHeadset);
            try {
                mContext.unregisterReceiver(mHeadsetBroadcastReceiver);
            } catch (Exception e) {
                e.printStackTrace();
            }

            mBluetoothAdapter.closeProfileProxy(BluetoothHeadset.HEADSET, mBluetoothHeadset);
            mBluetoothHeadset = null;
        }
    }

    BluetoothProfile.ServiceListener mHeadsetProfileListener = new BluetoothProfile.ServiceListener() {
        @Override
        public void onServiceConnected(int profile, BluetoothProfile proxy) {
            Log.d(TAG, "HandsFree Connected");

            mBluetoothHeadset = (BluetoothHeadset) proxy;

            List<BluetoothDevice> devices = mBluetoothHeadset.getConnectedDevices();

            if (devices.size() > 0) {
                mConnectedHeadset = devices.get(0);

                if (isOnOutput()) {
                    if (DEBUG) {
                        Log.d(TAG, "onServiceConnected: mute Microphone");
                    }
                } else {
                    if(DEBUG) {
                        Log.d(TAG, "onServiceConnected: Not mute Microphone");
                    }
                }
                mBluetoothHeadset.startVoiceRecognition(mConnectedHeadset);
                // During the active life time of the app, a user may turn on and off the headset.
                // So register for broadcast of connection states.
                mContext.registerReceiver(mHeadsetBroadcastReceiver, new IntentFilter(BluetoothHeadset.ACTION_CONNECTION_STATE_CHANGED));
                // Calling startVoiceRecognition does not result in immediate audio connection.
                // So register for broadcast of audio connection states. This broadcast will
                // only be sent if startVoiceRecognition returns true.
                mContext.registerReceiver(mHeadsetBroadcastReceiver, new IntentFilter(BluetoothHeadset.ACTION_AUDIO_STATE_CHANGED));
            }
        }

        @Override
        public void onServiceDisconnected(int profile) {
            Log.d(TAG, "HandsFree Disconnected");
            stopBluetooth();
        }
    };

    private BroadcastReceiver mHeadsetBroadcastReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            int state;

            if (action.equals(BluetoothHeadset.ACTION_CONNECTION_STATE_CHANGED)) {
                state = intent.getIntExtra(BluetoothHeadset.EXTRA_STATE, BluetoothHeadset.STATE_DISCONNECTED);

                Log.d(TAG, "\nAction = " + action + "\nState = " + state);
                if (state == BluetoothHeadset.STATE_CONNECTED) {
                    mConnectedHeadset = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                } else if (state == BluetoothHeadset.STATE_DISCONNECTED) {
                    mConnectedHeadset = null;
                }
            }
            else {
                state = intent.getIntExtra(BluetoothHeadset.EXTRA_STATE, BluetoothHeadset.STATE_AUDIO_DISCONNECTED);
                Log.d(TAG, "\nAction = " + action + "\nState = " + state);
                if (state == BluetoothHeadset.STATE_AUDIO_CONNECTED) {
                    Log.d(TAG, "\nHeadset audio connected");
                } else if (state == BluetoothHeadset.STATE_AUDIO_DISCONNECTED) {
                    mBluetoothHeadset.stopVoiceRecognition(mConnectedHeadset);
                }
            }
        }
    };
}
