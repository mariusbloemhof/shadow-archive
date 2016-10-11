package com.servabosafe.shadow.adapter;

import android.content.Context;
import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import com.servabosafe.shadow.R;
import com.servabosafe.shadow.data.model.Scenario;
import com.servabosafe.shadow.helper.U;

import java.util.ArrayList;

/**
 * Created by brandon.burton on 10/14/14.
 */
public class ScenarioAdapter extends EasyAdapter<Scenario> {

    private LayoutInflater mInflater;

    //the color of the highlight
    private int highlightColor = Color.argb(128, 255, 192, 0);

    //the item
    private int mSelectedItem = -1;

    public ScenarioAdapter(Context context) {

        super(context);

        init(context);
    }

    public ScenarioAdapter(Context context, ArrayList<Scenario> items) {

        super(context, items);

        init(context);
    }

    private void init(Context context)
    {
        mInflater = (LayoutInflater)context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);

        highlightColor = context.getResources().getColor(R.color.highlight);
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {

        ScenarioHolder holder;

        Scenario contact = getItem(position);

        if (convertView == null)
        {
            convertView = mInflater.inflate(R.layout.cell_scenario, null);

            holder = new ScenarioHolder();

            holder.mTitle = (TextView)convertView.findViewById(R.id.scenario_name);

            convertView.setTag(holder);

        }
        else {
            holder = (ScenarioHolder)convertView.getTag();
        }

        holder.mTitle.setText(contact.getTitle());

        if (position == mSelectedItem)
        {
            convertView.setBackgroundColor(highlightColor);
        }
        else
        {
            convertView.setBackgroundColor(Color.TRANSPARENT);
        }

        return convertView;
    }

    public class ScenarioHolder
    {
        public TextView mTitle;
    }

    public void setSelectedItemPosition(int position)
    {
        mSelectedItem = position;
    }

    public Integer getSelectedItemPosition()
    {
        return mSelectedItem;
    }

    public Scenario getSelectedItem()
    {
        try
        {
            return mItems.get(mSelectedItem);
        }
        catch (IndexOutOfBoundsException i)
        {
            U.log("Not in range");
            return null;
        }
        catch (NullPointerException n)
        {
            U.log("No item found");
            return null;
        }
    }

}
