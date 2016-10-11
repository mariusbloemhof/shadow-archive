package com.servabosafe.shadow.adapter;

import android.app.Activity;
import android.content.Context;
import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import com.servabosafe.shadow.R;
import com.servabosafe.shadow.data.model.PhysicalLocation;

import java.util.ArrayList;

/**
 * Created by brandon.burton on 11/7/14.
 */
public class LocationArrayAdapter extends EasyAdapter<PhysicalLocation> {
    //private ArrayList<PhysicalLocation> mItems;

    public LocationArrayAdapter(Context context) {
        super(context);
        //mItems = items;
    }

    public LocationArrayAdapter(Context context, ArrayList<PhysicalLocation> mLocations) {
        super(context, mLocations);

        //mItems = items;
    }

    public void add( PhysicalLocation newItem )
    {
        mItems.add( newItem );
        notifyDataSetChanged();
    }


    @Override
    public View getView(int position, View convertView, ViewGroup parent) {

        LocationHolder viewHolder;

        PhysicalLocation p = getItem(position);

        if (convertView == null) {
            LayoutInflater inflater = ((Activity)mContext.get()).getLayoutInflater();
            convertView = inflater.inflate(R.layout.cell_phys, parent, false);

            viewHolder = new LocationHolder();
            viewHolder.mInfo = (TextView)convertView.findViewById(R.id.label_info);

            convertView.setTag(viewHolder);

        } else {
            viewHolder = (LocationHolder)convertView.getTag();
        }

        viewHolder.mInfo.setText(p.mMethod + "\n" + p.mLatLng + "\n" + p.mPhysAddress);
        if (p.mDistance > -1) //other
            convertView.setBackgroundColor(Color.rgb(0, 64, 128));
        if (p.mDistance > 10.0f) //dormant
            convertView.setBackgroundColor(Color.rgb(0, 128, 96));
        if (p.mDistance > 20.0f) //level 1
            convertView.setBackgroundColor(Color.rgb(0, 128, 32));
        if (p.mDistance > 40.0f) //level 2
            convertView.setBackgroundColor(Color.rgb(64, 128, 64));
        if (p.mDistance > 200.0f) //level 3
            convertView.setBackgroundColor(Color.rgb(128, 64, 0));
        if (p.mDistance > 800.0f) //level 4
            convertView.setBackgroundColor(Color.rgb(160, 64, 0));


        return convertView;
    }

    public static class LocationHolder {
        public TextView mInfo;
    }
}


