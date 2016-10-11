package com.servabosafe.shadow.activity;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import com.servabosafe.shadow.R;
import com.servabosafe.shadow.fragment.SSCreateEventFragment;

/**
 * Created by brandon.burton on 10/14/14.
 */
public class SSCreateEventActivity extends Activity {

    /**
     * Any integer that allows this activity to be recognized as the returning activity
     */
    public static final int REQUEST_CODE = 200;

    public static final String KEY_DB_POSIITON = "dbPosition";


    @Override
    public void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_home);

        //if we have scenarios
        SSCreateEventFragment fragment = new SSCreateEventFragment();
        Bundle b = new Bundle();
        if (getIntent().getExtras() != null)
            b.putInt(SSCreateEventFragment.KEY_DB_POSIITON, getIntent().getExtras().getInt(KEY_DB_POSIITON, SSCreateEventFragment.KEY_NEW_RECORD));
        fragment.setArguments(b);
        getFragmentManager().beginTransaction().add(R.id.layout_fragment_holder, fragment).commit();

    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
    }
}
