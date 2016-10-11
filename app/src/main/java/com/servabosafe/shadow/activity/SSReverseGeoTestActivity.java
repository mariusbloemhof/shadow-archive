package com.servabosafe.shadow.activity;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.location.*;
import android.os.Bundle;
import android.os.Handler;
import android.widget.ListView;
import android.widget.Toast;
import com.androidquery.AQuery;
import com.servabosafe.shadow.R;
import com.servabosafe.shadow.adapter.LocationArrayAdapter;
import com.servabosafe.shadow.data.model.PhysicalLocation;
import com.servabosafe.shadow.helper.U;

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Calendar;

/**
 * Created by brandon.burton on 11/5/14.
 */
public class SSReverseGeoTestActivity extends Activity {

    private AQuery $;

    //private TextView mReverseLookup;
    private ListView mListAddress;

    private LocationManager mLocationManager;

    private LocationListener mNetworkListener;
    private LocationListener mGPSListener;
    private LocationListener mPassiveListener;

    private Location mPrevLocation;
    private Location mPrevPassiveLocation;
    private Location mPrevGPSLocation;

    private ArrayList<PhysicalLocation> mLocations;
    private LocationArrayAdapter mLocationAdapter;

    private GpsStatus mGpsStatus;

    private Calendar mCalendar;

    private DecimalFormat df = new DecimalFormat("#.##");

    private Handler restartHandler;

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);

        setContentView(R.layout.test);

        $ = new AQuery(this);

        mCalendar = Calendar.getInstance();

        mLocationManager = (LocationManager)getSystemService(Context.LOCATION_SERVICE);

        mGpsStatus = mLocationManager.getGpsStatus(null);

        mNetworkListener = new NetworkLocationListener();
        mGPSListener = new GPSLocationListener();
        mPassiveListener = new PassiveNetworkListener();

        mLocations = new ArrayList<PhysicalLocation>();

        mListAddress = (ListView)findViewById(R.id.list_loc);

        mLocationAdapter = new LocationArrayAdapter(this, mLocations);

        mListAddress.setAdapter(mLocationAdapter);
    }

    @Override
    protected void onResume() {
        super.onResume();

        mPrevLocation = mLocationManager.getLastKnownLocation(LocationManager.NETWORK_PROVIDER);

        mLocationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 30000, 0, mGPSListener);
        mLocationManager.requestLocationUpdates(LocationManager.NETWORK_PROVIDER, 15000, 0, mNetworkListener);
        //mLocationManager.requestLocationUpdates(LocationManager.PASSIVE_PROVIDER, 10000, 0, mPassiveListener);

        LocationProvider networkProvider = mLocationManager.getProvider(LocationManager.NETWORK_PROVIDER);
        U.log("Network Provider: " + networkProvider.getPowerRequirement());
        U.log("Monetary Cost: " + networkProvider.hasMonetaryCost());
        U.log("Requires Cell: " + networkProvider.requiresCell());
        U.log("Requires Netw: " + networkProvider.requiresNetwork());
        U.log("Requires Sate: " + networkProvider.requiresSatellite());
        U.log("Has Altitude : " + networkProvider.supportsAltitude());
        U.log("Has Bearing  : " + networkProvider.supportsBearing());
        U.log("Has Speed    : " + networkProvider.supportsSpeed());

//        LocationProvider gpsProvider = mLocationManager.getProvider(LocationManager.GPS_PROVIDER);
//        U.log("GPS Provider: " + gpsProvider.getPowerRequirement());
//        U.log("Monetary Cost: " + gpsProvider.hasMonetaryCost());
//        U.log("Requires Cell: " + gpsProvider.requiresCell());
//        U.log("Requires Netw: " + gpsProvider.requiresNetwork());
//        U.log("Requires Sate: " + gpsProvider.requiresSatellite());
//        U.log("Has Altitude : " + gpsProvider.supportsAltitude());
//        U.log("Has Bearing  : " + gpsProvider.supportsBearing());
//        U.log("Has Speed    : " + gpsProvider.supportsSpeed());

//        LocationProvider passiveProvider = mLocationManager.getProvider(LocationManager.PASSIVE_PROVIDER);
//        U.log("Passive Provider: " + passiveProvider.getPowerRequirement());
//        U.log("Monetary Cost: " + passiveProvider.hasMonetaryCost());
//        U.log("Requires Cell: " + passiveProvider.requiresCell());
//        U.log("Requires Netw: " + passiveProvider.requiresNetwork());
//        U.log("Requires Sate: " + passiveProvider.requiresSatellite());
//        U.log("Has Altitude : " + passiveProvider.supportsAltitude());
//        U.log("Has Bearing  : " + passiveProvider.supportsBearing());
//        U.log("Has Speed    : " + passiveProvider.supportsSpeed());
    }

    @Override
    protected void onStop() {
        super.onStop();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        mLocationManager.removeUpdates(mGPSListener);
        mLocationManager.removeUpdates(mNetworkListener);
        //mLocationManager.removeUpdates(mPassiveListener);
    }

    //    private void getSatData(){
//        Iterable<GpsSatellite> sats = mGpsStatus.getSatellites();
//
//        for(GpsSatellite sat : sats){
//            StringBuilder sb = new StringBuilder();
//            sb.append(sat.getPrn());
//            sb.append("\t");
//            sb.append(sat.getElevation());
//            sb.append("\t");
//            sb.append(sat.getAzimuth());
//            sb.append("\t");
//            sb.append(sat.getSnr());
//            U.log(sb.toString());
//        }
//
//        mGpsStatus = mLocationManager.getGpsStatus(mGpsStatus);
//    }

    private class NetworkLocationListener implements LocationListener {

        @Override
        public void onStatusChanged(String provider, int status, Bundle extras) {
            try {
                String strStatus = "";
                switch (status) {
                    case GpsStatus.GPS_EVENT_FIRST_FIX:
                        strStatus = "GPS_EVENT_FIRST_FIX";
                        break;
                    case GpsStatus.GPS_EVENT_SATELLITE_STATUS:
                        strStatus = "GPS_EVENT_SATELLITE_STATUS";
                        break;
                    case GpsStatus.GPS_EVENT_STARTED:
                        strStatus = "GPS_EVENT_STARTED";
                        break;
                    case GpsStatus.GPS_EVENT_STOPPED:
                        strStatus = "GPS_EVENT_STOPPED";
                        break;
                    default:
                        strStatus = String.valueOf(status);
                        break;
                }
                Toast.makeText(getApplicationContext(), "Status: " + strStatus,
                        Toast.LENGTH_SHORT).show();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        @Override
        public void onProviderEnabled(String provider) {

        }

        @Override
        public void onProviderDisabled(String provider) {

        }
        @Override
        public void onLocationChanged(Location location) {

            final Location temp = location;

            //Toast.makeText(getApplicationContext(), "There was an update", Toast.LENGTH_SHORT).show();

//            final double latitude = location.getLatitude();
//            final double longitude = location.getLongitude();

            //if (mPrevLocation != null)
//                mLocationAdapter.add(new PhysicalLocation("Network", location.distanceTo(mPrevLocation)+"m", "No data"));

            Intent batteryIntent = getApplicationContext().registerReceiver(null, new IntentFilter(Intent.ACTION_BATTERY_CHANGED));
            int rawlevel = batteryIntent.getIntExtra("level", -1);
            double scale = batteryIntent.getIntExtra("scale", -1);
            double level = -1;
            if (rawlevel >= 0 && scale > 0) {
                level = rawlevel / scale;
            }

            int minDistance = 0;
            int minTime = 60000; //1 minute

            mCalendar = null;
            mCalendar = Calendar.getInstance();
            String time = mCalendar.get(Calendar.HOUR_OF_DAY) + ":" + mCalendar.get(Calendar.MINUTE) + ":" +mCalendar.get(Calendar.SECOND);

            if (mPrevLocation != null) {
                mLocationAdapter.add(new PhysicalLocation(
                        "Network", location.getLatitude() + ", " + location.getLongitude(),
                                "Speed: " + df.format(location.getSpeed()) + "m/s\n" +
                                "Distance: " + df.format(location.distanceTo(mPrevLocation)) + "m\n" +
                                "Time: " + time +"\n"+
                                "Accuracy: " + location.getAccuracy() + "\n",
                        location.getSpeed(),
                        location.distanceTo(mPrevLocation),
                        mCalendar.getTimeInMillis(), level));

                mListAddress.setSelection(mLocationAdapter.getCount() - 1);

                float distance = location.distanceTo(mPrevLocation);

                if (distance > 800.0f) //level 4
                {
                    Toast.makeText(getApplicationContext(), "Update is at level 4", Toast.LENGTH_SHORT).show();
                    minTime = 60000;
                    //mLocationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 60000, 0, mNetworkListener);
                }
                else if (distance > 200.0f) { //level 3
                    Toast.makeText(getApplicationContext(), "Update to level 4", Toast.LENGTH_SHORT).show();
                    minTime = 30000;
                    //mLocationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 30000, 0, mNetworkListener);
                }
                else if (distance > 40.0f) { //level 2
                    Toast.makeText(getApplicationContext(), "Update to level 3", Toast.LENGTH_SHORT).show();
                    minTime = 30000;
                    //mLocationManager.requestLocationUpdates(LocationManager.NETWORK_PROVIDER, 30000, 0, mNetworkListener);
                }
                else if (distance > 20.0f) { //level 1
                    Toast.makeText(getApplicationContext(), "Update to level 2", Toast.LENGTH_SHORT).show();
                    minTime = 120000;
                    //mLocationManager.requestLocationUpdates(LocationManager.NETWORK_PROVIDER, 120000, 0, mNetworkListener);
                }
                else if (distance > 10.0f) {//dormant
                    Toast.makeText(getApplicationContext(), "User is at level 1", Toast.LENGTH_SHORT).show();
                    minTime = 600000;
                    //mLocationManager.requestLocationUpdates(LocationManager.NETWORK_PROVIDER, 600000, 0, mNetworkListener);
                }
                else if (distance > -1) //other
                    Toast.makeText(getApplicationContext(), "User is dormant", Toast.LENGTH_SHORT).show();
                    minTime = 60000;
                    //mLocationManager.requestLocationUpdates(LocationManager.NETWORK_PROVIDER, 600000, 100, mNetworkListener);
                }
                else {
                        mLocationAdapter.add(new PhysicalLocation(
                        "Network", location.getLatitude() + ", " + location.getLongitude(),
                                "Accuracy: " + location.getAccuracy()));
                        //mLocationManager.requestLocationUpdates(LocationManager.NETWORK_PROVIDER, 15000, 0, mNetworkListener);
                }

              mLocationManager.removeUpdates(mNetworkListener);
              mLocationManager.removeUpdates(mGPSListener);

              final int updateTime = minTime;
              final int updateDistance = minDistance;
//
              new Handler().postDelayed(new Runnable() {
                  @Override
                  public void run() {
                      if (updateTime < 33000)
                          mLocationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, updateTime, updateDistance, mGPSListener);
                      else
                          mLocationManager.requestLocationUpdates(LocationManager.NETWORK_PROVIDER, updateTime, updateDistance, mNetworkListener);
                  }
              }, updateTime);
//              //mLocationManager.requestLocationUpdates(LocationManager.NETWORK_PROVIDER, minTime, minDistance, mNetworkListener);

//            String loc = String.valueOf(latitude) + "," + String.valueOf(longitude);
//            mReverseLookup.setText(mReverseLookup.getText().toString() + "\n" + loc);
//            $.ajax("https://maps.googleapis.com/maps/api/geocode/json?latlng="+String.valueOf(latitude)+","+String.valueOf(longitude)+"&key=AIzaSyDE0SA-EspEOQPeEzNOvcv1jigOjhAzhhM", JSONObject.class, new AjaxCallback<JSONObject>() {
//                @Override
//                public void callback(String url, JSONObject object, AjaxStatus status) {
//                    try {
//                        JSONArray j = object.getJSONArray("results");
//                        //mLocations.add(new PhysicalLocation("Network", String.valueOf(latitude) + "," + String.valueOf(longitude), location));
//                        //for (int i = 0; i < j.length(); i++) {
//                        final String loc = (j.getJSONObject(0).getString("formatted_address"));
//                        if (mPrevLocation != null)
//                            mLocationAdapter.add(new PhysicalLocation("Network", mPrevLocation.distanceTo(temp)+"m", loc));
//                        else
//                            mLocationAdapter.add(new PhysicalLocation("Network", String.valueOf(latitude)+","+String.valueOf(longitude), loc));
//                        //}
////                        runOnUiThread(new Runnable() {
////                            @Override
////                            public void run() {
////                                mLocationAdapter.notifyDataSetChanged();
////                                mListAddress.setAdapter(mLocationAdapter);
////                            }
////                        });
////                        mReverseLookup.setText(mReverseLookup.getText().toString() + "\n" + location);
//                    } catch (JSONException e) {
////                        mReverseLookup.setText("Error in location");
//                        e.printStackTrace();
//                    }
////
//                    super.callback(url, object, status);
//                }
//            });

            mPrevLocation = location;

        }
    }

    private class PassiveNetworkListener implements LocationListener {

        @Override
        public void onStatusChanged(String provider, int status, Bundle extras) {

        }

        @Override
        public void onProviderEnabled(String provider) {

        }

        @Override
        public void onProviderDisabled(String provider) {

        }

        @Override
        public void onLocationChanged(Location location) {

        }


    }


    private class GPSLocationListener implements LocationListener {

        @Override
        public void onLocationChanged(Location location) {

            final double latitude = location.getLatitude();
            final double longitude = location.getLongitude();

            final Location temp = location;

            Toast.makeText(getApplicationContext(), "There was a GPS update", Toast.LENGTH_SHORT).show();

            Intent batteryIntent = getApplicationContext().registerReceiver(null, new IntentFilter(Intent.ACTION_BATTERY_CHANGED));
            int rawlevel = batteryIntent.getIntExtra("level", -1);
            double scale = batteryIntent.getIntExtra("scale", -1);
            double level = -1;
            if (rawlevel >= 0 && scale > 0) {
                level = rawlevel / scale;
            }

            int minDistance = 0;
            int minTime = 60000; //1 minute

            mCalendar = null;
            mCalendar = Calendar.getInstance();
            String time = mCalendar.get(Calendar.HOUR_OF_DAY) + ":" + mCalendar.get(Calendar.MINUTE) + ":" +mCalendar.get(Calendar.SECOND);

            if (mPrevLocation != null) {
                mLocationAdapter.add(new PhysicalLocation(
                        "GPS", location.getLatitude() + ", " + location.getLongitude(),
                        "Speed: " + df.format(location.getSpeed()) + "m/s\n" +
                                "Distance: " + df.format(location.distanceTo(mPrevLocation)) + "m\n" +
                                "Time: " + time +"\n"+
                                "Accuracy: " + location.getAccuracy() + "\n",
                        location.getSpeed(),
                        location.distanceTo(mPrevLocation),
                        mCalendar.getTimeInMillis(), level));

                mListAddress.setSelection(mLocationAdapter.getCount() - 1);

                float distance = location.distanceTo(mPrevLocation);

                if (distance > 800.0f) //level 4
                {
                    //Toast.makeText(getApplicationContext(), "Update is at level 4", Toast.LENGTH_SHORT).show();
                    minTime = 60000;
                    minDistance = 1600;
                    //mLocationManager.requestLocationUpdates(LocationManager.NETWORK_PROVIDER, 30000, 0, mNetworkListener);
                }
                else if (distance > 200.0f) { //level 3
                    //Toast.makeText(getApplicationContext(), "Update to level GPS 4", Toast.LENGTH_SHORT).show();
                    minTime = 30000;
                    //mLocationManager.requestLocationUpdates(LocationManager.NETWORK_PROVIDER, 30000, 0, mNetworkListener);
                }
                else if (distance > 40.0f) { //level 2
                    //Toast.makeText(getApplicationContext(), "Update to level GPS 3", Toast.LENGTH_SHORT).show();
                    minTime = 30000;
                    //mLocationManager.requestLocationUpdates(LocationManager.NETWORK_PROVIDER, 30000, 0, mNetworkListener);
                }
                else if (distance > 20.0f) { //level 1
                    //Toast.makeText(getApplicationContext(), "Update to level GPS 2", Toast.LENGTH_SHORT).show();
                    minTime = 60000;
                    //mLocationManager.requestLocationUpdates(LocationManager.NETWORK_PROVIDER, 120000, 0, mNetworkListener);
                }
                else if (distance > 10.0f) {//dormant
                    //Toast.makeText(getApplicationContext(), "User is at level GPS 1", Toast.LENGTH_SHORT).show();
                    minTime = 60000;
                    //mLocationManager.requestLocationUpdates(LocationManager.NETWORK_PROVIDER, 600000, 0, mNetworkListener);
                }
                else if (distance > -1) //other
                    //Toast.makeText(getApplicationContext(), "User is dormant", Toast.LENGTH_SHORT).show();
                    minTime = 600000;
                //mLocationManager.requestLocationUpdates(LocationManager.NETWORK_PROVIDER, 600000, 100, mNetworkListener);
            }
            else {
                mLocationAdapter.add(new PhysicalLocation(
                        "Network", location.getLatitude() + ", " + location.getLongitude(),
                        "Accuracy: " + location.getAccuracy()));
                //mLocationManager.requestLocationUpdates(LocationManager.NETWORK_PROVIDER, 15000, 0, mNetworkListener);
            }

            mLocationManager.removeUpdates(mGPSListener);
            mLocationManager.removeUpdates(mNetworkListener);

            final int updateTime = minTime;
            final int updateDistance = minDistance;

            new Handler().postDelayed(new Runnable() {
                @Override
                public void run() {
                    if (updateTime > 500000) //if the update time is greater than 5 mintues
                        mLocationManager.requestLocationUpdates(LocationManager.NETWORK_PROVIDER, updateTime, updateDistance, mNetworkListener);
                    else
                        mLocationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, updateTime, updateDistance, mNetworkListener);
                }
            }, updateTime);

            mPrevGPSLocation = location;

        }

        @Override
        public void onStatusChanged(String provider, int status, Bundle extras) {
            U.log(provider + " : " + status);
        }

        @Override
        public void onProviderEnabled(String provider) {
            U.log("Provider enabled: " + provider);
        }

        @Override
        public void onProviderDisabled(String provider) {
            U.log("Provider disabled: " + provider);
        }
    }
}
