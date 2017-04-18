package com.example.android.recyclerplayground;

import android.os.Bundle;
import android.support.design.widget.NavigationView;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBar;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;

import com.example.android.recyclerplayground.fragments.FixedTwoWayFragment;
import com.example.android.recyclerplayground.fragments.HorizontalFragment;
import com.example.android.recyclerplayground.fragments.InformationFragment;
import com.example.android.recyclerplayground.fragments.NavigationDrawerFragment;
import com.example.android.recyclerplayground.fragments.VerticalFragment;
import com.example.android.recyclerplayground.fragments.VerticalGridFragment;
import com.example.android.recyclerplayground.fragments.VerticalStaggeredGridFragment;
import com.wakereality.storyfinding.R;
import com.yrek.incant.StoryListSpot;


/*
Git commit history will show the evolution from the base RecyclerPlayground from
  https://github.com/devunwired/recyclerview-playground
 */
public class BrowseStoriesNewDrawerActivity extends AppCompatActivity implements
        NavigationDrawerFragment.NavigationDrawerCallbacks {

    public static final String TAG = "BrowseStories";

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

    /**
     * Used to store the last screen title. For use in {@link #restoreActionBar()}.
     */
    private CharSequence mTitle;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        boolean originalPlaygroundDrawer = false;

        if (originalPlaygroundDrawer) {
            setContentView(R.layout.story_browse_activity_main);

            mNavigationDrawerFragment = (NavigationDrawerFragment)
                    getSupportFragmentManager().findFragmentById(R.id.navigation_drawer);

            // Set up the drawer.
            mNavigationDrawerFragment.setUp(
                    R.id.navigation_drawer,
                    (DrawerLayout) findViewById(R.id.drawer_layout));
        } else {
            setContentView(R.layout.story_browse_activity_main_drawer2);

            // Set a Toolbar to replace the ActionBar.
            toolbar = (Toolbar) findViewById(R.id.toolbar);
            setSupportActionBar(toolbar);

            // Find our drawer view
            mDrawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        }

        mTitle = getTitle();
    }

    @Override
    public void onNavigationDrawerItemSelected(int position) {
        Log.d(TAG, "onNavigationDrawerItemSelected " + position);
        // update the main content by replacing fragments
        FragmentManager fragmentManager = getSupportFragmentManager();
        FragmentTransaction ft = fragmentManager.beginTransaction();
        // These need to be in the same index order as the setAdapter
        switch (position) {
            case 0:
                ft.replace(R.id.container, VerticalFragment.newInstance());
                break;
            case 6:
                ft.replace(R.id.container, HorizontalFragment.newInstance());
                break;
            case 3:
                StoryListSpot.listNumberOfColumns = 2;
                ft.replace(R.id.container, VerticalGridFragment.newInstance());
                break;
            case 4:
                StoryListSpot.listNumberOfColumns = 3;
                ft.replace(R.id.container, VerticalGridFragment.newInstance());
                break;
            case 1:
                StoryListSpot.listNumberOfColumns = 2;
                ft.replace(R.id.container, VerticalStaggeredGridFragment.newInstance());
                break;
            case 2:
                StoryListSpot.listNumberOfColumns = 3;
                ft.replace(R.id.container, VerticalStaggeredGridFragment.newInstance());
                break;
            case 5:
                ft.replace(R.id.container, FixedTwoWayFragment.newInstance());
                break;
            case 7:
                ft.replace(R.id.container, InformationFragment.newInstance());
                break;
            default:
                // Do nothing
                break;
        }

        ft.commit();
    }

    public void restoreActionBar() {
        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.setDisplayShowTitleEnabled(true);
            actionBar.setTitle(mTitle);
            actionBar.setDisplayHomeAsUpEnabled(true);
            actionBar.setHomeButtonEnabled(true);
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        if (mNavigationDrawerFragment != null) {
            if (!mNavigationDrawerFragment.isDrawerOpen()) {
                // Only show items in the action bar relevant to this screen
                // if the drawer is not showing. Otherwise, let the drawer
                // decide what to show in the action bar.
                restoreActionBar();
                return true;
            }
        }
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        return super.onOptionsItemSelected(item);
    }

}
