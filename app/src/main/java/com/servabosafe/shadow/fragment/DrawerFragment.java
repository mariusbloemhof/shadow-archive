package com.servabosafe.shadow.fragment;

import android.app.Fragment;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.IBinder;
import android.support.v4.widget.DrawerLayout;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.*;
import com.servabosafe.shadow.R;
import com.servabosafe.shadow.activity.SSConnectActivity;
import com.servabosafe.shadow.activity.SSReverseGeoTestActivity;
import com.servabosafe.shadow.adapter.DrawerAdapter;
import com.servabosafe.shadow.data.model.Setting;
import com.servabosafe.shadow.data.service.ShadowListenerService;
import com.servabosafe.shadow.utility.command.Command;

import java.util.ArrayList;
import java.util.Arrays;

/**
 * Created by brandon.burton on 1/27/14.
 */
public class DrawerFragment extends Fragment {

    private EditText mDSearchBar;

    private DrawerLayout mDrawerLayout;
    private ListView mDrawerList;

    private boolean isBound = false;

    private DrawerAdapter mAdapter;

    private ShadowListenerService mShadowService;

    private ServiceConnection mConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {

            //mBluetoothLeService = ((ShadowListenerService.BluetoothBinder)service).getService();
            mShadowService = ((ShadowListenerService.BluetoothBinder)service).getService();

            if (mShadowService != null) {

            } else {
                Toast.makeText(getActivity(), "Could not connect to Shadow app services! Please close and open app.", Toast.LENGTH_SHORT).show();
            }
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {

            mShadowService = null;
        }
    };

    private void doBindService() {

        //the service is bound
        getActivity().bindService(new Intent(getActivity(), ShadowListenerService.class), mConnection, Context.BIND_AUTO_CREATE);

        isBound = true;

    }

    private void doUnbindService() {

        if (isBound) {
            getActivity().unbindService(mConnection);
            isBound = false;
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {

        return inflater.inflate(R.layout.base_fragment_drawer_main, null);

    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {

        super.onActivityCreated(savedInstanceState);

        ArrayList<Setting> items= new ArrayList<Setting>(Arrays.asList(
                new Setting(android.R.drawable.ic_menu_view, "Change Device","Change the Shadow Device",new Command() {
                    @Override
                    public void execute(Object data) {
                        Intent i = new Intent(getActivity(), SSConnectActivity.class);
                        i.putExtra("reset_conn", 0);
                        getActivity().startActivity(i);
                    }
                }),
                new Setting(android.R.drawable.ic_menu_view, "Update Battery","Check the battery status",new Command() {
                    @Override
                    public void execute(Object data) {
                        if (mShadowService != null)
                        {
                            mShadowService.getBatteryLevel();
                            Toast.makeText(getActivity(), "Checking battery....", Toast.LENGTH_SHORT).show();
                        }
                    }
                }),
                new Setting(android.R.drawable.ic_menu_view, "Update Device", "Update the Shadow Firmware",new Command() {
                    @Override
                    public void execute(Object data) {

                    }
                }),
                new Setting(android.R.drawable.ic_menu_info_details, "About Us","",new Command() {
                    @Override
                    public void execute(Object data) {

                    }
                }),
                new Setting(android.R.drawable.ic_menu_info_details, "Terms of Use","",new Command() {
                    @Override
                    public void execute(Object data) {

                    }
                }),
                new Setting(android.R.drawable.ic_menu_mapmode, "Map Mode", "", new Command() {
                    @Override
                    public void execute(Object data) {
                        Intent i = new Intent(getActivity(), SSReverseGeoTestActivity.class);
                        i.putExtra("reset_conn", 0);
                        getActivity().startActivity(i);
                    }
                })));

        mAdapter = new DrawerAdapter(getActivity(), items);

        mDrawerList = (ListView) getView().findViewById(R.id.left_drawer);

        // Set the adapter for the list view
//        mDrawerList.setAdapter(new ArrayAdapter<String>(this,
//                R.layout.drawer_list_item, mPlanetTitles));

        mDrawerList.setAdapter(mAdapter);

        // Set the list's click listener
        mDrawerList.setOnItemClickListener(new DrawerItemClickListener());

        mDSearchBar = (EditText)getView().findViewById(R.id.edit_drawer_search);

        mDSearchBar.setOnEditorActionListener(new TextView.OnEditorActionListener() {

            @Override
            public boolean onEditorAction(TextView view, int actionId, KeyEvent event) {
                if (actionId == EditorInfo.IME_ACTION_SEARCH)
                {
                    // show the progress spinner
                    getActivity().setProgressBarIndeterminateVisibility(true);
                    //TODO fix
                    //getActivity().setSupportProgressBarIndeterminateVisibility(true);

                    // hide the keyboard
                    ((InputMethodManager) getActivity().getSystemService(Context.INPUT_METHOD_SERVICE)).hideSoftInputFromWindow(mDSearchBar.getWindowToken(), 0);

                    // what we're searching for
                    String searchString = mDSearchBar.getText().toString();

                    searchString = searchString.trim();

                    // is the trimmed string long enough?
//                    if (searchString.length() >= 3) {
//                        // run the search, with a safe string
//                        Intent i = new Intent(getActivity(), FLCoreApplication.getInstance().getSearchActivityClass());
//                        i.putExtra("query", searchString);
//                        startActivity(i);
//
//                    } else {
//                        // tell the user to stop putting in spaces
//                        Toast.makeText(getActivity(), getString(R.string.at_least_) + "3" + getString(R.string._characters_are_required_to_search_), Toast.LENGTH_SHORT).show();
//                    }
                    return true;
                }
                else
                {
                    return true;
                }
            }
        });

        doBindService();

    }

    @Override
    public void onResume() {

        super.onResume();

    }

    @Override
    public void onPause() {

        super.onPause();

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

    }

    private class DrawerItemClickListener implements ListView.OnItemClickListener {
        @Override
        public void onItemClick(AdapterView parent, View view, int position, long id) {
            if (mAdapter.getItem(position).getCommand() != null)
                mAdapter.getItem(position).executeCommand();
        }
    }
}
