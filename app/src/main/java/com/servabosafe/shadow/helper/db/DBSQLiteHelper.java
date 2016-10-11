package com.servabosafe.shadow.helper.db;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;

/**
 * Created by brandon.burton on 10/13/14.
 */
public class DBSQLiteHelper extends SQLiteOpenHelper {

    public static final String DATABASE_NAME = "scenarios.db";
    private static final int DATABASE_VERSION = 1;

    public static final String SCENARIO_TABLE = "scenarioTable";

    public static final String COLUMN_ID = "_id";
    public static final String COLUMN_SCENARIO_TITLE = "title";
    public static final String COLUMN_SCENARIO_MESSAGE = "label";
    public static final String COLUMN_CONTACT_DATA = "data";

    // Database creation sql statement
    private static final String SCENARIO_TABLE_CREATE = "create table "
            + SCENARIO_TABLE + "("
            + COLUMN_ID + " integer primary key autoincrement, "
            + COLUMN_SCENARIO_TITLE + " text not null, "
            + COLUMN_SCENARIO_MESSAGE + " text not null, "
            + COLUMN_CONTACT_DATA + " text not null);";

    public DBSQLiteHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase database) {
        database.execSQL(SCENARIO_TABLE_CREATE);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        Log.w(DBSQLiteHelper.class.getName(),
                "Upgrading database from version " + oldVersion + " to "
                        + newVersion + ", which will destroy all old data");
        db.execSQL("DROP TABLE IF EXISTS " + SCENARIO_TABLE);
        onCreate(db);
    }

}
