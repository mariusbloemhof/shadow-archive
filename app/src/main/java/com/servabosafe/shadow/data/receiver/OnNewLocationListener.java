package com.servabosafe.shadow.data.receiver;

import android.location.Location;

/**
 * Created by brandon.burton on 11/20/14.
 */
public interface OnNewLocationListener {
    public abstract void onNewLocationReceived(Location location);
}
