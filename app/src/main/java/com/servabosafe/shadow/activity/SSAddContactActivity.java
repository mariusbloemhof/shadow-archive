package com.servabosafe.shadow.activity;

import android.app.Activity;
import android.os.Bundle;
import com.servabosafe.shadow.R;
import com.servabosafe.shadow.fragment.SSContactsFragment;

/**
 * Created by brandon.burton on 10/10/14.
 */
public class SSAddContactActivity extends Activity {

    @Override
    public void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);

        getActionBar().setDisplayHomeAsUpEnabled(true);

        setContentView(R.layout.activity_home);

        if (findViewById(R.id.layout_fragment_holder) != null)
        {
            //if the screen is rotated, do not re(create) the fragment
            if (savedInstanceState != null)
                return;

            //if we have scenarios
            SSContactsFragment fragment = new SSContactsFragment();
            getFragmentManager().beginTransaction().add(R.id.layout_fragment_holder, fragment).commit();

            //else
            //HomeDefaultFragment fragment = new HomeDetailFragment();
            //getFragmentManager().beginTransaction().add(R.id.layout_fragment_holder, fragment).addToBackStack("home").commit();

        }

    }


}
