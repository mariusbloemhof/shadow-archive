package com.servabosafe.shadow.activity;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.content.*;
import android.content.pm.PackageManager;
import android.content.res.Resources;
import android.location.LocationManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.preference.PreferenceManager;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;
import com.servabosafe.shadow.R;
import com.servabosafe.shadow.ble.BleDeviceInfo;
import com.servabosafe.shadow.data.service.ShadowListenerService;
import com.servabosafe.shadow.helper.U;
import com.servabosafe.shadow.helper.prefs.Prefs;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Set;

/**
 * Created by brandon.burton on 12/1/14.
 */
public class SSConnectBLEActivity extends SSCoreDrawerActivity {

    private boolean isBound = false;

    //placeholder to segue to next screen
    private Button mScanButton;

    //bluetooth reference
    private BluetoothAdapter mBluetoothAdapter;

    //a list of bluetooth devices that are currently paired
    private BluetoothDevice[] mPairedDevices;


    private boolean mBleSupported = true;
    private boolean mScanning = false;

    // Requests to other activities
    private static final int REQ_ENABLE_BT = 0;
    private static final int REQ_DEVICE_ACT = 1;

    private static final int NO_DEVICE = -1;
    private boolean mInitialised = false;

    private int mConnIndex = NO_DEVICE;

    private int mNumDevs = 0;

    private boolean isRegisterReceived = false;

    private BluetoothDevice mBluetoothDevice;

    //private BluetoothDevice mServiceDevice;

    //get a list of bluetooth devices
    private ArrayList<BluetoothDevice> mDeviceList = new ArrayList<BluetoothDevice>();

    private List<BleDeviceInfo> mDeviceInfoList;

    //device map
    private LinkedHashMap<Integer, BluetoothDevice> mDeviceMap;

    //the device address
    //private String mDeviceAddress;

    //dialog progress
    private ProgressDialog mProgressDialog;

    //debug
    private TextView mStatus;

    private SharedPreferences mSharedPreferences;

    private IntentFilter mFilter;
    private String[] mDeviceFilter = {"SensorTag"};

    //data
    //private DeviceFoundReceiver mDataReceiver;

    //our listening service
    private ShadowListenerService mShadowService;


    private ServiceConnection mConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {

            //mBluetoothLeService = ((ShadowListenerService.BluetoothBinder)service).getService();
            mShadowService = ((ShadowListenerService.BluetoothBinder)service).getService();

            mScanButton.setOnClickListener(new ConnectDeviceListener());

            if (mShadowService != null) {

            } else {
                Toast.makeText(getApplicationContext(), "Could not connect to Shadow app services! Please close and open app.", Toast.LENGTH_SHORT).show();
            }
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {

            mShadowService = null;
        }
    };

    private void doBindService() {

        //the service is bound
        bindService(new Intent(SSConnectBLEActivity.this, ShadowListenerService.class), mConnection, Context.BIND_AUTO_CREATE);

        isBound = true;

    }

    private void doUnbindService() {

        if (isBound) {
            unbindService(mConnection);
            isBound = false;
        }
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {

        requestWindowFeature(Window.FEATURE_INDETERMINATE_PROGRESS);

        // Use this check to determine whether BLE is supported on the device. Then
        // you can selectively disable BLE-related features.
        if (!getPackageManager().hasSystemFeature(
                PackageManager.FEATURE_BLUETOOTH_LE)) {
            Toast.makeText(this, R.string.ble_not_supported, Toast.LENGTH_LONG)
                    .show();
            mBleSupported = false;
        }

        super.onCreate(savedInstanceState);

        mSharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);

        mDeviceInfoList = new ArrayList<BleDeviceInfo>();

        mDeviceMap = new LinkedHashMap<Integer, BluetoothDevice>();

        Resources res = getResources();
        mDeviceFilter = res.getStringArray(R.array.device_filter);

        //this sets the layout and notifys the core activity
        setActivityView(R.layout.activity_connect);

        mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();


        final LocationManager manager = (LocationManager) getSystemService( Context.LOCATION_SERVICE );

        if (!manager.isProviderEnabled(LocationManager.GPS_PROVIDER) || !manager.isProviderEnabled(LocationManager.NETWORK_PROVIDER))
        {
            AlertDialog.Builder builder = new AlertDialog.Builder(this);
            builder.setTitle("Warning").setMessage("Your GPS is currently off. Please enable location services.");
            builder.setPositiveButton("Open", new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    Intent gpsStart = new Intent(android.provider.Settings.ACTION_LOCATION_SOURCE_SETTINGS);
                    startActivity(gpsStart);
                }
            });
            builder.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    AlertDialog.Builder warn = new AlertDialog.Builder(SSConnectBLEActivity.this);
                    warn.setTitle("Error");
                    warn.setMessage("Please enable GPS to use this app.");
                    warn.setCancelable(true);
                    warn.setNegativeButton("Ok", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {

                        }
                    }).create().show();
                }
            }).create().show();

        }

        if (!mBluetoothAdapter.isEnabled()) {
            startActivityForResult(new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE), 200);
        }

        //mDataReceiver = new DeviceFoundReceiver();

        mScanButton = (Button)findViewById(R.id.button_continue);

        mStatus = (TextView)findViewById(R.id.label_status);

        mScanButton.setOnClickListener(new TemporalConnectDeviceListener());

        doBindService();

        mProgressDialog 		= new ProgressDialog(this);

        mProgressDialog.setMessage("Scanning will take 5 seconds...");
        mProgressDialog.setCancelable(false);
        mProgressDialog.setButton(DialogInterface.BUTTON_NEGATIVE, "Cancel", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.dismiss();

                mBluetoothAdapter.cancelDiscovery();
                mScanButton.setEnabled(true);
            }
        });

        mFilter = new IntentFilter(BluetoothAdapter.ACTION_STATE_CHANGED);
        mFilter.addAction(ShadowListenerService.ACTION_GATT_CONNECTED);
        mFilter.addAction(ShadowListenerService.ACTION_GATT_DISCONNECTED);
        mFilter.addAction(BluetoothDevice.ACTION_FOUND);
        mFilter.addAction(BluetoothAdapter.ACTION_DISCOVERY_STARTED);
        mFilter.addAction(BluetoothAdapter.ACTION_DISCOVERY_FINISHED);
        mFilter.addAction(BluetoothDevice.ACTION_BOND_STATE_CHANGED);

        registerReceiver(mReceiver, mFilter);

        isRegisterReceived = true;

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

        if (bestAddress == null)
        {
            mScanButton.setText("Scan to Pair Devices");
        }
        else
        {
            mScanButton.setText("Connect Shadow Device");
        }
    }

    @Override
    public void onPause() {

        if (mBluetoothAdapter != null) {
            if (mBluetoothAdapter.isDiscovering()) {
                mBluetoothAdapter.cancelDiscovery();
            }
        }

        super.onPause();

    }

    @Override
    protected void onStop() {

        super.onStop();

        if (isRegisterReceived) {
            unregisterReceiver(mReceiver);
            isRegisterReceived = false;
        }

        //when the activity is out of view, just finish it
        finish();
    }

    @Override
    public void onDestroy() {

        super.onDestroy();

        if (mShadowService != null) {
            //if (mScanning)
            //    scanLeDevice(false);
            //unregisterReceiver(mReceiver);
            doUnbindService();
        }

        mBluetoothAdapter = null;

        // Clear cache
        File cache = getCacheDir();
        String path = cache.getPath();
        try {
            Runtime.getRuntime().exec(String.format("rm -rf %s", path));
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }

    }


    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {

        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == 200 && resultCode == Activity.RESULT_OK)
        {
            Toast.makeText(this, "Bluetooth is enabled", Toast.LENGTH_SHORT).show();
            //perform scan
        }

    }

    private class TemporalConnectDeviceListener implements View.OnClickListener {

        @Override
        public void onClick(View view) {
            Toast.makeText(getApplicationContext(), "Waiting for service connection...", Toast.LENGTH_SHORT).show();
        }
    }

    private class ConnectDeviceListener implements View.OnClickListener {

        @Override
        public void onClick(View view) {

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

            setProgressBarIndeterminateVisibility(true);

            if (bestAddress == null) {
//                Toast.makeText(getApplicationContext(), "No paired devices match SensorTag. Please pair the device from the Bluetooth menu", Toast.LENGTH_SHORT).show();
                startScan();
                //mBluetoothAdapter.startDiscovery();
                mProgressDialog.show();
                updateGUI(false);
                final Handler h = new Handler();
                Runnable runnable = new Runnable() {
                    @Override
                    public void run() {
                        stopScan();
                        //mBluetoothAdapter.cancelDiscovery();
                        updateGUI(false);
                        mProgressDialog.dismiss();
                        setProgressBarIndeterminateVisibility(false);
                    }
                };
                h.postDelayed(runnable, 5000);

                setProgressBarIndeterminateVisibility(false);
            } else {
                mScanButton.setEnabled(false);
                mShadowService.connect(bestAddress);
            }
        }
    }

    // ////////////////////////////////////////////////////////////////////////////////////////////////
    //
    // Broadcasted actions from Bluetooth adapter and BluetoothLeService
    //
    private BroadcastReceiver mReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {

            final String action = intent.getAction();

            if (BluetoothAdapter.ACTION_STATE_CHANGED.equals(action)) {
                // Bluetooth adapter state change
                switch (mBluetoothAdapter.getState()) {
                    case BluetoothAdapter.STATE_ON:
                        mConnIndex = NO_DEVICE;
                        //startBluetoothLeService();
                        U.log("Adapter is on");
                        break;
                    case BluetoothAdapter.STATE_OFF:
                        U.log("Adapter is off");
                        Toast.makeText(context, "Please enable Bluetooth.", Toast.LENGTH_LONG).show();
                        //finish();
                        break;
                    default:
                        // Log.w(TAG, "Action STATE CHANGED not processed ");
                        break;
                }

                //updateGuiState();
            }

            else if (ShadowListenerService.ACTION_GATT_CONNECTED.equals(action)) {
                // GATT connect
                int status = intent.getIntExtra(ShadowListenerService.EXTRA_STATUS, BluetoothGatt.GATT_FAILURE);

                if (status == BluetoothGatt.GATT_SUCCESS) {
                    //setBusy(false);
                    setProgressBarIndeterminateVisibility(false);
                    startHomeActivity();
                } else
                    Toast.makeText(SSConnectBLEActivity.this, "Connect failed. Status: . Please reset by turning your Bluetooth off then re-enter the app." + status, Toast.LENGTH_SHORT).show();
                    mScanButton.setEnabled(true);
            }

            else if (ShadowListenerService.ACTION_GATT_DISCONNECTED.equals(action)) {
                // GATT disconnect
                int status = intent.getIntExtra(ShadowListenerService.EXTRA_STATUS,
                        BluetoothGatt.GATT_FAILURE);
                //stopDeviceActivity();
                if (status == BluetoothGatt.GATT_SUCCESS) {
                    //setBusy(false);
//                    mScanView.setStatus(mBluetoothDevice.getName() + " disconnected",
//                            STATUS_DURATION);
                    U.log("Disconnected", Toast.LENGTH_SHORT);
                } else {
                    U.log("Disconnect failed", Toast.LENGTH_SHORT);
                }
                mConnIndex = NO_DEVICE;
                //mShadowService.close();
            }

            else if (BluetoothDevice.ACTION_FOUND.equals(action))
            {
                BluetoothDevice device = (BluetoothDevice)intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                //U.log("device name: " + device.getName());
                U.log("Found device");
                mDeviceMap.put(device.hashCode(), device);
            }

            else if (BluetoothAdapter.ACTION_DISCOVERY_STARTED.equals(action))
            {
                mDeviceList = new ArrayList<BluetoothDevice>();
            }

            else if (BluetoothAdapter.ACTION_DISCOVERY_FINISHED.equals(action))
            {
                final ArrayList<BluetoothDevice> mSensorTags = new ArrayList<BluetoothDevice>();
                for (BluetoothDevice device : mDeviceMap.values())
                {
                    if (device != null) {
                        if (device.getName() != null) {
                            if (device.getName().contains("SensorTag") || device.getAddress().contains("D0"))
                            {
                                mSensorTags.add(device);
                                U.log("Sensor tag was found");

                            }
                        }
                    }
                }

                if (mSensorTags.size() == 1)
                {
                    mShadowService.attemptPairWithDevice(mSensorTags.get(0));
                    mScanButton.setEnabled(false);

                    final Handler handler = new Handler();

                    Runnable waitRunnable = new Runnable() {
                        @Override
                        public void run() {

                            mScanButton.setEnabled(true);
                        }
                    };

                    //runOnUiThread(waitRunnable);
                    handler.postDelayed(waitRunnable, 10000);
                }
                else if (mSensorTags.size() > 1)
                {
                    mScanButton.setEnabled(false);
                    showDevicesDialog();


                    final Handler handler = new Handler();

                    Runnable waitRunnable = new Runnable() {
                        @Override
                        public void run() {

                            mScanButton.setEnabled(true);
                        }
                    };

                    //runOnUiThread(waitRunnable);
                    handler.postDelayed(waitRunnable, 5000);

                }
                else if (mSensorTags.isEmpty())
                {
                    Toast.makeText(SSConnectBLEActivity.this, "No devices were found", Toast.LENGTH_SHORT).show();
                }
                else
                {
                    mScanButton.setEnabled(true);
                    mScanButton.setText("Scan for Devices");
                }
            }
            else if (BluetoothDevice.ACTION_BOND_STATE_CHANGED.equals(action)) {
                final int state        = intent.getIntExtra(BluetoothDevice.EXTRA_BOND_STATE, BluetoothDevice.ERROR);
                final int prevState    = intent.getIntExtra(BluetoothDevice.EXTRA_PREVIOUS_BOND_STATE, BluetoothDevice.ERROR);

                if (state == BluetoothDevice.BOND_BONDED && prevState == BluetoothDevice.BOND_BONDING)
                {
                    U.log("You have paired a device.");

                    Toast.makeText(getApplicationContext(), "You have successfully paired the device", Toast.LENGTH_SHORT).show();

//                    String prevAddress = mSharedPreferences.getString(Prefs.KEY_ADDRESS, null);
//
//                    U.log("PrevAd: " + prevAddress);
//
                    String pairedDevice = checkForSensorTag();

                    if (pairedDevice != null)
                    {
                        mSharedPreferences.edit().putString(Prefs.KEY_ADDRESS, pairedDevice);

                        if (mShadowService.connect(pairedDevice))
                        {
                            U.log("Connecting");
                        }
                        else
                        {
                            U.log("Error in connecting");
                        }

                    }
//
//                    //connected
//                    if (pairState.equals("0x1"))
//                    {
//                        U.log("Previous device is paired from activity");
//
//                        mShadowService.connect(pairState);
//                    }
//                    //unpaired
//                    else if (pairState.equals("0x0"))
//                    {
//                        //attempt to trash connection
//                        U.log("Previous device is unpaired from activity");
//
//                        //mShadowService.connect(pairState);
//                        //connect(prevAddress);
//                    }
//                    //switched
//                    else {
//
//                        mShadowService.disconnect(prevAddress);
//                        mShadowService.close();
//
//                        //connect to address
//                        mShadowService.connect(pairState);
//                    }
                    //mScanButton.setEnabled(true);
                    //mScanButton.setText("Tap To Enter App");

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
                    U.log("You have unpaired a device.");

                    mSharedPreferences.getString(Prefs.KEY_ADDRESS, null);

                    if (checkForSensorTag() == null)
                    {
                        mShadowService.disconnect(checkForSensorTag());
                        mShadowService.close();
                        mSharedPreferences.edit().putString(Prefs.KEY_ADDRESS, null).apply();
                    }


                    mScanButton.setText("Scan To Pair Device");

                    //setCurrentDevice(null);
                    Toast.makeText(getApplicationContext(), "Device is unpaired", Toast.LENGTH_SHORT).show();
                }
            }

            else if (BluetoothDevice.ACTION_PAIRING_REQUEST.equals(action))
            {

            }

            else {
                // Log.w(TAG,"Unknown action: " + action);
            }

        }
    };

    public String checkForSensorTag() {

        Set<BluetoothDevice> pairedDevices = mBluetoothAdapter.getBondedDevices();
        BluetoothDevice[] mPairedDevices = new BluetoothDevice[pairedDevices.size()];
        pairedDevices.toArray(mPairedDevices);

        for (int i = 0; i < mPairedDevices.length; i++)
        {
            BluetoothDevice b = mPairedDevices[i];
            if (b.getAddress().contains("D0") || b.getName().contains("SensorTag")) {
                return mPairedDevices[i].getAddress();
            }

        }

        return null;
    }

    private void startScan() {
        // Start device discovery
        if (mBleSupported) {
            mNumDevs = 0;
            mDeviceInfoList.clear();
            //mScanView.notifyDataSetChanged();
            scanLeDevice(true);
//            mScanView.updateGui(mScanning);
//            if (!mScanning) {
//                setError("Device discovery start failed");
//                setBusy(false);
//            }
        } else {
            //setError("BLE not supported on this device");
        }

    }

    private void stopScan() {
        mScanning = false;
        //mScanView.updateGui(false);
        scanLeDevice(false);
        showDevicesDialog();
    }

    public void onScanTimeout() {
        runOnUiThread(new Runnable() {
            public void run() {
                stopScan();
            }
        });
    }

    public void onConnectTimeout() {
        runOnUiThread(new Runnable() {
            public void run() {
                Toast.makeText(SSConnectBLEActivity.this, "Time out", Toast.LENGTH_SHORT).show();
                //setError("Connection timed out");
            }
        });
        if (mConnIndex != NO_DEVICE) {
            mShadowService.disconnect(mBluetoothDevice.getAddress());
            mConnIndex = NO_DEVICE;
        }
    }

    private boolean scanLeDevice(boolean enable) {
        if (enable) {
            mScanning = mBluetoothAdapter.startLeScan(mLeScanCallback);
        } else {
            mScanning = false;
            mBluetoothAdapter.stopLeScan(mLeScanCallback);
        }
        return mScanning;
    }


    private void startHomeActivity()
    {
        mScanButton.setEnabled(true);
        mScanButton.setText("Tap To Enter App");
        mScanButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent i = new Intent(SSConnectBLEActivity.this, SSHomeActivity.class);
                startActivity(i);
                finish();
                overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
            }
        });
    }

    // Device scan callback.
    // NB! Nexus 4 and Nexus 7 (2012) only provide one scan result per scan
    private BluetoothAdapter.LeScanCallback mLeScanCallback = new BluetoothAdapter.LeScanCallback() {

        public void onLeScan(final BluetoothDevice device, final int rssi,
                             byte[] scanRecord) {
            runOnUiThread(new Runnable() {
                public void run() {
                    // Filter devices
                    if (checkDeviceFilter(device.getName())) {
                        if (!deviceInfoExists(device.getAddress())) {
                            // New device
                            BleDeviceInfo deviceInfo = createDeviceInfo(device, rssi);
                            addDevice(deviceInfo);
                        } else {
                            // Already in list, update RSSI info
                            BleDeviceInfo deviceInfo = findDeviceInfo(device);
                            deviceInfo.updateRssi(rssi);
                            //mScanView.notifyDataSetChanged();
                        }
                    }
                }

            });
        }
    };

    private BleDeviceInfo createDeviceInfo(BluetoothDevice device, int rssi) {
        BleDeviceInfo deviceInfo = new BleDeviceInfo(device, rssi);

        return deviceInfo;
    }

    private boolean checkDeviceFilter(String deviceName) {
        if (deviceName == null)
            return false;

        int n = mDeviceFilter.length;
        if (n > 0) {
            boolean found = false;
            for (int i = 0; i < n && !found; i++) {
                found = deviceName.equals(mDeviceFilter[i]);
            }
            return found;
        } else
            // Allow all devices if the device filter is empty
            return true;
    }

    private boolean deviceInfoExists(String address) {
        for (int i = 0; i < mDeviceInfoList.size(); i++) {
            if (mDeviceInfoList.get(i).getBluetoothDevice().getAddress()
                    .equals(address)) {
                return true;
            }
        }
        return false;
    }

    private BleDeviceInfo findDeviceInfo(BluetoothDevice device) {
        for (int i = 0; i < mDeviceInfoList.size(); i++) {
            if (mDeviceInfoList.get(i).getBluetoothDevice().getAddress()
                    .equals(device.getAddress())) {
                return mDeviceInfoList.get(i);
            }
        }
        return null;
    }

    private void addDevice(BleDeviceInfo device) {
        mNumDevs++;
        mDeviceInfoList.add(device);
        //mScanView.notifyDataSetChanged();
        //if (mNumDevs > 1)
           // mScanView.setStatus(mNumDevs + " devices");
        //else
            //mScanView.setStatus("1 device");
    }

    private void updateGUI(boolean isScanning)
    {
        if (mScanButton != null)
        {
            if (isScanning) {
                mScanButton.setText("Scanning...");
                mScanButton.setEnabled(false);
            }
            else {
                mScanButton.setText("Scan To Pair Devices");
                mScanButton.setEnabled(true);
            }
        }
    }

    private void showDevicesDialog()
    {

        AlertDialog.Builder alertDialog;

        List<String> filteredList = new ArrayList<String>();
        final List<BluetoothDevice> filteredItems = new ArrayList<BluetoothDevice>();

        //perform search of devices
        //for (BluetoothDevice bd : bluetoothDevices) {
//        for (BluetoothDevice bd : mDeviceList) {
//            if (bd.getName() != null) {
//                if (bd.getName().contains("SensorTag") || bd.getAddress().contains("D0")) {
//                    filteredItems.add(bd);
//                    filteredList.add(bd.getName());
//                }
//            }
//        }

        for (BleDeviceInfo bd : mDeviceInfoList) {
            if (bd.getBluetoothDevice() != null) {
                if (bd.getBluetoothDevice().getName().contains("SensorTag") || bd.getBluetoothDevice().getAddress().contains("D0")) {
                    filteredItems.add(bd.getBluetoothDevice());
                    filteredList.add(bd.getBluetoothDevice().getName());
                }
            }
        }

        alertDialog = new AlertDialog.Builder(SSConnectBLEActivity.this)
                .setTitle(R.string.title_dialog_select_device)
                .setCancelable(true)
                .setOnCancelListener(new DialogInterface.OnCancelListener() {
                    @Override
                    public void onCancel(DialogInterface dialog) {

                    }
                })
                .setSingleChoiceItems(filteredList.toArray(new CharSequence[filteredItems.size()]), -1, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                        //setDeviceAddress(which);
                        dialog.dismiss();
                        if (which != -1) {
                            if (mShadowService != null)
                                mShadowService.attemptPairWithDevice(filteredItems.get(which));
                                updateGUI(true);
                        }
                        else {
                            Toast.makeText(SSConnectBLEActivity.this, "Please select a SensorTag device.", Toast.LENGTH_SHORT).show();
                        }
                        //select device
                        //connect.setChecked(getPairedTag());
                    }
                });

        try {
            alertDialog.create().show();
            mScanButton.setEnabled(true);
            mScanButton.setText("Scan for Devices");
        } catch (WindowManager.BadTokenException b) {
            U.log("Cannot launch device window.");
        }
    }

}

