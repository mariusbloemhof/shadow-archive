package com.servabosafe.shadow.data.receiver;

import android.os.Bundle;

/**
 * Created by brandon.burton on 10/22/14.
 */
public interface BLECallback {

    /**
     * When BLE GATT communicates, send this information to the listener class.
     * @param key The type of action
     * @param b The data inside
     */
    public void getMessage(int key, Bundle b);

}
