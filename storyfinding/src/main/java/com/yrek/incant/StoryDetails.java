package com.yrek.incant;

import android.app.Activity;
import android.content.Intent;
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


public class StoryDetails extends Activity {
    private static final String TAG = StoryDetails.class.getSimpleName();

    private Story story;
    private TextAppearanceSpan titleStyle;
    private TextAppearanceSpan authorStyle;
    private TextAppearanceSpan headlineStyle;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.story_details);
        story = (Story) getIntent().getSerializableExtra(ParamConst.SERIALIZE_KEY_STORY);
        titleStyle = new TextAppearanceSpan(this, R.style.story_details_title);
        authorStyle = new TextAppearanceSpan(this, R.style.story_details_author);
        headlineStyle = new TextAppearanceSpan(this, R.style.story_details_headline);

        ((SubactivityView) findViewById(R.id.subactivity_view)).setActivity(this);
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
            final String storyName = story.getName(StoryDetails.this);
            ((TextView) findViewById(R.id.name)).setText(makeName());
            ((TextView) findViewById(R.id.author)).setText(makeAuthor());
            ((TextView) findViewById(R.id.description)).setText(story.getDescription(StoryDetails.this));

            TextView storyExtra0 = (TextView) findViewById(R.id.storyextra0);
            storyExtra0.setText("Category " + story.getStoryCategory());
            File keepFile = story.getDownloadKeepFile(StoryDetails.this);
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
            storyHashInfo.setText("MD5: " + story.getHash() + " SHA-256: " + story.getStoryHashSHA256());

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
                        Log.d(TAG, "[playClick");
                        EventBus.getDefault().post(new EventLocalStoryLaunch(StoryDetails.this, story));
                    }
                });

                findViewById(R.id.play_via_external_engine_provider_container).setVisibility(View.VISIBLE);
                findViewById(R.id.play_via_external_engine_provider).setOnClickListener(new View.OnClickListener() {
                    @Override public void onClick(View v) {
                        Log.d(TAG, "[playClick");
                        EventBus.getDefault().post(new EventExternalEngineStoryLaunch(StoryDetails.this, story, StoryListSpot.optionLaunchExternalActivityCode, StoryListSpot.optionaLaunchInterruptEngine));
                    }
                });

                pickEngineProviderHelper.spinnerForThunderwordActivity((Spinner) findViewById(R.id.external_provider_activity), (CheckBox) findViewById((R.id.external_provider_noprompt)));

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
                ((Button) findViewById(R.id.download_delete)).setText(R.string.delete_story);
                findViewById(R.id.download_delete).setVisibility(View.VISIBLE);
                findViewById(R.id.download_delete).setOnClickListener(new View.OnClickListener() {
                    @Override public void onClick(View v) {
                        story.delete(StoryDetails.this);
                        finish();
                    }
                });
                ((TextView) findViewById(R.id.download_delete_text)).setText(Story.getTimeString(StoryDetails.this, R.string.downloaded_recently, R.string.downloaded_at, story.getStoryFile(StoryDetails.this).lastModified()));
                Log.d(TAG, "[storyDetail] lastModified " + story.getStoryFile(StoryDetails.this).lastModified() + " file " + story.getStoryFile(StoryDetails.this).getPath());
                if (!story.getSaveFile(StoryDetails.this).exists()) {
                    findViewById(R.id.save_container).setVisibility(View.GONE);
                } else {
                    findViewById(R.id.save_container).setVisibility(View.VISIBLE);
                    findViewById(R.id.delete_save).setOnClickListener(new View.OnClickListener() {
                        @Override public void onClick(View v) {
                            story.getSaveFile(StoryDetails.this).delete();
                            setView.run();
                        }
                    });
                    ((TextView) findViewById(R.id.save_text)).setText(Story.getTimeString(StoryDetails.this, R.string.saved_recently, R.string.saved_at, story.getSaveFile(StoryDetails.this).lastModified()));
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


    PickEngineProviderHelper pickEngineProviderHelper = new PickEngineProviderHelper();

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
