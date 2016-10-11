package com.servabosafe.shadow.helper;

import android.annotation.TargetApi;
import android.os.Build;
import android.os.StrictMode;
import android.util.Log;

/**
 * The U(tility) class
 * 
 * @author josh.oneal
 * 
 */
public class U
{
  /**
   * A simple proxy function to LogCat that will make logging much easier and
   * consistent
   * 
   * @param objects
   *          The variable length objects to be logged
   */
  public static void log( Object... objects )
  {
    // should we show output? Don't in release
    if( C.SHOULD_SHOW_LOG_OUTPUT )
    {
      // was at least one object sent in?
      if( null != objects && objects.length > 0 )
      {
        // we'll concat each of the object's toString values
        String logString = "";

        for( Object o : objects )
        {
          if( null != o )
          {
            logString += o.toString() + " | ";
          }
          else
          {
            log("** U.log() RECEIVED A NULL OBJECT **");
          }
        }

        Log.e( C.TAG, logString.substring( 0, logString.length() - 3 ) );
      }
    }
  }

    /**
     * Enables strict mode. This should only be called when debugging the application and is useful
     * for finding some potential bugs or best practice violations.
     */
    @TargetApi(11)
    public static void enableStrictMode() {
        // Strict mode is only available on gingerbread or later
        if (U.hasGingerbread()) {

            // Enable all thread strict mode policies
            StrictMode.ThreadPolicy.Builder threadPolicyBuilder =
                    new StrictMode.ThreadPolicy.Builder()
                            .detectAll()
                            .penaltyLog();

            // Enable all VM strict mode policies
            StrictMode.VmPolicy.Builder vmPolicyBuilder =
                    new StrictMode.VmPolicy.Builder()
                            .detectAll()
                            .penaltyLog();

            // Use builders to enable strict mode policies
            StrictMode.setThreadPolicy(threadPolicyBuilder.build());
            StrictMode.setVmPolicy(vmPolicyBuilder.build());
        }
    }

    /**
     * Uses static final constants to detect if the device's platform version is Gingerbread or
     * later.
     */
    public static boolean hasGingerbread() {
        return Build.VERSION.SDK_INT >= Build.VERSION_CODES.GINGERBREAD;
    }

    /**
     * Uses static final constants to detect if the device's platform version is Honeycomb or
     * later.
     */
    public static boolean hasHoneycomb() {
        return Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB;
    }

    /**
     * Uses static final constants to detect if the device's platform version is Honeycomb MR1 or
     * later.
     */
    public static boolean hasHoneycombMR1() {
        return Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB_MR1;
    }

    /**
     * Uses static final constants to detect if the device's platform version is ICS or
     * later.
     */
    public static boolean hasICS() {
        return Build.VERSION.SDK_INT >= Build.VERSION_CODES.ICE_CREAM_SANDWICH;
    }
}
