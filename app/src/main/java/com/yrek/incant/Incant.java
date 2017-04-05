package com.yrek.incant;

import android.Manifest;
import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.HandlerThread;
import android.preference.PreferenceManager;
import android.provider.Settings;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
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
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

import com.wakereality.incant.AboutAppActivity;
import com.wakereality.storyfinding.ReadCommaSepValuesFile;
import com.wakereality.storyfinding.StoryEntryIFDB;
import com.wakereality.thunderstrike.dataexchange.EventEngineProviderChange;
import com.yrek.incant.gamelistings.StoryHelper;
import com.yrek.incant.glk.GlkActivity;
import com.yrek.runconfig.SettingsCurrent;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;


public class Incant extends Activity {
    private static final String TAG = Incant.class.getSimpleName();
    static final String STORY = "STORY";

    private StoryLister storyLister;
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
    private LruCache<String,Bitmap> coverImageCache;
    protected SharedPreferences spref;

    protected static boolean useStyledIntroStrings = true;
    protected boolean showOnScreenListingDebug = false;
    private static boolean storagePermissionReady = true;


    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.main);

        spref = PreferenceManager.getDefaultSharedPreferences(this);

        // User can revoke permissions even after agreeing to install app.
        if (Build.VERSION.SDK_INT >= 23) {
            storagePermissionReady = false;
            confirmPermissionToUseStorage();
        }
        confirmPermissionToUseSpeechListener();
        // getPermissionToUseWindow();

        // We may need to create paths
        createDiskPathsOnce();

        storyLister = new StoryLister(this);
        storyList = (ListView) findViewById(R.id.storylist);
        storyListAdapter = new StoryListAdapter();

        storyList.setAdapter(storyListAdapter);

        titleStyle = new TextAppearanceSpan(this, R.style.story_title);
        authorStyle = new TextAppearanceSpan(this, R.style.story_author);
        headlineStyle = new TextAppearanceSpan(this, R.style.story_headline);
        descriptionStyle = new TextAppearanceSpan(this, R.style.story_description);
        saveTimeStyle = new TextAppearanceSpan(this, R.style.story_save_time);
        downloadTimeStyle = new TextAppearanceSpan(this, R.style.story_download_time);

        coverImageCache = new LruCache<String,Bitmap>(10);

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


    public void queryRemoteStoryEngineProviders() {
        // Query for Interactive Fiction engine providers.
        Intent intent = new Intent();
        // Tell Android to start Thunderword app if not already running.
        intent.addFlags(Intent.FLAG_INCLUDE_STOPPED_PACKAGES);
        intent.setAction("interactivefiction.enginemeta.runstory");
        getApplicationContext().sendBroadcast(intent);
    }

    public void createDiskPathsOnce()
    {
        File rootPath = Story.getRootDir(getApplicationContext());
        if (! rootPath.exists())
        {
            rootPath.mkdirs();
        }
        Log.i(TAG, "exists? " + rootPath.exists() + " " + rootPath + " free " + rootPath.getFreeSpace() + " writable " + rootPath.canWrite());
    }


    // Identifier for the permission request
    private static final int WRITE_STORAGE_PERMISSIONS_REQUEST = 1;
    private static final int RECORD_AUDIO_PERMISSION_REQUEST = 2;
    private static final int SYSTEM_ALERT_WINDOW_PERMISSION_REQUEST = 3;
    private static final int SYSTEM_OVERLAY_WINDOW_PERMISSION_REQUEST = 4;


    public void confirmPermissionToUseStorage() {
        if (ContextCompat.checkSelfPermission(this,
                Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {

            storagePermissionReady = false;

            // Should we show an explanation?
            if (ActivityCompat.shouldShowRequestPermissionRationale(this,
                    Manifest.permission.WRITE_EXTERNAL_STORAGE)) {

                // Show an explanation to the user *asynchronously* -- don't
                // block
                // this thread waiting for the user's response! After the user
                // sees the explanation, try again to request the permission.

                ActivityCompat.requestPermissions(this,
                        new String[] { Manifest.permission.WRITE_EXTERNAL_STORAGE },
                        WRITE_STORAGE_PERMISSIONS_REQUEST);
            } else {

                // No explanation needed, we can request the permission.
                ActivityCompat.requestPermissions(this,
                        new String[] { Manifest.permission.WRITE_EXTERNAL_STORAGE },
                        WRITE_STORAGE_PERMISSIONS_REQUEST);

            }
        } else {
            storagePermissionReady = true;
        }
    }

    public void confirmPermissionToUseSpeechListener() {
        if (ContextCompat.checkSelfPermission(this,
                Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {

            // Should we show an explanation?
            if (ActivityCompat.shouldShowRequestPermissionRationale(this, Manifest.permission.RECORD_AUDIO)) {

                // Show an explanation to the user *asynchronously* -- don't
                // block
                // this thread waiting for the user's response! After the user
                // sees the explanation, try again to request the permission.

            } else {

                // No explanation needed, we can request the permission.
                ActivityCompat.requestPermissions(this,
                        new String[] { Manifest.permission.RECORD_AUDIO },
                        RECORD_AUDIO_PERMISSION_REQUEST);

            }
        }
    }

    public void checkDrawOverlayPermission() {
        /** check if we already  have permission to draw over other apps */
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (!Settings.canDrawOverlays(this)) {
                /** if not construct intent to request permission */
                Intent intent = new Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                        Uri.parse("package:" + getPackageName()));
                /** request permission via start activity for result */
                startActivityForResult(intent, SYSTEM_OVERLAY_WINDOW_PERMISSION_REQUEST);
            }
        }
    }

    public void getPermissionToUseWindow() {
        checkDrawOverlayPermission();

        if (ContextCompat.checkSelfPermission(this,
                Manifest.permission.SYSTEM_ALERT_WINDOW) != PackageManager.PERMISSION_GRANTED) {

            Log.w("Incant", "Permission not granted SYSTEM_ALERT_WINDOW");

            // Should we show an explanation?
            if (ActivityCompat.shouldShowRequestPermissionRationale(this, Manifest.permission.SYSTEM_ALERT_WINDOW)) {

                // Show an explanation to the user *asynchronously* -- don't
                // block
                // this thread waiting for the user's response! After the user
                // sees the explanation, try again to request the permission.

            } else {

                // No explanation needed, we can request the permission.
                ActivityCompat.requestPermissions(this,
                        new String[] { Manifest.permission.SYSTEM_ALERT_WINDOW },
                        SYSTEM_ALERT_WINDOW_PERMISSION_REQUEST);

            }
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String permissions[], int[] grantResults) {
        switch (requestCode) {
            case WRITE_STORAGE_PERMISSIONS_REQUEST: {
                // If request is cancelled, the result arrays are empty.
                if (grantResults.length > 0
                        && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    // Toast.makeText(this, "Write Storage permission granted", Toast.LENGTH_SHORT).show();
                    // permission was granted, yay! Do the
                    // contacts-related task you need to do.
                    storagePermissionReady = true;
                    refreshStoryList();

                } else {
                    Toast.makeText(this, "Write Storage permission denied",
                            Toast.LENGTH_SHORT).show();
                    // permission denied, boo! Disable the
                    // functionality that depends on this permission.
                }
                return;
            }
            case RECORD_AUDIO_PERMISSION_REQUEST:
                return;
            case SYSTEM_ALERT_WINDOW_PERMISSION_REQUEST:
                return;

            // other 'case' lines to check for other
            // permissions this app might request
        }
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

        queryRemoteStoryEngineProviders();
        refreshStoryList();
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (EventBus.getDefault().isRegistered(this)) {
            EventBus.getDefault().unregister(this);
        }
    }


    private final Runnable refreshStoryList = new Runnable() {
        @Override
        public void run() {
            Log.d(TAG, "refreshStoryList Runnable run()");
            refreshStoryList();
        }
    };

    private void refreshStoryList() {
        if (spref.getBoolean("intro_dismiss", false)) {
            findViewById(R.id.main_top_intro).setVisibility(View.GONE);
        }
        if (storagePermissionReady) {
            findViewById(R.id.main_top_error).setVisibility(View.GONE);
        } else {
            findViewById(R.id.main_top_error).setVisibility(View.VISIBLE);
            // Skip doing the listing, more obvious that permissions are troubled.
            return;
        }

        storyListAdapter.setNotifyOnChange(false);
        storyListAdapter.clear();
        try {
            List<Story> freshList = storyLister.getStories(storyLister.SortByDefault);
            storyListAdapter.addAll(freshList);

            if (readCommaSepValuesFile != null) {
                ArrayList<Story> stories = new ArrayList<>();
                // No Concurrency lock. If a user rotates screen in the middle of a building of this Array... crash.
                for (int i = 0; i < readCommaSepValuesFile.foundEntries.size(); i++) {
                    StoryEntryIFDB ifdbListEntry = readCommaSepValuesFile.foundEntries.get(i);

                    URL downloadLink = null;
                    try {
                        downloadLink = new URL(ifdbListEntry.downloadLink);
                    } catch (MalformedURLException e) {
                        e.printStackTrace();
                    }
                    URL imageLink = null;
                    try {
                        imageLink = new URL(getString(R.string.ifdb_cover_image_url, ifdbListEntry.siteIdentity));
                    } catch (MalformedURLException e) {
                        e.printStackTrace();
                    }

                    // Story(String name, String author, String headline, String description, URL downloadURL, String zipEntry, URL imageURL)
                    Story newStory = new Story(ifdbListEntry.storyTitle, ifdbListEntry.storyAuthor, ifdbListEntry.storyWhimsy, ifdbListEntry.storyDescription, downloadLink, null /* not zip */, imageLink);
                    StoryHelper.addStory(this, newStory, stories, 1000);

                    // Scraper.writeStory();
                    // writeStory(out, name, author, extraURL, zipFile, context.getString(R.string.ifdb_cover_image_url, currentStoryID[0]));
                }
                storyListAdapter.addAll(stories);
            }
        } catch (Exception e) {
            Log.wtf(TAG,e);
        }
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
        SharedPreferences.Editor sprefEditor = spref.edit();
        sprefEditor.putBoolean("intro_dismiss", true);
        sprefEditor.commit();
        refreshStoryList();
    }

    public void storageTryAgainClick(View view) {
        confirmPermissionToUseStorage();
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
                                storyList.post(refreshStoryList);
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

            if (story == null) {
                info.setVisibility(View.GONE);
                play.setVisibility(View.GONE);
                cover.setVisibility(View.GONE);
                convertView.setOnLongClickListener(new View.OnLongClickListener() {
                    @Override public boolean onLongClick(View v) {
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
                                    processAssetsCommaSeparatedValuesList();
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
                                                storyLister.scrape();
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
                if (story.isDownloaded(Incant.this)) {
                    download.setVisibility(View.GONE);
                    convertView.setOnClickListener(new View.OnClickListener() {
                        @Override public void onClick(View v) {
                            Log.d(TAG, "OnClick SPOT_C");
                            EventBus.getDefault().post(new EventLocalStoryLaunch(Incant.this, story));
                        }
                    });
                    play.setVisibility(View.VISIBLE);
                    cover.setVisibility(View.GONE);
                    if (story.getCoverImageFile(Incant.this).exists()) {
                        cover.setTag(story);
                        handler.post(new Runnable() {
                            @Override public void run() {
                                Bitmap image = coverImageCache.get(storyName);
                                if (image == null) {
                                    image = story.getCoverImageBitmap(Incant.this);
                                    if (image == null) {
                                        return;
                                    }
                                    coverImageCache.put(storyName, image);
                                }
                                final Bitmap bitmap = image;
                                cover.post(new Runnable() {
                                    @Override public void run() {
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
                            download.setText(R.string.download);
                            final View finalConvertView = convertView;
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
                confirmPermissionToUseSpeechListener();
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
                processAssetsCommaSeparatedValuesList();
                break;
        }

        sprefEditor.commit();

        return super.onOptionsItemSelected(item);
    }


    public ReadCommaSepValuesFile readCommaSepValuesFile;

    public void processAssetsCommaSeparatedValuesList() {
        if (readCommaSepValuesFile == null) {
            readCommaSepValuesFile = new ReadCommaSepValuesFile();
        }
        // readCommaSepValuesFile.readComplexSetOfFilesCSV(getApplicationContext());
        readCommaSepValuesFile.readSimpleFileOneObjectCSV(getApplicationContext());
        System.gc();
        refreshStoryList();
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
        Log.i(TAG, "EventEngineProviderChange, updating refreshStoryList()");
        refreshStoryList();
    }

}
