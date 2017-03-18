package com.yrek.incant;

import android.app.Activity;
import android.content.Intent;
import android.content.res.TypedArray;
import android.net.Uri;
import android.os.Bundle;
import android.text.SpannableStringBuilder;
import android.text.style.TextAppearanceSpan;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.ImageView;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import com.squareup.phrase.Phrase;
import com.wakereality.thunderstrike.EchoSpot;
import com.wakereality.thunderstrike.dataexchange.EngineProvider;
import com.wakereality.thunderstrike.dataexchange.EventEngineProviderChange;
import com.yrek.incant.glk.GlkActivity;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;

import java.io.File;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

public class StoryDetails extends Activity {
    private static final String TAG = StoryDetails.class.getSimpleName();

    private Story story;
    private TextAppearanceSpan titleStyle;
    private TextAppearanceSpan authorStyle;
    private TextAppearanceSpan headlineStyle;
    private boolean launchInterruptStory = true;
    protected int selectedLaunchActivity = 0;
    protected static AtomicInteger launchToken = new AtomicInteger(0);

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.story_details);
        story = (Story) getIntent().getSerializableExtra(Incant.SERIALIZE_KEY_STORY);
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

    public static void animateClickedView(final View view) {
        // Poor man's animation to show visual feedback of click.
        view.setAlpha(0.2f);
        view.postDelayed(new Runnable() {
            @Override
            public void run() {
                view.setAlpha(1.0f);
            }
        }, 1200L);
    }

    public final Runnable setView = new Runnable() {
        private Thread downloadingObserver = null;

        private void setDownloadingObserver() {
            synchronized (Incant.downloading) {
                if (downloadingObserver == null) {
                    downloadingObserver = new Thread() {
                        @Override
                        public void run() {
                            synchronized (Incant.downloading) {
                                try {
                                    Incant.downloading.wait();
                                } catch (Exception e) {
                                    Log.wtf(TAG,e);
                                }
                                downloadingObserver = null;
                                findViewById(R.id.progressbar).post(setView);
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
            ((TextView) findViewById(R.id.storyextra0)).setText("Category " + story.getStoryCategory());
            ((TextView) findViewById(R.id.story_hash_info)).setText("MD5: " + story.getHash());
            if (!story.isDownloaded(StoryDetails.this)) {
                findViewById(R.id.play_container).setVisibility(View.GONE);
                findViewById(R.id.play_via_external_engine_provider_container).setVisibility(View.GONE);
                findViewById(R.id.cover).setVisibility(View.GONE);
                findViewById(R.id.progressbar).setVisibility(View.INVISIBLE);
                ((Button) findViewById(R.id.download_delete)).setText(R.string.download);
                findViewById(R.id.download_delete).setVisibility(View.VISIBLE);
                findViewById(R.id.download_delete).setOnClickListener(new View.OnClickListener() {
                    @Override public void onClick(final View v) {
                        v.setVisibility(View.INVISIBLE);
                        findViewById(R.id.progressbar).setVisibility(View.VISIBLE);
                        synchronized (Incant.downloading) {
                            Incant.downloading.add(storyName);
                            setDownloadingObserver();
                        }
                        new Thread() {
                            @Override public void run() {
                                String error = null;
                                try {
                                    if (!story.download(StoryDetails.this)) {
                                        error = StoryDetails.this.getString(R.string.download_invalid, story.getName(StoryDetails.this));
                                    }
                                } catch (Exception e) {
                                    Log.wtf(TAG,e);
                                    error = StoryDetails.this.getString(R.string.download_failed, story.getName(StoryDetails.this));
                                }
                                synchronized (Incant.downloading) {
                                    Incant.downloading.remove(storyName);
                                    Incant.downloading.notifyAll();
                                }
                                if (error != null) {
                                    final String msg = error;
                                    v.post(new Runnable() {
                                        @Override public void run() {
                                            Toast.makeText(StoryDetails.this, msg, Toast.LENGTH_SHORT).show();
                                        }
                                    });
                                }
                                v.post(setView);
                            }
                        }.start();
                    }
                });

                String downloadText = story.getDownloadURL(StoryDetails.this).toString();
                String zipEntry = story.getZipEntry(StoryDetails.this);
                if (zipEntry != null) {
                    downloadText = downloadText + "[" + zipEntry + "]";
                }
                ((TextView) findViewById(R.id.download_delete_text)).setText(downloadText);
                findViewById(R.id.download_text).setVisibility(View.VISIBLE);
                ((TextView) findViewById(R.id.download_text)).setText(downloadText);
                findViewById(R.id.save_container).setVisibility(View.GONE);
                synchronized (Incant.downloading) {
                    if (Incant.downloading.contains(storyName)) {
                        findViewById(R.id.download_delete).setVisibility(View.GONE);
                        findViewById(R.id.progressbar).setVisibility(View.VISIBLE);
                        setDownloadingObserver();
                    }
                }
            } else {
                findViewById(R.id.play_container).setVisibility(View.VISIBLE);
                findViewById(R.id.play).setOnClickListener(new View.OnClickListener() {
                    @Override public void onClick(View v) {
                        Intent intent = new Intent(StoryDetails.this, GlkActivity.class);
                        if (story.isZcode(StoryDetails.this)) {
                            intent.putExtra(GlkActivity.GLK_MAIN, new ZCodeStory(story, story.getName(StoryDetails.this)));
                        } else {
                            intent.putExtra(GlkActivity.GLK_MAIN, new GlulxStory(story, story.getName(StoryDetails.this)));
                        }
                        startActivity(intent);
                    }
                });

                findViewById(R.id.play_via_external_engine_provider_container).setVisibility(View.VISIBLE);
                findViewById(R.id.play_via_external_engine_provider).setOnClickListener(new View.OnClickListener() {
                    @Override public void onClick(View v) {
                        Intent intent = new Intent();
                        // Tell Android to start Thunderword app if not already running.
                        intent.addFlags(Intent.FLAG_INCLUDE_STOPPED_PACKAGES);

                        // Inform the Engine Provider who to call back.
                        intent.putExtra("sender", BuildConfig.APPLICATION_ID);

                        String targetPackage = "";
                        // Send to specific selected Engine Provider.
                        if (EchoSpot.currentEngineProvider != null) {
                            intent.setPackage(EchoSpot.currentEngineProvider.providerAppPackage);
                            targetPackage = EchoSpot.currentEngineProvider.providerAppPackage;
                        }

                        long launchWhen = System.currentTimeMillis();
                        intent.putExtra("sentwhen", launchWhen);


                        if (story.isZcode(StoryDetails.this)) {
                            intent.setAction("interactivefiction.engine.zmachine");
                        } else {
                            intent.setAction("interactivefiction.engine.glulx");
                        }
                        // Not all stories come in Blorb packages, check first, but if missing go for the data file.
                        File exportStoryDataFile = story.getBlorbFile(StoryDetails.this);
                        if (! exportStoryDataFile.exists()) {
                            if (story.isZcode(StoryDetails.this)) {
                                exportStoryDataFile = story.getZcodeFile(StoryDetails.this);
                            } else {
                                exportStoryDataFile = story.getGlulxFile(StoryDetails.this);
                            }
                        }
                        intent.putExtra("path", exportStoryDataFile.getPath());
                        int myLaunchToken = launchToken.incrementAndGet();
                        Log.i(TAG, "path " + exportStoryDataFile.getPath() + " sender " + BuildConfig.APPLICATION_ID + " launchToken " + myLaunchToken + " selectedLaunchActivity " + selectedLaunchActivity + " when " + launchWhen + " target " + targetPackage);
                        // Set default value.
                        if (selectedLaunchActivity == 0) {
                            selectedLaunchActivity = 1;   /* Bidirectional Scrolling Activity */
                        }
                        intent.putExtra("activitycode", selectedLaunchActivity);
                        intent.putExtra("interrupt", launchInterruptStory);
                        intent.putExtra("launchtoken", "A" + myLaunchToken);
                        sendBroadcast(intent);
                    }
                });

                final TypedArray selectedActivityValues = getResources().obtainTypedArray(R.array.thunderword_activity_values);

                ((Spinner) findViewById(R.id.external_provider_activity)).setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
                    @Override
                    public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                        selectedLaunchActivity = selectedActivityValues.getInt(position, -1);
                    }

                    @Override
                    public void onNothingSelected(AdapterView<?> parent) {
                        selectedLaunchActivity = 0;
                    }
                });

                int outEngine = story.isZcode(StoryDetails.this) ? R.string.play_zcode : R.string.play_glulx;
                ((TextView) findViewById(R.id.play_text)).setText(outEngine);
                if (story.getCoverImageFile(StoryDetails.this).exists()) {
                    findViewById(R.id.cover).setVisibility(View.VISIBLE);
                    ((ImageView) findViewById(R.id.cover)).setImageBitmap(story.getCoverImageBitmap(StoryDetails.this));
                } else {
                    findViewById(R.id.cover).setVisibility(View.GONE);
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
                ((TextView) findViewById(R.id.download_delete_text)).setText(Incant.getTimeString(StoryDetails.this, R.string.downloaded_recently, R.string.downloaded_at, story.getStoryFile(StoryDetails.this).lastModified()));
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
                    ((TextView) findViewById(R.id.save_text)).setText(Incant.getTimeString(StoryDetails.this, R.string.saved_recently, R.string.saved_at, story.getSaveFile(StoryDetails.this).lastModified()));
                }

                // Show the storage path
                findViewById(R.id.download_text).setVisibility(View.VISIBLE);
                ((TextView) findViewById(R.id.download_text)).setText(story.getDir(StoryDetails.this).toString());

                // Show the Engine Provider (Thunderword) status.
                if (EchoSpot.currentEngineProvider != null) {
                    findViewById(R.id.play_via_external_engine_provider_container).setVisibility(View.VISIBLE);
                    findViewById(R.id.engine_provider_status).setVisibility(View.VISIBLE);
                    findViewById(R.id.engine_provider_suggestion).setVisibility(View.GONE);
                    redrawEngineProvider();
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

    private void redrawEngineProvider() {
        CharSequence extraA = "";
        if (EchoSpot.detectedEngineProviders.size() > 0) {
            // Phrase library seems to drop the leading space that is in the string resource, so add it back here.
            extraA = " " + Phrase.from(getResources(), R.string.engine_provider_detected_extra).put("quantity", EchoSpot.detectedEngineProviders.size()).format();
            // show current index.
            if (EchoSpot.detectedEngineProviders.size() > 1) {
                extraA = extraA + "/" + EchoSpot.currentEngineProviderIndex;
            };
        }

        TextView engineProviderStatus = (TextView) findViewById(R.id.engine_provider_status);
        engineProviderStatus.setText(Phrase.from(getResources(), R.string.engine_provider_detected_named)
                .put("engine", EchoSpot.currentEngineProvider.providerAppPackage.replace("com.wakereality.", "wakereality.") )
                .put("extra_a", extraA )
                .format()
        );

        // Switch providers with touch if multiple available
        // ToDo: make this smarter about not picking the one that is already visible on first touch.
        if (EchoSpot.detectedEngineProviders.size() > 1) {
            engineProviderStatus.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                            /*
                            Kind of a mess, but don't want to assign number ID to engines, they are strings and want to allow any value.
                            Does java have a data structure one can put(StringName, latestValue) over and over and index back out for picking?
                            I suppose the other option is that every time a new provider is detected build a list of string key indexes into an array and assign an index integer here
                               on this client app side?
                             */
                    int newIndex = EchoSpot.currentEngineProviderIndex + 1;
                    if (newIndex >= EchoSpot.detectedEngineProviders.size()) {
                        // wrap back to zero
                        newIndex = 0;
                    }
                    EchoSpot.currentEngineProviderIndex = newIndex;
                    int onLoopIndex = 0;
                    for (Map.Entry<String, EngineProvider> entry : EchoSpot.detectedEngineProviders.entrySet()) {
                        EchoSpot.currentEngineProvider = entry.getValue();
                        if (onLoopIndex == newIndex) {
                            break;
                        }
                        onLoopIndex++;
                    }
                    // ToDo: save to shared preferences?
                    // redraw
                    redrawEngineProvider();
                    animateClickedView(v);
                }
            });
        }
    }

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

    public void onProviderInterruptClicked(View view) {
        launchInterruptStory = ((CheckBox) view).isChecked();
    }


    /*
    Main thread to touch GUI.
     */
    @SuppressWarnings("unused")
    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onEventMainThread(EventEngineProviderChange event) {
        redrawEngineProvider();
    }
}
