package com.servabosafe.shadow.adapter;

import android.content.Context;
import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import com.servabosafe.shadow.R;
import com.servabosafe.shadow.data.model.Contact;

import java.util.*;

/**
 * Created by brandon.burton on 1/24/14.
 */
public class ContactAdapter extends EasyAdapter<Contact> {

    private LayoutInflater mInflater;

    //the color of the highlight
    private int highlightColor;

    private HashMap<Integer, Contact> mMap;

    public ContactAdapter(Context context) {

        super(context);

        init(context);
    }

    public ContactAdapter(Context context, ArrayList<Contact> items) {

        super(context, items);

        init(context);
    }

    private void init(Context context)
    {
        mInflater = (LayoutInflater)context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);

        mMap = new HashMap<Integer, Contact>();

        highlightColor = context.getResources().getColor(R.color.highlight);
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {

        ContactHolder holder;

        Contact contact = getItem(position);

        if (convertView == null)
        {
            convertView = mInflater.inflate(R.layout.cell_contact, null);

            holder = new ContactHolder();

            holder.mTitle = (TextView)convertView.findViewById(R.id.scenario_name);
            holder.mSubTitle = (TextView)convertView.findViewById(R.id.contact_phone_number);

            convertView.setTag(holder);

        }
        else {
            holder = (ContactHolder)convertView.getTag();
        }

        holder.mTitle.setText(contact.getName());
        holder.mSubTitle.setText(contact.getPhone());

        if (mMap.containsKey(position))
        {
            convertView.setBackgroundColor(highlightColor);
            holder.mSubTitle.setTextColor(Color.BLACK);
        }
        else
        {
            convertView.setBackgroundColor(Color.TRANSPARENT);
            holder.mSubTitle.setTextColor(Color.LTGRAY);
        }

        return convertView;
    }

    public class ContactHolder
    {
        public TextView mTitle;
        public TextView mSubTitle;
    }

    public void highlightItem(int position)
    {
        if (mMap.containsKey(position)) {
            mMap.remove(position);
        }
        else
        {
            mMap.put(position, getItem(position));
        }

    }

    public int getSelectedItemCount()
    {
        return mMap.size();
    }

    public ArrayList<Contact> getAllContacts()
    {
        return mItems;
    }

    public ArrayList<Contact> getSelectedContacts()
    {
        ArrayList<Contact> contacts = new ArrayList<Contact>();

        for (Contact c : mMap.values())
        {
            contacts.add(c);
        }

        return contacts;
    }

    public void removeSelectedItems()
    {

        Set<Integer> keys = mMap.keySet();

        List<Integer> list = new ArrayList<Integer>();
        list.addAll(keys);

        //reverse order of removal so there are no indicies out of range
        Collections.reverse(list);

        for(Integer i : list)
        {
            //remove selected item from map
            //mMap.remove(i);

            //then remove it from array
            mItems.remove((int)i);
        }
        mMap.clear();
    }
}
