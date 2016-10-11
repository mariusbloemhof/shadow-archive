package com.servabosafe.shadow.data.model;

import org.json.JSONArray;
import org.json.JSONException;

import java.util.ArrayList;

/**
 * Created by brandon.burton on 10/13/14.
 */
public class Scenario {

    private int mId;

    private String title;

    private String message;

    private ArrayList<Contact> contactData;

    public Scenario() {
        mId = -1;
        title = "";
        message = "";
        contactData = new ArrayList<Contact>();
    }

    public int getId() {
        return mId;
    }

    public Scenario setId(int mId) {
        this.mId = mId;
        return this;
    }

    public String getTitle() {
        return title;
    }

    public Scenario setTitle(String title) {
        this.title = title;
        return this;
    }


    public String getMessage() {
        return message;
    }

    public Scenario setMessage(String message) {
        this.message = message;
        return this;
    }

    public ArrayList<Contact> getContactData() {
        return contactData;
    }

    /**
     * The parsed contact data was saved in the database
     * @param parsedData
     * @return
     * @throws JSONException
     */
    public void setContactData(String parsedData) throws JSONException {

        JSONArray jsonArray = new JSONArray(parsedData);
        for (int i = 0; i < jsonArray.length(); i++)
        {
            String s = jsonArray.getString(i);
            Contact c = new Contact(s);
            contactData.add(c);
        }
    }
}
