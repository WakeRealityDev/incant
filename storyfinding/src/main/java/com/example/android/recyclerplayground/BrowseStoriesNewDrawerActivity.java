package com.example.android.recyclerplayground;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.design.widget.NavigationView;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;

import android.view.MenuItem;
import android.view.View;

import com.example.android.recyclerplayground.fragments.InformationFragment;
import com.example.android.recyclerplayground.fragments.NavigationDrawerFragment;
import com.example.android.recyclerplayground.fragments.VerticalFragment;
import com.example.android.recyclerplayground.fragments.VerticalGridFragment;
import com.example.android.recyclerplayground.fragments.VerticalStaggeredGridFragment;
import com.wakereality.storyfinding.R;
import com.yrek.incant.StoryListSpot;

import static com.example.android.recyclerplayground.fragments.NavigationDrawerFragment.PREF_USER_LEARNED_DRAWER;


/*
Git commit history will show the evolution from the base RecyclerPlayground from
  https://github.com/devunwired/recyclerview-playground
 */
public class BrowseStoriesNewDrawerActivity extends AppCompatActivity {

    public static final String TAG = "BrowseStories";

    /**
     * Remember the position of the selected item.
     * Different save from other Nav Drawer
     */
    public static final String STATE_SELECTED_POSITION = "navigation_drawer2_position";

    /**
     * Fragment managing the behaviors, interactions and presentation of the navigation drawer.
     */
    private NavigationDrawerFragment mNavigationDrawerFragment;

    // Make sure to be using android.support.v7.app.ActionBarDrawerToggle version.
    // The android.support.v4.app.ActionBarDrawerToggle has been deprecated.
    private ActionBarDrawerToggle drawerToggle;

    private NavigationView nvDrawer;
    private DrawerLayout mDrawer;
    private Toolbar toolbar;

    private int mCurrentSelectedPosition = 0;
    private boolean mUserLearnedDrawer;
    private SharedPreferences sp;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.story_browse_activity_main_drawer2);

        // Read in the flag indicating whether or not the user has demonstrated awareness of the
        // drawer. See PREF_USER_LEARNED_DRAWER for details.
        sp = PreferenceManager.getDefaultSharedPreferences(this);
        mUserLearnedDrawer = sp.getBoolean(PREF_USER_LEARNED_DRAWER, false);

        // New logic beyond RecyclerPlayground save preference for next app startup.
        int appOpeningChoice = getResources().getInteger(R.integer.story_browse_app_opening_choice);
        if (sp.contains(STATE_SELECTED_POSITION)) {
            mCurrentSelectedPosition = sp.getInt(STATE_SELECTED_POSITION, appOpeningChoice);
        } else {
            mCurrentSelectedPosition = appOpeningChoice;
        }

        // Find the toolbar view inside the activity layout
        toolbar = (Toolbar) findViewById(R.id.toolbar);
        // Sets the Toolbar to act as the ActionBar for this Activity window.
        // Make sure the toolbar exists in the activity and is not null
        setSupportActionBar(toolbar);

        // Find our drawer view
        mDrawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        drawerToggle = setupDrawerToggle();

        // Tie DrawerLayout events to the ActionBarToggle
        mDrawer.addDrawerListener(drawerToggle);

        nvDrawer = (NavigationView) findViewById(R.id.nvView);
        setupDrawerContent(nvDrawer);

        // Pick on startup
        switch (mCurrentSelectedPosition) {
            case 0:
            case 1:
            case 2:
            case 3:
            case 4:
                selectDrawerItem(nvDrawer.getMenu().getItem(mCurrentSelectedPosition));
                break;
            case 5:   // Settings
            case 6:   // Get More
                // don't set these
                break;
        }

        // If the user hasn't 'learned' about the drawer, open it to introduce them to the drawer,
        // per the navigation drawer design guidelines.
        if (!mUserLearnedDrawer) {
            mDrawer.openDrawer(nvDrawer);
        }
    }

    private ActionBarDrawerToggle setupDrawerToggle() {
        return new ActionBarDrawerToggle(this, mDrawer, toolbar, R.string.navigation_drawer_open,
                R.string.navigation_drawer_close)
            {
            @Override
            public void onDrawerClosed(View drawerView) {
                super.onDrawerClosed(drawerView);

                invalidateOptionsMenu(); // calls onPrepareOptionsMenu()
            }

            @Override
            public void onDrawerOpened(View drawerView) {
                super.onDrawerOpened(drawerView);
                if (!mUserLearnedDrawer) {
                    // The user manually opened the drawer; store this flag to prevent auto-showing
                    // the navigation drawer automatically in the future.
                    mUserLearnedDrawer = true;

                    sp.edit().putBoolean(PREF_USER_LEARNED_DRAWER, true).commit();
                }

                invalidateOptionsMenu(); // calls onPrepareOptionsMenu()
            }
        };
    }

    public int getSaveIndexForNavOrder(MenuItem menuItem) {
        int returnPosition  = 0;
        int i = menuItem.getItemId();
        if (i == R.id.nav_second_fragment) {
            returnPosition = 1;
        } else if (i == R.id.nav_third_fragment) {
            returnPosition = 2;
        } else if (i == R.id.nav_list_2col_aligned) {
            returnPosition = 3;
        } else if (i == R.id.nav_list_3col_aligned) {
            returnPosition = 4;
        } else if (i == R.id.nav_settings) {
            returnPosition = 5;
        } else if (i == R.id.nav_get_more){
            returnPosition = 6;
        }
        return returnPosition;
    }

    @Override
    protected void onPostCreate(Bundle savedInstanceState) {
        super.onPostCreate(savedInstanceState);
        // Sync the toggle state after onRestoreInstanceState has occurred.
        drawerToggle.syncState();
    }

    private void setupDrawerContent(NavigationView navigationView) {
        navigationView.setNavigationItemSelectedListener(
                new NavigationView.OnNavigationItemSelectedListener() {
                    @Override
                    public boolean onNavigationItemSelected(MenuItem menuItem) {
                        selectDrawerItem(menuItem);
                        return true;
                    }
                });
    }

    public void selectDrawerItem(MenuItem menuItem) {
        // Create a new fragment and specify the fragment to show based on nav item clicked
        Fragment fragment = null;
        Class fragmentClass;
        int i = menuItem.getItemId();
        StoryListSpot.listNumberOfColumns = 1;
        if (i == R.id.nav_first_fragment) {
            fragmentClass = VerticalFragment.class;

        } else if (i == R.id.nav_second_fragment) {
            StoryListSpot.listNumberOfColumns = 2;
            fragmentClass = VerticalStaggeredGridFragment.class;

        } else if (i == R.id.nav_third_fragment) {
            StoryListSpot.listNumberOfColumns = 3;
            fragmentClass = VerticalStaggeredGridFragment.class;

        } else if (i == R.id.nav_list_2col_aligned) {
            StoryListSpot.listNumberOfColumns = 2;
            fragmentClass = VerticalGridFragment.class;

        } else if (i == R.id.nav_list_3col_aligned) {
            StoryListSpot.listNumberOfColumns = 3;
            fragmentClass = VerticalGridFragment.class;

        } else if (i == R.id.nav_settings) {
            fragmentClass = InformationFragment.class;

        } else if (i == R.id.nav_get_more) {
            fragmentClass = InformationFragment.class;

        } else {
            fragmentClass = VerticalFragment.class;
        }

        try {
            fragment = (Fragment) fragmentClass.newInstance();
        } catch (Exception e) {
            e.printStackTrace();
        }

        // Insert the fragment by replacing any existing fragment
        FragmentManager fragmentManager = getSupportFragmentManager();
        fragmentManager.beginTransaction().replace(R.id.container, fragment).commit();

        // Highlight the selected item has been done by NavigationView
        menuItem.setChecked(true);
        // Set action bar title
        CharSequence outTitle = menuItem.getTitle();
        if (StoryListSpot.listNumberOfColumns > 1) {
            outTitle = StoryListSpot.listNumberOfColumns + " " + outTitle;
        }
        setTitle(outTitle);
        // Close the navigation drawer
        mDrawer.closeDrawers();

        // Save
        int layoutSelectIndex = getSaveIndexForNavOrder(menuItem);
        if (layoutSelectIndex <= 4) {
            // Save only if it is a layout preference, not the settings and get more
            sp.edit().putInt(STATE_SELECTED_POSITION, layoutSelectIndex).commit();
        }
    }


    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        return super.onOptionsItemSelected(item);
    }

}
