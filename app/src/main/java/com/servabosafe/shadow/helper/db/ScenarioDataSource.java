package com.servabosafe.shadow.helper.db;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.CursorIndexOutOfBoundsException;
import android.database.DatabaseUtils;
import android.database.sqlite.SQLiteDatabase;
import com.servabosafe.shadow.data.model.Contact;
import com.servabosafe.shadow.data.model.Scenario;
import com.servabosafe.shadow.helper.U;
import org.json.JSONArray;
import org.json.JSONException;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by brandon.burton on 10/13/14.
 */
public class ScenarioDataSource {

    // Database fields
    private SQLiteDatabase database;
    private DBSQLiteHelper dbHelper;
    private String[] allColumns = { DBSQLiteHelper.COLUMN_ID, DBSQLiteHelper.COLUMN_SCENARIO_TITLE, DBSQLiteHelper.COLUMN_SCENARIO_MESSAGE, DBSQLiteHelper.COLUMN_CONTACT_DATA };

    public ScenarioDataSource(Context context) {
        dbHelper = new DBSQLiteHelper(context);
    }

    public void open() throws SQLException {
        database = dbHelper.getWritableDatabase();
    }

    public void close() {
        dbHelper.close();
    }

//
//    public void deleteFile(InfoFile file) {
//        long id = file.getId();
//        System.out.println("file deleted with id: " + id);
//        database.delete(MySQLiteHelper.TABLE_FILES, MySQLiteHelper.COLUMN_ID
//                + " = " + id, null);
//    }
//
    public List<Scenario> getAllScenarios() {

        List<Scenario> scenarios = new ArrayList<Scenario>();

        Cursor cursor = database.query(DBSQLiteHelper.SCENARIO_TABLE, allColumns, null, null, null, null, null);

        try {


            cursor.moveToFirst();
            while (!cursor.isAfterLast()) {
                Scenario file = cursorToScenario(cursor);
                scenarios.add(file);
                cursor.moveToNext();
            }
            // make sure to close the cursor
            cursor.close();
        } catch (CursorIndexOutOfBoundsException c) {
            U.log("Cursour out of bounds in data source");
        }

        return scenarios;
    }

    public Scenario getScenario(int id) {

        Scenario scenario;

        //get all columns
        Cursor cursor = database.rawQuery("SELECT * FROM " + DBSQLiteHelper.SCENARIO_TABLE + " where _id="+id, null);

        cursor.moveToFirst();

        //convert to java object
        try {
            scenario = cursorToScenario(cursor);
        } catch (CursorIndexOutOfBoundsException c) {
            U.log("Cursor out of bounds in data source");
            return null;
        }

        cursor.close();

        return scenario;
    }
//

    private Scenario cursorToScenario(Cursor cursor){

        Scenario scenario = null;
        try {
            scenario = new Scenario();
            scenario.setId(cursor.getInt(0)).setTitle(cursor.getString(1)).setMessage(cursor.getString(2));
            scenario.setContactData(cursor.getString(3));
        } catch (JSONException e) {
            e.printStackTrace();
        } catch (CursorIndexOutOfBoundsException c) {
            U.log("Cursor out of bounds");
            return null;
        }
        //parse json
        return scenario;
    }

    /**
     * Add a scenario to the database
     * @param title - Title of the scenario
     * @param message - What will be sent
     * @param contactData - The phone numbers the message will be sent to
     */
    public void addScenario(String title, String message, ArrayList<Contact> contactData) {

        ContentValues values = new ContentValues();
        values.put(DBSQLiteHelper.COLUMN_SCENARIO_TITLE, title);
        values.put(DBSQLiteHelper.COLUMN_SCENARIO_MESSAGE, message);

        JSONArray j = new JSONArray();
        for (Contact c : contactData)
        {
            j.put(c.ToJSON());
        }

        //parse JSON
        values.put(DBSQLiteHelper.COLUMN_CONTACT_DATA, j.toString());

        long insertId = database.insert(DBSQLiteHelper.SCENARIO_TABLE, null,
                values);

        Cursor cursor = database.query(DBSQLiteHelper.SCENARIO_TABLE,
                allColumns, DBSQLiteHelper.COLUMN_ID + " = " + insertId, null,
                null, null, null);
        cursor.moveToFirst();


       cursor.close();

    }

    /**
     * Add a scenario to the database
     * @param row - row in db
     * @param title - Title of the scenario
     * @param message - What will be sent
     * @param contactData - The phone numbers the message will be sent to
     */
    public void editScenario(int row, String title, String message, ArrayList<Contact> contactData) {

        ContentValues values = new ContentValues();
        values.put(DBSQLiteHelper.COLUMN_SCENARIO_TITLE, title);
        values.put(DBSQLiteHelper.COLUMN_SCENARIO_MESSAGE, message);

        JSONArray j = new JSONArray();
        for (Contact c : contactData)
        {
            j.put(c.ToJSON());
        }

        //parse JSON
        values.put(DBSQLiteHelper.COLUMN_CONTACT_DATA, j.toString());

        database.update(DBSQLiteHelper.SCENARIO_TABLE, values, "_id "+"="+row, null);

//        long editId = database.update(DBSQLiteHelper.SCENARIO_TABLE, values, "_id "+"="+row, null);

//        Cursor cursor = database.query(DBSQLiteHelper.SCENARIO_TABLE,
//                allColumns, DBSQLiteHelper.COLUMN_ID + " = " + editId, null,
//                null, null, null);
//        cursor.moveToFirst();
//
//        Scenario scenario = cursorToScenario(cursor);
//        cursor.close();

    }

    /**
     * Remove a record
     * @param row - row in db
     */
    public void removeScenario(int row) {

        database.delete(DBSQLiteHelper.SCENARIO_TABLE, "_id "+"="+row, null);

    }



    public boolean isEmpty()
    {
        long num = DatabaseUtils.queryNumEntries(database, DBSQLiteHelper.SCENARIO_TABLE);
        return (num < 1);
    }

}
