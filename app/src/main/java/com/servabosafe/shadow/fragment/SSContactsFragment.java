package com.servabosafe.shadow.fragment;

import android.app.Fragment;
import android.content.ContentResolver;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.provider.ContactsContract;
import android.view.*;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.AdapterView;
import android.widget.ListView;
import android.widget.TextView;
import com.servabosafe.shadow.R;
import com.servabosafe.shadow.activity.SSCreateEventActivity;
import com.servabosafe.shadow.adapter.ContactAdapter;
import com.servabosafe.shadow.data.model.Contact;
import org.json.JSONArray;

import java.util.ArrayList;

/**
 * Created by brandon.burton on 10/13/14.
 */
public class SSContactsFragment extends Fragment implements AdapterView.OnItemClickListener, ActionMode.Callback {

    private ListView mContactsList;

    private TextView mInfo;

    private ActionMode mActionMode;

    private ContactAdapter mContactAdapter;

    @Override
    public void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);

        setHasOptionsMenu(true);

    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the fragment layout
        return inflater.inflate(R.layout.contact_list_fragment, container, false);
    }

    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        // Gets the ListView from the View list of the parent activity
        mContactsList = (ListView)getView().findViewById(R.id.contacts_list_view);

//        mInfo = (TextView)getView().findViewById(R.id.label_contacts);

        mContactAdapter = new ContactAdapter(getActivity());

        //get contact info
        fetchContacts();

        // Sets the adapter for the ListView
        mContactsList.setAdapter(mContactAdapter);

        mContactsList.setOnItemClickListener(this);

    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            // Respond to the action bar's Up/Home button
            case android.R.id.home:
                getActivity().finish();
                return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {

        mContactAdapter.highlightItem(position);

        //notify the data set
        mContactAdapter.notifyDataSetChanged();

        final int firstListItemPosition = mContactsList.getFirstVisiblePosition();

        final Animation anim = AnimationUtils.loadAnimation(getActivity(), android.R.anim.fade_in);
        anim.setDuration(200);
        //because position is the item in the adapter, the firstlistitemposition is the position of the element in the view
        mContactsList.getChildAt(position - firstListItemPosition).startAnimation(anim);
        new Handler().postDelayed(new Runnable() {

            public void run() {

            }

        }, anim.getDuration());


        //refresh the list view
        mContactsList.invalidate();

        if (mActionMode == null) {
            mActionMode = getActivity().startActionMode(this);
            view.setSelected(true);

        }
        else
            mActionMode.invalidate();
    }

    public void fetchContacts() {

        String phoneNumber = null;

        Uri CONTENT_URI = ContactsContract.Contacts.CONTENT_URI;
        String _ID = ContactsContract.Contacts._ID;
        String DISPLAY_NAME = ContactsContract.Contacts.DISPLAY_NAME;
        String HAS_PHONE_NUMBER = ContactsContract.Contacts.HAS_PHONE_NUMBER;

        Uri PhoneCONTENT_URI = ContactsContract.CommonDataKinds.Phone.CONTENT_URI;
        String Phone_CONTACT_ID = ContactsContract.CommonDataKinds.Phone.CONTACT_ID;
        String NUMBER = ContactsContract.CommonDataKinds.Phone.NUMBER;

        //StringBuffer output = new StringBuffer();

        ContentResolver contentResolver = getActivity().getContentResolver();

        Cursor cursor = contentResolver.query(CONTENT_URI, null, null, null, null);

        // Loop for every contact in the phone
        if (cursor.getCount() > 0) {

            while (cursor.moveToNext()) {

                String contact_id = cursor.getString(cursor.getColumnIndex(_ID));
                String name = cursor.getString(cursor.getColumnIndex( DISPLAY_NAME ));

                //int hasPhoneNumber = Integer.parseInt(cursor.getString(cursor.getColumnIndex( HAS_PHONE_NUMBER )));

                //if (hasPhoneNumber > 0) {

                    //output.append("\n First Name:" + name);

                    // Query and loop for every phone number of the contact
                    Cursor phoneCursor = contentResolver.query(PhoneCONTENT_URI, null, Phone_CONTACT_ID + " = ?", new String[] { contact_id }, null);

                    while (phoneCursor.moveToNext()) {

                        phoneNumber = phoneCursor.getString(phoneCursor.getColumnIndex(NUMBER));
                        //output.append("\n Phone number:" + phoneNumber);

                        mContactAdapter.add(new Contact(Integer.valueOf(contact_id), name, phoneNumber));

                    }

                    phoneCursor.close();

                //};
            }

            mContactAdapter.notifyDataSetChanged();
        }
    }

    // Called when the action mode is created; startActionMode() was called
    @Override
    public boolean onCreateActionMode(ActionMode mode, Menu menu) {
        // Inflate a menu resource providing context menu items
//        MenuInflater inflater = mode.getMenuInflater();
//        inflater.inflate(R.menu.menu_cab_add_contact, menu);
        return true;
    }

    // Called each time the action mode is shown. Always called after onCreateActionMode, but
    // may be called multiple times if the mode is invalidated.
    @Override
    public boolean onPrepareActionMode(ActionMode mode, Menu menu) {

        // remove previous items
        //menu.clear();
        final int checked = mContactAdapter.getCount();

        // update title with number of checked items
        mode.setTitle(mContactAdapter.getSelectedItemCount() + " selected");

        switch (checked) {
            case 0:
                break;
        }

        return true;
    }

    // Called when the user selects a contextual menu item
    @Override
    public boolean onActionItemClicked(ActionMode mode, MenuItem item) {

        switch (item.getItemId()) {

//            case R.id.action_save:
//                //save into database
//                Intent i = new Intent();
//
//                //convert contacts to json array
//                ArrayList<Contact> contacts = mContactAdapter.getSelectedContacts();
//                JSONArray j = new JSONArray();
//                for (Contact c : contacts)
//                {
//                    j.put(c.ToJSON());
//                }
//
//                i.putExtra("contacts", j.toString());
//                getActivity().setResult(SSCreateEventActivity.REQUEST_CODE, i);
//                getActivity().finish();
//                return true;
//            break;

            default:
                //save into database
//                Intent intent = new Intent();
//
//                //convert contacts to json array
//                ArrayList<Contact> mContacts = mContactAdapter.getSelectedContacts();
//                JSONArray j = new JSONArray();
//                for (Contact c : mContacts)
//                {
//                    j.put(c.ToJSON());
//                }
//
//                intent.putExtra("contacts", j.toString());
//                getActivity().setResult(SSCreateEventActivity.REQUEST_CODE, intent);
//                getActivity().finish();
                return true;

        }
    }

    // Called when the user exits the action mode
    @Override
    public void onDestroyActionMode(ActionMode mode) {

        mActionMode = null;

        // if nothing checked - exit action mode
        Intent intent = new Intent();

        //convert contacts to json array
        ArrayList<Contact> mContacts = mContactAdapter.getSelectedContacts();
        JSONArray j = new JSONArray();
        for (Contact c : mContacts)
        {
            j.put(c.ToJSON());
        }

        intent.putExtra("contacts", j.toString());
        getActivity().setResult(SSCreateEventActivity.REQUEST_CODE, intent);
        getActivity().finish();

    }

}
