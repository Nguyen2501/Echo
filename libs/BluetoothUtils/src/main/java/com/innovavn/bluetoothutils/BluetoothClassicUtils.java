package com.innovavn.bluetoothutils;

public interface BluetoothClassicUtils {
    boolean isOnOutput();

    boolean startBluetooth();

    void stopBluetooth();

    enum State{
        // HandsFree profile
        HANDSFREE,

        // A2DP profile
        A2DP,
    }

//    void Log(String message);
}
