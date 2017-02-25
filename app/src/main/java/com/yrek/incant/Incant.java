package com.yrek.incant;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
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
import android.widget.Button;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import java.io.File;
import java.util.HashSet;
import java.util.List;

import com.wakereality.incant.AboutAppActivity;
import com.yrek.incant.glk.GlkActivity;
import com.yrek.runconfig.SettingsCurrent;

public class Incant extends Activity {
    private static final String TAG = Incant.class.getSimpleName();
    static final String SERIALIZE_KEY_STORY = "SERIALIZE_KEY_STORY";
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

    protected static HashSet<String> downloading = new HashSet<String>();
    protected static boolean useStyledIntroStrings = true;


    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);

        spref = PreferenceManager.getDefaultSharedPreferences(this);

        if (Build.VERSION.SDK_INT >= 23) {
            getPermissionToUseStorage();
        }
        getPermissionToUseSpeechListener();
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

        if (useStyledIntroStrings) {
            // Android does not seem to have a way to getText() directly from XML, so try here in code.
            ((TextView) findViewById(R.id.main_top_intro_title0)).setText(getText(R.string.main_intro_title0_styled));
            ((TextView) findViewById(R.id.main_top_intro_message0)).setText(getText(R.string.main_intro_message0_styled));
            // ((TextView) findViewById(R.id.main_top_intro_message1)).setText(getText(R.string.main_intro_message1_styled));
            ((TextView) findViewById(R.id.main_top_intro_message1)).setVisibility(View.GONE);
        }
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


    public void getPermissionToUseStorage() {
        if (ContextCompat.checkSelfPermission(this,
                Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {

            // Should we show an explanation?
            if (ActivityCompat.shouldShowRequestPermissionRationale(this,
                    Manifest.permission.WRITE_EXTERNAL_STORAGE)) {

                // Show an explanation to the user *asynchronously* -- don't
                // block
                // this thread waiting for the user's response! After the user
                // sees the explanation, try again to request the permission.

            } else {

                // No explanation needed, we can request the permission.
                ActivityCompat.requestPermissions(this,
                        new String[] { Manifest.permission.WRITE_EXTERNAL_STORAGE },
                        WRITE_STORAGE_PERMISSIONS_REQUEST);

            }
        }
    }

    public void getPermissionToUseSpeechListener() {
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
                    Toast.makeText(this, "Write Storage permission granted",
                            Toast.LENGTH_SHORT).show();
                    // permission was granted, yay! Do the
                    // contacts-related task you need to do.

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
        refreshStoryList();
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

        storyListAdapter.setNotifyOnChange(false);
        storyListAdapter.clear();
        try {
            List<Story> freshList = storyLister.getStories(storyLister.SortByDefault);
            storyListAdapter.addAll(freshList);
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
            sb.append(getTimeString(this, R.string.saved_recently, R.string.saved_at, saveFile.lastModified()));
            sb.setSpan(saveTimeStyle, start, sb.length(), 0);
        } else if (description != null) {
            sb.append(description);
            sb.setSpan(descriptionStyle, start, sb.length(), 0);
        } else if (storyFile.exists()) {
            sb.append(getTimeString(this, R.string.downloaded_recently, R.string.downloaded_at, storyFile.lastModified()));
            sb.setSpan(downloadTimeStyle, start, sb.length(), 0);
        }
        return sb;
    }

    private SpannableStringBuilder makeStoryExtra0(Story story)
    {
        SpannableStringBuilder sb = new SpannableStringBuilder();
        int storyCategory = story.getStoryCategory();
        if (storyCategory > 0)
        {
            sb.append("category " + storyCategory);
        }
        return sb;
    }

    public void hideIntroMessage(View view) {
        SharedPreferences.Editor sprefEditor = spref.edit();
        sprefEditor.putBoolean("intro_dismiss", true);
        sprefEditor.commit();
        refreshStoryList();
    }


    private class StoryListAdapter extends ArrayAdapter<Story> {
        private Thread downloadingObserver = null;

        StoryListAdapter() {
            super(Incant.this, R.layout.story);
        }

        private void setDownloadingObserver() {
            synchronized (downloading) {
                if (downloadingObserver == null && !downloading.isEmpty()) {
                    downloadingObserver = new Thread() {
                        @Override
                        public void run() {
                            Log.d(TAG, "setDownloadingObserver run() " + Thread.currentThread());
                            synchronized (downloading) {
                                try {
                                    downloading.wait();
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

            View info = convertView.findViewById(R.id.info);
            cover.setTag(null);
            progressBar.setVisibility(View.GONE);
            if (story == null) {
                info.setVisibility(View.GONE);
                play.setVisibility(View.GONE);
                cover.setVisibility(View.GONE);
                convertView.setOnLongClickListener(new View.OnLongClickListener() {
                    @Override public boolean onLongClick(View v) {
                        Log.d(TAG, "OnLongClick SPOT_B");
                        startActivity(new Intent(Incant.this, StoryDownload.class));
                        return true;
                    }
                });
                synchronized (downloading) {
                    if (downloading.contains("")) {
                        download.setVisibility(View.GONE);
                        progressBar.setVisibility(View.VISIBLE);
                        convertView.setOnClickListener(null);
                    } else {
                        download.setVisibility(View.VISIBLE);
                        progressBar.setVisibility(View.GONE);
                        download.setText(R.string.scrape);
                        convertView.setOnClickListener(new View.OnClickListener() {
                            @Override public void onClick(View v) {
                                Log.d(TAG, "OnClick SPOT_B");
                                download.setVisibility(View.GONE);
                                progressBar.setVisibility(View.VISIBLE);
                                synchronized (downloading) {
                                    downloading.add("");
                                    setDownloadingObserver();
                                }
                                new Thread() {
                                    @Override public void run() {
                                        try {
                                            storyLister.scrape();
                                        } catch (Exception e) {
                                            Log.wtf(TAG,e);
                                        }
                                        synchronized (downloading) {
                                            downloading.remove("");
                                            downloading.notifyAll();
                                        }
                                    }
                                }.start();
                            }
                        });
                    }
                }
            } else {
                info.setVisibility(View.VISIBLE);
                ((TextView) convertView.findViewById(R.id.name)).setText(makeName(story));
                ((TextView) convertView.findViewById(R.id.author)).setText(makeAuthor(story));
                ((TextView) convertView.findViewById(R.id.description)).setText(makeDescription(story));
                ((TextView) convertView.findViewById(R.id.storyextra0)).setText(makeStoryExtra0(story));
                convertView.setOnLongClickListener(new View.OnLongClickListener() {
                    @Override public boolean onLongClick(View v) {
                        Log.d(TAG, "OnLongClick SPOT_A");
                        Intent intent = new Intent(Incant.this, StoryDetails.class);
                        intent.putExtra(SERIALIZE_KEY_STORY, story);
                        startActivity(intent);
                        return true;
                    }
                });
                if (story.isDownloaded(Incant.this)) {
                    download.setVisibility(View.GONE);
                    convertView.setOnClickListener(new View.OnClickListener() {
                        @Override public void onClick(View v) {
                            Log.d(TAG, "OnClick SPOT_C");
                            Intent intent = new Intent(Incant.this, GlkActivity.class);
                            if (story.isZcode(Incant.this)) {
                                intent.putExtra(GlkActivity.GLK_MAIN, new ZCodeStory(story, story.getName(Incant.this)));
                            } else {
                                intent.putExtra(GlkActivity.GLK_MAIN, new GlulxStory(story, story.getName(Incant.this)));
                            }
                            startActivity(intent);
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
                    synchronized (downloading) {
                        if (downloading.contains(storyName)) {
                            download.setVisibility(View.GONE);
                            progressBar.setVisibility(View.VISIBLE);
                            convertView.setOnClickListener(null);
                        } else {
                            download.setVisibility(View.VISIBLE);
                            download.setText(R.string.download);
                            convertView.setOnClickListener(new View.OnClickListener() {
                                @Override public void onClick(final View v) {
                                    Log.d(TAG, "OnClick SPOT_D download");
                                    download.setVisibility(View.GONE);
                                    progressBar.setVisibility(View.VISIBLE);
                                    synchronized (downloading) {
                                        downloading.add(storyName);
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
                                            synchronized (downloading) {
                                                downloading.remove(storyName);
                                                downloading.notifyAll();
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

    public static String getTimeString(Context context, int recentStringId, int stringId, long time) {
        if (time + 86400000L > System.currentTimeMillis()) {
            return context.getString(recentStringId, time);
        } else {
            return context.getString(stringId, time);
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
        }

        sprefEditor.commit();

        return super.onOptionsItemSelected(item);
    }
}
