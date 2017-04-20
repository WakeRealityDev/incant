package com.yrek.incant;

import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import android.os.HandlerThread;
import android.preference.PreferenceManager;
import android.text.SpannableStringBuilder;
import android.text.style.TextAppearanceSpan;
import android.util.Log;
import android.util.LruCache;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import java.io.File;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

import com.example.android.recyclerplayground.BrowseStoriesActivity;
import com.wakereality.incant.AboutAppActivity;
import com.wakereality.storyfinding.AddStoriesToStoryList;
import com.wakereality.storyfinding.CommonAppSetup;
import com.wakereality.storyfinding.EventLocalStoryLaunch;
import com.wakereality.thunderstrike.dataexchange.EventEngineProviderChange;
import com.yrek.runconfig.SettingsCurrent;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;


public class Incant extends Activity {
    private static final String TAG = Incant.class.getSimpleName();
    static final String STORY = "STORY";

    private ListView storyList;
    private StoryListAdapter storyListAdapter;

    private TextAppearanceSpan titleStyle;
    private TextAppearanceSpan authorStyle;
    private TextAppearanceSpan headlineStyle;
    private TextAppearanceSpan descriptionStyle;
    private TextAppearanceSpan saveTimeStyle;
    private TextAppearanceSpan downloadTimeStyle;

    private Handler handler;
    private HandlerThread handlerThread;
    protected SharedPreferences spref;

    protected static boolean useStyledIntroStrings = true;
    protected boolean showOnScreenListingDebug = false;



    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.main);

        spref = PreferenceManager.getDefaultSharedPreferences(this);

        /*
        // User can revoke permissions even after agreeing to install app.
        if (Build.VERSION.SDK_INT >= 23) {
            storagePermissionReady = false;
            confirmPermissionToUseStorage();
        }
        confirmPermissionToUseSpeechListener();
        // getPermissionToUseWindow();

        // We may need to create paths
        createDiskPathsOnce();
        */

        if (StoryListSpot.storyLister == null) {
            StoryListSpot.storyLister = new StoryLister(getApplicationContext());
        }
        storyList = (ListView) findViewById(R.id.storylist);
        storyListAdapter = new StoryListAdapter();

        storyList.setAdapter(storyListAdapter);

        titleStyle = new TextAppearanceSpan(this, R.style.story_title);
        authorStyle = new TextAppearanceSpan(this, R.style.story_author);
        headlineStyle = new TextAppearanceSpan(this, R.style.story_headline);
        descriptionStyle = new TextAppearanceSpan(this, R.style.story_description);
        saveTimeStyle = new TextAppearanceSpan(this, R.style.story_save_time);
        downloadTimeStyle = new TextAppearanceSpan(this, R.style.story_download_time);

        if (StoryListSpot.coverImageCache == null) {
            StoryListSpot.coverImageCache = new LruCache<String, Bitmap>(10);
        }

        findViewById(R.id.main_top_intro_button_label0).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                hideIntroMessageClick(v);
            }
        });

        findViewById(R.id.main_top_error_storage_try).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                storageTryAgainClick(v);
            }
        });

        if (useStyledIntroStrings) {
            // Android does not seem to have a way to getText() directly from XML, so try here in code.
            TextView main_top_intro_title0 = (TextView) findViewById(R.id.main_top_intro_title0);
            main_top_intro_title0.setText(getText(R.string.main_intro_title0_styled));
            // Android does not seem to have a way to word-wrap two textviews next to each other, so just append to the same textview and it will word-wrap to fit screen width.
            main_top_intro_title0.append(". ");
            main_top_intro_title0.append(getText(R.string.main_intro_message0_styled));

            findViewById(R.id.main_top_intro_message0).setVisibility(View.GONE);
            findViewById(R.id.main_top_intro_message1).setVisibility(View.GONE);
        }

        showOnScreenListingDebug = spref.getBoolean("onscreen_debug", false);
    }


    @Override
    protected void onStart() {
        super.onStart();
        handlerThread = new HandlerThread("Incant.HandlerThread");
        handlerThread.start();
        handler = new Handler(handlerThread.getLooper());
    }

    @Override
    protected void onStop() {
        super.onStop();
        handlerThread.quit();
    }

    @Override
    protected void onResume() {
        super.onResume();
        // Try to be ready for events incoming before triggering any remote Thunderword activity.
        if (! EventBus.getDefault().isRegistered(this)) {
            EventBus.getDefault().register(this);
        }

        CommonAppSetup.queryRemoteStoryEngineProviders(getApplicationContext());
        refreshStoryList();
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (EventBus.getDefault().isRegistered(this)) {
            EventBus.getDefault().unregister(this);
        }
    }


    private final Runnable refreshStoryListRunnable = new Runnable() {
        @Override
        public void run() {
            Log.d(TAG, "refreshStoryListRunnable Runnable run()");
            refreshStoryList();
        }
    };

    private void refreshStoryList() {
        if (spref.getBoolean("intro_dismiss", false)) {
            findViewById(R.id.main_top_intro).setVisibility(View.GONE);
        }
        if (StoryListSpot.storagePermissionReady) {
            findViewById(R.id.main_top_error).setVisibility(View.GONE);
        } else {
            findViewById(R.id.main_top_error).setVisibility(View.VISIBLE);
            // Skip doing the listing, more obvious that permissions are troubled.
            return;
        }

        storyListAdapter.setNotifyOnChange(false);
        storyListAdapter.clear();
        try {
            if (StoryListSpot.storyLister != null) {
                ArrayList<Story> freshStoriesList = new ArrayList<>();
                StoryListSpot.storyLister.generateStoriesListAllSortedArrayListA(freshStoriesList);
                storyListAdapter.addAll(freshStoriesList);
            } else {
                Log.e(TAG, "storyList is null, unable to populate");
            }
        } catch (Exception e) {
            Log.e(TAG, "Exception ", e);
        }
        // What's this null added at end?
        storyListAdapter.add(null);
        storyListAdapter.notifyDataSetChanged();
    }

    private SpannableStringBuilder makeName(Story story) {
        SpannableStringBuilder sb = new SpannableStringBuilder();
        int start = sb.length();
        sb.append(story.getName(this));
        sb.setSpan(titleStyle, start, sb.length(), 0);
        String headline = story.getHeadline(this);
        if (headline != null) {
            sb.append(' ');
            start = sb.length();
            sb.append(headline);
            sb.setSpan(headlineStyle, start, sb.length(), 0);
        }
        return sb;
    }

    private SpannableStringBuilder makeAuthor(Story story) {
        SpannableStringBuilder sb = new SpannableStringBuilder();
        String author = story.getAuthor(this);
        int start = sb.length();
        if (author == null) {
            sb.append("author unknown");
        } else {
            sb.append("by ");
            sb.append(author);
        }
        sb.setSpan(authorStyle, start, sb.length(), 0);
        return sb;
    }

    private SpannableStringBuilder makeDescription(Story story) {
        SpannableStringBuilder sb = new SpannableStringBuilder();
        String description = story.getDescription(this);
        File saveFile = story.getSaveFile(this);
        File storyFile = story.getStoryFile(this);
        int start = sb.length();
        if (saveFile.exists()) {
            sb.append(Story.getTimeString(this, R.string.saved_recently, R.string.saved_at, saveFile.lastModified()));
            sb.setSpan(saveTimeStyle, start, sb.length(), 0);
        } else if (description != null) {
            sb.append(description);
            sb.setSpan(descriptionStyle, start, sb.length(), 0);
        } else if (storyFile.exists()) {
            sb.append(Story.getTimeString(this, R.string.downloaded_recently, R.string.downloaded_at, storyFile.lastModified()));
            sb.setSpan(downloadTimeStyle, start, sb.length(), 0);
        }
        return sb;
    }

    private SpannableStringBuilder makeStoryExtra0(Story story)
    {
        SpannableStringBuilder sb = new SpannableStringBuilder();
        // Preferences has setting to enable extra debugging content on screen.
        if (! showOnScreenListingDebug) {
            return sb;
        }

        int storyCategory = story.getStoryCategory();
        if (storyCategory > 0)
        {
            sb.append("Category " + storyCategory );
            URL downloadURL = story.getDownloadURL(this);
            if (downloadURL != null) {
                sb.append("\nURL " + downloadURL);
            }
        }
        return sb;
    }

    public void hideIntroMessageClick(View view) {
        spref.edit().putBoolean("intro_dismiss", true).commit();
        refreshStoryList();
    }

    public void storageTryAgainClick(View view) {
        //confirmPermissionToUseStorage();
    }


    private class StoryListAdapter extends ArrayAdapter<Story> {
        private Thread downloadingObserver = null;

        StoryListAdapter() {
            super(Incant.this, R.layout.story);
        }

        private void setDownloadingObserver() {
            synchronized (DownloadSpot.downloading) {
                if (downloadingObserver == null && ! DownloadSpot.downloading.isEmpty()) {
                    downloadingObserver = new Thread() {
                        @Override
                        public void run() {
                            Log.d(TAG, "setDownloadingObserver run() " + Thread.currentThread());
                            synchronized (DownloadSpot.downloading) {
                                try {
                                    DownloadSpot.downloading.wait();
                                } catch (Exception e) {
                                    Log.wtf(TAG,e);
                                }
                                downloadingObserver = null;
                                storyList.post(refreshStoryListRunnable);
                            }
                        }
                    };
                    downloadingObserver.setName("downloadingObserver");
                    downloadingObserver.start();
                }
            }
        }


        public class ListingViewHolder {
            public TextView name;
            public TextView author;
            public TextView description;
            public TextView storyExtra0;

            public void populateFromContainer(ViewGroup container) {
                name = (TextView) container.findViewById(R.id.name);
                author = (TextView) container.findViewById(R.id.author);
                description = (TextView) container.findViewById(R.id.description);
                storyExtra0 = (TextView) container.findViewById(R.id.storyextra0);
            }

            public void clearAllViews() {
                name.setText("");
                author.setText("");
                description.setText("");
                storyExtra0.setText("");
            }
        }

        ListingViewHolder scrapeMoreViewHolder = new ListingViewHolder();

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            final Story story = getItem(position);
            final String storyName = story == null ? null : story.getName(Incant.this);
            if (convertView == null) {
                convertView = Incant.this.getLayoutInflater().inflate(R.layout.story, parent, false);
            }

            final TextView download = (TextView) convertView.findViewById(R.id.download);
            final TextView play = (TextView) convertView.findViewById(R.id.play);
            final ProgressBar progressBar = (ProgressBar) convertView.findViewById(R.id.progressbar);
            final ImageView cover = (ImageView) convertView.findViewById(R.id.cover);

            final View info = convertView.findViewById(R.id.info);
            cover.setTag(null);
            progressBar.setVisibility(View.GONE);
            final View finalConvertView1 = convertView;

            // The one final "Get More" entry?
            if (story == null) {
                info.setVisibility(View.GONE);
                play.setVisibility(View.GONE);
                cover.setVisibility(View.GONE);
                convertView.setOnLongClickListener(new View.OnLongClickListener() {
                    @Override public boolean onLongClick(View v) {
                        // How to reach this code path: long-press on the the space RIGHT OF the "Get More"
                        Log.d(TAG, "[downloadStory] OnLongClick SPOT_B");
                        startActivity(new Intent(Incant.this, StoryDownload.class));
                        return true;
                    }
                });
                synchronized (DownloadSpot.downloading) {
                    if (DownloadSpot.downloading.contains("")) {
                        download.setVisibility(View.GONE);
                        progressBar.setVisibility(View.VISIBLE);
                        convertView.setOnClickListener(null);
                    } else {
                        download.setVisibility(View.VISIBLE);
                        progressBar.setVisibility(View.GONE);
                        download.setText(R.string.scrape);
                        convertView.setOnClickListener(new View.OnClickListener() {
                            @Override public void onClick(View v) {
                                if (SettingsCurrent.getGetMoreBypassLiveScrape()) {
                                    AddStoriesToStoryList.processAssetsCommaSeparatedValuesList(getApplicationContext());
                                }
                                else {
                                    Log.d(TAG, "[downloadStory] OnClick SPOT_B (scrape fetch)");
                                    download.setVisibility(View.GONE);
                                    progressBar.setVisibility(View.VISIBLE);
                                    progressBar.setBackgroundColor(Color.parseColor("#E1BEE7"));
                                    info.setVisibility(View.VISIBLE);
                                    // Idea: could use EventBus to update these with each web fetch
                                    scrapeMoreViewHolder.populateFromContainer((ViewGroup) finalConvertView1);
                                    scrapeMoreViewHolder.clearAllViews();
                                    scrapeMoreViewHolder.name.setText("Searching for more stories...");

                                    synchronized (DownloadSpot.downloading) {
                                        DownloadSpot.downloading.add("");
                                        setDownloadingObserver();
                                    }
                                    Thread downloadWorker = new Thread() {
                                        @Override
                                        public void run() {
                                            try {
                                                StoryListSpot.storyLister.scrape();
                                            } catch (Exception e) {
                                                Log.wtf(TAG, e);
                                            }
                                            synchronized (DownloadSpot.downloading) {
                                                DownloadSpot.downloading.remove("");
                                                DownloadSpot.downloading.notifyAll();
                                            }
                                        }
                                    };
                                    downloadWorker.setName("DownloadWorker");
                                    downloadWorker.start();
                                }

                            }
                        });
                    }
                }
            } else {
                info.setVisibility(View.VISIBLE);
                ListingViewHolder listingViewHolder = new ListingViewHolder();
                listingViewHolder.populateFromContainer((ViewGroup) finalConvertView1);
                listingViewHolder.name.setText(makeName(story));
                listingViewHolder.author.setText(makeAuthor(story));
                listingViewHolder.description.setText(makeDescription(story));
                listingViewHolder.storyExtra0.setText(makeStoryExtra0(story));
                convertView.setOnLongClickListener(new View.OnLongClickListener() {
                    @Override public boolean onLongClick(View v) {
                        Log.d(TAG, "OnLongClick SPOT_A");
                        Intent intent = new Intent(Incant.this, StoryDetails.class);
                        intent.putExtra(ParamConst.SERIALIZE_KEY_STORY, story);
                        startActivity(intent);
                        return true;
                    }
                });

                if (story.isDownloadedExtensiveCheck(Incant.this)) {
                    boolean showThisEntry = true;
                    if (1==2 /* SettingsCurrent.getStoryListFilterOnlyNotDownloaded() */) {
                        convertView.setVisibility(View.GONE);
                        showThisEntry = false;
                    }

                    if (showThisEntry) {
                        download.setVisibility(View.GONE);
                        convertView.setOnClickListener(new View.OnClickListener() {
                            @Override
                            public void onClick(View v) {
                                Log.d(TAG, "OnClick SPOT_C");
                                EventBus.getDefault().post(new EventLocalStoryLaunch(Incant.this, story));
                            }
                        });
                        play.setVisibility(View.VISIBLE);
                        cover.setVisibility(View.GONE);
                        if (story.getCoverImageFile(Incant.this).exists()) {
                            cover.setTag(story);
                            handler.post(new Runnable() {
                                @Override
                                public void run() {
                                    Bitmap image = StoryListSpot.coverImageCache.get(storyName);
                                    if (image == null) {
                                        image = story.getCoverImageBitmap(Incant.this);
                                        if (image == null) {
                                            return;
                                        }
                                        StoryListSpot.coverImageCache.put(storyName, image);
                                    }
                                    final Bitmap bitmap = image;
                                    cover.post(new Runnable() {
                                        @Override
                                        public void run() {
                                            if (story == cover.getTag()) {
                                                play.setVisibility(View.GONE);
                                                cover.setVisibility(View.VISIBLE);
                                                cover.setImageBitmap(bitmap);
                                                cover.setTag(null);
                                            }
                                        }
                                    });
                                }
                            });
                        }
                    }
                } else {
                    play.setVisibility(View.GONE);
                    cover.setVisibility(View.GONE);
                    synchronized (DownloadSpot.downloading) {
                        if (DownloadSpot.downloading.contains(storyName)) {
                            download.setVisibility(View.GONE);
                            progressBar.setVisibility(View.VISIBLE);
                            convertView.setOnClickListener(null);
                        } else {
                            download.setVisibility(View.VISIBLE);
                            download.setText(R.string.download_story);
                            convertView.setOnClickListener(new View.OnClickListener() {
                                @Override public void onClick(final View v) {
                                    Log.d(TAG, "[downloadStory] OnClick SPOT_D download");
                                    download.setVisibility(View.GONE);
                                    progressBar.setVisibility(View.VISIBLE);
                                    progressBar.setBackgroundColor(Color.parseColor("#E1BEE7"));
                                    synchronized (DownloadSpot.downloading) {
                                        DownloadSpot.downloading.add(storyName);
                                        setDownloadingObserver();
                                    }
                                    Thread downloadThreadA = new Thread() {
                                        @Override public void run() {
                                            Log.d(TAG, "run() " + Thread.currentThread());
                                            String error = null;
                                            try {
                                                if (!story.download(Incant.this)) {
                                                    Log.w(TAG, "download_invalid " + story.getName(Incant.this));
                                                    error = Incant.this.getString(R.string.download_invalid, story.getName(Incant.this));
                                                }
                                            } catch (Exception e) {
                                                Log.wtf(TAG,e);
                                                error = Incant.this.getString(R.string.download_failed, story.getName(Incant.this));
                                            }
                                            synchronized (DownloadSpot.downloading) {
                                                DownloadSpot.downloading.remove(storyName);
                                                DownloadSpot.downloading.notifyAll();
                                            }
                                            if (error != null) {
                                                Log.w(TAG, "download error " + error);
                                                final String msg = error;
                                                v.post(new Runnable() {
                                                    @Override public void run() {
                                                        Toast.makeText(Incant.this, msg, Toast.LENGTH_SHORT).show();
                                                    }
                                                });
                                            }
                                        }
                                    };
                                    downloadThreadA.setName("downloadA");
                                    downloadThreadA.start();
                                }
                            });
                        }
                    }
                }
            }

            setDownloadingObserver();
            return convertView;
        }
    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        menu.getItem(0).setChecked(SettingsCurrent.getInterpreterProfileEnabled());
        menu.getItem(1).setChecked(SettingsCurrent.getSpeechRecognizerEnabled());
        menu.getItem(2).setChecked(SettingsCurrent.getSpeechRecognizerMute());
        menu.getItem(3).setChecked(SettingsCurrent.getSpeechEnabled());
        menu.getItem(4).setChecked(showOnScreenListingDebug);
        menu.getItem(5).setChecked(SettingsCurrent.getEnableAutoEnterOnGlkCharInput());
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        SharedPreferences.Editor sprefEditor = spref.edit();

        switch (id)
        {
            case R.id.action_about_app:
                startActivity(new Intent(Incant.this, AboutAppActivity.class));
                return true;
            case R.id.action_profile:
                SettingsCurrent.flipInterpreterProfileEnabled();
                item.setChecked(SettingsCurrent.getInterpreterProfileEnabled());
                sprefEditor.putBoolean("profile_enabled", SettingsCurrent.getInterpreterProfileEnabled());
                break;
            case R.id.action_speech_input:
                // confirmPermissionToUseSpeechListener();
                SettingsCurrent.flipSpeechRecognizerEnabled();
                item.setChecked(SettingsCurrent.getSpeechRecognizerEnabled());
                sprefEditor.putBoolean("recognition_enabled", SettingsCurrent.getSpeechRecognizerEnabled());
                break;
            case R.id.action_speech_input_beepmute:
                SettingsCurrent.flipSpeechRecognizerMute();
                item.setChecked(SettingsCurrent.getSpeechRecognizerMute());
                sprefEditor.putBoolean("recognition_mute_enabled", SettingsCurrent.getSpeechRecognizerMute());
                break;
            case R.id.action_speech_output:
                SettingsCurrent.flipSpeechEnabled();
                item.setChecked(SettingsCurrent.getSpeechEnabled());
                sprefEditor.putBoolean("speech_enabled", SettingsCurrent.getSpeechEnabled());
                break;
            case R.id.action_onscreen_debug:
                showOnScreenListingDebug = ! showOnScreenListingDebug;
                sprefEditor.putBoolean("onscreen_debug", showOnScreenListingDebug);
                recreate();
                break;
            case R.id.action_glk_auto_enter_char_input:
                SettingsCurrent.flipEnableAutoEnterOnGlkCharInput();
                item.setChecked(SettingsCurrent.getEnableAutoEnterOnGlkCharInput());
                sprefEditor.putBoolean("glk_auto_enter_char", SettingsCurrent.getEnableAutoEnterOnGlkCharInput());
                break;
            case R.id.action_story_database_test:
                AddStoriesToStoryList.processAssetsCommaSeparatedValuesList(getApplicationContext());
                refreshStoryList();
                break;
            case R.id.action_storylist_not_downloaded:
                SettingsCurrent.flipStoryListFilterOnlyNotDownloaded();
                refreshStoryList();
                break;
            case R.id.action_list_test:
                startActivity(new Intent(Incant.this, BrowseStoriesActivity.class));
                return true;
        }

        sprefEditor.commit();

        return super.onOptionsItemSelected(item);
    }



    /*
    ================================================================================================
    SECTION: Incoming background events from BroadcastReceivers and Services
    These Events serve to allow thread choices of execution and to determine if the app is on-screen
        If the app is not on screen, this Activity will be closed and the Events will silently
        be dropped as there are no registered receivers.
    */

    @SuppressWarnings("unused")
    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onEventMainThread(EventEngineProviderChange event) {
        Log.i(TAG, "EventEngineProviderChange, updating refreshStoryListRunnable()");
        refreshStoryList();
    }

}
