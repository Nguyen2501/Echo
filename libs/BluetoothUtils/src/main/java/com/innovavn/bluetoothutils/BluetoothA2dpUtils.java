package com.innovavn.bluetoothutils;

import android.bluetooth.BluetoothA2dp;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothProfile;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.util.Log;
import android.widget.Toast;

import java.util.List;

public class BluetoothA2dpUtils implements BluetoothClassicUtils {
    private static final String TAG = "BluetoothA2dpUtils";
    private static final boolean DEBUG = true;

    private Context mContext;
    private BluetoothAdapter mBluetoothAdapter;
    private BluetoothA2dp mBluetoothA2dp;
    private BluetoothDevice mBluetoothDevice;

    private boolean isOnHeadsetA2dp = false;

    public BluetoothA2dpUtils(Context context) {
        this.mContext = context;
        mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
    }

    @Override
    public boolean isOnOutput() {
        return isOnHeadsetA2dp;
    }

    @Override
    public void stopBluetooth() {
        if (DEBUG) {
            Log.d(TAG, "stopBluetooth");
        }

        if (mBluetoothA2dp != null) {
            try {
                mContext.unregisterReceiver(mA2dpBroadcastReceiver);
            } catch (Exception e) {
                e.printStackTrace();
            }
            mBluetoothAdapter.closeProfileProxy(BluetoothProfile.A2DP, mBluetoothA2dp);
            mBluetoothDevice = null;
        }
    }

    @Override
    public boolean startBluetooth() {
        if (DEBUG) {
            Log.d(TAG, "startBluetooth");
        }

        if (mBluetoothAdapter != null) {
            if (mBluetoothAdapter.getProfileProxy(mContext, mA2dpProfileListener, BluetoothProfile.A2DP)) {
                return true;
            }
        }
        return false;
    }

    private BroadcastReceiver mA2dpBroadcastReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();

            int state;

            if (action.equals(BluetoothA2dp.ACTION_CONNECTION_STATE_CHANGED)) {
                state = intent.getIntExtra(BluetoothA2dp.EXTRA_STATE, BluetoothA2dp.STATE_DISCONNECTED);

                if (state == BluetoothA2dp.STATE_CONNECTED) {
                    mBluetoothDevice = intent.getParcelableExtra(mBluetoothDevice.EXTRA_DEVICE);
                    isOnHeadsetA2dp = true;
                } else if (state == BluetoothA2dp.STATE_DISCONNECTED) {
                    mBluetoothDevice = null;
                    isOnHeadsetA2dp = false;
                }
            } else if (action.equals(BluetoothA2dp.ACTION_PLAYING_STATE_CHANGED)) {
                state = intent.getIntExtra(BluetoothA2dp.EXTRA_STATE, BluetoothA2dp.STATE_NOT_PLAYING);
                if (state == BluetoothA2dp.STATE_PLAYING) {
                    Log.d(TAG, "A2DP starts playing");
                } else {
                    Log.d(TAG, "A2DP stops playing");
                }
            }
        }
    };

    private BluetoothProfile.ServiceListener mA2dpProfileListener = new BluetoothProfile.ServiceListener() {
        @Override
        public void onServiceConnected(int profile, BluetoothProfile proxy) {
            Log.d(TAG, "A2DP Service connected, profile = " + profile);

            if (profile == BluetoothProfile.A2DP) {
                mBluetoothA2dp = (BluetoothA2dp) proxy;

                List<BluetoothDevice> devices = mBluetoothA2dp.getConnectedDevices();

                if (devices.size() > 0) {
                    mBluetoothDevice = devices.get(0);
                    // TODO: 6/24/2019 Implement A2DP or Handsfree
                    isOnHeadsetA2dp = true;

                    mContext.registerReceiver(mA2dpBroadcastReceiver,
                            new IntentFilter(BluetoothA2dp.ACTION_CONNECTION_STATE_CHANGED));
                    mContext.registerReceiver(mA2dpBroadcastReceiver,
                            new IntentFilter(BluetoothA2dp.ACTION_PLAYING_STATE_CHANGED));
                } else {
                    Toast.makeText(mContext, "No device is connecting to application", Toast.LENGTH_SHORT).show();
                }
            }
        }

        @Override
        public void onServiceDisconnected(int profile) {
            Log.d(TAG, "A2DP Disconnected");
            stopBluetooth();
        }
    };
}
