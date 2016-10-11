package com.servabosafe.shadow.data.receiver;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import com.servabosafe.shadow.data.model.Const;

/**
 * Created by brandon.burton on 10/27/14.
 */
public class BatteryReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        String action = intent.getAction();
        if(action.equalsIgnoreCase("BatteryCheck")){
            Bundle extra = intent.getExtras();
            byte username = extra.getByte(Const.KEY_BATTERY_LEVEL.toString());
        }
    }
}
