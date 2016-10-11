package com.servabosafe.shadow.fragment;

import android.app.Fragment;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import com.servabosafe.shadow.R;

/**
 * Created by brandon.burton on 10/21/14.
 */
public class DashboardFragment extends Fragment {

    private TextView mNetworkTest;

    private TextView mDeviceTest;

    private TextView mLocationTest;

    private TextView mBluetoothTest;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {

        return inflater.inflate(R.layout.fragment_dashboard, container, false);

    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {

        super.onActivityCreated(savedInstanceState);

        mNetworkTest = (TextView)getView().findViewById(R.id.label_network_connected);

        mDeviceTest = (TextView)getView().findViewById(R.id.label_device_connected);

        mLocationTest = (TextView)getView().findViewById(R.id.label_location_on);

        mBluetoothTest = (TextView)getView().findViewById(R.id.label_bluetooth_on);

        updateDashboard();
    }

    public void updateDashboard()
    {
//        try
//        {
//            mShadowReceiver.getCurrentDevice();
//        }
//        catch (NullPointerException n)
//        {
//            mDeviceTest.setText("No Device Connected");
//        }
//
    }


}
