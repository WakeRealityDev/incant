package com.yrek.incant;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.text.SpannableStringBuilder;
import android.text.style.TextAppearanceSpan;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.yrek.incant.glk.GlkActivity;

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
        story = (Story) getIntent().getSerializableExtra(Incant.SERIALIZE_KEY_STORY);
        titleStyle = new TextAppearanceSpan(this, R.style.story_details_title);
        authorStyle = new TextAppearanceSpan(this, R.style.story_details_author);
        headlineStyle = new TextAppearanceSpan(this, R.style.story_details_headline);

        ((SubactivityView) findViewById(R.id.subactivity_view)).setActivity(this);
    }

    @Override
    protected void onResume() {
        super.onResume();
        setView.run();
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
            if (!story.isDownloaded(StoryDetails.this)) {
                findViewById(R.id.play_container).setVisibility(View.GONE);
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
}
