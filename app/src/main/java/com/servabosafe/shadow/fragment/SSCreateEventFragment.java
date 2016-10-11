package com.servabosafe.shadow.fragment;

import android.app.Activity;
import android.app.Fragment;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.view.*;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.*;
import com.servabosafe.shadow.R;
import com.servabosafe.shadow.activity.SSAddContactActivity;
import com.servabosafe.shadow.activity.SSCreateEventActivity;
import com.servabosafe.shadow.adapter.ContactAdapter;
import com.servabosafe.shadow.data.model.Contact;
import com.servabosafe.shadow.data.model.Scenario;
import com.servabosafe.shadow.helper.U;
import com.servabosafe.shadow.helper.db.ScenarioDataSource;
import org.json.JSONArray;
import org.json.JSONException;

import java.sql.SQLException;
import java.util.ArrayList;

/**
 * Created by brandon.burton on 10/10/14.
 */
public class SSCreateEventFragment extends Fragment implements ActionMode.Callback {

    public static final String KEY_DB_POSIITON = "dbPosition";

    public static final int KEY_NEW_RECORD = -1;

    private Button mCreateEmergency;

    private Button mAddContact;

    private EditText mScenarioName;

    private EditText mMessageName;

    private ListView mContactsList;

    private ContactAdapter mContactAdapter;

    private ScenarioDataSource mDataSource;

    private ActionMode mActionMode;

    private int dbRow = -1;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {

        return inflater.inflate(R.layout.fragment_create_event, container, false);

    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {

        super.onActivityCreated(savedInstanceState);

        getActivity().setTitle("Create/Edit Emergency");

        dbRow = getArguments().getInt(KEY_DB_POSIITON, KEY_NEW_RECORD);

        mDataSource = new ScenarioDataSource(getActivity());

        initUi();

        try {
            loadData();
        } catch (SQLException e) {
            Toast.makeText(getActivity(), e.toString(), Toast.LENGTH_LONG).show();
        }
    }

    private void initUi()
    {
        mAddContact = (Button)getView().findViewById(R.id.button_add_contact);

        mAddContact.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent i = new Intent(getActivity(), SSAddContactActivity.class);
                startActivityForResult(i, SSCreateEventActivity.REQUEST_CODE);
                getActivity().overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
            }
        });

        mContactsList = (ListView)getView().findViewById(R.id.list_contact_list);

        mContactAdapter = new ContactAdapter(getActivity());

        mContactsList.setAdapter(mContactAdapter);

        mContactsList.setOnItemClickListener(new AdapterView.OnItemClickListener() {
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
                    mActionMode = getActivity().startActionMode(SSCreateEventFragment.this);
                    view.setSelected(true);

                }
                else
                    mActionMode.invalidate();
            }
        });

        mScenarioName = (EditText)getView().findViewById(R.id.edit_scenario_title);

        mMessageName = (EditText)getView().findViewById(R.id.edit_message);

        mCreateEmergency = (Button)getView().findViewById(R.id.button_save_scenario);

        mCreateEmergency.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                try
                {
                    mDataSource.open();

                    ArrayList<Contact> contacts = mContactAdapter.getAllContacts();
                    String title = mScenarioName.getText().toString();
                    String message = mMessageName.getText().toString();

                    if (dbRow != KEY_NEW_RECORD)
                        mDataSource.editScenario(dbRow, title, message, contacts);
                    else
                        mDataSource.addScenario(title, message, contacts);

                    mDataSource.close();

                    Intent intent = new Intent();
                    intent.putExtra("id", dbRow);

                    getActivity().setResult(Activity.RESULT_OK, intent);
                    getActivity().finish();
                }
                catch (SQLException e)
                {
                    e.printStackTrace();
                }
            }
        });
    }

    private void loadData() throws SQLException {

        if (dbRow != KEY_NEW_RECORD)
        {
            mDataSource.open();

            Scenario scenario = mDataSource.getScenario(dbRow);

            mDataSource.close();

            mScenarioName.setText(scenario.getTitle());

            mMessageName.setText(scenario.getMessage());

            for (Contact c : scenario.getContactData())
                mContactAdapter.add(c);

            mContactAdapter.notifyDataSetChanged();
        }
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {

        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == SSCreateEventActivity.REQUEST_CODE) {

            //parse selected items to object
            mContactAdapter.clear();

            try
            {
                if (data != null) {
                    JSONArray j = new JSONArray(data.getExtras().getString("contacts", "empty"));
                    for (int i = 0; i < j.length(); i++) {
                        mContactAdapter.add(new Contact(j.getString(i)));
                    }
                }
                else
                {
                    Toast.makeText(getActivity(), "No contacts selected.", Toast.LENGTH_SHORT).show();
                }

            }
            catch (JSONException e)
            {
                e.printStackTrace();
            }

            mContactAdapter.notifyDataSetChanged();
        }
        else
        {
            U.log("No items selected");
        }

    }

    // Called when the action mode is created; startActionMode() was called
    @Override
    public boolean onCreateActionMode(ActionMode mode, Menu menu) {
        // Inflate a menu resource providing context menu items
        MenuInflater inflater = mode.getMenuInflater();
        inflater.inflate(R.menu.menu_cab_remove_contact, menu);
        return true;
    }

    @Override
    public void onPause() {

        super.onPause();

        if (mActionMode != null)
            mActionMode.finish();
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
                // if nothing checked - exit action mode
                mode.finish();
                return true;
            default:
                return true;
        }
    }

    // Called when the user selects a contextual menu item
    @Override
    public boolean onActionItemClicked(ActionMode mode, MenuItem item) {

        switch (item.getItemId()) {

            case R.id.action_remove: {
                //remove selected items
                mContactAdapter.removeSelectedItems();

                //remove selected items
                mContactAdapter.notifyDataSetChanged();

                //refresh view to show update
                mContactsList.invalidate();

                mActionMode.finish();

                return true;
            }
            default:
                mode.finish();
                return false;
        }
    }

    // Called when the user exits the action mode
    @Override
    public void onDestroyActionMode(ActionMode mode) {
        mActionMode = null;
    }
}
