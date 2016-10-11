package com.servabosafe.shadow.helper;

import android.os.Handler;
import android.os.Looper;

/**
 * Created by brandon.burton on 10/29/14.
 */
public class EmergencyUpdater {


    // Create a Handler that uses the Main Looper to run in
    private Handler mHandler = new Handler(Looper.getMainLooper());

    /**
     * Notify service that there are no emergencies
     */
    private OnUpdateFinishedListener mListener;

    private Runnable mStatusChecker;
    private int UPDATE_INTERVAL = 10000;
    //private int UPDATE_INTERVAL = Const.LENGTH_OF_DELAY;

    /**
     * Creates an UIUpdater object, that can be used to
     * perform UIUpdates on a specified time interval.
     *
     * @param uiUpdater A runnable containing the update routine.
     */
    public EmergencyUpdater(final Runnable uiUpdater) {
        mStatusChecker = new Runnable() {
            @Override
            public void run() {
                // Run the passed runnable
                try {
                    uiUpdater.run();
                    U.log("Fire no update!");
                } catch (NullPointerException e) {
                    U.log("No runnable.\n" + e);
                }

                mListener.onUpdateFinished();
                // Re-run it after the update interval
                //mHandler.postDelayed(this, UPDATE_INTERVAL);
            }
        };
    }

    /**
     * The same as the default constructor, but specifying the
     * intended update interval.
     *
     * @param uiUpdater A runnable containing the update routine.
     * @param interval  The interval over which the routine
     *                  should run (milliseconds).
     */
//    public EmergencyUpdater(Runnable uiUpdater, int interval){
//        UPDATE_INTERVAL = interval;
//        this(uiUpdater);
//    }

    /**
     * Starts the periodical update routine (mStatusChecker
     * adds the callback to the handler).
     */
    public synchronized void startUpdates(){
        if (mStatusChecker != null)
            mHandler.postDelayed(mStatusChecker, UPDATE_INTERVAL);
        //mStatusChecker.run();
        U.log("Start");
    }

    /**
     * Stops the periodical update routine from running,
     * by removing the callback.
     */
    public synchronized void stopUpdates(){
        mHandler.removeCallbacks(mStatusChecker);
        U.log("Stopped");
    }

    public synchronized void setStatusChecker(Runnable runnable) {
        mStatusChecker = runnable;
    }

    public void setOnUpdateFinishedListener(OnUpdateFinishedListener l)
    {
        mListener = l;
    }

}
