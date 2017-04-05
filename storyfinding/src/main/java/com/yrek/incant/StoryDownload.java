package com.yrek.incant;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.TextView;

import com.wakereality.storyfinding.R;

import java.net.URL;
import java.net.URLDecoder;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class StoryDownload extends Activity {
    private static final String TAG = StoryDownload.class.getSimpleName();

    private EditText editName;
    private EditText editUrl;
    private EditText editZipEntry;
    private InputMethodManager inputMethodManager;
    private View progressbar;

    private Thread downloading;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.story_download);
        editName = (EditText) findViewById(R.id.edit_name);
        editUrl = (EditText) findViewById(R.id.edit_url);
        editZipEntry = (EditText) findViewById(R.id.edit_zip_entry);
        inputMethodManager = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
        progressbar = findViewById(R.id.progressbar);

        ((SubactivityView) findViewById(R.id.subactivity_view)).setActivity(this);
        editName.setOnFocusChangeListener(editTextOnFocusChangeListener);
        editUrl.setOnFocusChangeListener(editTextOnFocusChangeListener);
        editZipEntry.setOnFocusChangeListener(editTextOnFocusChangeListener);
        editName.setOnEditorActionListener(editTextOnEditorActionListener);
        editUrl.setOnEditorActionListener(editTextOnEditorActionListener);
        editZipEntry.setOnEditorActionListener(editTextOnEditorActionListener);

        findViewById(R.id.ifarchive).setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) {
                if (downloading == null) {
                    editUrl.getText().insert(0, getString(R.string.ifarchive_download_url_input_prefix));
                }
            }
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        resetView.run();
    }

    @Override
    protected void onPause() {
        super.onPause();
        downloading = null;
    }

    private final Runnable resetView = new Runnable() {
        @Override
        public void run() {
            downloading = null;
            progressbar.setVisibility(View.GONE);
            editName.setEnabled(true);
            editName.setFocusable(true);
            editUrl.setEnabled(true);
            editUrl.setFocusable(true);
            editZipEntry.setEnabled(true);
            editZipEntry.setFocusable(true);
            editUrl.postDelayed(new Runnable() {
                @Override public void run() {
                    editUrl.requestFocus();
                }
            }, 250);
        }
    };

    private final View.OnFocusChangeListener editTextOnFocusChangeListener = new View.OnFocusChangeListener() {
        @Override
        public void onFocusChange(View v, boolean hasFocus) {
            if (hasFocus && v instanceof EditText) {
                inputMethodManager.showSoftInput(v, InputMethodManager.SHOW_IMPLICIT);
            } else {
                inputMethodManager.hideSoftInputFromWindow(v.getWindowToken(), 0);
            }
        }
    };

    private final TextView.OnEditorActionListener editTextOnEditorActionListener = new TextView.OnEditorActionListener() {
        private final Pattern nameExtractor = Pattern.compile(".*/([^/?.]+)($|\\.[^/?]*($|\\?)|\\?)");

        @Override
        public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
            String name = editName.getText().toString();
            String url = editUrl.getText().toString();
            String zipEntry = editZipEntry.getText().toString();
            Log.d(TAG,"name="+name+",url="+url+",zipEntry="+zipEntry);
            if (url == null || url.length() == 0) {
                if (v != editUrl) {
                    editUrl.requestFocus();
                }
                return true;
            }
            if ((url.endsWith(".zip") || url.endsWith(".ZIP")) && (zipEntry == null || zipEntry.length() == 0) && v != editZipEntry) {
                editZipEntry.requestFocus();
                return true;
            }
            if (name == null || name.length() == 0) {
                if (zipEntry != null && zipEntry.length() > 0) {
                    Matcher m = nameExtractor.matcher(zipEntry);
                    if (m.find()) {
                        name = m.group(1);
                    } else if (zipEntry.lastIndexOf('.') > 0) {
                        name = zipEntry.substring(0, zipEntry.lastIndexOf('.'));
                    } else {
                        name = zipEntry;
                    }
                } else {
                    Matcher m = nameExtractor.matcher(url);
                    if (m.find()) {
                        try {
                            name = URLDecoder.decode(m.group(1), "UTF-8");
                        } catch (Exception e) {
                            Log.wtf(TAG,e);
                        }
                    }
                }
                editName.getText().insert(0, name);
                if (v != editName) {
                    editName.requestFocus();
                }
                return true;
            }
            URL downloadURL = null;
            try {
                downloadURL = new URL(url);
            } catch (Exception e) {
                if (v != editUrl) {
                    editUrl.requestFocus();
                }
                return true;
            }

            final Story story = new Story(name, null, null, null, downloadURL, zipEntry != null && zipEntry.length() > 0 ? zipEntry : null, null);
            final Runnable gotoStoryDetails = new Runnable() {
                @Override public void run() {
                    Intent intent = new Intent(StoryDownload.this, StoryDetails.class);
                    intent.putExtra(ParamConst.SERIALIZE_KEY_STORY, story);
                    startActivity(intent);
                }
            };
            if (story.isDownloaded(StoryDownload.this)) {
                gotoStoryDetails.run();
                return true;
            }

            Log.d(TAG,"downloading:name="+name+",url="+url+",zipEntry="+zipEntry);
            progressbar.setVisibility(View.VISIBLE);
            editName.setEnabled(false);
            editName.setFocusable(false);
            editUrl.setEnabled(false);
            editUrl.setFocusable(false);
            editZipEntry.setEnabled(false);
            editZipEntry.setFocusable(false);
            progressbar.requestFocus();
            final String storyName = name;
            downloading = new Thread("StoryDownload") {
                @Override public void run() {
                    synchronized (DownloadSpot.downloading) {
                        DownloadSpot.downloading.add(storyName);
                    }
                    try {
                        story.download(StoryDownload.this);
                    } catch (Exception e) {
                        Log.wtf(TAG,e);
                    }
                    synchronized (DownloadSpot.downloading) {
                        DownloadSpot.downloading.remove(storyName);
                        DownloadSpot.downloading.notifyAll();
                    }
                    if (story.isDownloaded(StoryDownload.this)) {
                        progressbar.post(gotoStoryDetails);
                    } else {
                        progressbar.post(resetView);
                    }
                }
            };
            downloading.start();
            return true;
        }
    };
}
