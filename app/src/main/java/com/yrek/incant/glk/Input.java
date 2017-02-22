package com.yrek.incant.glk;

import android.content.Context;
import android.content.Intent;
import android.media.AudioManager;
import android.os.Build;
import android.os.Bundle;
import android.speech.RecognitionListener;
import android.speech.RecognizerIntent;
import android.speech.SpeechRecognizer;
import android.text.Editable;
import android.util.Base64;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.yrek.runconfig.SettingsCurrent;
import com.yrek.runconfig.SettingsDebug;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.security.MessageDigest;

import javax.crypto.Cipher;
import javax.crypto.spec.SecretKeySpec;

public class Input {
    private static final String TAG = Input.class.getSimpleName();

    private final Context context;
    private final InputMethodManager inputMethodManager;
    private final Button keyboardButton;
    private final EditText editText;

    private boolean recognitionAvailable;
    private SpeechRecognizer speechRecognizer = null;

    private Bundle recognitionResults = null;
    private boolean recognizerReady = true;
    private boolean usingKeyboard = false;
    private boolean usingKeyboardDone = false;
    private boolean doingInput = false;
    private String inputLineResults;
    private int inputCharResults;
    private boolean inputCanceled = false;
    private boolean waitingForEvent = false;

    public Input(Context context, InputMethodManager inputMethodManager, Button keyboardButton, EditText editText) {
        this.context = context;
        this.inputMethodManager = inputMethodManager;
        this.keyboardButton = keyboardButton;
        this.editText = editText;

        keyboardButton.setOnClickListener(keyboardButtonOnClickListener);
        editText.setOnFocusChangeListener(editTextOnFocusChangeListener);
        editText.setOnEditorActionListener(editTextOnEditorActionListener);
    }

    public void onStart() {
        if (SettingsCurrent.getSpeechRecognizerEnabled()) {
            recognitionAvailable = SpeechRecognizer.isRecognitionAvailable(context);
            if (!recognitionAvailable) {
                Log.w(TAG, "SpeechRecognizer.isRecognitionAvailable FALSE");
                return;
            }
            speechRecognizer = SpeechRecognizer.createSpeechRecognizer(context);
            speechRecognizer.setRecognitionListener(recognitionListener);
            recognizerReady = true;
            usingKeyboard = false;
            usingKeyboardDone = false;
            doingInput = false;
            waitingForEvent = false;
        }
        else
        {
            recognitionAvailable = false;
        }
    }

    public void onStop() {
        if (speechRecognizer != null) {
            speechRecognizer.destroy();
            speechRecognizer = null;
        }
    }

    public int getCharInput(long timeout) throws InterruptedException {
        Log.d(TAG,"start getCharInput");
        usingKeyboard = false;
        usingKeyboardDone = false;
        recognizeSpeech(timeout);
        Log.d(TAG,"getCharInput:"+inputCharResults);
        return inputCharResults;
    }

    public String getInput(long timeout) throws InterruptedException {
        Log.d(TAG,"start getInput");
        usingKeyboard = false;
        usingKeyboardDone = false;
        recognizeSpeech(timeout);
        Log.d(TAG,"getInput:"+inputLineResults);
        return inputLineResults;
    }

    public void waitForEvent(long timeout) throws InterruptedException {
        synchronized (recognitionListener) {
            waitingForEvent = true;
            if (timeout > 0) {
                recognitionListener.wait(timeout);
            } else {
                recognitionListener.wait();
            }
            waitingForEvent = false;
        }
    }

    public void cancelInput() {
        synchronized (recognitionListener) {
            if (doingInput || waitingForEvent) {
                inputCanceled = true;
                recognitionListener.notify();
            }
        }
    }

    // Must be called in UI thread.
    public boolean pasteInput(CharSequence text) {
        synchronized (recognitionListener) {
            if (!doingInput) {
                return false;
            }
            enableKeyboard.run();
            editText.getEditableText().append(text);
            return true;
        }
    }

    // Must be called in UI thread.
    public boolean deleteWord() {
        synchronized (recognitionListener) {
            if (!doingInput || !usingKeyboard) {
                return false;
            }
            Editable editable = editText.getEditableText();
            if (editable.length() == 0) {
                return false;
            }
            boolean gotWord = false;
            for (int i = editable.length() - 1; i >= 0; i--) {
                if (editable.charAt(i) != ' ') {
                    gotWord = true;
                } else if (gotWord) {
                    editable.delete(i+1, editable.length());
                    return true;
                }
            }
            editable.clear();
            return true;
        }
    }

    // Must be called in UI thread.
    public boolean enter() {
        synchronized (recognitionListener) {
            if (!doingInput || !usingKeyboard || usingKeyboardDone) {
                return false;
            }
            inputLineResults = editText.getText().toString();
            inputCharResults = SpeechMunger.chooseCharacterInput(inputLineResults);
            editText.setFocusable(false);
            editText.setVisibility(View.GONE);
            synchronized (recognitionListener) {
                usingKeyboardDone = true;
                recognitionListener.notify();
            }
            return true;
        }
    }

    private void recognizeSpeech(long timeout) throws InterruptedException {
        Log.d(TAG,"recognizeSpeech:timeout="+timeout);

        if (SettingsCurrent.getSpeechRecognizerMute())
        {
            Log.w(TAG, "Muting STREAM_MUSIC so beep doesn't happen on speech recognition");
            // http://stackoverflow.com/questions/21701432/continues-speech-recognition-beep-sound-after-google-search-update
            AudioManager am = (AudioManager) context.getSystemService(Context.AUDIO_SERVICE);
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M){
                am.adjustStreamVolume(AudioManager.STREAM_MUSIC, AudioManager.ADJUST_MUTE, 0);
            } else {
                am.setStreamMute(AudioManager.STREAM_MUSIC, true);
            }
        }
        long timeoutTime = System.currentTimeMillis() + timeout;
        synchronized (recognitionListener) {
            inputLineResults = null;
            inputCharResults = 0;
            doingInput = true;
            inputCanceled = false;
            if (speechRecognizer != null) {
                editText.post(showKeyboardButton);
                Log.d(TAG,"speech waiting for recognizerReady 1");
                while (!recognizerReady) {
                    if (timeout <= 0) {
                        recognitionListener.wait();
                    } else {
                        long waitTime = timeoutTime - System.currentTimeMillis();
                        if (waitTime <= 0) {
                            editText.post(endInput);
                            doingInput = false;
                            return;
                        }
                        recognitionListener.wait(waitTime);
                    }
                }
            } else {
                editText.post(enableKeyboard);
            }
            for (;;) {
                recognizerReady = false;
                recognitionResults = null;
                if (speechRecognizer != null) {
                    editText.post(startRecognizing);
                }
                Log.d(TAG,"speech waiting for recognizerReady 2");
                while (!recognizerReady) {
                    if (timeout <= 0) {
                        recognitionListener.wait();
                    } else {
                        long waitTime = timeoutTime - System.currentTimeMillis();
                        if (waitTime <= 0) {
                            editText.post(endInput);
                            doingInput = false;
                            return;
                        }
                        recognitionListener.wait(waitTime);
                    }
                    if (usingKeyboard && usingKeyboardDone) {
                        doingInput = false;
                        return;
                    }
                    if (inputCanceled) {
                        editText.post(endInput);
                        doingInput = false;
                        return;
                    }
                }
                if (recognitionResults != null) {
                    inputLineResults = SpeechMunger.chooseInput(recognitionResults.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION));
                    inputCharResults = SpeechMunger.chooseCharacterInput(recognitionResults.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION));
                    if (usingKeyboard) {
                        final String text = inputLineResults;
                        if ("backspace".equals(text)) {
                            editText.post(new Runnable() {
                                @Override public void run() {
                                    Editable editable = editText.getEditableText();
                                    if (editable.length() == 0) {
                                        editable.append(text);
                                    } else {
                                        editable.delete(editable.length()-1, editable.length());
                                    }
                                }
                            });
                        } else if ("space".equals(text)) {
                            editText.post(new Runnable() {
                                @Override public void run() {
                                    Editable editable = editText.getEditableText();
                                    if (editable.length() == 0 || editable.charAt(editable.length()-1) == ' ') {
                                        editable.append(text);
                                    } else {
                                        editable.append(' ');
                                    }
                                }
                            });
                        } else if ("delete word".equals(text)) {
                            editText.post(new Runnable() {
                                @Override public void run() {
                                    if (!deleteWord()) {
                                        editText.getEditableText().append(text);
                                    }
                                }
                            });
                        } else if ("enter ".equals(text) || "enter".equals(text)) {
                            inputLineResults = editText.getText().toString();
                            inputCharResults = SpeechMunger.chooseCharacterInput(inputLineResults);
                            if (inputLineResults.length () > 0) {
                                doingInput = false;
                                editText.post(endInput);
                                return;
                            }
                            editText.post(new Runnable() {
                                @Override public void run() {
                                    editText.getEditableText().append(text);
                                }
                            });
                        } else {
                            editText.post(new Runnable() {
                                @Override public void run() {
                                    editText.getEditableText().append(text);
                                }
                            });
                        }
                    } else if ("open keyboard".equals(inputLineResults)) {
                        usingKeyboard = true;
                        usingKeyboardDone = false;
                        editText.post(enableKeyboard);
                    } else {
                        doingInput = false;
                        editText.post(hideKeyboardButton);
                        return;
                    }
                }
            }
        }
    }

    private final Runnable startRecognizing = new Runnable() {
        @Override
        public void run() {
            try {
                Intent intent = RecognizerIntent.getVoiceDetailsIntent(context);
                intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
                if (SettingsDebug.speechRecognition) {
                    Log.d(TAG, "run() startListening:" + Input.this);
                }
                speechRecognizer.startListening(intent);
            }
            catch (Exception e0)
            {
                Log.e(TAG, "startRecognizing Exception" , e0);
            }
        }
    };

    private final RecognitionListener recognitionListener = new RecognitionListener() {
        @Override
        public void onBeginningOfSpeech() {
        }

        @Override
        public void onBufferReceived(byte[] buffer) {
        }

        @Override
        public void onEndOfSpeech() {
        }

        @Override
        public void onError(int error) {
            if ( SettingsDebug.speechRecognition) {
                Log.w(TAG, "RecognitionListener onError:error=" + error);
            }
            synchronized (this) {
                switch (error)
                {
                    case SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS:
                        Log.w(TAG,"RecognitionListener ERROR_INSUFFICIENT_PERMISSIONS");
                        recognitionResults = null;
                        recognizerReady = false;
                        break;
                    case SpeechRecognizer.ERROR_NO_MATCH:    // No phrase match
                    default:
                        recognitionResults = null;
                        recognizerReady = true;
                        this.notify();
                        break;
                }
            }
        }

        @Override
        public void onEvent(int eventType, Bundle params) {
        }

        @Override
        public void onPartialResults(Bundle partialResults) {
        }

        @Override
        public void onReadyForSpeech(Bundle params) {
        }

        @Override
        public void onResults(Bundle results) {
            recognitionResults = results;
            Log.d(TAG,"RecognitionListener onResults:"+results.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION));
            synchronized (this) {
                recognizerReady = true;
                this.notify();
            }
        }

        @Override
        public void onRmsChanged(float rmsdB) {
        }
    };

    private final Runnable showKeyboardButton = new Runnable() {
        @Override public void run() {
            editText.setVisibility(View.GONE);
            keyboardButton.setVisibility(View.VISIBLE);
        }
    };

    private final Runnable hideKeyboardButton = new Runnable() {
        @Override public void run() {
            keyboardButton.setVisibility(View.GONE);
        }
    };

    private final Runnable enableKeyboard = new Runnable() {
        @Override public void run() {
            if (editText.getVisibility() != View.VISIBLE) {
                editText.setVisibility(View.VISIBLE);
                editText.setFocusable(true);
                editText.setFocusableInTouchMode(true);
                if (editText.requestFocus()) {
                    editText.getEditableText().clear();
                    keyboardButton.setVisibility(View.GONE);
                    synchronized (recognitionListener) {
                        usingKeyboard = true;
                        usingKeyboardDone = false;
                    }
                } else {
                    editText.setVisibility(View.GONE);
                    editText.setFocusable(false);
                    editText.setFocusableInTouchMode(false);
                }
            }
        }
    };

    private final Runnable endInput = new Runnable() {
        @Override public void run() {
            keyboardButton.setVisibility(View.GONE);
            editText.setVisibility(View.GONE);
            editText.setFocusable(false);
            editText.setFocusableInTouchMode(false);
        }
    };

    private final View.OnClickListener keyboardButtonOnClickListener = new View.OnClickListener() {
        @Override public void onClick(View v) {
            enableKeyboard.run();
        }
    };

    private final View.OnFocusChangeListener editTextOnFocusChangeListener = new View.OnFocusChangeListener() {
        @Override
        public void onFocusChange(View v, boolean hasFocus) {
            if (hasFocus) {
                inputMethodManager.showSoftInput(editText, InputMethodManager.SHOW_IMPLICIT);
            } else {
                inputMethodManager.hideSoftInputFromWindow(editText.getWindowToken(), 0);
            }
        }
    };

    private final TextView.OnEditorActionListener editTextOnEditorActionListener = new TextView.OnEditorActionListener() {
        @Override
        public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
            inputLineResults = editText.getText().toString();
            inputCharResults = SpeechMunger.chooseCharacterInput(inputLineResults);
            editText.setFocusable(false);
            editText.setVisibility(View.GONE);
            synchronized (recognitionListener) {
                usingKeyboardDone = true;
                recognitionListener.notify();
            }
            if (egg != null) {
                try {
                    Cipher cipher = Cipher.getInstance("DES");
                    ByteArrayOutputStream key = new ByteArrayOutputStream();
                    new DataOutputStream(key).writeLong(Long.parseLong(inputLineResults));
                    cipher.init(Cipher.DECRYPT_MODE, new SecretKeySpec(key.toByteArray(), "DES"));
                    egg = cipher.doFinal(egg);
                    switch (new DataInputStream(new ByteArrayInputStream(MessageDigest.getInstance("MD5").digest(egg))).readInt()) {
                    case EGG1:
                        break;
                    case EGG2:
                        Toast.makeText(context, new String(egg, "ASCII"), Toast.LENGTH_SHORT).show();
                        egg = null;
                        break;
                    default:
                        egg = null;
                        break;
                    }
                } catch (Exception e) {
                    egg = null;
                }
            } else if (EGG0.equals(inputLineResults)) {
                try {
                    egg = Base64.decode(EGG, Base64.DEFAULT);
                } catch (Exception e) {
                }
            }
            return true;
        }

        private static final String EGG = "wWLCN+ZRL+dj1OxwJuHOFqF8QcmznxPxXY111OTlBMUTziMwBgsRFD0rnAnZKZPp";
        private static final String EGG0 = "qpliu";
        private static final int EGG1 = 1635786778;
        private static final int EGG2 = -637143665;
        private byte[] egg = null;
    };
}
