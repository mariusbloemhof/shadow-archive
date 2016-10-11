package com.servabosafe.shadow.activity;

import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.*;
import android.location.LocationManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;
import com.servabosafe.shadow.R;
import com.servabosafe.shadow.data.service.ShadowListenerService;
import com.servabosafe.shadow.helper.U;
import com.servabosafe.shadow.helper.prefs.Prefs;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Set;

/**
 * Created by brandon.burton on 10/10/14.
 */
public class SSConnectActivity extends SSCoreDrawerActivity {

    private boolean isBound = false;

    //placeholder to segue to next screen
    private Button mScanButton;

    //bluetooth reference
    private BluetoothAdapter mBluetooth;

    //a list of bluetooth devices that are currently paired
    private BluetoothDevice[] mPairedDevices;

    private boolean isRegisterReceived = false;

    private BluetoothDevice mSelectedDevice;

    //get a list of bluetooth devices
    private ArrayList<BluetoothDevice> mDeviceList = new ArrayList<BluetoothDevice>();

    //device map
    private LinkedHashMap<Integer, BluetoothDevice> mDeviceMap;

    //the device address
    private String mDeviceAddress;

    //dialog progress
    private ProgressDialog mProgressDialog;

    //debug
    private TextView mStatus;

    //data
    private DeviceFoundReceiver mDataReceiver;

    //our listening service
    private ShadowListenerService mShadowService;

    //location services
    private LocationManager mLocManager;

    private BroadcastReceiver mBatteryReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {

            startHomeActivity();
            // Extract data included in the Intent
//            byte batteryLevel = intent.getByteExtra(Const.KEY_BATTERY_LEVEL.toString(), Byte.valueOf("-1"));
//            String level = String.valueOf(batteryLevel);
//
//            Toast.makeText(SSHomeActivity.this, "Battery level is " + level, Toast.LENGTH_LONG).show();
//            //mRefreshManager.onRefreshComplete();
//
//            int lastKnownPower = Integer.valueOf(level);
//
//            refreshView(lastKnownPower);
//
//            SharedPreferences prefs = getSharedPreferences(Prefs.PREFS_KEY, Context.MODE_PRIVATE);
//            SharedPreferences.Editor edit = prefs.edit();
//            edit.putInt(Prefs.KEY_BATTERY_LEVEL, lastKnownPower);
//            edit.apply();
        }
    };


    private class DeviceFoundReceiver extends BroadcastReceiver {

        @Override
        public void onReceive(Context context, Intent intent) {

            String action = intent.getAction();

            if (BluetoothDevice.ACTION_FOUND.equals(action))
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
                ArrayList<BluetoothDevice> mSensorTags = new ArrayList<BluetoothDevice>();
                for (BluetoothDevice device : mDeviceMap.values())
                {
                    if (device != null) {
                        if (device.getName() != null) {
                            if (device.getName().contains("SensorTag"))
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
                }
                else if (mSensorTags.size() > 1)
                {
                    showDevicesDialog();
                }
                else if (mSensorTags.isEmpty())
                {
                    Toast.makeText(SSConnectActivity.this, "No devices were found", Toast.LENGTH_SHORT).show();
                }
                else
                {
                    mScanButton.setEnabled(true);
                    mScanButton.setText("Scan for Devices");
                }
            }
            else if (BluetoothDevice.ACTION_PAIRING_REQUEST.equals(action))
            {

            }
        }
    }

    private ServiceConnection mConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {

            mShadowService = ((ShadowListenerService.BluetoothBinder)service).getService();

            isRegisterReceived = true;

            registerReceiver(mBatteryReceiver, new IntentFilter("com.servabosafe.shadow.batterybroadcast"));

            if (mScanButton != null)
            {
                mScanButton.setOnClickListener(new ConnectDeviceListener());

                try {
                    if (mShadowService.getCurrentDevice().getAddress().equals(getPairedTag().getAddress())) {
                        mScanButton.setText("Tap Here To Enter");
                    } else {
                        mScanButton.setText("Different Device Connected");
                    }
                } catch (NullPointerException n) {
                    mScanButton.setText("Scan For Devices");
                }

            }

            Toast.makeText(SSConnectActivity.this, "Activity connected to service.", Toast.LENGTH_SHORT).show();
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {

            mShadowService = null;

            unregisterReceiver(mBatteryReceiver);

            Toast.makeText(SSConnectActivity.this, "Activity disconnected from service.", Toast.LENGTH_SHORT).show();

        }
    };

    private void doBindService() {

        //the service is bound
        bindService(new Intent(SSConnectActivity.this, ShadowListenerService.class), mConnection, Context.BIND_AUTO_CREATE);

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

        super.onCreate(savedInstanceState);

        //this sets the layout and notifys the core activity
        setActivityView(R.layout.activity_connect);

        mBluetooth = BluetoothAdapter.getDefaultAdapter();

        mDataReceiver = new DeviceFoundReceiver();

        mDeviceMap = new LinkedHashMap<Integer, BluetoothDevice>();

        mScanButton = (Button)findViewById(R.id.button_continue);

        mStatus = (TextView)findViewById(R.id.label_status);


        doBindService();

        mProgressDialog 		= new ProgressDialog(this);

        mProgressDialog.setMessage("Scanning...");
        mProgressDialog.setCancelable(false);
        mProgressDialog.setButton(DialogInterface.BUTTON_NEGATIVE, "Cancel", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.dismiss();

                mBluetooth.cancelDiscovery();
            }
        });

        mScanButton.setOnClickListener(new TemporalConnectDeviceListener());

        if (!mBluetooth.isEnabled()) {
            startActivityForResult(new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE), 200);
        }

        //listen for these over Bluetooth
        IntentFilter filter = new IntentFilter();
        filter.addAction(BluetoothDevice.ACTION_FOUND);
        filter.addAction(BluetoothAdapter.ACTION_STATE_CHANGED);
        filter.addAction(BluetoothAdapter.ACTION_DISCOVERY_STARTED);
        filter.addAction(BluetoothAdapter.ACTION_DISCOVERY_FINISHED);
        filter.addAction(BluetoothDevice.ACTION_BOND_STATE_CHANGED);

        registerReceiver(mDataReceiver, filter);

    }

    @Override
    public void onPause() {

        if (mBluetooth != null) {
            if (mBluetooth.isDiscovering()) {
                mBluetooth.cancelDiscovery();
                //mBluetooth.startLeScan(this);
            }
        }


        super.onPause();

    }

    @Override
    protected void onStop() {

        if (isRegisterReceived) {
            isRegisterReceived = false;
            unregisterReceiver(mBatteryReceiver);
        }

        super.onStop();

        //when the activity is out of view, just finish it
        finish();
    }

    @Override
    public void onDestroy() {

        super.onDestroy();

        doUnbindService();

        try {
            unregisterReceiver(mDataReceiver);
        } catch (IllegalArgumentException i) {
            U.log("No receiver registered.");
        }

    }


    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {

        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == 200)
        {
            Toast.makeText(this, "Bluetooth is enabled", Toast.LENGTH_SHORT).show();
            //perform scan
        }

    }

    private BluetoothDevice getPairedTag() {

        Set<BluetoothDevice> pairedDevices = mBluetooth.getBondedDevices();
        mPairedDevices = new BluetoothDevice[pairedDevices.size()];
        pairedDevices.toArray(mPairedDevices);

        SharedPreferences prefs = getSharedPreferences(Prefs.PREFS_KEY, MODE_PRIVATE);
        String address = prefs.getString(Prefs.KEY_ADDRESS, "");

        //check if the device matches address
        for (int i = 0; i < pairedDevices.size(); i++)
        {
            if (mPairedDevices[i].getAddress().equals(address)) {
                return mPairedDevices[i];
            }
        }

        //check if there is a sensor tag attached
        for (int i = 0; i < pairedDevices.size(); i++)
        {
             if (mPairedDevices[i].getName().equals("SensorTag")) {
                return mPairedDevices[i];
            }
        }

        return null;

    }

//    @Override
//    public void onLeScan(BluetoothDevice device, int rssi, byte[] scanRecord) {
//
//        U.log("New LE Device: " + device.getName() + " @ " + rssi);
//        mDeviceMap.put(device.hashCode(), device);
//
//    }

    private class TemporalConnectDeviceListener implements View.OnClickListener {

        @Override
        public void onClick(View v) {
            Toast.makeText(SSConnectActivity.this, "Please wait until the service is connected.", Toast.LENGTH_SHORT).show();
        }
    }

    private class ConnectDeviceListener implements View.OnClickListener {

        @Override
        public void onClick(View view) {

            final View v = view;

            if (mShadowService.getCurrentDevice() == null && getPairedTag() != null) {
                //mShadowService.setCurrentDevice(getPairedTag());
                Toast.makeText(getApplicationContext(), "Please wait while a new connection is set.", Toast.LENGTH_SHORT).show();
                //startHomeActivity();
            } else if (getPairedTag() == null) {
                if (v instanceof Button) {
                    ((Button) v).setText("Scanning...");
                    v.setEnabled(false);
                    normalScanForDevices();
                }
            } else {
                try {
                    if (mShadowService.getCurrentDevice().getAddress().equals(getPairedTag().getAddress())) {
                        //Toast.makeText(getApplicationContext(), "Please wait while we connect your device.", Toast.LENGTH_SHORT).show();
                        startHomeActivity();
                    }
                } catch (NullPointerException n) {
                    Toast.makeText(SSConnectActivity.this, "Device does not match!", Toast.LENGTH_SHORT).show();
                }
            }
        }
    }

    private boolean normalScanForDevices()
    {
        if (!mBluetooth.isEnabled()) {
            return false;
        } else {
            mBluetooth.startDiscovery();
            new Handler().postDelayed(new Runnable() {
                @Override
                public void run() {
                    mBluetooth.cancelDiscovery();
                }
            }, 10000);
            return true;
        }
    }

    private void showDevicesDialog()
    {

        AlertDialog.Builder alertDialog;

        List<String> filteredList = new ArrayList<String>();
        final List<BluetoothDevice> filteredItems = new ArrayList<BluetoothDevice>();

        //perform search of devices
        //for (BluetoothDevice bd : bluetoothDevices) {
        for (BluetoothDevice bd : mDeviceList) {
            if (bd.getName() != null) {
                if (bd.getName().contains("SensorTag")) {
                    filteredItems.add(bd);
                    filteredList.add(bd.getName());
                }
            }
        }

        alertDialog = new AlertDialog.Builder(SSConnectActivity.this)
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
                            if (mShadowService.getCurrentDevice() == null)
                                mShadowService.attemptPairWithDevice(filteredItems.get(which));
                        }
                        else {
                            Toast.makeText(SSConnectActivity.this, "Please select a SensorTag device.", Toast.LENGTH_SHORT).show();
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

    private void startHomeActivity()
    {
        Intent i = new Intent(SSConnectActivity.this, SSHomeActivity.class);
        startActivity(i);
        finish();
        overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);

    }



}
