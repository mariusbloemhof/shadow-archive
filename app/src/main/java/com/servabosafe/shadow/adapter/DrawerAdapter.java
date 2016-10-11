package com.servabosafe.shadow.adapter;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import com.servabosafe.shadow.R;
import com.servabosafe.shadow.data.model.Setting;

import java.util.ArrayList;

/**
 * Created by brandon.burton on 1/24/14.
 */
public class DrawerAdapter extends EasyAdapter<Setting> {

    public DrawerAdapter(Context context) {
        super(context);
    }

    public DrawerAdapter(Context context, ArrayList<Setting> items) {
        super(context, items);
    }


    @Override
    public View getView(int position, View convertView, ViewGroup parent) {

        LayoutInflater inflater = LayoutInflater.from(mContext.get());

        if (mItems.get(position).isHeader())
        {
            View v = inflater.inflate(R.layout.cell_drawer_header, null);

            TextView title = (TextView)v.findViewById(R.id.text_header);

            title.setText(mItems.get(position).getTitle());

            return v;
        }
        else
        {
            View v = inflater.inflate(R.layout.cell_drawer_choice, null);

            ImageView image = (ImageView)v.findViewById(R.id.image_cell_icon);
            TextView title = (TextView)v.findViewById(R.id.label_settings_cell_title);
            TextView subtitle = (TextView)v.findViewById(R.id.label_settings_cell_subtitle);

            title.setText(mItems.get(position).getTitle());
            subtitle.setText(mItems.get(position).getSubtitle());

            if (mItems.get(position).getmImageResource() != Setting.NO_RESOURCE)
            {
                image.setVisibility(View.VISIBLE);
                image.setImageResource(mItems.get(position).getmImageResource());
            }

            if (mItems.get(position).getSubtitle().isEmpty())
                subtitle.setVisibility(View.GONE);

            return v;
        }

    }
}
