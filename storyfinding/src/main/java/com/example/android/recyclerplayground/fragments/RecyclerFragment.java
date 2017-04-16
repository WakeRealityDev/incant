package com.example.android.recyclerplayground.fragments;

import android.content.Intent;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v4.app.Fragment;
import android.support.v7.widget.RecyclerView;
import android.text.Spannable;
import android.text.Spanned;
import android.text.method.LinkMovementMethod;
import android.text.style.ClickableSpan;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.AdapterView;
import android.widget.CheckBox;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import com.example.android.recyclerplayground.adapters.StoryBrowseAdapter;
import com.wakereality.storyfinding.AddStoriesToStoryList;
import com.wakereality.storyfinding.EventExternalEngineStoryLaunch;
import com.wakereality.storyfinding.EventStoryListDownloadResult;
import com.wakereality.storyfinding.EventStoryNonListDownload;
import com.wakereality.storyfinding.R;
import com.wakereality.thunderstrike.EchoSpot;
import com.wakereality.thunderstrike.dataexchange.EventEngineProviderChange;
import com.wakereality.thunderstrike.userinterfacehelper.PickEngineProviderHelper;
import com.yrek.incant.DownloadSpot;
import com.wakereality.storyfinding.EventLocalStoryLaunch;
import com.yrek.incant.ParamConst;
import com.yrek.incant.Story;
import com.yrek.incant.StoryDetails;
import com.yrek.incant.StoryListSpot;
import com.yrek.runconfig.SettingsCurrent;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;

public abstract class RecyclerFragment extends Fragment implements AdapterView.OnItemClickListener, AdapterView.OnItemLongClickListener {

    public RecyclerFragment() {
        // Fragments should have empty https://github.com/devunwired/recyclerview-playground/issues/28
    }

    private RecyclerView mList;
    private StoryBrowseAdapter mAdapter;

    /** Required Overrides for Sample Fragments */

    protected abstract RecyclerView.LayoutManager getLayoutManager();
    protected abstract RecyclerView.ItemDecoration getItemDecoration();
    protected abstract int getDefaultItemCount();
    protected abstract StoryBrowseAdapter getAdapter();
    protected Animation myFadeInOutAnimation;
    protected Animation myTouchWobbleAnimation;
    protected CheckBox launchDefaultTopPanelCheckbox;
    protected TextView listHeaderExtraNoThunderwordDetected;

    protected PickEngineProviderHelper pickEngineProviderHelper = new PickEngineProviderHelper();

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

        myFadeInOutAnimation = AnimationUtils.loadAnimation(getContext(), R.anim.fade_in_fade_out_repeat_2sec);
        myTouchWobbleAnimation = AnimationUtils.loadAnimation(getContext(), R.anim.shake);

        launchDefaultTopPanelCheckbox = (CheckBox) rootView.findViewById(R.id.storylist_header_extra_checkenginelaunch);
        listHeaderExtraNoThunderwordDetected = (TextView) rootView.findViewById(R.id.storylist_header_extra_info0);

        pickEngineProviderHelper.redrawEngineProvider((TextView) rootView.findViewById(R.id.engine_provider_status));

        headerSectionSetup(rootView);

        return rootView;
    }


    protected boolean doHeaderOnce = false;


    protected void headerSectionTipsSetup(final View rootView) {
        final TextView storylist_header_extra_info2 = (TextView) rootView.findViewById(R.id.storylist_header_extra_info2);
        if (! StoryListSpot.showInterfaceTipsA) {
            storylist_header_extra_info2.setVisibility(View.GONE);
        } else {
            storylist_header_extra_info2.setVisibility(View.VISIBLE);
            // Appending keeps word wrapping, two textViews would not

            String outMessage = " :hide.";
            Spannable span = Spannable.Factory.getInstance().newSpannable(outMessage);
            span.setSpan(new ClickableSpan() {
                @Override
                public void onClick(View v) {
                    StoryListSpot.showInterfaceTipsA = false;
                    PreferenceManager.getDefaultSharedPreferences(getContext()).edit().putBoolean("storylist_intro_tips", StoryListSpot.showInterfaceTipsA).commit();
                    storylist_header_extra_info2.setVisibility(View.GONE);
                    headerSectionSetup(rootView);
                }
            }, 0, outMessage.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);

            storylist_header_extra_info2.setText(getText(R.string.storylist_header_interface_tips0));
            storylist_header_extra_info2.append(span);
            storylist_header_extra_info2.setMovementMethod(LinkMovementMethod.getInstance());
        }
    }

    protected void headerSectionSetup(final View rootView) {
        TextView expandControl = (TextView) rootView.findViewById(R.id.storyList_header_expand_control);
        final View expandableHolder = rootView.findViewById(R.id.storylist_header_expandholder);

        // Menu item reflect open or closed
        if (actionExpandOptions != null) {
            actionExpandOptions.setChecked(StoryListSpot.showHeadingExpanded);
        }

        if (! doHeaderOnce) {
            doHeaderOnce = true;
            expandControl.startAnimation(myFadeInOutAnimation);
            expandControl.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    StoryListSpot.showHeadingExpanded = !StoryListSpot.showHeadingExpanded;
                    headerSectionSetup(rootView);
                }
            });
            launchDefaultTopPanelCheckbox.setChecked(StoryListSpot.optionLaunchExternal);
            launchDefaultTopPanelCheckbox.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    CheckBox viewAsCheckbox = (CheckBox) v;
                    viewAsCheckbox.setChecked(viewAsCheckbox.isChecked());
                    if (actionLaunchExternal != null) {
                        actionLaunchExternal.setChecked(viewAsCheckbox.isChecked());
                    }
                    StoryListSpot.optionLaunchExternal = viewAsCheckbox.isChecked();
                    PreferenceManager.getDefaultSharedPreferences(getContext()).edit().putBoolean("storylist_launchexternal", StoryListSpot.optionLaunchExternal).commit();
                    // Redraw to show launch icon change.
                    mAdapter.notifyDataSetChanged();
                }
            });
            // This is little bit confusing: remember there are two controls, one can expand nad open without changing default, The other sets default for next app startup or activity open.
            final CheckBox hideExpandDefault = (CheckBox) rootView.findViewById(R.id.storylist_header_extra_checkhide);
            // checkbox is a negative meaning, "hide"
            hideExpandDefault.setChecked(StoryListSpot.showHeadingExpandedHideByDefault);
            hideExpandDefault.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    StoryListSpot.showHeadingExpandedHideByDefault = ! StoryListSpot.showHeadingExpandedHideByDefault;
                    // checkbox is a negative meaning, "hide"
                    hideExpandDefault.setChecked(StoryListSpot.showHeadingExpandedHideByDefault);
                    PreferenceManager.getDefaultSharedPreferences(getContext()).edit().putBoolean("storylist_expand_default", StoryListSpot.showHeadingExpandedHideByDefault).commit();
                    // This will not expand or contract, only adjusting the default for future.
                    expandableHolder.startAnimation(myTouchWobbleAnimation);
                }
            });

            headerSectionTipsSetup(rootView);
        }

        // If no engine provider detected, suggest install of app and remove controls related to Thunderword
        int providerViewHideOrShow = View.VISIBLE;
        if (EchoSpot.currentEngineProvider == null) {
            listHeaderExtraNoThunderwordDetected.setVisibility(View.VISIBLE);
            providerViewHideOrShow = View.GONE;
        } else {
            listHeaderExtraNoThunderwordDetected.setVisibility(View.GONE);
        }
        launchDefaultTopPanelCheckbox.setVisibility(providerViewHideOrShow);
        rootView.findViewById(R.id.external_provider_label0).setVisibility(providerViewHideOrShow);
        rootView.findViewById(R.id.external_provider_activity).setVisibility(providerViewHideOrShow);
        rootView.findViewById(R.id.external_provider_activity_label).setVisibility(providerViewHideOrShow);
        rootView.findViewById(R.id.external_provider_noprompt).setVisibility(providerViewHideOrShow);

        if (StoryListSpot.showHeadingExpanded) {
            expandControl.setText(getText(R.string.storyList_header_contract));
            expandableHolder.setVisibility(View.VISIBLE);
        } else {
            expandControl.setText(getText(R.string.storyList_header_expand));
            expandableHolder.setVisibility(View.GONE);
        }

        pickEngineProviderHelper.spinnerForThunderwordActivity((Spinner) rootView.findViewById(R.id.external_provider_activity), (CheckBox) rootView.findViewById(R.id.external_provider_noprompt));
    }

    protected MenuItem actionLaunchExternal;
    protected MenuItem actionExpandOptions;

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        inflater.inflate(R.menu.grid_options, menu);
        actionLaunchExternal = menu.findItem(R.id.action_launch_external);
        actionLaunchExternal.setChecked(StoryListSpot.optionLaunchExternal);
        actionExpandOptions = menu.findItem(R.id.action_expand);
        actionExpandOptions.setChecked(StoryListSpot.showHeadingExpanded);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int i = item.getItemId();
        if (i == R.id.action_add) {
            mList.startAnimation(myTouchWobbleAnimation);
            AddStoriesToStoryList.processAssetsCommaSeparatedValuesList(getContext());
            mAdapter.setAdapterContent(getContext());
            mAdapter.notifyDataSetChanged();
            return true;
        } else if (i == R.id.action_remove) {
            mList.startAnimation(myTouchWobbleAnimation);
            SettingsCurrent.flipStoryListFilterOnlyNotDownloaded();
            if (SettingsCurrent.getStoryListFilterOnlyNotDownloaded()) {
                item.setTitle("Show Downloaded");
            } else {
                item.setTitle("Hide Downloaded");
            }
            mAdapter.setAdapterContent(getContext());
            mAdapter.notifyDataSetChanged();
            return true;
        } else if (i == R.id.action_launch_external) {
            item.setChecked(! item.isChecked());
            StoryListSpot.optionLaunchExternal = item.isChecked();
            launchDefaultTopPanelCheckbox.setChecked(item.isChecked());
            PreferenceManager.getDefaultSharedPreferences(getContext()).edit().putBoolean("storylist_launchexternal", StoryListSpot.optionLaunchExternal).commit();
            mAdapter.notifyDataSetChanged();
            return true;
        } else if (i == R.id.action_scroll_zero) {
            mList.scrollToPosition(0);
            return true;
        } else if (i == R.id.action_smooth_zero) {
            mList.smoothScrollToPosition(0);
            return true;
        } else if (i == R.id.action_scroll_max) {
            mList.scrollToPosition(mAdapter.getItemCount() - 1);
            return true;
        } else if (i == R.id.action_smooth_max) {
            mList.smoothScrollToPosition(mAdapter.getItemCount() - 1);
            return true;
        } else if (i == R.id.action_expand) {
            StoryListSpot.showHeadingExpanded = ! StoryListSpot.showHeadingExpanded;
            headerSectionSetup(getView());
            return true;
        } else if (i == R.id.action_restore_tips) {
            StoryListSpot.showInterfaceTipsA = true;
            PreferenceManager.getDefaultSharedPreferences(getContext()).edit().putBoolean("storylist_intro_tips", StoryListSpot.showInterfaceTipsA).commit();
            PreferenceManager.getDefaultSharedPreferences(getContext()).edit().putBoolean("intro_dismiss", false).commit();
            headerSectionTipsSetup(getView());
            return true;
        } else {
            return super.onOptionsItemSelected(item);
        }
    }

    @Override
    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
        Story story = mAdapter.getStoryForPosition(position);
        if (1==2) {
            Toast.makeText(getActivity(),
                    "Clicked: " + position + ", index " + mList.indexOfChild(view) + " " + story.getName(getContext()),
                    Toast.LENGTH_SHORT).show();
        }

        if (story.isDownloaded(getContext())) {
            // click means launch
            if (StoryListSpot.optionLaunchExternal) {
                // There is a visual delay, nothing seems to happen, on Launching to Thunderword - so animate.
                view.startAnimation(myTouchWobbleAnimation);
                EventBus.getDefault().post(new EventExternalEngineStoryLaunch(getActivity(), story, StoryListSpot.optionLaunchExternalActivityCode,  StoryListSpot.optionaLaunchInterruptEngine));
            } else {
                EventBus.getDefault().post(new EventLocalStoryLaunch(getActivity(), story));
            }
        } else {
            if (story.isDownloadingNow()) {
                // Cancel download?
            } else {
                story.setDownloadingNow(true);
                mAdapter.notifyDataSetChanged();
                // Will clear downloadingNow when done
                story.startDownloadThread(getContext());
            }
        }
    }

    @Override
    public boolean onItemLongClick(AdapterView<?> parent, View view, int position, long id) {
        Story story = mAdapter.getStoryForPosition(position);
        if (1==2) {
            Toast.makeText(getActivity(),
                    "LONG Clicked: " + position + ", index " + mList.indexOfChild(view) + " " + story.getName(getContext()),
                    Toast.LENGTH_SHORT).show();
        }

        Intent intent = new Intent(getContext(), StoryDetails.class);
        intent.putExtra(ParamConst.SERIALIZE_KEY_STORY, story);
        getActivity().startActivity(intent);

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

    /*
    Main thread to touch GUI.
    */
    @SuppressWarnings("unused")
    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onEventMainThread(EventStoryListDownloadResult event) {
        Log.i("RVfrag", "[storyDownload] EventStoryListDownloadResult, error: " + event.downloadResultError);
        if (event.downloadResultError) {
            Toast.makeText(getContext(), "Download error: " + event.downloadStory.getDownloadErrorDetail(), Toast.LENGTH_SHORT).show();
        }
        storyNonListDownload();
    }

    /*
    Main thread to touch GUI.
    */
    @SuppressWarnings("unused")
    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onEventMainThread(EventEngineProviderChange event) {
        pickEngineProviderHelper.redrawEngineProvider((TextView) getView().findViewById(R.id.engine_provider_status));
        headerSectionSetup(getView());
    }
}
