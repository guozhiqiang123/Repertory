package com.gzq.lib_bluetooth;

import android.bluetooth.BluetoothDevice;

public interface ConnectListener {
    void success(BluetoothDevice device);
    void failed();
    void disConnect(String address);
}
