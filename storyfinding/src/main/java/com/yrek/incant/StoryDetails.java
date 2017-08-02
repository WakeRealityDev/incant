package com.yrek.incant;

import android.app.Activity;
import android.content.Intent;
import android.content.res.TypedArray;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.text.SpannableStringBuilder;
import android.text.style.TextAppearanceSpan;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.ImageView;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import com.example.android.recyclerplayground.adapters.StoryBrowseAdapter;
import com.wakereality.storyfinding.EventExternalEngineStoryLaunch;
import com.wakereality.storyfinding.EventLocalStoryLaunch;
import com.wakereality.storyfinding.EventStoryNonListDownload;
import com.wakereality.storyfinding.R;
import com.wakereality.thunderstrike.EchoSpot;
import com.wakereality.thunderstrike.dataexchange.EngineConst;
import com.wakereality.thunderstrike.dataexchange.EventEngineProviderChange;
import com.wakereality.thunderstrike.userinterfacehelper.PickEngineProviderHelper;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;

import java.io.File;
import java.util.Locale;

/*
This works except in this sequence
1. Start with RecyclerView
2. Download
3. Open StoryDetails, Delete Incant structure
4. Open StoryDetails, Delete Keep file
5. ReyclerView now correctly shows [Download] link
6. FAILURE: Open StoryDetails, return to RecyclerView - shows play
 */
public class StoryDetails extends Activity {
    private static final String TAG = StoryDetails.class.getSimpleName();

    private Story story = null;
    private TextAppearanceSpan titleStyle;
    private TextAppearanceSpan authorStyle;
    private TextAppearanceSpan headlineStyle;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.story_details);

        pickEngineProviderHelper = new PickEngineProviderHelper(getApplicationContext());

        // Two ways of passing in story from caller
        if (getIntent().hasExtra(ParamConst.CREATE_INDEX_KEY_STORY)) {
            int createIndexKeyShouldBe = getIntent().getIntExtra(ParamConst.CREATE_INDEX_KEY_STORY, -1);
            if (StoryListSpot.storyDetailStory0 != null) {
                if (StoryListSpot.storyDetailStory0.getCreateIndex() == createIndexKeyShouldBe) {
                    story = StoryListSpot.storyDetailStory0;
                }
            }
        } else {
            // Note, this creates a clone, a deep-copy of the object. Changes will not be seen on the RecyclerView
            story = (Story) getIntent().getSerializableExtra(ParamConst.SERIALIZE_KEY_STORY);
        }

        titleStyle = new TextAppearanceSpan(this, R.style.story_details_title);
        authorStyle = new TextAppearanceSpan(this, R.style.story_details_author);
        headlineStyle = new TextAppearanceSpan(this, R.style.story_details_headline);

        ((SubactivityView) findViewById(R.id.subactivity_view)).setActivity(this);

        if (story == null) {
            Log.e(TAG, "Error finding Story SD-A00");
            Toast.makeText(this, "Error finding Story SD-A00", Toast.LENGTH_LONG).show();
            finish();
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (! EventBus.getDefault().isRegistered(this)) {
            EventBus.getDefault().register(this);
        }
        setView.run();
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (EventBus.getDefault().isRegistered(this)) {
            EventBus.getDefault().unregister(this);
        }
    }


    public final Runnable setView = new Runnable() {
        private Thread downloadingObserver = null;

        private void setDownloadingObserver() {
            synchronized (DownloadSpot.downloading) {
                if (downloadingObserver == null) {
                    downloadingObserver = new Thread() {
                        @Override
                        public void run() {
                            synchronized (DownloadSpot.downloading) {
                                try {
                                    DownloadSpot.downloading.wait();
                                } catch (Exception e) {
                                    Log.wtf(TAG,e);
                                }
                                downloadingObserver = null;
                                findViewById(R.id.progressbar).post(setView);
                                Log.i(TAG, "[storyDownload] StoryDetails page download");
                                DownloadSpot.storyNonListDownloadFlag = true;
                                EventBus.getDefault().post(new EventStoryNonListDownload());
                            }
                        }
                    };
                    downloadingObserver.setName("downloadingObserver");
                    downloadingObserver.start();
                }
            }
        }

        @Override
        public void run() {
            // This is the StoryDetails page, this is NOT the RecyclerView where scrolling is at blazing speed.
            story.invalidateAllStorageCache(getApplicationContext());

            final String storyName = story.getName(StoryDetails.this);
            ((TextView) findViewById(R.id.name)).setText(makeName());
            ((TextView) findViewById(R.id.author)).setText(makeAuthor());
            String storyDescription = story.getDescription(StoryDetails.this);
            CharSequence outDescription = storyDescription;

            if (storyDescription != null) {
                // Simple HTML can be found, the easiest way to detect is a close tag like "</b>" as that pattern isn't likely to happen in non-HTML. See ToDo: above.
                // Performance probably isn't a huge concern, as RecyclerView is very good about only hitting the data rows that are on-screen at the time.
                boolean useRenderingHTML = false;
                if (storyDescription.contains("</")) {
                    useRenderingHTML = true;
                } else if (storyDescription.contains("&#")) {
                    useRenderingHTML = true;
                }

                if (useRenderingHTML) {
                    // Going to HTML will strip whitespace behavior, so re-add newlines.  description of story "Gaucho" is a good test.
                    // Trim leading and trailing too, we only want inner newlines.
                    // ToDo: prep of CSV needs to trim. story example: "bsifhw1ik8524evd","Under the Bed"
                    String outReworkedHTML = storyDescription.trim().replace("<p>", "\n").replace("\n\n", "\n").replace("\n", "<br />");
                    Log.d(TAG, "[RVdescHTML] '" + outReworkedHTML + "'");
                    if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.N) {
                        outDescription = android.text.Html.fromHtml(outReworkedHTML, android.text.Html.FROM_HTML_MODE_LEGACY);
                    } else {
                        outDescription = android.text.Html.fromHtml(outReworkedHTML);
                    }
                }
            }


            ((TextView) findViewById(R.id.description)).setText(outDescription);

            TextView storyExtra0 = (TextView) findViewById(R.id.storyextra0);
            storyExtra0.setText("Category " + story.getStoryCategory());
            if (story.getRatingIFDB() >= 0.0f) {
                storyExtra0.append(". IFDB rating: " + String.format(Locale.US, "%.1f", story.getRatingIFDB()) + " of 5.0");
            } else {
                storyExtra0.append(". IFDB rating unknown.");
            }

            final File keepFile = story.getDownloadKeepFile(StoryDetails.this);
            if (keepFile.exists()) {
                storyExtra0.append("\nDownloadKeep " + keepFile.getPath() + " size " + keepFile.length());
                storyExtra0.setBackgroundColor(Color.TRANSPARENT);
            } else {
                storyExtra0.append("\nMISSING? DownloadKeep " + keepFile.getPath());
                storyExtra0.setBackgroundColor(Color.parseColor("#F8BBD0"));
            }

            if (!story.getLanguageIdentifier().equals("en")) {
                storyExtra0.append("\nStory language: " + story.getLanguageIdentifier());
            }

            // Using append allows one thing multiple textviews do not, word-wrapping.

            TextView storyHashInfo = (TextView) findViewById(R.id.story_hash_info);
            storyHashInfo.setText("MD5: " + story.getHash());
            String storyCalculatedHashSHA256 = story.getStoryHashSHA256(getApplicationContext());
            if (storyCalculatedHashSHA256 != null) {
                storyHashInfo.append(" SHA-256: " + storyCalculatedHashSHA256);
            }
            String storyExpectedHashSHA256 = story.getDownloadExpectedHashSHA256();
            if (storyExpectedHashSHA256 != null) {
                if (storyCalculatedHashSHA256 != null) {
                    if (storyCalculatedHashSHA256.equals(storyExpectedHashSHA256)) {
                        storyHashInfo.append(" [match]");
                    } else {
                        storyHashInfo.append(" mismatch for Expected SHA-256: " + storyExpectedHashSHA256);
                    }
                } else {
                    storyHashInfo.append(" SHA-256 (Expected): " + storyExpectedHashSHA256);
                }
            }

            if (!story.isDownloadedExtensiveCheck(StoryDetails.this)) {
                findViewById(R.id.play_container).setVisibility(View.GONE);
                findViewById(R.id.play_via_external_engine_provider_container).setVisibility(View.GONE);
                findViewById(R.id.cover).setVisibility(View.GONE);
                findViewById(R.id.progressbar).setVisibility(View.INVISIBLE);
                ((Button) findViewById(R.id.download_delete)).setText(R.string.download_story);
                findViewById(R.id.download_delete).setVisibility(View.VISIBLE);
                findViewById(R.id.download_delete).setOnClickListener(new View.OnClickListener() {
                    @Override public void onClick(final View v) {
                        v.setVisibility(View.INVISIBLE);
                        findViewById(R.id.progressbar).setVisibility(View.VISIBLE);

                        // ToDo: replace this download logic here with the new call to   story.startDownloadThread(StoryDetails.this);
                        synchronized (DownloadSpot.downloading) {
                            DownloadSpot.downloading.add(storyName);
                            setDownloadingObserver();
                        }
                        Thread downloadStory = new Thread() {
                            @Override public void run() {
                                String error = null;
                                try {
                                    if (!story.download(StoryDetails.this)) {
                                        error = StoryDetails.this.getString(R.string.download_invalid, story.getName(StoryDetails.this));
                                    }
                                } catch (Exception e) {
                                    Log.wtf(TAG, "download_delete Exception", e);
                                    error = StoryDetails.this.getString(R.string.download_failed, story.getName(StoryDetails.this));
                                }
                                synchronized (DownloadSpot.downloading) {
                                    DownloadSpot.downloading.remove(storyName);
                                    DownloadSpot.downloading.notifyAll();
                                }
                                if (error != null) {
                                    final String msg = error;
                                    Log.w(TAG, "download_delete error: " + error);
                                    v.post(new Runnable() {
                                        @Override public void run() {
                                            Toast.makeText(StoryDetails.this, msg, Toast.LENGTH_SHORT).show();
                                        }
                                    });
                                }
                                v.post(setView);
                                story.invalidateAllStorageCache(getApplicationContext());
                                // Right now, this event will trigger the RecyclerView to notify invalid listings on the current screen. Cover image removed.
                                Log.i(TAG, "[RVadaptNotify] story download, posting EventStoryNonListDownload");
                                DownloadSpot.storyNonListDownloadFlag = true;
                                EventBus.getDefault().post(new EventStoryNonListDownload());
                            }
                        };
                        downloadStory.setName("DownloadStory");
                        downloadStory.start();
                    }
                });

                if (story.getDownloadURL(StoryDetails.this) == null) {
                    Log.e(TAG, "donloadURL is null");
                    return;
                }

                String downloadText = story.getDownloadURL(StoryDetails.this).toString();
                String zipEntry = story.getZipEntry(StoryDetails.this);
                if (zipEntry != null) {
                    downloadText = downloadText + "[" + zipEntry + "]";
                }
                ((TextView) findViewById(R.id.download_delete_text)).setText(downloadText);
                findViewById(R.id.download_text).setVisibility(View.VISIBLE);
                ((TextView) findViewById(R.id.download_text)).setText(downloadText);
                findViewById(R.id.save_container).setVisibility(View.GONE);
                synchronized (DownloadSpot.downloading) {
                    if (DownloadSpot.downloading.contains(storyName)) {
                        findViewById(R.id.download_delete).setVisibility(View.GONE);
                        findViewById(R.id.progressbar).setVisibility(View.VISIBLE);
                        setDownloadingObserver();
                    }
                }
            } else {
                findViewById(R.id.play_container).setVisibility(View.VISIBLE);
                findViewById(R.id.play).setOnClickListener(new View.OnClickListener() {
                    @Override public void onClick(View v) {
                        Log.d(TAG, "[playClick] EventLocalStoryLaunch");
                        EventBus.getDefault().post(new EventLocalStoryLaunch(StoryDetails.this, story));
                    }
                });

                findViewById(R.id.play_via_external_engine_provider_container).setVisibility(View.VISIBLE);
                findViewById(R.id.play_via_external_engine_provider).setOnClickListener(new View.OnClickListener() {
                    @Override public void onClick(View v) {
                        Log.d(TAG, "[playClick] EventExternalEngineStoryLaunch");
                        EventBus.getDefault().post(new EventExternalEngineStoryLaunch(StoryDetails.this, story, StoryListSpot.optionLaunchExternalActivityCode, StoryListSpot.optionaLaunchInterruptEngine));
                    }
                });

                TypedArray selectedActivityValues = getResources().obtainTypedArray(R.array.thunderword_activity_values);
                pickEngineProviderHelper.spinnerForThunderwordActivity((Spinner) findViewById(R.id.external_provider_activity), (CheckBox) findViewById((R.id.external_provider_noprompt)), 4 /* TwoWindow Activity default */, selectedActivityValues, "TWactivityPos");

                int outEngineStringId = story.isZcode(StoryDetails.this) ? R.string.play_zcode : R.string.play_glulx;
                CharSequence outEngine = StoryDetails.this.getText(outEngineStringId);
                if (story.getEngineCode() != EngineConst.ENGINE_UNKNOWN) {
                    outEngine = "[" + StoryBrowseAdapter.engineCodeToNameCrossReference.get(story.getEngineCode()) + "]";
                }
                ((TextView) findViewById(R.id.play_text)).setText(outEngine);
                File coverImageFile = story.getCoverImageFile(StoryDetails.this);
                if (coverImageFile.exists()) {
                    findViewById(R.id.cover).setVisibility(View.VISIBLE);
                    ((ImageView) findViewById(R.id.cover)).setImageBitmap(story.getCoverImageBitmap(StoryDetails.this));
                    storyExtra0.append("\nCoverImage " + coverImageFile.getPath());
                } else {
                    findViewById(R.id.cover).setVisibility(View.GONE);
                    storyExtra0.append("\nCoverImage (missing) " + coverImageFile.getPath());
                }

                findViewById(R.id.progressbar).setVisibility(View.INVISIBLE);

                Button download_delete = (Button) findViewById(R.id.download_delete);
                download_delete.setText(R.string.delete_story);
                findViewById(R.id.download_delete).setVisibility(View.VISIBLE);
                findViewById(R.id.download_delete).setOnClickListener(new View.OnClickListener() {
                    @Override public void onClick(View v) {
                        story.delete(StoryDetails.this);
                        // Right now, this event will trigger the RecyclerView to notify invalid listings on the current screen. Cover image removed.
                        Log.i(TAG, "[RVadaptNotify] delete download, posting EventStoryNonListDownload");
                        DownloadSpot.storyNonListDownloadFlag = true;
                        EventBus.getDefault().post(new EventStoryNonListDownload());
                        finish();
                    }
                });

                TextView download_delete_text = (TextView) findViewById(R.id.download_delete_text);
                long storyLastModified = story.getStoryFile(StoryDetails.this).lastModified();
                if (storyLastModified == 0L) {
                    download_delete_text.setText("date missing, no file");
                } else {
                    download_delete_text.setText(Story.getTimeString(StoryDetails.this, R.string.downloaded_recently, R.string.downloaded_at, storyLastModified));
                }
                Log.d(TAG, "[storyDetail] lastModified " + storyLastModified + " file " + story.getStoryFile(StoryDetails.this).getPath());

                if (!story.getRestoreFile(StoryDetails.this).exists()) {
                    findViewById(R.id.save_container).setVisibility(View.GONE);
                } else {
                    findViewById(R.id.save_container).setVisibility(View.VISIBLE);
                    findViewById(R.id.delete_save).setOnClickListener(new View.OnClickListener() {
                        @Override public void onClick(View v) {
                            story.getRestoreFile(StoryDetails.this).delete();
                            setView.run();
                        }
                    });
                    ((TextView) findViewById(R.id.save_text)).setText(Story.getTimeString(StoryDetails.this, R.string.saved_recently, R.string.saved_at, story.getRestoreFile(StoryDetails.this).lastModified()));
                }

                // Show the storage path
                findViewById(R.id.download_text).setVisibility(View.VISIBLE);
                File storyExtractedDir = story.getDir(StoryDetails.this);
                TextView downloadText = (TextView) findViewById(R.id.download_text);
                if (storyExtractedDir.isDirectory()) {
                    downloadText.setText("ExtractedFolder " + storyExtractedDir.getPath());
                    downloadText.setBackgroundColor(Color.TRANSPARENT);
                } else {
                    downloadText.setText("MISSING? ExtractedFolder " + storyExtractedDir.getPath());
                    downloadText.setBackgroundColor(Color.parseColor("#F8BBD0"));

                    // Rework the delete button
                    download_delete.setText(R.string.delete_story_keepfile);
                    if (keepFile.exists()) {
                        // Logic here is that the extracted is MISSING and the keepFile exists
                        download_delete_text.setText("KeepFile " + Story.getTimeString(StoryDetails.this, R.string.downloaded_recently, R.string.downloaded_at, keepFile.lastModified()));

                        findViewById(R.id.download_delete).setOnClickListener(new View.OnClickListener() {
                            @Override public void onClick(View v) {
                                keepFile.delete();
                                story.invalidateAllStorageCache(getApplicationContext());
                                // Right now, this event will trigger the RecyclerView to notify invalid listings on the current screen. Cover image removed.
                                Log.i(TAG, "[RVadaptNotify] delete KeepFile, posting EventStoryNonListDownload");
                                DownloadSpot.storyNonListDownloadFlag = true;
                                EventBus.getDefault().post(new EventStoryNonListDownload());
                                finish();
                            }
                        });
                    }
                }

                // Show the Engine Provider (Thunderword) status.
                if (EchoSpot.currentEngineProvider != null) {
                    findViewById(R.id.play_via_external_engine_provider_container).setVisibility(View.VISIBLE);
                    findViewById(R.id.engine_provider_status).setVisibility(View.VISIBLE);
                    findViewById(R.id.engine_provider_suggestion).setVisibility(View.GONE);
                    pickEngineProviderHelper.redrawEngineProvider((TextView) findViewById(R.id.engine_provider_status), null /* Clear */);
                } else {
                    findViewById(R.id.play_via_external_engine_provider_container).setVisibility(View.GONE);
                    findViewById(R.id.engine_provider_status).setVisibility(View.GONE);
                    findViewById(R.id.engine_provider_suggestion).setVisibility(View.VISIBLE);
                }
            }

            final String ifid = story.getIFID(StoryDetails.this);
            if (ifid == null) {
                findViewById(R.id.ifdb_container).setVisibility(View.GONE);
            } else {
                findViewById(R.id.ifdb_container).setVisibility(View.VISIBLE);
                findViewById(R.id.ifdb).setOnClickListener(new View.OnClickListener() {
                    @Override public void onClick(View v) {
                        startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(StoryDetails.this.getString(R.string.ifdb_viewgame, Uri.encode(ifid)))));
                    }
                });
            }
        }
    };


    protected PickEngineProviderHelper pickEngineProviderHelper;

    private SpannableStringBuilder makeName() {
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

    private SpannableStringBuilder makeAuthor() {
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


    /*
    Main thread to touch GUI.
     */
    @SuppressWarnings("unused")
    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onEventMainThread(EventEngineProviderChange event) {
        pickEngineProviderHelper.redrawEngineProvider((TextView) findViewById(R.id.engine_provider_status), null /* Clear */);
    }
}
