package com.servabosafe.shadow.activity;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import com.servabosafe.shadow.R;

/**
 * Created by brandon.burton on 10/10/14.
 */
public class SSSplashActivity extends Activity {

    private final int SPLASH_DISPLAY_LENGTH = 3000;

    @Override
    public void onCreate(Bundle savedInstanceState){

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_splash_screen);

        /* New Handler to start the Menu-Activity
         * and close this Splash-Screen after some seconds.*/
        getActionBar().setTitle("");
        getActionBar().hide();

        new Handler().postDelayed(new Runnable() {

            @Override
            public void run() {
                Intent mainIntent = new Intent(SSSplashActivity.this, SSConnectBLEActivity.class);
                startActivity(mainIntent);
                overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
                finish();
            }

        }, SPLASH_DISPLAY_LENGTH);

    }

}
