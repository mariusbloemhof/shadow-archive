package com.servabosafe.shadow.fragment;

import android.app.Fragment;
import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import com.servabosafe.shadow.R;
import com.servabosafe.shadow.activity.SSCreateEventActivity;

/**
 * Created by brandon.burton on 10/10/14.
 */
public class SSHomeDefaultFragment extends Fragment {

    private Button mCreateEmergency;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {

        return inflater.inflate(R.layout.fragment_home_default, container, false);

    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {

        super.onActivityCreated(savedInstanceState);

        mCreateEmergency = (Button)getView().findViewById(R.id.button_add_emergency);

        mCreateEmergency.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

            Intent i = new Intent(getActivity(), SSCreateEventActivity.class);
            startActivity(i);
            getActivity().overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);

//            SSCreateEventFragment fragment = new SSCreateEventFragment();
//            getFragmentManager()
//                    .beginTransaction()
//                    .setCustomAnimations(android.R.animator.fade_in, android.R.animator.fade_out, android.R.animator.fade_in, android.R.animator.fade_out)
//                    .replace(R.id.layout_fragment_holder, fragment)
//                    .addToBackStack("home_default")
//                    .commit();

            }
        });
    }

    @Override
    public void onResume() {

        super.onResume();

        getActivity().setTitle("Home");
    }
}
