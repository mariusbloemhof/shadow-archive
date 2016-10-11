package com.servabosafe.shadow.data.receiver;

import android.bluetooth.BluetoothDevice;

/**
 * Created by brandon.burton on 10/30/14.
 */
public interface OnPairRequestListener {

    public void onDevicePair(BluetoothDevice d);
}
