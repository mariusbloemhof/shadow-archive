package com.servabosafe.shadow.data.service;

import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.bluetooth.*;
import android.content.*;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.media.Ringtone;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.*;
import android.support.v4.app.NotificationCompat;
import android.telephony.SmsManager;
import android.widget.Toast;
import com.androidquery.AQuery;
import com.androidquery.callback.AjaxCallback;
import com.androidquery.callback.AjaxStatus;
import com.servabosafe.shadow.R;
import com.servabosafe.shadow.ble.BarometerCalibrationCoefficients;
import com.servabosafe.shadow.ble.GattInfo;
import com.servabosafe.shadow.ble.Sensor;
import com.servabosafe.shadow.ble.SensorTagGatt;
import com.servabosafe.shadow.data.model.Const;
import com.servabosafe.shadow.data.model.Contact;
import com.servabosafe.shadow.data.model.Scenario;
import com.servabosafe.shadow.data.receiver.BLECallback;
import com.servabosafe.shadow.data.receiver.ShadowGattCallback;
import com.servabosafe.shadow.helper.EmergencyUpdater;
import com.servabosafe.shadow.helper.OnUpdateFinishedListener;
import com.servabosafe.shadow.helper.U;
import com.servabosafe.shadow.helper.db.ScenarioDataSource;
import com.servabosafe.shadow.helper.prefs.Prefs;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.lang.ref.WeakReference;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.sql.SQLException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;

/**
 * Created by brandon.burton on 10/16/14.
 */
public class ShadowListenerService extends Service implements LocationListener, BluetoothAdapter.LeScanCallback {

    public final static String ACTION_GATT_CONNECTED = "com.ti.ble.common.ACTION_GATT_CONNECTED";
    public final static String ACTION_GATT_DISCONNECTED = "com.ti.ble.common.ACTION_GATT_DISCONNECTED";
    public final static String ACTION_GATT_SERVICES_DISCOVERED = "com.ti.ble.common.ACTION_GATT_SERVICES_DISCOVERED";
    public final static String ACTION_DATA_READ = "com.ti.ble.common.ACTION_DATA_READ";
    public final static String ACTION_DATA_NOTIFY = "com.ti.ble.common.ACTION_DATA_NOTIFY";
    public final static String ACTION_DATA_WRITE = "com.ti.ble.common.ACTION_DATA_WRITE";
    public final static String EXTRA_DATA = "com.ti.ble.common.EXTRA_DATA";
    public final static String EXTRA_UUID = "com.ti.ble.common.EXTRA_UUID";
    public final static String EXTRA_STATUS = "com.ti.ble.common.EXTRA_STATUS";
    public final static String EXTRA_ADDRESS = "com.ti.ble.common.EXTRA_ADDRESS";

    private BLECallback mBLECallback;

    private static ShadowListenerService mThis = null;
    private static final int GATT_TIMEOUT = 250; // milliseconds

    //private String mBluetoothDeviceAddress;

    private SharedPreferences mPreferences;

    /**
     */
    private boolean isWaiting = false;

    /**
     * If the bluetooth is lost...
     */
    private boolean mBluetoothLost = false;

    /**
     * If the device is unpaired
     */
    private boolean mBluetoothOff = false;

    private static Scenario lowSeverity = null;

    private static Scenario highSeverity = null;

    private String mLastKnownAddress = "";

    /**
     * Location settings
     */
    private static LocationManager mLocationManager = null;
    private static Location mLastKnownLocation = null;

    private static NotificationManager mNoteManager;

    private static ScenarioDataSource mDataSource = null;

    private static ShadowGattCallback mGattCallback;

    //Bluetooth
    /**
     *  The main bluetooth adapter
      */
    private static BluetoothAdapter mBluetoothAdapter = null;

    /**
     *  The current device
     */
    private static BluetoothDevice mCurrentDevice;

    private static BluetoothManager mBluetoothManager;

    /**
     *
     */
    private static String mDeviceAddress;

    /**
     * The bluetooth GATT Profile
     */
    private static BluetoothGatt mBluetoothGatt;

    // SensorTagGatt
    private List<Sensor> mEnabledSensors = new ArrayList<Sensor>();
    private BluetoothGattService mOadService = null;
    private BluetoothGattService mConnControlService = null;
    private boolean mMagCalibrateRequest = true;
    private boolean mHeightCalibrateRequest = true;
    private static boolean mIsSensorTag2;
    private static String mFwRev;

    /**
     * AQuery Instance
     */
    private AQuery $;

    //private static OnLeScanFinishedListener mBluetoothScan = null;

    private final IBinder mBinder = new BluetoothBinder();

    /**
     * Amount of presses from the device
     */
    private int mButtonPresses = 0;

    private volatile boolean mBusy = false; // Write/read pending response

    private BluetoothGattCharacteristic keypress = null;

    private Handler mHandler = new Handler();
    private Handler stopHandler = new Handler();

    private Runnable mLowEmergencyVibrate = new Runnable() {

        @Override
        public void run() {
            if (keypress != null && mBluetoothGatt != null && isWaiting) {
                keypress.setValue(new byte[]{0x21});
                mBluetoothGatt.writeCharacteristic(keypress);
                /** Do something **/

                mHandler.postDelayed(mLowEmergencyVibrate, 1000);
            }
        }
    };


    private Runnable mStopLowVibrate = new Runnable() {

        @Override
        public void run() {
            if (keypress != null && mBluetoothGatt != null && isWaiting) {
                keypress.setValue(new byte[]{0x13});

                mBluetoothGatt.writeCharacteristic(keypress);
                stopHandler.postDelayed(mStopLowVibrate, 1000);
            }
        }
    };

    private Runnable mHighEmergencyVibrate = new Runnable() {

        @Override
        public void run() {
            if (keypress != null && mBluetoothGatt != null && isWaiting) {
                keypress.setValue(new byte[]{0x21});
                mBluetoothGatt.writeCharacteristic(keypress);
                /** Do something **/
                mHandler.postDelayed(mHighEmergencyVibrate, 500);
            }
        }
    };

    private Runnable mStopHighVibrate = new Runnable() {

        @Override
        public void run() {
            if (keypress != null && mBluetoothGatt != null && isWaiting) {
                keypress.setValue(new byte[]{0x13});

                mBluetoothGatt.writeCharacteristic(keypress);
                stopHandler.postDelayed(mStopHighVibrate, 500);
            }
        }
    };

    /**
     * Emergency manager delay
     */
    private EmergencyUpdater mEmergencyManager;


    private Runnable mLowHandleEmergency = new Runnable() {
        @Override
        public void run() {
            //notify the user
            notifyUser(Const.NOTIFY_LOW, R.drawable.ic_low_severity, lowSeverity.getTitle(), lowSeverity.getMessage());
            U.log("Fire update!");
            sendEmergency(lowSeverity);
            cancelNotification(Const.NOTIFY_LOW);
            mButtonPresses = 0;
        }
    };

    private Runnable mHighHandleEmergency = new Runnable() {
        @Override
        public void run() {
            //notify the user
            notifyUser(Const.NOTIFY_HIGH, R.drawable.ic_high_severity, highSeverity.getTitle(), highSeverity.getMessage());
            U.log("Fire update!");
            sendEmergency(highSeverity);
            cancelNotification(Const.NOTIFY_HIGH);
            mButtonPresses = 0;
        }
    };

    @Override
    public void onCreate() {

        super.onCreate();

        U.log("Created");

    }


    @Override
    public void onDestroy() {

        super.onDestroy();

        if (mBluetoothGatt != null) {
            unregisterReceiver(mGattUpdateReceiver);
            unregisterReceiver(mBluetoothStateReceiver);
            mBluetoothGatt.close();
            mBluetoothGatt = null;

        }

        U.log("Shadow listener service has been destroyed");
    }

    @Override
    public IBinder onBind(Intent intent) {
        return mBinder;
    }

    @Override
    public boolean onUnbind(Intent intent) {

        return super.onUnbind(intent);
    }

    public void attemptPairWithDevice(BluetoothDevice b) {

        pairTag(b);

    }

    public String checkForSensorTag(String address) {
        if (address == null)
            return "0x0";

        Set<BluetoothDevice> pairedDevices = mBluetoothAdapter.getBondedDevices();
        BluetoothDevice[] mPairedDevices = new BluetoothDevice[pairedDevices.size()];
        pairedDevices.toArray(mPairedDevices);


        for (int i = 0; i < mPairedDevices.length; i++)
        {
            BluetoothDevice b = mPairedDevices[i];
            if (b.getAddress().equals(address))
                return "0x1";
            if (b.getAddress().contains("D0") && !b.getAddress().equals(address)) {
                return mPairedDevices[i].getAddress();
            }

        }

        return "0x0";
    }


    /**
     * Set the current bluetooth device that Gatt is communicating over
     * @param b Current bluetooth device
     */
//    public void setCurrentDevice(BluetoothDevice b)
//    {
//        mCurrentDevice = b;
//
//        if (b != null)
//        {
//            if (mBluetoothGatt == null)
//            {
//                mGattCallback = new ShadowGattCallback(this);
//
//                // Assign GATT callback to selected device
//                //mGattCallback.setOnBLECallback(new BLECallbackListener());
//
//                mBluetoothGatt = mCurrentDevice.connectGatt(this, true, mGattCallback);
//
//                notifyUser(Const.NOTIF_LE_CONNECTED, R.drawable.logo_ab_servabo, "Info", "Attempting to connect Shadow...");
//
//                setDeviceAddress(b.getAddress());
//
//                U.log("Connected Bluetooth GATT");
//
//            }
//            else
//            {
//                U.log("Bluetooth GATT is live.");
//            }
//        }
//        else
//        {
//            //extinguish current BT information
//            if (mBluetoothGatt != null)
//            {
//                mBluetoothGatt.disconnect();
//                mBluetoothGatt.close();
//            }
//
//            mGattCallback = null;
//
//            mBluetoothGatt = null;
//
//            mCurrentDevice = null;
//
//        }
//
//
//        // GATT profile for device
//
//        //mPreferences.edit().putInt(Prefs.KEY_BATTERY_LEVEL, getBatteryLevel()).apply();
//
//        cancelNotification(Const.NOTIFY_DC);
//
//    }

    /**
     * If the device that is connected is not present, check nil
     * @return Current device
     * @throws NullPointerException
     */
    public BluetoothDevice getCurrentDevice() throws NullPointerException
    {
        return mCurrentDevice;
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {

        //notifyUser(Const.NOTIF_SERVICE_IS_LIVE, R.drawable.logo_ab_servabo, "You are connected to Shadow", "Stay connected!");

        mNoteManager = (NotificationManager)getSystemService(NOTIFICATION_SERVICE);

        $ = new AQuery(this);

        initialize();

        //mBluetoothAdapter = ((BluetoothManager)getSystemService(Context.BLUETOOTH_SERVICE)).getAdapter();

        mButtonPresses = 0;

        mLastKnownAddress = "No known location";

        mEmergencyManager = new EmergencyUpdater(null);

        mEmergencyManager.setOnUpdateFinishedListener(new OnUpdateFinishedListener() {
            @Override
            public void onUpdateFinished() {
                mButtonPresses = 0;
            }
        });

        mPreferences = getSharedPreferences(Prefs.PREFS_KEY, MODE_PRIVATE);

        mDataSource = new ScenarioDataSource(this);

        try {
            loadScenarioData();
        } catch (SQLException e) {
            Toast.makeText(this, "There was an error loading the DB", Toast.LENGTH_SHORT).show();
        }


        mLocationManager = (LocationManager)getSystemService(LOCATION_SERVICE);
        mLocationManager.requestLocationUpdates(LocationManager.NETWORK_PROVIDER, Const.UPDATE_TIME, Const.UPDATE_DISTANCE, this);
        mLocationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, Const.UPDATE_TIME, Const.UPDATE_DISTANCE, this);
//        mLocationManager.requestLocationUpdates(LocationManager.PASSIVE_PROVIDER, Const.UPDATE_TIME * 4, Const.UPDATE_DISTANCE, this);

        IntentFilter filter = new IntentFilter();

        filter.addAction(BluetoothAdapter.ACTION_STATE_CHANGED);
        filter.addAction(BluetoothDevice.ACTION_BOND_STATE_CHANGED);
        filter.addAction(BluetoothDevice.ACTION_PAIRING_REQUEST);

        registerReceiver(mGattUpdateReceiver, makeGattUpdateIntentFilter());
        registerReceiver(mBluetoothStateReceiver, makeBluetoothAdapterFilter());

        updateSensorList();

        //setCurrentDeviceToPaired();

        if (mPreferences.getBoolean("dismissedBackground", false)) {
            U.log("Service is restarted.");
            String lastknown = mPreferences.getString(Prefs.KEY_ADDRESS, null);
            if (lastknown != null)
            {
                connect(lastknown);
            }
        }
        else
        {

        }

        //TODO do something useful
        return Service.START_STICKY;
    }

    @Override
    public void onLeScan(BluetoothDevice device, int rssi, byte[] scanRecord) {

    }

    public class BluetoothBinder extends Binder {

        public ShadowListenerService getService() {
            return ShadowListenerService.this;
        }

    }

    public boolean isBluetoothConnected()
    {
//        boolean isConnected = mBluetoothAdapter.isEnabled();
//        if (isConnected) {
//            int connectionState = mBluetoothGatt.getConnectionState()
//            if (connectionState == BluetoothProfile.STATE_CONNECTED)
//                return true;
//            else
//                return false;
//        }
//        else
            return mBluetoothAdapter.isEnabled();
    }

    private void loadScenarioData() throws SQLException {

        mDataSource.open();

        Integer low = mPreferences.getInt(Prefs.KEY_DB_SCENARIO_LOW, -1);
        Integer high = mPreferences.getInt(Prefs.KEY_DB_SCENARIO_HIGH, -1);

        //get severity
        if (low != -1) {
            lowSeverity = mDataSource.getScenario(low);
        }
        if (high != -1) {
            highSeverity = mDataSource.getScenario(high);
        }

        mDataSource.close();

    }

    private void notifyUser(int key, int logoId, String title, String message)
    {
        NotificationCompat.Builder mBuilder =
                new NotificationCompat.Builder(this)
                        .setSmallIcon(logoId)
                        .setContentTitle(title)
                        .setContentText(message);

        mNoteManager.notify(key, mBuilder.build());
    }

    private void notifyUser(int key, int logoId, String title, String message, Intent i)
    {

        PendingIntent intent = PendingIntent.getActivity(this, 200, i, PendingIntent.FLAG_UPDATE_CURRENT);

        NotificationCompat.Builder mBuilder =
                new NotificationCompat.Builder(this)
                        .setSmallIcon(logoId)
                        .setContentTitle(title)
                        .setContentText(message)
                        .setContentIntent(intent);


        mNoteManager.notify(key, mBuilder.build());
    }

    private void notifyUser(int key, int logoId, String title, String message, Intent i, boolean soundNotification)
    {

        if (i != null) {
            PendingIntent intent = PendingIntent.getActivity(this, 200, i, PendingIntent.FLAG_UPDATE_CURRENT);

            NotificationCompat.Builder mBuilder =
                    new NotificationCompat.Builder(this)
                            .setSmallIcon(logoId)
                            .setContentTitle(title)
                            .setContentText(message)
                            .setContentIntent(intent);

            mNoteManager.notify(key, mBuilder.build());
        } else {
            NotificationCompat.Builder mBuilder =
                    new NotificationCompat.Builder(this)
                            .setSmallIcon(logoId)
                            .setContentTitle(title)
                            .setContentText(message);

            mNoteManager.notify(key, mBuilder.build());
        }

        if (soundNotification)
        {
            Uri notification = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);
            Ringtone r = RingtoneManager.getRingtone(getApplicationContext(), notification);
            r.play();

            Vibrator v = (Vibrator)getSystemService(Context.VIBRATOR_SERVICE);
            // Vibrate for 500 milliseconds
            v.vibrate(500);
        }
    }

    private void cancelNotification(int key)
    {
        mNoteManager.cancel(key);
    }

    private void handleNotification(int key, Bundle b, BluetoothGattCharacteristic characteristic)
    {
        switch (key) {
            case Const.NOTIFY_LOW:
                if (lowSeverity != null) {
                    if (characteristic != null && mButtonPresses == 0 && !isWaiting) {
                        U.log("Writing characteristic");
                        keypress = characteristic;
                        mHandler.postDelayed(mLowEmergencyVibrate, 500);
                        mHandler.postDelayed(mStopLowVibrate, 1000);
                        //Task t = new TimerTask();
                        //characteristic.setValue(new byte[]{0x21});
                        //mBluetoothGatt.writeCharacteristic(characteristic);
                    }
                    mButtonPresses++;
                    U.log(mButtonPresses);
                    if (!isWaiting) {

                        notifyUser(Const.NOTIFY_LOW, R.drawable.ic_low_severity, lowSeverity.getTitle(), lowSeverity.getMessage());
                        isWaiting = true;
                        mEmergencyManager.setStatusChecker(mLowHandleEmergency);
                        getAddressFromLocation();
                        mEmergencyManager.startUpdates();
                        mButtonPresses = 0;

                    }
                    else if (mButtonPresses > 2) {
                        mEmergencyManager.stopUpdates();
                        cancelNotification(Const.NOTIFY_LOW);
                        cancelNotification(Const.NOTIFY_HIGH);
                        isWaiting = false;
                        mButtonPresses = 0;
                        if (characteristic != null) {
                            characteristic.setValue(new byte[]{0x13});
                            mBluetoothGatt.writeCharacteristic(characteristic);
                        }
                    }
                }
                else {
                    notifyUser(Const.NOTIFY_LOW, R.drawable.logo_ab_servabo, "Warning!", "There are no emergencies for low severity!", null, true);
                }
                break;

            case Const.NOTIFY_HIGH:
                if (highSeverity != null && !isWaiting) {
                    notifyUser(Const.NOTIFY_HIGH, R.drawable.ic_high_severity, highSeverity.getTitle(), highSeverity.getMessage());
                    mEmergencyManager.setStatusChecker(mHighHandleEmergency);
                    getAddressFromLocation();
                    mEmergencyManager.startUpdates();
                    mButtonPresses = 0;
                    mHandler.postDelayed(mHighEmergencyVibrate, 250);
                    mHandler.postDelayed(mStopHighVibrate, 500);
                    if (characteristic != null) {
                        characteristic.setValue(new byte[]{0x22});
                        mBluetoothGatt.writeCharacteristic(characteristic);
                    }
                    isWaiting = true;
                } else {
                    notifyUser(Const.NOTIFY_NO_SCENARIO, R.drawable.logo_ab_servabo, "Warning!", "There are no emergencies for high severity!", null, true);
                }
                break;
            case Const.NOTIF_LE_CONNECTED:
                cancelNotification(Const.NOTIF_LE_CONNECTED);
                break;
            case Const.NOTIFY_DC:
                if (b != null)
                {
                    if (b.getBoolean("isConnected", true))
                    {
                        cancelNotification(Const.NOTIFY_DC);
                    }
                }
                else {
                    notifyUser(Const.NOTIFY_DC, R.drawable.logo_ab_servabo, "Warning!", "Your Shadow device has disconnected. Please bring it closer to the phone.", null, true);
                }
                break;

            case Const.KEY_BLE_BATTERY:
                if (b.getByte(Const.KEY_BATTERY_LEVEL.toString(), Byte.valueOf("-1")) < 11)
                    notifyUser(Const.NOTIFY_DC, R.drawable.logo_ab_servabo, "Information", "Your battery is low! No emergency messages will be sent until you change your battery.", null, true );
                break;
        }
    }

    private void handleNotification(int key, Bundle b)
    {
        handleNotification(key, b, null);
    }


    public synchronized void setLowSeverity(Scenario s)
    {
        lowSeverity = s;
        cancelNotification(Const.NOTIFY_NO_SCENARIO);
    }

    public synchronized void setHighSeverity(Scenario s)
    {
        highSeverity = s;
        cancelNotification(Const.NOTIFY_NO_SCENARIO);
    }

    public void sendEmergency(Scenario scenario)
    {


        Calendar calendar = Calendar.getInstance();
        TimeZone tz = calendar.getTimeZone();
        SimpleDateFormat sdf = new SimpleDateFormat("hh:mm a");
        sdf.setTimeZone(tz);

        String localTime = sdf.format(new Date(mLastKnownLocation.getTime()));
        Date date = new Date();

        try {
            date = sdf.parse(localTime);//get local date
        } catch (ParseException e) {
            e.printStackTrace();
        }

        String prettyString = sdf.format(date);
//        int seconds = calendar.get(Calendar.SECOND);
//        int minutes = calendar.get(Calendar.MINUTE);
//        int hours = calendar.get(Calendar.HOUR);


        SmsManager sms = SmsManager.getDefault();

        if (sms != null) {
            ArrayList<Contact> contacts = scenario.getContactData();
            for (Contact c : contacts) {
                U.log(c.getPhone().replace("(","").replace("-","").replace(")","").replace(" ",""));
                sms.sendTextMessage(c.getPhone(), null, scenario.getMessage() + "@" + mLastKnownAddress, null, null);
                sms.sendTextMessage(c.getPhone(), null, "Location updated @ " + prettyString, null, null);
            }
        } else {
            U.log("No SMS manager found");
        }

        //stop all vibration by waiting 2500 to resolve
        final Handler h = new Handler();
        Runnable r = new Runnable() {
            @Override
            public void run() {
                if (keypress != null && mBluetoothGatt != null) {
                    keypress.setValue(new byte[]{0x13});
                    mBluetoothGatt.writeCharacteristic(keypress);
                }
            }
        };
        isWaiting = false;
        h.postDelayed(r, 2500);
        mButtonPresses = 0;

    }

    private String getDeviceAddress() {
        return getSharedPreferences(Prefs.PREFS_KEY, MODE_PRIVATE).getString(Prefs.KEY_ADDRESS, "");
    }

    private void setDeviceAddress(String address) {
        mDeviceAddress = address;
        SharedPreferences.Editor editor = getSharedPreferences(Prefs.PREFS_KEY, MODE_PRIVATE).edit();
        editor.putString(Prefs.KEY_ADDRESS, mDeviceAddress);
        editor.apply();
    }

    private void pairTag(BluetoothDevice device)
    {
        try {


            Method mm = device.getClass().getMethod("createBond", (Class[]) null);
            mm.invoke(device, (Object[]) null);

            Intent pairIntent = new Intent("android.bluetooth.device.action.PAIRING_REQUEST");
            String EXTRA_DEVICE = "android.bluetooth.device.extra.DEVICE";
            pairIntent.putExtra(EXTRA_DEVICE, device);
            sendBroadcast(pairIntent);


        }
        catch (IllegalAccessException e)
        {
            e.printStackTrace();
        }
        catch (InvocationTargetException e)
        {
            e.printStackTrace();
        }
        catch (NoSuchMethodException e)
        {
            e.printStackTrace();
        }
        catch (Exception e)
        {
            e.printStackTrace();
        }

    }

    @Override
    public void onLocationChanged(Location location) {

        final Location temp = location;

        int minDistance = 0;
        int minTime = 60000; //1 minute

        if (mLastKnownLocation != null) {

            float distance = location.distanceTo(mLastKnownLocation);

            if (distance > 800.0f) //level 4
            {
                Toast.makeText(getApplicationContext(), "Update is at level 4", Toast.LENGTH_SHORT).show();
                minTime = 60000;
                //mLocationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 60000, 0, mNetworkListener);
            }
            else if (distance > 200.0f) { //level 3
                Toast.makeText(getApplicationContext(), "Update to level 4", Toast.LENGTH_SHORT).show();
                minTime = 30000;
                //mLocationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 30000, 0, mNetworkListener);
            }
            else if (distance > 40.0f) { //level 2
                Toast.makeText(getApplicationContext(), "Update to level 3", Toast.LENGTH_SHORT).show();
                minTime = 30000;
                //mLocationManager.requestLocationUpdates(LocationManager.NETWORK_PROVIDER, 30000, 0, mNetworkListener);
            }
            else if (distance > 20.0f) { //level 1
                //Toast.makeText(getApplicationContext(), "Update to level 2", Toast.LENGTH_SHORT).show();
                minTime = 120000;
                //mLocationManager.requestLocationUpdates(LocationManager.NETWORK_PROVIDER, 120000, 0, mNetworkListener);
            }
            else if (distance > 10.0f) {//dormant
                //Toast.makeText(getApplicationContext(), "User is at level 1", Toast.LENGTH_SHORT).show();
                minTime = 600000;
                //mLocationManager.requestLocationUpdates(LocationManager.NETWORK_PROVIDER, 600000, 0, mNetworkListener);
            }
            else if (distance > -1) //other
                //Toast.makeText(getApplicationContext(), "User is dormant", Toast.LENGTH_SHORT).show();
            minTime = 60000;
            //mLocationManager.requestLocationUpdates(LocationManager.NETWORK_PROVIDER, 600000, 100, mNetworkListener);
        }
        else {
//            mLocationAdapter.add(new PhysicalLocation(
//                    "Network", location.getLatitude() + ", " + location.getLongitude(),
//                    "Accuracy: " + location.getAccuracy()));
            //mLocationManager.requestLocationUpdates(LocationManager.NETWORK_PROVIDER, 15000, 0, mNetworkListener);
        }

        mLocationManager.removeUpdates(this);
        mLocationManager.removeUpdates(this);

        final int updateTime = minTime;
        final int updateDistance = minDistance;
        final WeakReference<Context> mContext = new WeakReference<Context>(this);
//
        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                if (updateTime < 33000)
                    mLocationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, updateTime, updateDistance, (LocationListener)mContext.get());
                else
                    mLocationManager.requestLocationUpdates(LocationManager.NETWORK_PROVIDER, updateTime, updateDistance, (LocationListener)mContext.get());
            }
        }, updateTime);


        U.log("Location has changed");
        mLastKnownLocation = location;

    }

    @Override
    public void onStatusChanged(String provider, int status, Bundle extras) {
        U.log(provider + " : " + status);
    }

    @Override
    public void onProviderEnabled(String provider) {
        U.log("Provider enabled: " + provider);
        cancelNotification(Const.NOTIFY_GPS_LOST);
    }

    @Override
    public void onProviderDisabled(String provider) {
        U.log("Provider disabled: " + provider);

        Intent gpsStart = new Intent(android.provider.Settings.ACTION_LOCATION_SOURCE_SETTINGS);
        notifyUser(Const.NOTIFY_GPS_LOST, R.drawable.logo_ab_servabo, "GPS is Off", "Tap here to enable GPS.", gpsStart, true);
    }

    public void getAddressFromLocation() {

        if (mLastKnownLocation != null) {

            final String latitude = String.valueOf(mLastKnownLocation.getLatitude());
            final String longitude = String.valueOf(mLastKnownLocation.getLongitude());

            $.ajax("https://maps.googleapis.com/maps/api/geocode/json?latlng=" + latitude+ "," + longitude + "&key="+Const.GOOGLE_API_KEY, JSONObject.class, new AjaxCallback<JSONObject>() {
                @Override
                public void callback(String url, JSONObject object, AjaxStatus status) {
                    try {
                        JSONArray j = object.getJSONArray("results");

                        if (j != null) {
                            if (j.getJSONObject(0) != null)
                                mLastKnownAddress = j.getJSONObject(0).getString("formatted_address");
                            else
                                mLastKnownAddress = latitude + "," + longitude;
                        }

//                        mReverseLookup.setText(mReverseLookup.getText().toString() + "\n" + location);
                    } catch (JSONException e) {
//                        mReverseLookup.setText("Error in location");
                        e.printStackTrace();
                    }
//
                    super.callback(url, object, status);
                }
            });
        } else {

            Location lastKnownLocation = mLocationManager.getLastKnownLocation(LocationManager.NETWORK_PROVIDER);
            final String latitude = String.valueOf(lastKnownLocation.getLatitude());
            final String longitude = String.valueOf(lastKnownLocation.getLongitude());

            $.ajax("https://maps.googleapis.com/maps/api/geocode/json?latlng=" + latitude+ "," + longitude + "&key="+Const.GOOGLE_API_KEY , JSONObject.class, new AjaxCallback<JSONObject>() {
                @Override
                public void callback(String url, JSONObject object, AjaxStatus status) {
                    try {
                        JSONArray j = object.getJSONArray("results");

                        //mLocations.add(new PhysicalLocation("GPS", String.valueOf(latitude) + "," + String.valueOf(longitude), location));
//                        mLocationAdapter.add(new PhysicalLocation("GPS", String.valueOf(latitude) + "," + String.valueOf(longitude), location));
                        //for (int i = 0; i < j.length(); i++) {

                        if (j != null) {
                            if (j.getJSONObject(0) != null)
                                mLastKnownAddress = j.getJSONObject(0).getString("formatted_address");
                            else
                                mLastKnownAddress = latitude + "," + longitude;
                        }
                        //mLocationAdapter.add(new PhysicalLocation("Network", mPrevLocation.distanceTo(temp)+"m", loc));
                        //else
                        //mLocationAdapter.add(new PhysicalLocation("Network", String.valueOf(latitude)+","+String.valueOf(longitude), loc));
                        //}
//                        runOnUiThread(new Runnable() {
//                            @Override
//                            public void run() {
//                                mLocationAdapter.notifyDataSetChanged();
//                                mListAddress.setAdapter(mLocationAdapter);
//                            }
//                        });
//                        mReverseLookup.setText(mReverseLookup.getText().toString() + "\n" + location);
                    } catch (JSONException e) {
//                        mReverseLookup.setText("Error in location");
                        e.printStackTrace();
                    }
//
                }
            });
        }
    }

    @Override
    public void onTaskRemoved(Intent rootIntent) {

        U.log("REMOVED");

        mPreferences.edit().putBoolean("dismissedBackground", true).apply();

        if (mBluetoothGatt != null)
            mPreferences.edit().putString(Prefs.KEY_ADDRESS, mBluetoothGatt.getDevice().getAddress()).apply();

        super.onTaskRemoved(rootIntent);
    }

    /**
     * Initializes a reference to the local Bluetooth adapter.
     *
     * @return Return true if the initialization is successful.
     */
    public boolean initialize() {
        // For API level 18 and above, get a reference to BluetoothAdapter through
        // BluetoothManager.
        //mThis = this;
        if (mBluetoothManager == null) {
            mBluetoothManager = (BluetoothManager) getSystemService(Context.BLUETOOTH_SERVICE);
            if (mBluetoothManager == null) {
                // Log.e(TAG, "Unable to initialize BluetoothManager.");
                return false;
            }
        }

        mBluetoothAdapter = mBluetoothManager.getAdapter();
        if (mBluetoothAdapter == null) {
            U.log("No bluetooth adapter");
            return false;
        } else {

        }

        mBluetoothLost = !mBluetoothAdapter.isEnabled();
        return true;
    }

    private boolean checkGatt() {
        if (mBluetoothAdapter == null) {
            // Log.w(TAG, "BluetoothAdapter not initialized");
            return false;
        }
        if (mBluetoothGatt == null) {
            // Log.w(TAG, "BluetoothGatt not initialized");
            return false;
        }

        return true;

    }

    //
    // GATT API
    //
    /**
     * Request a read on a given {@code BluetoothGattCharacteristic}. The read
     * result is reported asynchronously through the
     * {@code BluetoothGattCallback#onCharacteristicRead(android.bluetooth.BluetoothGatt, android.bluetooth.BluetoothGattCharacteristic, int)}
     * callback.
     *
     * @param characteristic
     *          The characteristic to read from.
     */
    public void readCharacteristic(BluetoothGattCharacteristic characteristic) {
        if (!checkGatt())
            return;
        mBusy = true;
        //if (characteristic != null)
        mBluetoothGatt.readCharacteristic(characteristic);
    }

    public boolean writeCharacteristic(
            BluetoothGattCharacteristic characteristic, byte b) {
        if (!checkGatt())
            return false;

        byte[] val = new byte[1];
        val[0] = b;
        characteristic.setValue(val);

        mBusy = true;
        return mBluetoothGatt.writeCharacteristic(characteristic);
    }

    public boolean writeCharacteristic(
            BluetoothGattCharacteristic characteristic, boolean b) {
        if (!checkGatt())
            return false;

        byte[] val = new byte[1];

        val[0] = (byte) (b ? 1 : 0);
        characteristic.setValue(val);
        mBusy = true;
        return mBluetoothGatt.writeCharacteristic(characteristic);
    }

    public boolean writeCharacteristic(BluetoothGattCharacteristic characteristic) {
        if (!checkGatt())
            return false;

        mBusy = true;
        return mBluetoothGatt.writeCharacteristic(characteristic);
    }

    /**
     * Retrieves the number of GATT services on the connected device. This should
     * be invoked only after {@code BluetoothGatt#discoverServices()} completes
     * successfully.
     *
     * @return A {@code integer} number of supported services.
     */
    public int getNumServices() {
        if (mBluetoothGatt == null)
            return 0;

        return mBluetoothGatt.getServices().size();
    }

    /**
     * Retrieves a list of supported GATT services on the connected device. This
     * should be invoked only after {@code BluetoothGatt#discoverServices()}
     * completes successfully.
     *
     * @return A {@code List} of supported services.
     */
    public List<BluetoothGattService> getSupportedGattServices() {
        if (mBluetoothGatt == null)
            return null;

        return mBluetoothGatt.getServices();
    }

    /**
     * Enables or disables notification on a give characteristic.
     *
     * @param characteristic
     *          Characteristic to act on.
     * @param enable
     *          If true, enable notification. False otherwise.
     */
    public boolean setCharacteristicNotification(
            BluetoothGattCharacteristic characteristic, boolean enable) {
        if (!checkGatt())
            return false;

        boolean ok = false;
        if (mBluetoothGatt.setCharacteristicNotification(characteristic, enable)) {

            BluetoothGattDescriptor clientConfig = characteristic
                    .getDescriptor(GattInfo.CLIENT_CHARACTERISTIC_CONFIG);
            if (clientConfig != null) {

                if (enable) {
                    // Log.i(TAG, "Enable notification: " +
                    // characteristic.getUuid().toString());
                    ok = clientConfig
                            .setValue(BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE);
                } else {
                    // Log.i(TAG, "Disable notification: " +
                    // characteristic.getUuid().toString());
                    ok = clientConfig
                            .setValue(BluetoothGattDescriptor.DISABLE_NOTIFICATION_VALUE);
                }

                if (ok) {
                    mBusy = true;
                    ok = mBluetoothGatt.writeDescriptor(clientConfig);
                    // Log.i(TAG, "writeDescriptor: " +
                    // characteristic.getUuid().toString());
                }
            }
        }

        return ok;
    }

    public boolean isNotificationEnabled(
            BluetoothGattCharacteristic characteristic) {
        if (!checkGatt())
            return false;

        BluetoothGattDescriptor clientConfig = characteristic
                .getDescriptor(GattInfo.CLIENT_CHARACTERISTIC_CONFIG);
        if (clientConfig == null)
            return false;

        return clientConfig.getValue() == BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE;
    }

    /**
     * Connects to the GATT server hosted on the Bluetooth LE device.
     *
     * @param address
     *          The device address of the destination device.
     *
     * @return Return true if the connection is initiated successfully. The
     *         connection result is reported asynchronously through the
     *         {@code BluetoothGattCallback#onConnectionStateChange(android.bluetooth.BluetoothGatt, int, int)}
     *         callback.
     */
    public boolean connect(final String address) {

        if (mBluetoothAdapter == null || address == null) {
            U.log("There is something wrong with Bluetooth Adapter" );
            // Log.w(TAG, "BluetoothAdapter not initialized or unspecified address.");
            return false;
        }

        if (mBluetoothLost)
        {
            mBluetoothGatt = null;
        }

        final BluetoothDevice device = mBluetoothAdapter.getRemoteDevice(address);
        int connectionState = mBluetoothManager.getConnectionState(device, BluetoothProfile.GATT);

        if (connectionState == BluetoothProfile.STATE_DISCONNECTED) {

            // Previously connected device. Try to reconnect.
            //if (mBluetoothDeviceAddress != null && address.equals(mBluetoothDeviceAddress) && mBluetoothGatt != null) {
            if (mBluetoothGatt != null) {
                // Log.d(TAG, "Re-use GATT connection");

                if (mBluetoothGatt.connect()) {
                    return true;
                }
                else
                {
                    mBluetoothGatt = device.connectGatt(this, true, mGattCallbacks);
                    mBluetoothLost = false;
                }


            } else {
                //Make new connection
                mBluetoothGatt = device.connectGatt(this, true, mGattCallbacks);
                mBluetoothLost = false;
                //return true;
            }

//            if (device == null) {
//                // Log.w(TAG, "Device not found.  Unable to connect.");
//                return false;
//            }
            // We want to directly connect to the device, so we are setting the
            // autoConnect parameter to false.
            // Log.d(TAG, "Create a new GATT connection.");
            //mBluetoothGatt = device.connectGatt(this, true, mGattCallbacks);
            //mBluetoothDeviceAddress = address;
        }
        else if (connectionState == BluetoothProfile.STATE_CONNECTED){
//            sendBroadcast(new Intent(getApplicationContext(), SSHomeActivity.class));
            broadcastUpdate(ACTION_GATT_CONNECTED, address,  BluetoothGatt.GATT_SUCCESS);
            mPreferences.edit().putString(Prefs.KEY_ADDRESS, address);
        }
        else {
            // Log.w(TAG, "Attempt to connect in state: " + connectionState);
            return false;
        }
        U.log("Last known ", device.getAddress());
        return true;
    }

    /**
     * Disconnects an existing connection or cancel a pending connection. The
     * disconnection result is reported asynchronously through the
     * {@code BluetoothGattCallback#onConnectionStateChange(android.bluetooth.BluetoothGatt, int, int)}
     * callback.
     */
    public void disconnect(String address) {
        if (mBluetoothAdapter == null) {
            // Log.w(TAG, "disconnect: BluetoothAdapter not initialized");
            return;
        }
        final BluetoothDevice device = mBluetoothAdapter.getRemoteDevice(address);
        int connectionState = mBluetoothManager.getConnectionState(device,
                BluetoothProfile.GATT);

        if (mBluetoothGatt != null) {
            if (connectionState != BluetoothProfile.STATE_DISCONNECTED) {
                mBluetoothGatt.disconnect();
            } else {
                // Log.w(TAG, "Attempt to disconnect in state: " + connectionState);
            }
        }
    }

    /**
     * After using a given BLE device, the app must call this method to ensure
     * resources are released properly.
     */
    public void close() {
        if (mBluetoothGatt != null) {
            // Log.i(TAG, "close");
            mBluetoothGatt.close();
            mBluetoothGatt = null;
        }
    }

    public int numConnectedDevices() {
        int n = 0;

        if (mBluetoothGatt != null) {
            List<BluetoothDevice> devList;
            devList = mBluetoothManager.getConnectedDevices(BluetoothProfile.GATT);
            n = devList.size();
        }
        return n;
    }

    private void broadcastUpdate(final String action, final String address,
                                 final int status) {
        //U.log("Action: " + action + " address: " + address + " status " + status);
        final Intent intent = new Intent(action);
        intent.putExtra(EXTRA_ADDRESS, address);
        intent.putExtra(EXTRA_STATUS, status);
        sendBroadcast(intent);
        mBusy = false;
    }

    private void broadcastUpdate(final String action, final BluetoothGattCharacteristic characteristic, final int status) {

        //U.log("Action: " + action + " characteristic: " + characteristic.getUuid() + " status " + status);
        final Intent intent = new Intent(action);
        intent.putExtra(EXTRA_UUID, characteristic.getUuid().toString());
        intent.putExtra(EXTRA_DATA, characteristic.getValue());
        intent.putExtra(EXTRA_STATUS, status);
        sendBroadcast(intent);
        mBusy = false;
    }

    //
    // Utility functions
    //
    public static BluetoothGatt getBtGatt() {
        return mBluetoothGatt;
    }

    public static BluetoothManager getBtManager() {
        return mBluetoothManager;
    }

    public static ShadowListenerService getInstance() {
        return mThis;
    }

    public boolean waitIdle(int timeout) {
        timeout /= 10;
        while (--timeout > 0) {
            if (mBusy)
                try {
                    Thread.sleep(10);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            else
                break;
        }

        return timeout > 0;
    }

    private void discoverServices() {
        if (mBluetoothGatt.discoverServices()) {
            //mServiceList.clear();
            //setBusy(true);
            //setStatus("Service discovery started");
            U.log("Discovering services ");
        } else {
            //setError("Service discovery start failed");
            U.log("Failed discovering services ");
        }
    }

    private void enableSensors(boolean f) {
        final boolean enable = f;

        for (Sensor sensor : mEnabledSensors) {
            UUID servUuid = sensor.getService();
            UUID confUuid = sensor.getConfig();

            // Skip keys
            if (confUuid == null)
                break;

//            if (!mIsSensorTag2) {
//                // Barometer calibration
//                if (confUuid.equals(SensorTagGatt.UUID_BAR_CONF) && enable) {
//                    calibrateBarometer();
//                }
//            }

            BluetoothGattService serv = mBluetoothGatt.getService(servUuid);
            if (serv != null) {
                BluetoothGattCharacteristic charac = serv.getCharacteristic(confUuid);
                byte value = enable ? sensor.getEnableSensorCode() : Sensor.DISABLE_SENSOR_CODE;
                if (writeCharacteristic(charac, value)) {
                    waitIdle(GATT_TIMEOUT);
                } else {
                    U.log("Error sensor config failed:" + serv.getUuid().toString());
                    //setError("Sensor config failed: " + serv.getUuid().toString());
                    break;
                }
            }
        }
    }

    private void enableNotifications(boolean f) {
        final boolean enable = f;

        for (Sensor sensor : mEnabledSensors) {
            UUID servUuid = sensor.getService();
            UUID dataUuid = sensor.getData();
            BluetoothGattService serv = mBluetoothGatt.getService(servUuid);
            if (serv != null) {
                BluetoothGattCharacteristic charac = serv.getCharacteristic(dataUuid);

                if (setCharacteristicNotification(charac, enable)) {
                    waitIdle(GATT_TIMEOUT);
                    try {
                        Thread.sleep(50);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                } else {
                    U.log("Sensor notification failed");
                    //setError("Sensor notification failed: " + serv.getUuid().toString());
                    break;
                }
            }
        }
    }

    private void enableDataCollection(boolean enable) {
        //setBusy(true);
        enableSensors(enable);
        enableNotifications(enable);
        //setBusy(false);
    }

    //
    // Application implementation
    //
    private void updateSensorList() {
        mEnabledSensors.clear();

        for (int i = 0; i < Sensor.SENSOR_LIST.length; i++) {
            Sensor sensor = Sensor.SENSOR_LIST[i];
            //if (isEnabledByPrefs(sensor)) {
                mEnabledSensors.add(sensor);
            //}
        }
    }

    public boolean isSensorTag2() {
        return mIsSensorTag2;
    }

    private void getFirmwareRevison() {
        UUID servUuid = SensorTagGatt.UUID_DEVINFO_SERV;
        UUID charUuid = SensorTagGatt.UUID_DEVINFO_FWREV;
        BluetoothGattService serv = mBluetoothGatt.getService(servUuid);
        BluetoothGattCharacteristic charFwrev = serv.getCharacteristic(charUuid);

        // Write the calibration code to the configuration registers
        readCharacteristic(charFwrev);
        waitIdle(GATT_TIMEOUT);

    }

    public void getBatteryLevel() {
        UUID servUuid = Const.KEY_BATTERY_SERVICE;
        UUID charUuid = Const.KEY_BATTERY_LEVEL;

        BluetoothGattService serv = mBluetoothGatt.getService(servUuid);
        BluetoothGattCharacteristic charFwrev = serv.getCharacteristic(charUuid);

        // Write the calibration code to the configuration registers
        readCharacteristic(charFwrev);
        waitIdle(GATT_TIMEOUT);

    }

    private final BroadcastReceiver mBluetoothStateReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {

            //if (mBluetoothAdapter == null)
            //    mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();

            String action = intent.getAction();

            if (BluetoothAdapter.ACTION_STATE_CHANGED.equals(action)) {
                final int state = intent.getIntExtra(BluetoothAdapter.EXTRA_STATE, BluetoothAdapter.ERROR);

                if (state == BluetoothAdapter.STATE_ON) {

                    U.log("Bluetooth is switched on.");

                    if (mBluetoothGatt != null)
                    {
                        U.log("Attempting to connect GATT");

                        String bestAddress = null;

                        Set<BluetoothDevice> pairedDevices = mBluetoothAdapter.getBondedDevices();
                        BluetoothDevice[] mPairedDevices = new BluetoothDevice[pairedDevices.size()];
                        pairedDevices.toArray(mPairedDevices);

                        //SharedPreferences prefs = getSharedPreferences(Prefs.PREFS_KEY, MODE_PRIVATE);
                        //String address = prefs.getString(Prefs.KEY_ADDRESS, "");

                        for (int i = 0; i < pairedDevices.size(); i++) {
                            if (mPairedDevices[i].getAddress().contains("D0")) {
                                bestAddress = mPairedDevices[i].getAddress();
                            }
                        }

                        connect(bestAddress);
                    }

                    cancelNotification(Const.NOTIF_BLUETOOTH_DC);

                }
                if (state == BluetoothAdapter.STATE_OFF) {
                    //mListener.getMessage("Bluetooth is off.");
                    //mBluetoothGatt.disconnect();
                    U.log("Bluetooth is switched off.");
                    mBluetoothLost = true;
                    if (mBluetoothGatt != null)
                    {
                        mBluetoothGatt.disconnect();
                        mBluetoothGatt.close();
                    }
                    Intent i = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
                    notifyUser(Const.NOTIF_BLUETOOTH_DC, R.drawable.logo_ab_servabo, "Bluetooth Alert", "Please enable Bluetooth!", i, true);
                    //stopScan();
                }
            }

            else if (BluetoothDevice.ACTION_BOND_STATE_CHANGED.equals(action)) {
                final int state        = intent.getIntExtra(BluetoothDevice.EXTRA_BOND_STATE, BluetoothDevice.ERROR);
                final int prevState    = intent.getIntExtra(BluetoothDevice.EXTRA_PREVIOUS_BOND_STATE, BluetoothDevice.ERROR);

                if (state == BluetoothDevice.BOND_BONDED && prevState == BluetoothDevice.BOND_BONDING)
                {
                    U.log("You have bonded a device.");



                    //if any context is concerned with pairing
                    //if (mListener != null)
                    //    mListener.onDevicePair(mDeviceInstance);
                    //for (OnPairRequestListener listeners : mListeners)
                    //{
//                        listeners.onDevicePair(mDeviceInstance);
                    //reset connection
                    //setCurrentDeviceToPaired();

                    //cancelNotification(Const.NOTIF_UNPAIRED);
                    //Intent i = new Intent(getApplicationContext(), SSHomeActivity.class);
                    //i.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
                    //startActivity(i);
                    //}

                }
                else if (state == BluetoothDevice.BOND_NONE && prevState == BluetoothDevice.BOND_BONDED)
                {

                    //String prevAddress = mPreferences.getString(Prefs.KEY_ADDRESS, null);
                    //String pairState = checkForSensorTag(prevAddress);

                    //null
//                    if (pairState == "0x0")
//                    {
//                        notifyUser(Const.NOTIF_UNPAIRED, R.drawable.logo_ab_servabo, "Unpair Alert", "Your device has been unpaired!", null, true);
//                        mPreferences.edit().putString(Prefs.KEY_ADDRESS, null).apply();
//                    }

                    U.log("You have unpaired a device.");

                    //if (checkForSensorTag(mLastKnownAddress)){


                    //}

                    //setCurrentDevice(null);


                    ///mBluetoothGatt.disconnect();
                    //mBluetoothGatt.close();

                }
            }

            else if (BluetoothDevice.ACTION_PAIRING_REQUEST.equals(action))
            {
                U.log("Pairing");

            }
        }
    };

    private final BroadcastReceiver mGattUpdateReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {

            final String action = intent.getAction();
            int status = intent.getIntExtra(ShadowListenerService.EXTRA_STATUS,
                    BluetoothGatt.GATT_SUCCESS);

            if (ShadowListenerService.ACTION_GATT_SERVICES_DISCOVERED.equals(action)) {
                if (status == BluetoothGatt.GATT_SUCCESS) {
                    U.log("Discovery finished.");
                    enableDataCollection(true);
                    getFirmwareRevison();
                    getBatteryLevel();
                    //setStatus("Service discovery complete");
                    //displayServices();
                    //checkOad();
                    //getFirmwareRevison();
                } else {
                    Toast.makeText(getApplication(), "Service discovery failed",
                            Toast.LENGTH_LONG).show();
                    return;
                }
            } else if (ShadowListenerService.ACTION_DATA_NOTIFY.equals(action)) {
                // Notification
                byte[] value = intent.getByteArrayExtra(ShadowListenerService.EXTRA_DATA);
                String uuidStr = intent.getStringExtra(ShadowListenerService.EXTRA_UUID);

                onCharacteristicChanged(uuidStr, value);
            } else if (ShadowListenerService.ACTION_DATA_WRITE.equals(action)) {
                // Data written
                String uuidStr = intent.getStringExtra(ShadowListenerService.EXTRA_UUID);
                onCharacteristicWrite(uuidStr, status);
            } else if (ShadowListenerService.ACTION_DATA_READ.equals(action)) {
                // Data read
                String uuidStr = intent.getStringExtra(ShadowListenerService.EXTRA_UUID);
                byte[] value = intent.getByteArrayExtra(ShadowListenerService.EXTRA_DATA);
                onCharacteristicsRead(uuidStr, value, status);
            }

            if (status != BluetoothGatt.GATT_SUCCESS) {
                U.log("Error: " + status);
                //setError("GATT error code: " + status);
            }
        }
    };

    private void onCharacteristicWrite(String uuidStr, int status) {

        U.log("Service on characteristic write");
        // Log.d(TAG, "onCharacteristicWrite: " + uuidStr);
    }

    private void onCharacteristicChanged(String uuidStr, byte[] value) {

        U.log("Service on characteristic changed");

    }

    private void onCharacteristicsRead(String uuidStr, byte[] value, int status) {
        // Log.i(TAG, "onCharacteristicsRead: " + uuidStr);

        if (uuidStr.equals(SensorTagGatt.UUID_DEVINFO_FWREV.toString())) {
            mFwRev = new String(value, 0, 3);
            Toast.makeText(this, "Firmware revision: " + mFwRev,Toast.LENGTH_LONG).show();
        }

        if (mIsSensorTag2)
            return;

        if (uuidStr.equals(Const.KEY_BATTERY_LEVEL.toString())) {
            String level = String.valueOf(value[0]);
            Toast.makeText(this, "Battery level is " + level,Toast.LENGTH_LONG).show();
            mPreferences.edit().putInt(Prefs.KEY_BATTERY_LEVEL, Integer.valueOf(level)).apply();

            Intent intent = new Intent("com.servabosafe.shadow.batterybroadcast");
            intent.putExtra(Const.KEY_BATTERY_LEVEL.toString(), Byte.valueOf(level));
            sendBroadcast(intent);
            U.log(level);
        }


        if (uuidStr.equals(SensorTagGatt.UUID_BAR_CALI.toString())) {
            // Sanity check
            if (value.length != 16)
                return;

            // Barometer calibration values are read.
            List<Integer> cal = new ArrayList<Integer>();
            for (int offset = 0; offset < 8; offset += 2) {
                Integer lowerByte = (int) value[offset] & 0xFF;
                Integer upperByte = (int) value[offset + 1] & 0xFF;
                cal.add((upperByte << 8) + lowerByte);
            }

            for (int offset = 8; offset < 16; offset += 2) {
                Integer lowerByte = (int) value[offset] & 0xFF;
                Integer upperByte = (int) value[offset + 1];
                cal.add((upperByte << 8) + lowerByte);
            }

            BarometerCalibrationCoefficients.INSTANCE.barometerCalibrationCoefficients = cal;
        }
    }

    private static IntentFilter makeGattUpdateIntentFilter() {
        final IntentFilter fi = new IntentFilter();
        fi.addAction(ShadowListenerService.ACTION_GATT_SERVICES_DISCOVERED);
        fi.addAction(ShadowListenerService.ACTION_DATA_NOTIFY);
        fi.addAction(ShadowListenerService.ACTION_DATA_WRITE);
        fi.addAction(ShadowListenerService.ACTION_DATA_READ);
        return fi;
    }

    private static IntentFilter makeBluetoothAdapterFilter() {
        final IntentFilter fi = new IntentFilter();
        fi.addAction(BluetoothAdapter.ACTION_STATE_CHANGED);
        fi.addAction(BluetoothDevice.ACTION_BOND_STATE_CHANGED);
        fi.addAction(BluetoothDevice.ACTION_PAIRING_REQUEST);
        return fi;
    }

    /**
     * GATT client callbacks
     */
    private BluetoothGattCallback mGattCallbacks = new BluetoothGattCallback() {

        @Override
        public void onConnectionStateChange(BluetoothGatt gatt, int status,
                                            int newState) {
            if (mBluetoothGatt == null) {
                // Log.e(TAG, "mBluetoothGatt not created!");
                return;
            }

            BluetoothDevice device = gatt.getDevice();
            String address = device.getAddress();
            // Log.d(TAG, "onConnectionStateChange (" + address + ") " + newState +
            // " status: " + status);

            try {
                switch (newState) {
                    case BluetoothProfile.STATE_CONNECTED:
                        discoverServices();
                        broadcastUpdate(ACTION_GATT_CONNECTED, address, status);
                        cancelNotification(Const.NOTIFY_DC);
//                        Intent i = new Intent(ShadowListenerService.this, SSHomeActivity.class);
//                        startActivity(i);
                        break;
                    case BluetoothProfile.STATE_DISCONNECTED:
                        broadcastUpdate(ACTION_GATT_DISCONNECTED, address, status);
                        handleNotification(Const.NOTIFY_DC, null);
                        break;
                    default:
                        // Log.e(TAG, "New state not processed: " + newState);
                        break;
                }
            } catch (NullPointerException e) {
                e.printStackTrace();
            }
        }

        @Override
        public void onServicesDiscovered(BluetoothGatt gatt, int status) {
            if (status == BluetoothGatt.GATT_SUCCESS) {
                U.log("Services discovered: " + getSupportedGattServices().size());
                BluetoothDevice device = gatt.getDevice();
                broadcastUpdate(ACTION_GATT_SERVICES_DISCOVERED, device.getAddress(), status);

            } else {
                Toast.makeText(getApplication(), "Service discovery failed",
                        Toast.LENGTH_LONG).show();
                return;
            }

        }

        @Override
        public void onCharacteristicChanged(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic) {

            if (Const.KEY_DEVICE_TRIGGER.equals(characteristic.getUuid())) {

                int severity = characteristic.getValue()[0];

                if (severity == Const.CHAR_VALUE_LOW || severity == 0x1)
                {

                    //characteristic.setValue(new byte[]{0x21});
                    //U.log("Gatt callback found. Broadcasting key data");
                    //mBluetoothGatt.readCharacteristic(characteristic);
                    if (!mLocationManager.isProviderEnabled(LocationManager.GPS_PROVIDER) || !mLocationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER))
                    {
                        notifyUser(Const.NOTIFY_GPS_LOST, R.drawable.logo_ab_servabo, "GPS is Off", "You must turn on GPS to send a notification!", null, true);
                    }
                    else
                    {
                        handleNotification(Const.NOTIFY_LOW, null, characteristic);
                    }
                }

                else if (severity == Const.CHAR_VALUE_HIGH || severity == 0x2)
                {
                    if (!mLocationManager.isProviderEnabled(LocationManager.GPS_PROVIDER) || !mLocationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER))
                    {
                        notifyUser(Const.NOTIFY_GPS_LOST, R.drawable.logo_ab_servabo, "GPS is Off", "You must turn on GPS to send a notification!", null, true);
                    }
                    else
                    {
                        handleNotification(Const.NOTIFY_HIGH, null, characteristic);
                    }
                    //notifyUser(Const.NOTIFY_HIGH, R.drawable.ic_high_severity, "Emergency!", "I really need help!");
                }

                broadcastUpdate(ACTION_DATA_NOTIFY, characteristic, BluetoothGatt.GATT_SUCCESS);


            }
        }

        @Override
        public void onCharacteristicRead(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status) {

            if (Const.KEY_BATTERY_LEVEL.equals(characteristic.getUuid())) {

                int intensity = characteristic.getValue()[0];


            }

            broadcastUpdate(ACTION_DATA_READ, characteristic, status);


        }

        @Override
        public void onCharacteristicWrite(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status) {
            broadcastUpdate(ACTION_DATA_WRITE, characteristic, status);

            if (characteristic.getUuid().equals(SensorTagGatt.UUID_KEY_DATA) ) {

//                U.log("Write received");
//
//                String value1 = String.valueOf(characteristic.getValue()[0]);
//                String value2 = String.valueOf(characteristic.getValue()[1]);

//                byte[] confirmValue = new byte[]{"33", "33"};

//                if (Arrays.equals(confirmValue, characteristic.getValue()))
//                {
                    //characteristic.setValue("33");
                    //mBluetoothGatt.writeCharacteristic(characteristic);
//                }
//
            }
        }

        @Override
        public void onDescriptorRead(BluetoothGatt gatt,
                                     BluetoothGattDescriptor descriptor, int status) {
            mBusy = false;
        }

        @Override
        public void onDescriptorWrite(BluetoothGatt gatt,
                                      BluetoothGattDescriptor descriptor, int status) {
            // Log.i(TAG, "onDescriptorWrite: " + descriptor.getUuid().toString());
            mBusy = false;
        }
    };

}
