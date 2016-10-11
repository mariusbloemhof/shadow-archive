package com.servabosafe.shadow.data.model;

import org.json.JSONException;
import org.json.JSONObject;

/**
 * Created by brandon.burton on 10/10/14.
 */
public class Contact {

    public static final String KEY_ID = "id";
    public static final String KEY_NAME = "alias";
    public static final String KEY_PHONE = "phone";

    private int id;
    private String name;
    private String phone;

    public Contact() {
        id = -1;
        name = null;
        phone = null;
    }

    public Contact(int id, String name, String homePhone) {
        this.id = id;
        this.name = name;
        this.phone = homePhone;
    }

    /**
     *
     * @param j, string representation of JSON
     */
    public Contact(String j)
    {
        try {
            JSONObject json = new JSONObject(j);
            id = json.getInt(KEY_ID);
            name = json.getString(KEY_NAME);
            phone = json.getString(KEY_PHONE);
        } catch (JSONException e) {
            e.printStackTrace();
        }

    }
    /**
     *
     * @param json
     * @throws JSONException
     */
    public Contact(JSONObject json) throws JSONException {
        id = json.getInt(KEY_ID);
        name = json.getString(KEY_NAME);
        phone = json.getString(KEY_PHONE);
    }


    public boolean hasPhone() {
        if (phone == null || phone.length() == 0) {
            return false;
        }
        return true;
    }

    public String getName() {
        return name;
    }

    public String getPhone() {
        return phone;
    }

    public int getId()
    {
        return id;
    }

    public Contact setName(String name) {
        this.name = name;
        return this;
    }

    public Contact setPhone(String phone) {
        this.phone = phone;
        return this;
    }

    public Contact setId(int id)
    {
        this.id = id;
        return this;
    }

    /**
     *
     * @return
     */
    public String ToJSON()
    {
        JSONObject j = new JSONObject();
        try
        {
            j.put(KEY_ID, id);
            j.put(KEY_NAME, name);
            j.put(KEY_PHONE, phone);
        }
        catch (JSONException e)
        {
            e.printStackTrace();
        }

        return j.toString();
    }

}
