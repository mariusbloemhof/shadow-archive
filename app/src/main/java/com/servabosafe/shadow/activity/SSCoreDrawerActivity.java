package com.servabosafe.shadow.activity;

import android.app.Activity;
import android.content.res.Configuration;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.support.v4.app.ActionBarDrawerToggle;
import android.support.v4.widget.DrawerLayout;
import android.view.*;
import com.servabosafe.shadow.R;
import com.servabosafe.shadow.helper.U;

/**
 * Created by brandon.burton on 10/10/14.
 */
public class SSCoreDrawerActivity extends Activity {

    private DrawerLayout mDrawerLayout;

    private ActionBarDrawerToggle mDrawerToggle;

    @Override
    public void onCreate(Bundle savedInstanceState)
    {
        requestWindowFeature(Window.FEATURE_ACTION_BAR);

        super.onCreate(savedInstanceState);

        getActionBar().setIcon(new ColorDrawable(getResources().getColor(android.R.color.transparent)));

        //TODO obstacle
        getActionBar().setDisplayHomeAsUpEnabled(true);
        getActionBar().setHomeButtonEnabled(true);

        //supportRequestWindowFeature( Window.FEATURE_INDETERMINATE_PROGRESS );

        setContentView(R.layout.base_drawer_layout);

        mDrawerLayout = (DrawerLayout) findViewById(R.id.base_core_drawer_layout);

        // Set the adapter for the list view
        //mDrawerList.setAdapter(new ArrayAdapter<String>(this,
        //        R.layout.drawer_list_item, mPlanetTitles));

        mDrawerToggle = new ActionBarDrawerToggle(
                this,                  /* host Activity */
                mDrawerLayout,         /* DrawerLayout object */
                R.drawable.ic_navigation_drawer,  /* nav drawer icon to replace 'Up' caret */
                R.string.drawer_open,  /* "open drawer" description */
                R.string.drawer_close  /* "close drawer" description */
        ) {

            /** Called when a drawer has settled in a completely closed state. */
            public void onDrawerClosed(View view) {
                super.onDrawerClosed(view);
                //getSupportActionBar().setNavigationMode(android.support.v7.app.ActionBar.NAVIGATION_MODE_TABS);
            }

            /** Called when a drawer has settled in a completely open state. */
            public void onDrawerOpened(View drawerView) {
                super.onDrawerOpened(drawerView);
                //getSupportActionBar().setNavigationMode(android.support.v7.app.ActionBar.NAVIGATION_MODE_STANDARD);
                //getSupportActionBar().setTitle(mDrawerTitle);
            }
        };

        // Set the drawer toggle as the DrawerListener
        mDrawerLayout.setDrawerListener(mDrawerToggle);

    }

    @Override
    protected void onPostCreate(Bundle savedInstanceState) {

        super.onPostCreate(savedInstanceState);

        mDrawerToggle.syncState();
    }

    /**
     * When the device is rotated, call this function
     * @param newConfig
     */
    @Override
    public void onConfigurationChanged(Configuration newConfig) {

        super.onConfigurationChanged(newConfig);

        mDrawerToggle.onConfigurationChanged(newConfig);

    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
//        MenuInflater menuInflater = getMenuInflater();
//        menuInflater.inflate(R.menu.menu_with_dashboard, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {

        if (mDrawerToggle.onOptionsItemSelected(item)) {

            //close the right one if necessary
            if (mDrawerLayout.isDrawerOpen(Gravity.END))
                mDrawerLayout.closeDrawer(Gravity.END);

            return true;
        }
        else if (item.getItemId() == R.id.action_show_dashboard) {

//            //close the left one if necessary
//            if (mDrawerLayout.isDrawerOpen(Gravity.START))
//                mDrawerLayout.closeDrawer(Gravity.START);
//
//            //close the right one if necessary
//            if (mDrawerLayout.isDrawerOpen(Gravity.END))
//                mDrawerLayout.closeDrawer(Gravity.END);
//
//            //open the right one
//            else if (!mDrawerLayout.isDrawerOpen(Gravity.END))
//                mDrawerLayout.openDrawer(Gravity.END);


        }

        return super.onOptionsItemSelected(item);    //To change body of overridden methods use File | Settings | File Templates.
    }

    public void setActivityView(int layout)
    {
        try
        {
            View v = getLayoutInflater().inflate(layout, null);

            mDrawerLayout.addView(v, 0, mDrawerLayout.getLayoutParams());
        }
        catch (Exception e)
        {
            U.log(e);
        }

    }

    /**
     * The app drawer is on the left side, Gravity.START indicates the left and top of the main layout
     */
    @Override
    public void onBackPressed() {

        if (mDrawerLayout.isDrawerOpen(Gravity.START))
            mDrawerLayout.closeDrawer(Gravity.START);
        if (mDrawerLayout.isDrawerOpen(Gravity.END))
            mDrawerLayout.closeDrawer(Gravity.END);

        super.onBackPressed();

    }
}
