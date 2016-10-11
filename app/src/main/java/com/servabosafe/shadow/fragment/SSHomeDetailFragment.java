package com.servabosafe.shadow.fragment;

import android.app.Fragment;
import android.content.*;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.view.*;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.ListView;
import android.widget.TextView;
import com.servabosafe.shadow.R;
import com.servabosafe.shadow.activity.SSCreateEventActivity;
import com.servabosafe.shadow.adapter.ScenarioAdapter;
import com.servabosafe.shadow.data.model.Scenario;
import com.servabosafe.shadow.data.service.ShadowListenerService;
import com.servabosafe.shadow.helper.U;
import com.servabosafe.shadow.helper.db.ScenarioDataSource;
import com.servabosafe.shadow.helper.prefs.Prefs;

import java.sql.SQLException;
import java.util.List;

/**
 * Created by brandon.burton on 10/10/14.
 */
public class SSHomeDetailFragment extends Fragment implements AdapterView.OnItemClickListener, ActionMode.Callback {

    private Button mCreateEmergency;

    private ListView mEmergencyList;

    private TextView mLowPriority;

    private TextView mHighPriority;

    private MenuItem mStatusIcon;

    private ScenarioAdapter mDataAdapter;

    private ScenarioDataSource mDataSource;

    private ShadowListenerService mShadowService;

    private boolean isRegisterReceived = false;

    private boolean performWrite = false;

    //private AnimatingRefreshButtonManager mRefreshManager;

    //private Integer mLastKnownPower = -1;

    // handler for received Intents for the "my-event" event
//    private BroadcastReceiver mBatteryReceiver = new BroadcastReceiver() {
//        @Override
//        public void onReceive(Context context, Intent intent) {
//
//            // Extract data included in the Intent
//            byte batteryLevel = intent.getByteExtra(Const.KEY_BATTERY_LEVEL.toString(), Byte.valueOf("-1"));
//            String level = String.valueOf(batteryLevel);
//
//            Toast.makeText(getActivity(), "Battery level is " + level, Toast.LENGTH_LONG).show();
//            //mRefreshManager.onRefreshComplete();
//
//            int lastKnownPower = Integer.valueOf(level);
//
//            refreshView(lastKnownPower);
//
//            SharedPreferences prefs = getActivity().getSharedPreferences(Prefs.PREFS_KEY, Context.MODE_PRIVATE);
//            SharedPreferences.Editor edit = prefs.edit();
//            edit.putInt(Prefs.KEY_BATTERY_LEVEL, lastKnownPower);
//            edit.apply();
//        }
//    };

    private boolean isBound = false;

    private ServiceConnection mConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {

            mShadowService = ((ShadowListenerService.BluetoothBinder)service).getService();

            try {
                mDataSource.open();

                SharedPreferences s = getActivity().getSharedPreferences(Prefs.PREFS_KEY, Context.MODE_PRIVATE);

                Integer lowSeverity = s.getInt(Prefs.KEY_DB_SCENARIO_LOW, -1);
                Integer highSeverity = s.getInt(Prefs.KEY_DB_SCENARIO_HIGH, -1);

                if (performWrite)
                {
                    mShadowService.setLowSeverity(mDataSource.getScenario(lowSeverity));
                    mShadowService.setHighSeverity(mDataSource.getScenario(highSeverity));
                    performWrite = false;
                }
                mDataSource.close();
            } catch (SQLException e) {
                e.printStackTrace();
            }


            //mDataAdapter.clear();

//            List<Scenario> mScenarios = mDataSource.getAllScenarios();
//
//            SharedPreferences s = getActivity().getSharedPreferences(Prefs.PREFS_KEY, Context.MODE_PRIVATE);
//            Integer lowSeverity = s.getInt(Prefs.KEY_DB_SCENARIO_LOW, -1);
//            Integer highSeverity = s.getInt(Prefs.KEY_DB_SCENARIO_HIGH, -1);
//
//            try {
//                if (mScenarios.get(lowSeverity) != null) {
//                    mShadowService.setLowSeverity(mScenarios.get(lowSeverity));
//                }
//                if (mScenarios.get(highSeverity) != null) {
//                    mShadowService.setHighSeverity(mScenarios.get(highSeverity));
//                }
//            }


            //getActivity().registerReceiver(mBatteryReceiver, new IntentFilter("com.servabosafe.shadow.batterybroadcast"));

            isRegisterReceived = true;

//            final BluetoothManager bm = (BluetoothManager)getActivity().getSystemService(Context.BLUETOOTH_SERVICE);
//            bm.getConnectedDevices(BluetoothProfile.GATT);

            //mShadowService.getBatteryLevel();

            //Toast.makeText(getActivity(), "Fragment connected to service.", Toast.LENGTH_SHORT).show();
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {

            mShadowService = null;

            //getActivity().unregisterReceiver(mBatteryReceiver);

            //Toast.makeText(getActivity(), "Fragment disconnected from service.", Toast.LENGTH_SHORT).show();

        }
    };

    private ActionMode mActionMode = null;

    @Override
    public void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);

        //setHasOptionsMenu(true);

    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {

        return inflater.inflate(R.layout.fragment_home, container, false);

    }

    @Override
    public void onDestroy() {

        super.onDestroy();

        //getActivity().stopService(new Intent(getActivity(), ShadowListenerService.class));
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {

        super.onActivityCreated(savedInstanceState);

        mCreateEmergency = (Button)getView().findViewById(R.id.button_add_emergency);

        mEmergencyList = (ListView)getView().findViewById(R.id.list_emergencies);

        mLowPriority = (TextView)getView().findViewById(R.id.label_low_severity);

        mHighPriority = (TextView)getView().findViewById(R.id.label_high_severity);

        mDataAdapter = new ScenarioAdapter(getActivity());

        mEmergencyList.setAdapter(mDataAdapter);

        mEmergencyList.setOnItemClickListener(this);

        //get access to the database
        mDataSource = new ScenarioDataSource(getActivity());

        mCreateEmergency.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                Intent i = new Intent(getActivity(), SSCreateEventActivity.class);
                startActivity(i);
                getActivity().overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);

            }
        });

        try {

            loadScenarioData();

        }
        catch (SQLException e) {

            e.printStackTrace();
            U.log("Error retrieving data");

        }

        doBindService();



    }

    @Override
    public void onDetach() {

        super.onDetach();

        doUnbindService();
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {

        inflater.inflate(R.menu.menu_with_dashboard, menu);
        mStatusIcon = menu.findItem(R.id.action_show_dashboard);

        //mRefreshManager = new AnimatingRefreshButtonManager(getActivity(), mStatusIcon);
        //mRefreshManager.onRefreshBeginning();

        super.onCreateOptionsMenu(menu, inflater);
    }

    @Override
    public void onPrepareOptionsMenu(Menu menu) {

        mStatusIcon = menu.findItem(R.id.action_show_dashboard);

        //SharedPreferences prefs = getActivity().getSharedPreferences(Prefs.PREFS_KEY, Context.MODE_PRIVATE);
        //refreshView(prefs.getInt(Prefs.KEY_BATTERY_LEVEL, -1));

        //mRefreshManager = new AnimatingRefreshButtonManager(getActivity(), mStatusIcon);
        //mRefreshManager.onRefreshBeginning();

        super.onPrepareOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {

//        switch (item.getItemId())
//        {
//            case 4:
//                Toast.makeText(getActivity(), "The battery level is " + mLastKnownPower, Toast.LENGTH_SHORT).show();
//                break;
//        }

        return super.onOptionsItemSelected(item);

    }

    private void loadScenarioData() throws SQLException {

        mDataSource.open();

        mDataAdapter.clear();

        List<Scenario> mScenarios = mDataSource.getAllScenarios();

        SharedPreferences s = getActivity().getSharedPreferences(Prefs.PREFS_KEY, Context.MODE_PRIVATE);
        Integer lowSeverity = s.getInt(Prefs.KEY_DB_SCENARIO_LOW, -1);
        Integer highSeverity = s.getInt(Prefs.KEY_DB_SCENARIO_HIGH, -1);

        mLowPriority.setText("Select an emergency...");

        mHighPriority.setText("Select an emergency...");

        if (lowSeverity == -1)
        {
            try
            {
                Scenario scenario = mScenarios.get(0);
                String text = scenario.getTitle();
                mLowPriority.setText(text);

                if (!isRegisterReceived) {
                    //mShadowService.setLowSeverity(scenario);
                    //if the service is not bound
                    performWrite = true;
                }

                SharedPreferences prefs = getActivity().getSharedPreferences(Prefs.PREFS_KEY, Context.MODE_PRIVATE);
                prefs.edit().putInt(Prefs.KEY_DB_SCENARIO_LOW, scenario.getId()).apply();

            }
            catch (IndexOutOfBoundsException i)
            {
                U.log("No scenario matches low severity");
            }
            catch (NullPointerException n)
            {
                U.log("No scenarios");
            }

            //preferences
        }
        if (highSeverity == -1)
        {
            try
            {
                Scenario scenario = mScenarios.get(0);
                String text = scenario.getTitle();
                mHighPriority.setText(text);

                if (!isRegisterReceived) {
                    //mShadowService.setHighSeverity(scenario);
                    //if the service has not been bound
                    performWrite = true;
                }

                SharedPreferences prefs = getActivity().getSharedPreferences(Prefs.PREFS_KEY, Context.MODE_PRIVATE);
                prefs.edit().putInt(Prefs.KEY_DB_SCENARIO_HIGH, scenario.getId()).apply();
            }
            catch (IndexOutOfBoundsException i)
            {
                U.log("No scenario matches high severity");
            }
            catch (NullPointerException n)
            {
                U.log("No scenarios");
            }
        }

        for (Scenario c : mScenarios) {

            mDataAdapter.add(c);

            if (lowSeverity == c.getId())
            {
                mLowPriority.setText(c.getTitle());
            }
            if (highSeverity == c.getId())
            {
                mHighPriority.setText(c.getTitle());
            }
        }


        mDataAdapter.notifyDataSetChanged();

        mEmergencyList.invalidate();

        mDataSource.close();

    }

    @Override
    public void onResume() {

        super.onResume();

        getActivity().setTitle("Home");

    }

    @Override
    public void onPause() {

        super.onPause();

        if (mActionMode != null)
            mActionMode.finish();
    }

    @Override
    public void onStop() {

        if (isRegisterReceived) {
            isRegisterReceived = false;
            //getActivity().unregisterReceiver(mBatteryReceiver);
        }

        super.onStop();
    }

    // Called when the action mode is created; startActionMode() was called
    @Override
    public boolean onCreateActionMode(ActionMode mode, Menu menu) {
        // Inflate a menu resource providing context menu items
        MenuInflater inflater = mode.getMenuInflater();
        inflater.inflate(R.menu.menu_multi_opt, menu);
        return true;
    }

    // Called each time the action mode is shown. Always called after onCreateActionMode, but
    // may be called multiple times if the mode is invalidated.
    @Override
    public boolean onPrepareActionMode(ActionMode mode, Menu menu) {

        // update title with number of checked items
        mode.setTitle("Options");

        return true;
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {

        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == 200) {
            SharedPreferences prefs = getActivity().getSharedPreferences(Prefs.PREFS_KEY, Context.MODE_PRIVATE);
            int lowId = prefs.getInt(Prefs.KEY_DB_SCENARIO_LOW, -1);
            int highId = prefs.getInt(Prefs.KEY_DB_SCENARIO_HIGH, -1);

            if (data != null) {
                int idReturned = data.getExtras().getInt("id", -1);
                if (idReturned != -1) {
                    try {
                        loadScenarioData();

                        for (int i = 0; i < mDataAdapter.getCount(); i++) {
                            if (mDataAdapter.getItem(i).getId() == lowId) {
                                mShadowService.setLowSeverity(mDataAdapter.getItem(i));
                            }
                            if (mDataAdapter.getItem(i).getId() == highId) {
                                mShadowService.setHighSeverity(mDataAdapter.getItem(i));
                            }
                        }
                    } catch (SQLException e) {
                        U.log("Database error");
                    }
                }
            }
        }
    }

    // Called when the user selects a contextual menu item
    @Override
    public boolean onActionItemClicked(ActionMode mode, MenuItem item) {

        SharedPreferences s = getActivity().getSharedPreferences(Prefs.PREFS_KEY, Context.MODE_PRIVATE);
        SharedPreferences.Editor edit = s.edit();

        switch (item.getItemId()) {


            case R.id.action_low:
                edit.putInt(Prefs.KEY_DB_SCENARIO_LOW, mDataAdapter.getSelectedItem().getId());
                edit.apply();
                mLowPriority.setText(mDataAdapter.getItem(mDataAdapter.getSelectedItemPosition()).getTitle());
                mShadowService.setLowSeverity(mDataAdapter.getSelectedItem());
                break;

            case R.id.action_high:
                edit.putInt(Prefs.KEY_DB_SCENARIO_HIGH, mDataAdapter.getSelectedItem().getId());
                edit.apply();
                mHighPriority.setText(mDataAdapter.getItem(mDataAdapter.getSelectedItemPosition()).getTitle());
                mShadowService.setHighSeverity(mDataAdapter.getSelectedItem());
                break;

            case R.id.action_edit:
                Intent i = new Intent(getActivity(), SSCreateEventActivity.class);
                i.putExtra(SSCreateEventActivity.KEY_DB_POSIITON, mDataAdapter.getSelectedItem().getId());
                startActivityForResult(i, 200);
                break;

            case R.id.action_delete:
                try {
                    mDataSource.open();
                    mDataSource.removeScenario(mDataAdapter.getSelectedItem().getId());
                    mDataSource.close();

                    if (s.getInt(Prefs.KEY_DB_SCENARIO_LOW, -1) == mDataAdapter.getSelectedItem().getId())
                        edit.remove(Prefs.KEY_DB_SCENARIO_LOW);
                    if (s.getInt(Prefs.KEY_DB_SCENARIO_HIGH, -1) == mDataAdapter.getSelectedItem().getId())
                        edit.remove(Prefs.KEY_DB_SCENARIO_HIGH);

                    edit.apply();

                    loadScenarioData();
                } catch (SQLException e) {
                    e.printStackTrace();
                }
                mActionMode.finish();
                break;
        }

        return true;
    }

    // Called when the user exits the action mode
    @Override
    public void onDestroyActionMode(ActionMode mode) {
        mActionMode = null;
    }

    @Override
    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
        mDataAdapter.setSelectedItemPosition(position);

        //notify the data set
        mDataAdapter.notifyDataSetChanged();

        final int firstListItemPosition = mEmergencyList.getFirstVisiblePosition();

        final Animation anim = AnimationUtils.loadAnimation(getActivity(), android.R.anim.fade_in);
        anim.setDuration(200);
        //because position is the item in the adapter, the firstlistitemposition is the position of the element in the view
        mEmergencyList.getChildAt(position - firstListItemPosition).startAnimation(anim);
        new Handler().postDelayed(new Runnable() {

            public void run() {

            }

        }, anim.getDuration());


        //refresh the list view
        mEmergencyList.invalidate();

        if (mActionMode == null) {
            mActionMode = getActivity().startActionMode(this);
            view.setSelected(true);

        }
        else
            mActionMode.invalidate();
    }

    private void doBindService() {

        //the service is bound
        getActivity().bindService(new Intent(getActivity(), ShadowListenerService.class), mConnection, Context.BIND_AUTO_CREATE);

        //Toast.makeText(SSConnectActivity.this, "Bound from activity", Toast.LENGTH_SHORT).show();

        //our service bound
        isBound = true;

    }

    private void doUnbindService() {


        if (isBound) {
            getActivity().unbindService(mConnection);
            isBound = false;
            //Toast.makeText(SSConnectActivity.this, "Unbound from activity", Toast.LENGTH_SHORT).show();
        }
    }


}
