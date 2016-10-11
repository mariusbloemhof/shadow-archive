package com.servabosafe.shadow.data.model;

import java.util.UUID;

/**
 * Created by brandon.burton on 10/21/14.
 */
public final class Const {

    //Simple keys service
    public static final UUID KEY_SERVICE = UUID.fromString("0000ffe0-0000-1000-8000-00805f9b34fb");
    //Key press state
    public static final UUID KEY_DEVICE_TRIGGER = UUID.fromString("0000ffe1-0000-1000-8000-00805f9b34fb");
    //
    public static final UUID KEY_CONFIG_DESCRIPTOR = UUID.fromString("00002902-0000-1000-8000-00805f9b34fb");

    //battery level service
    public static final UUID KEY_BATTERY_SERVICE = UUID.fromString("0000180F-0000-1000-8000-00805f9b34fb");
    //battery level characteristic
    public static final UUID KEY_BATTERY_LEVEL = UUID.fromString("00002a19-0000-1000-8000-00805f9b34fb");


    public static final UUID KEY_GENERIC_ACCESS_SERVICE = UUID.fromString("00001800-0000-1000-8000-00805f9b34fb");
    public static final UUID KEY_DEVICE_NAME = UUID.fromString("00002A00-0000-1000-8000-00805f9b34fb");
    public static final UUID KEY_APPEARANCE = UUID.fromString("00002A01-0000-1000-8000-00805f9b34fb");


    public static final UUID KEY_ALERT_SERVICE = UUID.fromString("00001802-0000-1000-8000-00805f9b34fb");
    public static final UUID KEY_UNKNOWN_CHAR = UUID.fromString("00002A06-0000-1000-8000-00805f9b34fb");

    /**
     * If this is a one second press, this value is returned from GATT
     */
    public static final int CHAR_VALUE_LOW = 0x11;

    /**
     * If this is a long press, this value is returned from GATT
     */
    public static final int CHAR_VALUE_HIGH = 0x12;

    public static final int CHAR_VALUE_CANCEL = 0x13;
    /**
     * 10 seconds for length of delay
     */
    public static final int LENGTH_OF_DELAY = 10000;

    /**
     *  If there is no battery service or device return no battery.
     */
    public static final int KEY_BLE_BATTERY = 0xFF;
    public static final int KEY_NO_BLUETOOTH_GATT = -4;
    public static final int KEY_NO_BATTERY_SERVICE = -3;
    public static final int KEY_NO_BATTERY_LEVEL = -2;
    public static final int KEY_NO_BATTERY_VALUE = -1;
    public static final int KEY_BLE_CONNECTED = 10;

    /**
     *  If the service is live
     */
    //public static final int KEY_NOTIF_CREATED = 0xfe;



    /**
     * Notification index dedicated to low severity
     */
    public static final int NOTIFY_LOW = 20;

    /**
     * Notification index dedicated to high severity
     */
    public static final int NOTIFY_HIGH = 21;

    /**
     * Disconnected index
     */
    public static final int NOTIFY_DC = 22; //add alert

    /**
     * GPS IS LOST
     */
    public static final int NOTIFY_GPS_LOST = 25; //add alert

    /**
     * Unpaired Shadow
     */
    public static final int NOTIF_UNPAIRED = 30; //add alert

    /**
     * No keys defined
     */
    public static final int NOTIFY_NO_SCENARIO = 40; //add alert

    /**
     * Index of live service notification
     */
    public static final int NOTIF_SERVICE_IS_LIVE = 10;

    /**
     * If the bluetooth is turned off, index this
     */
    public static final int NOTIF_BLUETOOTH_DC = 50; //add alert

    /**
     *
     */
    public static final int NOTIF_LE_CONNECTED = 51;

    /**
     * Request update every 10 seconds
     */
    public static final long UPDATE_TIME = 60000;

    /**
     * Update every 10 meters
     */
    public static final float UPDATE_DISTANCE = 10;

    //004 Key
    //public static final String GOOGLE_API_KEY = "AIzaSyDE0SA-EspEOQPeEzNOvcv1jigOjhAzhhM";

    //Servabo
    public static final String GOOGLE_API_KEY = "AIzaSyCEgt-hT6nzzJiUvVRey4D1ClsxcSqGC00";
}
