package com.servabosafe.shadow.data.model;

/**
 * Created by brandon.burton on 11/7/14.
 */

public class PhysicalLocation {

    public String mMethod;
    public String mLatLng;
    public String mPhysAddress;
    public float mSpeed = -1.1f;
    public float mDistance = -1.1f;
    public long mTimestamp;
    public double battLevel;

    public PhysicalLocation() {
        mMethod = "";
        mLatLng = "";
        mPhysAddress = "";
    }

    public PhysicalLocation(String mMethod, String mLatLng, String mPhysAddress) {
        this.mMethod = mMethod;
        this.mLatLng = mLatLng;
        this.mPhysAddress = mPhysAddress;
    }

    public PhysicalLocation(String mMethod, String mLatLng, String mPhysAddress, float mSpeed, float mDistance, long timestamp, double battLevel) {
        this.mMethod = mMethod;
        this.mLatLng = mLatLng;
        this.mPhysAddress = mPhysAddress;
        this.mSpeed = mSpeed;
        this.mDistance = mDistance;
        this.mTimestamp = timestamp;
        this.battLevel = battLevel;
    }
}
