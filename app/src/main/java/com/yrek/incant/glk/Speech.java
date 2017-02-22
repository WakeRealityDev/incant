package com.yrek.incant.glk;

import android.content.Context;
import android.speech.tts.TextToSpeech;
import android.speech.tts.UtteranceProgressListener;
import android.util.Log;
import android.view.View;
import android.widget.Button;

import com.yrek.runconfig.SettingsCurrent;

import java.util.HashMap;

public class Speech {
    private static final String TAG = Speech.class.getSimpleName();

    private final Context context;
    private final Button skipButton;

    private TextToSpeech textToSpeech;
    private boolean speechStarted = false;
    private boolean speechCanceled = false;
    private boolean ready = false;
    private Runnable onDone = null;

    public Speech(Context context, Button skipButton) {
        this.context = context;
        this.skipButton = skipButton;

        skipButton.setOnClickListener(skipButtonOnClickListener);
    }

    public void onStart() {
        ready = false;
        textToSpeech = new TextToSpeech(context, textToSpeechOnInitListener);
        textToSpeech.setOnUtteranceProgressListener(utteranceProgressListener);
    }

    public void onStop() {
        textToSpeech.shutdown();
        synchronized (utteranceProgressListener) {
            onDone = null;
        }
    }

    public void waitForInit() throws InterruptedException {
        synchronized (utteranceProgressListener) {
            while (!ready) {
                utteranceProgressListener.wait();
            }
        }
    }

    public void resetSkip() {
        speechCanceled = false;
    }

    public void speak(String string, Runnable onDone) {
        Log.d(TAG,"speak:"+string+",onDone="+onDone+",this.onDone="+this.onDone+",this="+this+",SettingsCurrent.getSpeechEnabled()="+ SettingsCurrent.getSpeechEnabled());
        if (! SettingsCurrent.getSpeechEnabled())
            speechCanceled = true;

        if (onDone == null) {
            throw new IllegalArgumentException();
        }
        synchronized (utteranceProgressListener) {
            if (this.onDone != null) {
                throw new IllegalStateException();
            }
            if (speechCanceled) {
                skipButton.post(onDone);
                return;
            }
            this.onDone = onDone;
            speechStarted = false;
        }
        skipButton.post(showSkipButton);
        HashMap<String,String> params = new HashMap<String,String>();
        params.put(TextToSpeech.Engine.KEY_PARAM_UTTERANCE_ID, string);
        if (textToSpeech.speak(string, TextToSpeech.QUEUE_ADD, params) != TextToSpeech.SUCCESS) {
            synchronized (utteranceProgressListener) {
                skipButton.post(hideSkipButton);
                if (this.onDone != null) {
                    skipButton.post(this.onDone);
                    this.onDone = null;
                }
            }
        }
    }

    private final TextToSpeech.OnInitListener textToSpeechOnInitListener = new TextToSpeech.OnInitListener() {
        @Override public void onInit(int status) {
            Log.d(TAG,"onInit:status="+status);
            synchronized(utteranceProgressListener) {
                ready = true;
                utteranceProgressListener.notify();
            }
        }
    };

    private final UtteranceProgressListener utteranceProgressListener = new UtteranceProgressListener() {
        @Override
        public void onDone(String utteranceId) {
            skipButton.post(hideSkipButton);
            synchronized(this) {
                if (onDone != null) {
                    skipButton.post(onDone);
                    onDone = null;
                }
            }
        }

        @Override
        public void onError(String utteranceId) {
            skipButton.post(hideSkipButton);
            synchronized(this) {
                if (onDone != null) {
                    skipButton.post(onDone);
                    onDone = null;
                }
            }
        }

        @Override
        public void onStart(String utteranceId) {
            synchronized(this) {
                speechStarted = true;
            }
        }
    };

    private final Runnable showSkipButton = new Runnable() {
        @Override public void run() {
            skipButton.setVisibility(View.VISIBLE);
        }
    };

    private final Runnable hideSkipButton = new Runnable() {
        @Override public void run() {
            skipButton.setVisibility(View.GONE);
        }
    };

    private final View.OnClickListener skipButtonOnClickListener = new View.OnClickListener() {
        @Override public void onClick(View v) {
            synchronized (utteranceProgressListener) {
                if (onDone == null || !speechStarted) {
                    return;
                }
                speechCanceled = true;
                textToSpeech.stop();
            }
            skipButton.setVisibility(View.GONE);
        }
    };

}
