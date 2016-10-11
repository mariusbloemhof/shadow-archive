package com.servabosafe.shadow.data.receiver;

import android.bluetooth.*;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Message;
import com.servabosafe.shadow.data.model.Const;
import com.servabosafe.shadow.helper.U;
import com.servabosafe.shadow.helper.prefs.Prefs;

import java.lang.ref.WeakReference;

/**
 * Created by brandon.burton on 10/17/14.
 */
public class ShadowGattCallback extends BluetoothGattCallback {

    private OnMessageReceivedListener mListener;

    private BLECallback mBLECallback;

    public static final String PREFERENCES_DEVICE_KEY = "com.servabosafe.shadow.DEVICE_ADDRESS";
    //private static final String DEVICE_NAME = "SensorTag";
    private static final int REQUEST_CONTACT_NUMBER = 123456789;

    //Simple keys service
//    private static final UUID KEY_SERVICE = UUID.fromString("0000ffe0-0000-1000-8000-00805f9b34fb");
//    private static final UUID KEY_BATTERY_SERVICE = UUID.fromString("0000180F-0000-1000-8000-00805f9b34fb");

    //Key press state
//    private static final UUID KEY_DEVICE_TRIGGER = UUID.fromString("0000ffe1-0000-1000-8000-00805f9b34fb");

    /* Client Configuration Descriptor */
//    private static final UUID KEY_CONFIG_DESCRIPTOR = UUID.fromString("00002902-0000-1000-8000-00805f9b34fb");

//    /* Battery level */
//    private static final UUID KEY_BATTERY_LEVEL = UUID.fromString("00002a19-0000-1000-8000-00805f9b34fb");

    private static final int MSG_KEY = 101;

    private static WeakReference<Context> mContext = null;

    /* State Machine Tracking */
    private int mState = 0;

    private void reset() {
        mState = 0;
    }

    private void advance() {
        mState++;
    }

    public ShadowGattCallback(Context context) {

        mContext = new WeakReference<Context>(context);

    }

    /*
         * Enable notification of changes on the data characteristic for each sensor
         * by writing the ENABLE_NOTIFICATION_VALUE flag to that characteristic's
         * configuration descriptor.
         */
    private void NotifyKeySensor(BluetoothGatt gatt) {
        BluetoothGattCharacteristic characteristic;

        U.log("Set keys");
        characteristic = gatt.getService(Const.KEY_SERVICE).getCharacteristic(Const.KEY_DEVICE_TRIGGER);

        //Enable local notifications
        gatt.setCharacteristicNotification(characteristic, true);

        //Enabled remote notifications
        BluetoothGattDescriptor desc = characteristic.getDescriptor(Const.KEY_CONFIG_DESCRIPTOR);
        desc.setValue(BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE);

        gatt.writeDescriptor(desc);

        mBLECallback.getMessage(Const.NOTIF_LE_CONNECTED, null);

    }

    @Override
    public void onConnectionStateChange(BluetoothGatt gatt, int status, int newState) {

        U.log("Connection State Change: " + status + " -> " + connectionState(newState));
        if (status == BluetoothGatt.GATT_SUCCESS && newState == BluetoothProfile.STATE_CONNECTED) {
                /*
                 * Once successfully connected, we must next discover all the services on the
                 * device before we can read and write their characteristics.
                 */
            Bundle b = new Bundle();
            b.putBoolean("isConnected", true);
            mBLECallback.getMessage(Const.NOTIFY_DC, b);
            gatt.discoverServices();


            //return the value of the battery
            //return batteryLevel.getValue()[0];


        } else if (status == BluetoothGatt.GATT_SUCCESS && newState == BluetoothProfile.STATE_DISCONNECTED) {
            U.log("Issue notification for d/c");
            mBLECallback.getMessage(Const.NOTIFY_DC, null);
            //notifyUser(Const.KEY_NOTIFY_DC, R.drawable.logo_servabo, "Warning!", "Your device has disconnected. Please bring it closer to the phone.");
        }
        else if (status != BluetoothGatt.GATT_SUCCESS) {
            U.log( "FAIL !!!");
            U.log("Possibly disconnecting.");
                /*
                 * If there is a failure at any stage, simply disconnect
                 */
            gatt.disconnect();
            gatt.connect();
        }
    }

    @Override
    public void onServicesDiscovered(BluetoothGatt gatt, int status) {

        U.log("Services Discovered: " + status);

        BluetoothGattService batteryService = gatt.getService(Const.KEY_BATTERY_SERVICE);

        if (batteryService == null) {
            U.log("Battery service not found!");
            //return mPreferences.getInt(Prefs.KEY_BATTERY_LEVEL, Const.KEY_NO_BATTERY_SERVICE);
            //return -3;
        }
        else {
            BluetoothGattCharacteristic batteryLevel = batteryService.getCharacteristic(Const.KEY_BATTERY_LEVEL);
            if (batteryLevel == null) {
                U.log("Battery level not found!");
                //return mPreferences.getInt(Prefs.KEY_BATTERY_LEVEL, Const.KEY_NO_BATTERY_LEVEL);
                //return -2;
            } else {

                gatt.readCharacteristic(batteryLevel);
            }
        }

        NotifyKeySensor(gatt);
    }

    @Override
    public void onCharacteristicRead(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status) {
        //For each read, pass the data up to the UI thread to update the display
        if (Const.KEY_DEVICE_TRIGGER.equals(characteristic.getUuid())) {
//                mHandler.sendMessage(Message.obtain(null, MSG_KEY, characteristic));
            U.log(Message.obtain(null, MSG_KEY, characteristic));
            //Toast.makeText(mContext.get(), "Char read: You have pressed the button", Toast.LENGTH_SHORT).show();
            if (mContext.get() != null) {
                //After reading the initial value, next we enable notifications
                NotifyKeySensor(gatt);
                //SmsManager sms = SmsManager.getDefault();
                //sms.sendTextMessage("+13122872963", null, "Help! I'm stuck in the elevator!", null, null);
                //mBLECallback.getMessage(0x0, "There was a response.");
                //Toast.makeText(mContext.get(), "Char changed: You have pressed the button", Toast.LENGTH_SHORT).show();
            } else {
                U.log("There is something catastrophically wrong.");
            }

        }

        else {
            if (Const.KEY_BATTERY_LEVEL.equals(characteristic.getUuid())) {
                //U.log("Battery level is " + characteristic.getValue()[0]);
                //mListener.getMessage("Battery level is" + characteristic.getValue()[0]);

                Bundle b = new Bundle();
                b.putByte(Const.KEY_BATTERY_LEVEL.toString(), characteristic.getValue()[0]);
                U.log("Battery level is " + characteristic.getValue()[0]);
                mBLECallback.getMessage(Const.KEY_BLE_BATTERY, b);

//                Intent i = new Intent("BatteryCheck");
//                i.putExtra(Const.KEY_BATTERY_LEVEL.toString(), characteristic.getValue()[0]);
//                mContext.get().sendBroadcast(i);

                Intent i = new Intent();
                i.setAction("com.servabosafe.shadow.batterybroadcast");
                i.putExtra(Const.KEY_BATTERY_LEVEL.toString(), characteristic.getValue()[0]);
                mContext.get().sendBroadcast(i);

                SharedPreferences prefs = mContext.get().getSharedPreferences(Prefs.PREFS_KEY, Context.MODE_PRIVATE);
                SharedPreferences.Editor mEdit = prefs.edit();
                mEdit.putInt(Prefs.KEY_BATTERY_LEVEL, characteristic.getValue()[0]);
                mEdit.commit();
            }

            else {
                U.log(characteristic.getValue()[0]);
            }
        }

    }

    @Override
    public void onCharacteristicChanged(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic) {
            /*
             * After notifications are enabled, all updates from the device on characteristic
             * value changes will be posted here.  Similar to read, we hand these up to the
             * UI thread to update the display.
             */
        if (Const.KEY_DEVICE_TRIGGER.equals(characteristic.getUuid())) {
//            mHandler.sendMessage(Message.obtain(null, MSG_KEY, characteristic));
//            U.log(Message.obtain(null, MSG_KEY, characteristic));
//            U.log(Message.obtain(null, 107, characteristic));
            if (mContext.get() != null) {

                int severity = characteristic.getValue()[0];

                if (severity == Const.CHAR_VALUE_LOW ) {
                    mBLECallback.getMessage(Const.NOTIFY_LOW, null);
                    //notifyUser(Const.NOTIFY_LOW, R.drawable.ic_low_severity, "Help!", "I need help!");
                }
                else if (severity == Const.CHAR_VALUE_HIGH ) {
                    mBLECallback.getMessage(Const.NOTIFY_HIGH, null);
                    //notifyUser(Const.NOTIFY_HIGH, R.drawable.ic_high_severity, "Emergency!", "I really need help!");
                }

                mListener.getMessage("There was a response.");
            }
            else
            {
                U.log("There is something catastrophically wrong.");
            }
        }
    }

    @Override
    public void onReadRemoteRssi(BluetoothGatt gatt, int rssi, int status) {
        U.log("Remote RSSI: " + rssi);
    }

    private String connectionState(int status) {
        switch (status) {
            case BluetoothProfile.STATE_CONNECTED: {
                return "Connected";
            }
            case BluetoothProfile.STATE_DISCONNECTED:
                U.log("Disconnected from device");
                return "Disconnected";
            case BluetoothProfile.STATE_CONNECTING:
                U.log("Connecting to device.");
                return "Connecting";
            case BluetoothProfile.STATE_DISCONNECTING:
                U.log("Disconnecting from device");
                return "Disconnecting";
            default:
                return String.valueOf(status);
        }
    }

    public void setOnMessageReceivedListener(OnMessageReceivedListener l)
    {
        mListener = l;
    }

    public void setOnBLECallback(BLECallback b) {
        mBLECallback = b;
    }

}
