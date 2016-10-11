package com.servabosafe.shadow.application;

import android.app.Application;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.IBinder;
import android.widget.Toast;
import com.servabosafe.shadow.data.service.ShadowListenerService;
import com.servabosafe.shadow.helper.U;

/**
 * Created by brandon.burton on 10/13/14.
 */
public class ServaboApplication extends Application {

//    private static Scenario lowSeverity = null;
//
//    private static Scenario highSeverity = null;
//
//    private static ShadowReceiver mShadowReceiver = null;

//    private static Intent mShadowService;

    //are we bound to the service
    private boolean isBound = false;

    //our listening service
    private ShadowListenerService mShadowService;

    private ServiceConnection mConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {

            mShadowService = ((ShadowListenerService.BluetoothBinder)service).getService();

            U.log("Application connected to service.");

            //Toast.makeText(this, "Activity connected to service.", Toast.LENGTH_SHORT).show();
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {

            doUnbindService();

            mShadowService = null;

            //Toast.makeText(this, "Activity disconnected from service.", Toast.LENGTH_SHORT).show();

        }
    };

    @Override
    public void onCreate() {

        super.onCreate();

        doBindService();

    }

    @Override
    public void onTerminate() {

//        if (mShadowService != null)
//            stopService(mShadowService);

//        mShadowReceiver.stopScan();

//        unregisterReceiver(mShadowReceiver);
        unbindService(mConnection);

        U.log("Terminated");

        super.onTerminate();

    }

    private void doBindService() {

        boolean start = false;

        Intent i = new Intent(getApplicationContext(), ShadowListenerService.class);
        startService(i);
        //the service is bound
        start = bindService(new Intent(this, ShadowListenerService.class), mConnection, Context.BIND_AUTO_CREATE);

        if (!start) {
            Toast.makeText(this, "Bind to BluetoothLeService failed", Toast.LENGTH_SHORT).show();
            //finish();
        }

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
