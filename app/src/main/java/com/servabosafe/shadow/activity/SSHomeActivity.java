package com.servabosafe.shadow.activity;

import android.content.*;
import android.os.Bundle;
import android.os.IBinder;
import android.view.Menu;
import android.view.MenuItem;
import com.servabosafe.shadow.R;
import com.servabosafe.shadow.data.model.Const;
import com.servabosafe.shadow.data.service.ShadowListenerService;
import com.servabosafe.shadow.fragment.SSHomeDefaultFragment;
import com.servabosafe.shadow.fragment.SSHomeDetailFragment;
import com.servabosafe.shadow.helper.db.ScenarioDataSource;
import com.servabosafe.shadow.helper.prefs.Prefs;

import java.sql.SQLException;

/**
 * Created by brandon.burton on 10/10/14.
 */
public class SSHomeActivity extends SSCoreDrawerActivity {

    private ScenarioDataSource mDataSource;

    private ShadowListenerService mShadowService;

    private boolean isTableEmpty;

    private MenuItem mStatusIcon;

    private boolean isRegisterReceived = false;

    //private AnimatingRefreshButtonManager mRefreshManager;

    //private Integer mLastKnownPower = -1;

    // handler for received Intents for the "my-event" event


    private boolean isBound = false;

    private ServiceConnection mConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {

            mShadowService = ((ShadowListenerService.BluetoothBinder)service).getService();

            registerReceiver(mBatteryReceiver, new IntentFilter("com.servabosafe.shadow.batterybroadcast"));

            isRegisterReceived = true;

            //mShadowService.getBatteryLevel();

            //Toast.makeText(getActivity(), "Fragment connected to service.", Toast.LENGTH_SHORT).show();
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {

            mShadowService = null;

            unregisterReceiver(mBatteryReceiver);

            //Toast.makeText(getActivity(), "Fragment disconnected from service.", Toast.LENGTH_SHORT).show();

        }
    };

    private BroadcastReceiver mBatteryReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {

            // Extract data included in the Intent
            byte batteryLevel = intent.getByteExtra(Const.KEY_BATTERY_LEVEL.toString(), Byte.valueOf("-1"));
            String level = String.valueOf(batteryLevel);

            //Toast.makeText(SSHomeActivity.this, "Battery level is " + level, Toast.LENGTH_LONG).show();
            //mRefreshManager.onRefreshComplete();

            int lastKnownPower = Integer.valueOf(level);

            refreshView(lastKnownPower);

//            SharedPreferences prefs = getSharedPreferences(Prefs.PREFS_KEY, Context.MODE_PRIVATE);
//            SharedPreferences.Editor edit = prefs.edit();
//            edit.putInt(Prefs.KEY_BATTERY_LEVEL, lastKnownPower);
//            edit.apply();
        }
    };

    @Override
    public void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);

        setActivityView(R.layout.activity_home);

        mDataSource = new ScenarioDataSource(this);

        initUi();

        doBindService();
    }

    @Override
    protected void onResume() {

        super.onResume();

        initUi();
    }

    @Override
    protected void onStop() {

        if (isRegisterReceived) {
            isRegisterReceived = false;
            unregisterReceiver(mBatteryReceiver);
        }

        super.onStop();
    }

    @Override
    protected void onDestroy() {

        doUnbindService();

        super.onDestroy();

    }

    private void initUi()
    {
        try
        {
            mDataSource.open();
            isTableEmpty = mDataSource.isEmpty();
            mDataSource.close();
        }
        catch (SQLException e)
        {

        }

        if (findViewById(R.id.layout_fragment_holder) != null)
        {

            if (isTableEmpty) {
                //if we have scenarios
                SSHomeDefaultFragment fragment = new SSHomeDefaultFragment();
                getFragmentManager().beginTransaction().replace(R.id.layout_fragment_holder, fragment).commit();
            }
            else
            {
                SSHomeDetailFragment fragment = new SSHomeDetailFragment();
                getFragmentManager().beginTransaction().replace(R.id.layout_fragment_holder, fragment).commit();
            }
        }

        if (mShadowService != null) {
            if (!mShadowService.isBluetoothConnected()) {
                refreshView(-1);
            }
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {

        getMenuInflater().inflate(R.menu.menu_with_dashboard, menu);
        mStatusIcon = menu.findItem(R.id.action_show_dashboard);

        SharedPreferences prefs = getSharedPreferences(Prefs.PREFS_KEY, Context.MODE_PRIVATE);
        refreshView(prefs.getInt(Prefs.KEY_BATTERY_LEVEL, -1));

        //mRefreshManager = new AnimatingRefreshButtonManager(getActivity(), mStatusIcon);
        //mRefreshManager.onRefreshBeginning();

        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {

        mStatusIcon = menu.findItem(R.id.action_show_dashboard);

        SharedPreferences prefs = getSharedPreferences(Prefs.PREFS_KEY, Context.MODE_PRIVATE);
        refreshView(prefs.getInt(Prefs.KEY_BATTERY_LEVEL, -1));

        //mRefreshManager = new AnimatingRefreshButtonManager(getActivity(), mStatusIcon);
        //mRefreshManager.onRefreshBeginning();

        return super.onPrepareOptionsMenu(menu);
    }

    private void refreshView(int power)
    {
        if (mStatusIcon != null) {
            if (power > 50)
                mStatusIcon.setIcon(R.drawable.ic_battery_max);
            else if (power > 25)
                mStatusIcon.setIcon(R.drawable.ic_battery_mid);
            else if (power > 0)
                mStatusIcon.setIcon(R.drawable.ic_battery_low);
            else
                mStatusIcon.setIcon(R.drawable.logo_ab_servabo);
        }

    }


    private void doBindService() {

        //the service is bound
        bindService(new Intent(SSHomeActivity.this, ShadowListenerService.class), mConnection, Context.BIND_AUTO_CREATE);

        //Toast.makeText(SSConnectActivity.this, "Bound from activity", Toast.LENGTH_SHORT).show();

        //our service bound
        isBound = true;

    }

    private void doUnbindService() {

        if (isBound) {
            unbindService(mConnection);
            isBound = false;
            //Toast.makeText(SSConnectActivity.this, "Unbound from activity", Toast.LENGTH_SHORT).show();
        }
    }

}
