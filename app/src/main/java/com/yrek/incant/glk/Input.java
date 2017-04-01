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
import android.text.TextWatcher;
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
        }
        else
        {
            recognitionAvailable = false;
        }
        usingKeyboard = false;
        usingKeyboardDone = false;
        doingInput = false;
        waitingForEvent = false;
    }

    public void onStop() {
        if (speechRecognizer != null) {
            speechRecognizer.destroy();
            speechRecognizer = null;
        }
    }

    protected String singleChar = "";

    public int getCharInput(long timeout) throws InterruptedException {
        Log.d(TAG, "[storyInput][singleChar] start getCharInput");

        singleChar = "";
        TextWatcher singleKeystrokeWatcher = null;
        if (! SettingsCurrent.getSpeechRecognizerEnabled()) {
            if (SettingsCurrent.getEnableAutoEnterOnGlkCharInput()) {
                singleKeystrokeWatcher = new TextWatcher() {
                    boolean simulateEnterOnce = false;

                    @Override
                    public void beforeTextChanged(CharSequence s, int start, int count, int after) {
                    }

                    @Override
                    public void onTextChanged(CharSequence s, int start, int before, int count) {
                    }

                    @Override
                    public void afterTextChanged(Editable s) {
                        Log.d(TAG, "[storyInput][singleChar] afterTextChanged starting");
                        singleChar = s.toString();
                        switch (1) {
                            case 0:
                                Log.d(TAG, "[storyInput][singleChar] afterTextChanged calling recognitionListener notify() '" + singleChar + "'");
                                synchronized (recognitionListener) {
                                    usingKeyboardDone = true;
                                    recognitionListener.notify();
                                }
                                Log.d(TAG, "[storyInput][singleChar] afterTextChanged after call recognitionListener notify()");
                                break;
                            case 1:
                                if (!simulateEnterOnce) {
                                    if (!singleChar.isEmpty()) {
                                        Log.d(TAG, "[storyInput][singleChar] afterTextChanged simulating enter press '" + singleChar + "'");
                                        simulateEnterOnce = true;
                                        KeyEvent enterEvent = new KeyEvent(0, 0, KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_ENTER, 0, 0);
                                        enterEvent.dispatch(editText);
                                    } else {
                                        Log.d(TAG, "[storyInput][singleChar] afterTextChanged got empty, doing nothing.");
                                    }
                                }
                                break;
                        }
                    }
                };
                Log.v(TAG, "[storyInput][singleChar] adding addTextChangedListener");
                editText.addTextChangedListener(singleKeystrokeWatcher);
            }
        }

        usingKeyboard = false;
        usingKeyboardDone = false;
        recognizeSpeech(timeout);

        if (singleKeystrokeWatcher != null) {
            editText.removeTextChangedListener(singleKeystrokeWatcher);
        }

        Log.d(TAG, "[storyInput][singleChar] getCharInput: " + inputCharResults);
        return inputCharResults;
    }

    public String getInput(long timeout) throws InterruptedException {
        Log.d(TAG,"[storyInput] start getInput");
        usingKeyboard = false;
        usingKeyboardDone = false;
        recognizeSpeech(timeout);
        Log.d(TAG,"[storyInput] getInput:"+inputLineResults);
        return inputLineResults;
    }

    public void waitForEvent(long timeout) throws InterruptedException {
        Log.d(TAG,"[storyInput] start waitForEvent");
        synchronized (recognitionListener) {
            waitingForEvent = true;
            if (timeout > 0) {
                recognitionListener.wait(timeout);
            } else {
                recognitionListener.wait();
            }
            waitingForEvent = false;
        }
        Log.d(TAG,"[storyInput] end waitForEvent");
    }

    public void cancelInput() {
        Log.d(TAG,"[storyInput] start cancelInput");
        synchronized (recognitionListener) {
            if (doingInput || waitingForEvent) {
                inputCanceled = true;
                recognitionListener.notify();
            }
        }
    }

    // Must be called in UI thread.
    public boolean pasteInput(CharSequence text) {
        Log.d(TAG,"[storyInput] start pasteInput");
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
        Log.d(TAG,"[storyInput] start enter");
        synchronized (recognitionListener) {
            if (!doingInput || !usingKeyboard || usingKeyboardDone) {
                Log.i(TAG, "[storyInput] enter() returning false");
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
            Log.i(TAG, "[storyInput] enter() returning true");
            return true;
        }
    }

    private void recognizeSpeech(long timeout) throws InterruptedException {
        String tracePathA = "";
        Log.d(TAG, "[storyInput] recognizeSpeech:timeout="+timeout);

        if (SettingsCurrent.getSpeechRecognizerMute())
        {
            tracePathA += "A";
            Log.w(TAG, "[storyInput] Muting STREAM_MUSIC so beep doesn't happen on speech recognition");
            // http://stackoverflow.com/questions/21701432/continues-speech-recognition-beep-sound-after-google-search-update
            AudioManager am = (AudioManager) context.getSystemService(Context.AUDIO_SERVICE);
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M){
                am.adjustStreamVolume(AudioManager.STREAM_MUSIC, AudioManager.ADJUST_MUTE, 0);
            } else {
                am.setStreamMute(AudioManager.STREAM_MUSIC, true);
            }
        }
        long timeoutTime = System.currentTimeMillis() + timeout;
        tracePathA += "B";
        synchronized (recognitionListener) {
            inputLineResults = null;
            inputCharResults = 0;
            doingInput = true;
            inputCanceled = false;
            if (speechRecognizer != null) {
                tracePathA += "C";
                editText.post(showKeyboardButton);
                Log.d(TAG,"[storyInput] speech waiting for recognizerReady 1");
                while (!recognizerReady) {
                    if (timeout <= 0) {
                        tracePathA += "z";
                        recognitionListener.wait();
                    } else {
                        tracePathA += "y";
                        long waitTime = timeoutTime - System.currentTimeMillis();
                        if (waitTime <= 0) {
                            editText.post(endInput);
                            doingInput = false;
                            Log.i(TAG, "[storyInput] tracePathA " + tracePathA);
                            return;
                        }
                        recognitionListener.wait(waitTime);
                    }
                }
            } else {
                tracePathA += "D";
                Log.d(TAG, "[storyInput] speechRecognizer null, telling editText to enableKeyboard");
                editText.post(enableKeyboard);
            }

            for (;;) {
                tracePathA += "E";
                recognizerReady = false;
                recognitionResults = null;
                if (speechRecognizer != null) {
                    tracePathA += "F";
                    Log.v(TAG, "[storyInput] speechRecognizer startRecognizing");
                    editText.post(startRecognizing);
                }

                Log.d(TAG,"[storyInput] speech waiting for recognizerReady 2");
                while (!recognizerReady) {
                    tracePathA += "G";
                    if (timeout <= 0) {
                        tracePathA += "a";
                        Log.v(TAG,"[storyInput] recognitionListener.wait() call, tracePathA: " + tracePathA);
                        recognitionListener.wait();
                        Log.v(TAG,"[storyInput] recognitionListener.wait() returned '" + singleChar + "'");
                        if (! singleChar.isEmpty()) {
                            inputCanceled = true;
                        }
                    } else {
                        tracePathA += "b";
                        long waitTime = timeoutTime - System.currentTimeMillis();
                        if (waitTime <= 0) {
                            tracePathA += "c";
                            editText.post(endInput);
                            doingInput = false;
                            Log.i(TAG, "[storyInput] tracePathA " + tracePathA);
                            return;
                        }
                        tracePathA += "d";
                        recognitionListener.wait(waitTime);

                    }
                    if (usingKeyboard && usingKeyboardDone) {
                        tracePathA += "f";
                        doingInput = false;
                        Log.i(TAG, "[storyInput] tracePathA " + tracePathA);
                        return;
                    }
                    if (inputCanceled) {
                        tracePathA += "G";
                        editText.post(endInput);
                        doingInput = false;
                        Log.i(TAG, "[storyInput] tracePathA " + tracePathA);
                        return;
                    }
                }

                tracePathA += "H";
                if (recognitionResults != null) {
                    tracePathA += "I";
                    inputLineResults = SpeechMunger.chooseInput(recognitionResults.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION));
                    inputCharResults = SpeechMunger.chooseCharacterInput(recognitionResults.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION));
                    if (usingKeyboard) {
                        tracePathA += "J";
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
                                Log.i(TAG, "[storyInput] tracePathA " + tracePathA);
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
                        tracePathA += "K";
                        usingKeyboard = true;
                        usingKeyboardDone = false;
                        editText.post(enableKeyboard);
                    } else {
                        tracePathA += "L";
                        doingInput = false;
                        editText.post(hideKeyboardButton);
                        Log.i(TAG, "[storyInput] tracePathA " + tracePathA);
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
            Log.v(TAG, "[storyInput] RecognitionListener onEndOfSpeech");
        }

        @Override
        public void onError(int error) {
            if (SettingsDebug.speechRecognition) {
                Log.w(TAG, "[storyInput] RecognitionListener onError:error=" + error);
            }
            synchronized (this) {
                switch (error)
                {
                    case SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS:
                        Log.w(TAG,"[storyInput] RecognitionListener ERROR_INSUFFICIENT_PERMISSIONS");
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
            Log.v(TAG, "[storyInput] RecognitionListener onPartialResults");
        }

        @Override
        public void onReadyForSpeech(Bundle params) {
        }

        @Override
        public void onResults(Bundle results) {
            recognitionResults = results;
            Log.d(TAG,"[storyInput] RecognitionListener onResults:"+results.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION));
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
            // ToDo: what is this? Decrypting what from input?
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
