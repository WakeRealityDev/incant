package com.example.android.recyclerplayground.fragments;

import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.Toast;

import com.example.android.recyclerplayground.NumberPickerDialog;
import com.example.android.recyclerplayground.adapters.SimpleAdapter;
import com.wakereality.storyfinding.EventStoryNonListDownload;
import com.wakereality.storyfinding.R;
import com.yrek.incant.DownloadSpot;
import com.yrek.incant.Story;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;

public abstract class RecyclerFragment extends Fragment implements AdapterView.OnItemClickListener, AdapterView.OnItemLongClickListener {

    private RecyclerView mList;
    private SimpleAdapter mAdapter;

    /** Required Overrides for Sample Fragments */

    protected abstract RecyclerView.LayoutManager getLayoutManager();
    protected abstract RecyclerView.ItemDecoration getItemDecoration();
    protected abstract int getDefaultItemCount();
    protected abstract SimpleAdapter getAdapter();

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setHasOptionsMenu(true);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.fragment_recycler, container, false);

        mList = (RecyclerView) rootView.findViewById(R.id.section_list);
        mList.setLayoutManager(getLayoutManager());
        mList.addItemDecoration(getItemDecoration());

        mList.getItemAnimator().setAddDuration(1000);
        mList.getItemAnimator().setChangeDuration(1000);
        mList.getItemAnimator().setMoveDuration(1000);
        mList.getItemAnimator().setRemoveDuration(1000);

        mAdapter = getAdapter();
        mAdapter.setAdapterContent(getContext());
        mAdapter.setOnItemClickListener(this);
        mAdapter.setOnItemLongClickListener(this);
        mList.setAdapter(mAdapter);

        return rootView;
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        inflater.inflate(R.menu.grid_options, menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        NumberPickerDialog dialog;
        int i = item.getItemId();
        if (i == R.id.action_add) {
            dialog = new NumberPickerDialog(getActivity());
            dialog.setTitle("Position to Add");
            dialog.setPickerRange(0, mAdapter.getItemCount());
            dialog.setOnNumberSelectedListener(new NumberPickerDialog.OnNumberSelectedListener() {
                @Override
                public void onNumberSelected(int value) {
                    mAdapter.addItem(value);
                }
            });
            dialog.show();

            return true;
        } else if (i == R.id.action_remove) {
            dialog = new NumberPickerDialog(getActivity());
            dialog.setTitle("Position to Remove");
            dialog.setPickerRange(0, mAdapter.getItemCount() - 1);
            dialog.setOnNumberSelectedListener(new NumberPickerDialog.OnNumberSelectedListener() {
                @Override
                public void onNumberSelected(int value) {
                    mAdapter.removeItem(value);
                }
            });
            dialog.show();

            return true;
        } else if (i == R.id.action_empty) {
            mAdapter.setAdapterContent(getContext());
            return true;
        } else if (i == R.id.action_small) {
            mAdapter.setAdapterContent(getContext());
            return true;
        } else if (i == R.id.action_medium) {
            mAdapter.setAdapterContent(getContext());
            return true;
        } else if (i == R.id.action_large) {
            mAdapter.setAdapterContent(getContext());
            return true;
        } else if (i == R.id.action_scroll_zero) {
            mList.scrollToPosition(0);
            return true;
        } else if (i == R.id.action_smooth_zero) {
            mList.smoothScrollToPosition(0);
            return true;
        } else {
            return super.onOptionsItemSelected(item);
        }
    }

    @Override
    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
        Story story = mAdapter.getStoryForPosition(position);
        Toast.makeText(getActivity(),
                "Clicked: " + position + ", index " + mList.indexOfChild(view) + " " + story.getName(getContext()),
                Toast.LENGTH_SHORT).show();

        if (story.isDownloaded(getContext())) {
            // click means launch
        } else {
            story.setDownloadingNow(! story.isDownloadingNow());
            mAdapter.notifyDataSetChanged();
        }
    }

    @Override
    public boolean onItemLongClick(AdapterView<?> parent, View view, int position, long id) {
        Story story = mAdapter.getStoryForPosition(position);
        Toast.makeText(getActivity(),
                "LONG Clicked: " + position + ", index " + mList.indexOfChild(view) + " " + story.getName(getContext()),
                Toast.LENGTH_SHORT).show();
        return true;
    }


    @Override
    public void onResume() {
        super.onResume();
        if (! EventBus.getDefault().isRegistered(this)) {
            Log.i("RVfrag", "[storyDownload] EventBus register");
            EventBus.getDefault().register(this);
            if (DownloadSpot.storyNonListDownloadFlag) {
                // convention is to clear flag vars immediate
                DownloadSpot.storyNonListDownloadFlag = false;
                storyNonListDownload();
            }
        }
    }

    @Override
    public void onPause() {
        super.onPause();
        if (EventBus.getDefault().isRegistered(this)) {
            Log.i("RVfrag", "[storyDownload] EventBus unRegister");
            EventBus.getDefault().unregister(this);
        }
    }


     /*
        An activity, typically StoryDownload.java or StoryDetails.java
        Did a download that impacted this list while this list was not visible.
     */
    public void storyNonListDownload() {
        if (mAdapter != null) {
            Log.i("RVfrag", "[storyDownload] storyNonListDownload notifyDataSetChanged");
            mAdapter.notifyDataSetChanged();
        }
    }

    /*
   Main thread to touch GUI.
   */
    @SuppressWarnings("unused")
    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onEventMainThread(EventStoryNonListDownload event) {
        Log.i("RVfrag", "[storyDownload] EventStoryNonListDownload");
        storyNonListDownload();
    }
}
