package com.yrek.incant.glk;

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Parcel;
import android.util.Log;
import android.util.LruCache;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.RelativeLayout;
import android.widget.Toast;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.Serializable;
import java.net.URLEncoder;
import java.util.HashMap;
import java.util.HashSet;

import com.yrek.ifstd.blorb.Blorb;
import com.yrek.ifstd.glk.Glk;
import com.yrek.ifstd.glk.GlkByteArray;
import com.yrek.ifstd.glk.GlkDispatch;
import com.yrek.ifstd.glk.GlkEvent;
import com.yrek.ifstd.glk.GlkFile;
import com.yrek.ifstd.glk.GlkGestalt;
import com.yrek.ifstd.glk.GlkIntArray;
import com.yrek.ifstd.glk.GlkSChannel;
import com.yrek.ifstd.glk.GlkStream;
import com.yrek.ifstd.glk.GlkStreamMemory;
import com.yrek.ifstd.glk.GlkStreamMemoryUnicode;
import com.yrek.ifstd.glk.GlkWindow;
import com.yrek.ifstd.glk.UnicodeString;
import com.yrek.incant.gamemeta.EventDebugOutput;
import com.yrek.runconfig.BundleHelper;
import com.yrek.runconfig.SettingsCurrent;
import com.yrek.runconfig.StaticGameHold;

import org.greenrobot.eventbus.EventBus;

public class GlkActivity extends Activity {
    public static final String TAG = GlkActivity.class.getSimpleName();
    public static final String GLK_MAIN = "GLK_MAIN";
    private static final String SUSPEND_STATE = "SUSPEND_STATE";
    private static final String SUSPEND_ACTIVITY_STATE = "SUSPEND_ACTIVITY_STATE";
    private static final String SUSPEND_METHOD = "SUSPEND_METHOD";
    GlkMain main;
    private GlkDispatch glkDispatch;
    private Serializable suspendState;
    private transient boolean suspendedDuringInit;
    private transient boolean suspending;

    private transient boolean profilerFlag = false;
    private transient long profilerTime = 0L;

    private View progressBar;
    int progressBarCounter = 0;
    private FrameLayout frameLayout;
    int charWidth = 0;
    int charHeight = 0;
    int charHMargin = 0;
    int charVMargin = 0;
    transient Input input;
    transient Speech speech;
    private long lastTimerEvent = 0L;
    private long timerInterval = 0L;
    private boolean pendingArrangeEvent = false;
    private boolean pendingRedrawEvent = false;

    private Object ioLock = new Object();
    private boolean outputPending = false;

    ActivityState activityState = new ActivityState();

    private LruCache<Integer,Bitmap> imageResourceCache = new LruCache<Integer,Bitmap>(5);

    static class ActivityState implements Serializable {
        Window rootWindow;
        GlkStream currentStream;

        HashMap<Integer,Integer> foregroundColorHint = new HashMap<Integer,Integer>();
        HashMap<Integer,Integer> backgroundColorHint = new HashMap<Integer,Integer>();
        HashSet<Integer> reverseHint = new HashSet<Integer>();

        HashMap<Integer,GlkWindow> suspendWindows = null;
        HashMap<Integer,GlkStream> suspendStreams = null;
        HashMap<Integer,GlkFile> suspendFiles = null;
        HashMap<Integer,GlkSChannel> suspendSChannels = null;
    }


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        Log.d(TAG,"onCreate");
        super.onCreate(savedInstanceState);
        main = (GlkMain) getIntent().getSerializableExtra(GLK_MAIN);

        setContentView(main.getGlkLayout());

        if (SettingsCurrent.getGameLayoutPaddingA())
        {
            RelativeLayout holder = (RelativeLayout) findViewById(main.getHolderLayout());
            holder.setPadding(30, 0, 30, 5);
        }

        progressBar = findViewById(main.getProgressBar());
        frameLayout = (FrameLayout) findViewById(main.getFrameLayout());
        Log.d(TAG,"frameLayout="+frameLayout+",childCount="+frameLayout.getChildCount());
        input = new Input(this, (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE), (Button) findViewById(main.getKeyboardButton()), (EditText) findViewById(main.getEditText()));
        speech = new Speech(this, (Button) findViewById(main.getSkipButton()));
        findViewById(main.getOneByOneMeasurer()).addOnLayoutChangeListener(textMeasurer);
        findViewById(main.getTwoByTwoMeasurer()).addOnLayoutChangeListener(textMeasurer);

        if (SettingsCurrent.getGameLayoutInputColorA())
        {
            View editTextA = (EditText) findViewById(main.getEditText());
            editTextA.setBackgroundColor(Color.parseColor("#F8EE78"));
            editTextA.setAlpha(0.5f);
        }

        activityState.rootWindow = null;
        if (savedInstanceState != null) {
            int suspendMethod = savedInstanceState.getInt(SUSPEND_METHOD, 0);
            ActivityState tempActivityState = null;
            switch (suspendMethod)
            {
                case 1:
                    suspendState = savedInstanceState.getSerializable(SUSPEND_STATE);
                    tempActivityState = (ActivityState) savedInstanceState.getSerializable(SUSPEND_ACTIVITY_STATE);
                    Log.i(TAG, "savedInstanceState used method 1");
                    break;
                case 2:
                    try {
                        // http://stackoverflow.com/questions/14256809/save-bundle-to-file
                        Parcel parcel = Parcel.obtain();
                        try {
                            FileInputStream fis = new FileInputStream(SettingsCurrent.getSaveInstanceToDiskFileA());
                            byte[] array = new byte[(int) fis.getChannel().size()];
                            fis.read(array, 0, array.length);
                            fis.close();
                            parcel.unmarshall(array, 0, array.length);
                            parcel.setDataPosition(0);
                            Bundle readBundle = parcel.readBundle();
                            Log.i(TAG, "savedInstanceState method 2 " + readBundle.toString());
                            readBundle.putAll(readBundle);

                            suspendState = readBundle.getSerializable(SUSPEND_STATE);
                            tempActivityState = (ActivityState) readBundle.getSerializable(SUSPEND_ACTIVITY_STATE);
                        } catch (FileNotFoundException fnfe) {
                            Log.e(TAG, "FileNotFoundException on bundle read from disk" + fnfe);
                        } catch (IOException e) {
                            Log.e(TAG, "IOException on bundle read from disk" + e);
                        } finally {
                            parcel.recycle();
                        }
                    }
                    catch (Exception e)
                    {
                        Log.e(TAG, "Exception on bundle read from disk", e);
                    }
                    break;
                case 3:
                    SharedPreferences save = getPreferences(Context.MODE_PRIVATE);
                    Bundle readBundle = BundleHelper.loadPreferencesBundle(save, "game0");

                    suspendState = readBundle.getSerializable(SUSPEND_STATE);
                    tempActivityState = (ActivityState) readBundle.getSerializable(SUSPEND_ACTIVITY_STATE);

                    Log.i(TAG, "used method 3 of bundle");
                    break;
                case 4:
                    if (StaticGameHold.holdGameBundle != null)
                    {
                        suspendState = StaticGameHold.holdGameBundle.getSerializable(SUSPEND_STATE);
                        tempActivityState = (ActivityState) StaticGameHold.holdGameBundle.getSerializable(SUSPEND_ACTIVITY_STATE);
                        StaticGameHold.holdGameBundle = null;
                        System.gc();
                        Log.i(TAG, "used method 4 of bundle");
                    }
                    break;
            }

            if (tempActivityState != null) {
                activityState = tempActivityState;
                if (activityState.rootWindow != null) {
                    activityState.rootWindow.restoreActivity(this);
                    if (frameLayout.getChildCount() == 1) {
                        activityState.rootWindow.restoreView(frameLayout.getChildAt(0));
                    } else {
                        frameLayout.addView(activityState.rootWindow.restoreView(null));
                    }
                }
                if (activityState.suspendSChannels != null) {
                    for (GlkSChannel channel : activityState.suspendSChannels.values()) {
                        ((SChannel) channel).restoreActivity(this);
                    }
                }
            }
            else
            {
                Log.e(TAG, "activityState is null in onCreate");
            }
        }

        glkDispatch = new GlkDispatch(glk, activityState.suspendWindows, activityState.suspendStreams, activityState.suspendFiles, activityState.suspendSChannels);
        activityState.suspendWindows = null;
        activityState.suspendStreams = null;
        activityState.suspendFiles = null;
        activityState.suspendSChannels = null;

        main.init(this, glkDispatch, suspendState);
        suspendState = null;
        Log.d(TAG,"speech="+speech+",input="+input+",this="+this);
    }


    /*
    https://code.google.com/p/android/issues/detail?id=212316
    https://developer.android.com/about/versions/nougat/android-7.0-changes.html#other

    "Many platform APIs have now started checking for large payloads being sent across Binder transactions, and the system now rethrows TransactionTooLargeExceptions as RuntimeExceptions, instead of silently logging or suppressing them. One common example is storing too much data in Activity.onSaveInstanceState(), which causes ActivityThread.StopInfo to throw a RuntimeException when your app targets Android 7.0."
    */
    @Override
    protected void onSaveInstanceState(Bundle outState) {
        Log.d(TAG,"onSaveInstanceState");
        activityState.suspendWindows = new HashMap<Integer,GlkWindow>();
        activityState.suspendStreams = new HashMap<Integer,GlkStream>();
        activityState.suspendFiles = new HashMap<Integer,GlkFile>();
        activityState.suspendSChannels = new HashMap<Integer,GlkSChannel>();
        glkDispatch.saveToMap(activityState.suspendWindows, activityState.suspendStreams, activityState.suspendFiles, activityState.suspendSChannels);

        if (SettingsCurrent.getSaveInstanceToDiskB() > 0)
        {
            Bundle tempBundle = new Bundle();
            tempBundle.putSerializable(SUSPEND_STATE, suspendState);
            tempBundle.putSerializable(SUSPEND_ACTIVITY_STATE, activityState);

            switch (SettingsCurrent.getSaveInstanceToDiskB()) {
                case 2:
                    try {

                        File outputFile = SettingsCurrent.getSaveInstanceToDiskFileA();
                        FileOutputStream fos = new FileOutputStream(outputFile);
                        Parcel p = Parcel.obtain(); //creating empty parcel object
                        tempBundle.writeToParcel(p, 0); //saving bundle as parcel
                        fos.write(p.marshall()); //writing parcel to file
                        fos.flush();
                        fos.close();
                        Log.i(TAG, "onSaveInstanceState toDisk " + outputFile + " size " + outputFile.length());
                        outState.putInt(SUSPEND_METHOD, 2);
                    } catch (IOException e) {
                        Log.e(TAG, "IOExcepiton on bundle save to disk" + e);
                        outState.putInt(SUSPEND_METHOD, 0);
                    }
                    break;
                case 3:
                    SharedPreferences save = getPreferences(Context.MODE_PRIVATE);
                    SharedPreferences.Editor ed = save.edit();

                    BundleHelper.savePreferencesBundle(ed, "game0", tempBundle);
                    outState.putInt(SUSPEND_METHOD, 3);
                    ed.commit();
                    Log.i(TAG, "just onSaveInstanceState to SharedPrefs used method 3");
                    break;
                case 4:
                    StaticGameHold.holdGameBundle = tempBundle;
                    outState.putInt(SUSPEND_METHOD, 4);
                    break;
            }
        }
        else {
            // TransactionTooLargeException on "Molly and the Butter Theives" when change away from Activity. data parcel size 936576 bytes
            // "Rovers day out"              data parcel size 1093916 bytes
            // "Wisher, Theurgist, Fatalist" data parcel size  796916 bytes
            // "Cheesed Off"                 data parcel size  644084 bytes
            outState.putInt(SUSPEND_METHOD, 1);
            outState.putSerializable(SUSPEND_STATE, suspendState);
            outState.putSerializable(SUSPEND_ACTIVITY_STATE, activityState);
        }
        Log.d(TAG,"onSaveInstanceState::end " + outState.size());
    }

    @Override
    protected void onStart() {
        Log.d(TAG,"onStart:frameLayout="+frameLayout);
        super.onStart();
        input.onStart();
        speech.onStart();
        if (activityState.rootWindow != null) {
            pendingArrangeEvent = true;
            pendingRedrawEvent = true;
        }
    }

    @Override
    protected void onStop() {
        Log.d(TAG,"onStop");
        super.onStop();
        input.onStop();
        speech.onStop();
    }

    @Override
    protected void onResume() {
        Log.d(TAG,"onResume:frameLayout="+frameLayout);
        super.onResume();
        showProgressBar();
        suspendedDuringInit = false;
        suspending = false;
        main.start(new Runnable() {
            @Override public void run() {
                try {
                    speech.waitForInit();
                    waitForTextMeasurer();
                } catch (InterruptedException e) {
                    suspendedDuringInit = true;
                }
            }
        }, new Runnable() {
            @Override public void run() {
                post(new Runnable() {
                    @Override public void run() {
                        if (!suspending) {
                            finish();
                        }
                    }
                });
            }
        });
        for (GlkSChannel schannel : glkDispatch.sChannelList()) {
            ((SChannel) schannel).onResume();
        }
    }

    @Override
    protected void onPause() {
        Log.d(TAG,"onPause:main.finished()="+main.finished()+",suspendedDuringInit="+suspendedDuringInit);
        super.onPause();
        if (!suspendedDuringInit && !main.finished()) {
            suspending = true;
            main.requestSuspend();
            pendingArrangeEvent = true;
            suspendState = main.suspend();
        }
        for (GlkSChannel schannel : glkDispatch.sChannelList()) {
            ((SChannel) schannel).onPause();
        }
    }

    void post(Runnable runnable) {
        frameLayout.post(runnable);
    }

    void showProgressBar() {
        final int counter = progressBarCounter;
        frameLayout.postDelayed(new Runnable() {
            @Override public void run() {
                if (counter == progressBarCounter) {
                    progressBarCounter++;
                    progressBar.setVisibility(View.VISIBLE);
                }
            }
        }, 2000L);
    }

    final Runnable hideProgressBar = new Runnable() {
        @Override public void run() {
            progressBarCounter++;
            progressBar.setVisibility(View.GONE);
        }
    };

    void hideProgressBar() {
        post(hideProgressBar);
    }

    Integer getStyleColor(int winType, int style, boolean foreground) {
        if (foreground) {
            Integer color = activityState.foregroundColorHint.get(winType + (style << 8));
            if (color != null) {
                return color;
            }
            color = activityState.foregroundColorHint.get(style << 8);
            if (color != null) {
                return color;
            }
            return main.getStyleForegroundColor(style);
        } else {
            Integer color = activityState.backgroundColorHint.get(winType + (style << 8));
            if (color != null) {
                return color;
            }
            color = activityState.backgroundColorHint.get(style << 8);
            if (color != null) {
                return color;
            }
            return main.getStyleBackgroundColor(style);
        }
    }

    Integer getStyleForegroundColor(int winType, int style) {
        if (!activityState.reverseHint.contains(winType + (style << 8)) && !activityState.reverseHint.contains(style << 8)) {
            return getStyleColor(winType, style, true);
        } else {
            return getStyleColor(winType, style, false);
        }
    }

    Integer getStyleBackgroundColor(int winType, int style) {
        if (!activityState.reverseHint.contains(winType + (style << 8)) && !activityState.reverseHint.contains(style << 8)) {
            return getStyleColor(winType, style, false);
        } else {
            return getStyleColor(winType, style, true);
        }
    }

    private class Exit extends RuntimeException {}

    private final Glk glk = new Glk() {
        @Override
        public void main(Runnable runMain) throws IOException {
            boolean exited = false;
            Log.d(TAG,"main:suspendedDuringInit="+suspendedDuringInit);
            if (suspendedDuringInit) {
                return;
            }

            if (runMain == null) {
                return;
            }

            try {
                runMain.run();
            } catch (Exit e) {
                Log.i(TAG, "exit game");
                exited = true;
            } catch (Exception e)
            {
                Log.e(TAG, "Exception in Glk main ", e);
            }

            Log.d(TAG,"main:exited="+exited+",main.finished="+main.finished());
            if (exited || main.finished()) {
                post(new Runnable() {
                    @Override public void run() {
                        finish();
                    }
                });
            }
        }

        @Override
        public void exit() {
            throw new Exit();
        }

        @Override
        public void setInterruptHandler(Runnable handler) {
        }

        @Override
        public void tick() {
        }


        @Override
        public int gestalt(int selector, int value) {
            switch (selector) {
            case GlkGestalt.Version:
                return Glk.GlkVersion;
            case GlkGestalt.CharInput:
                if (value == 10 || (value >= 32 && value < 127) || (value <= -2 && value >= -13) || (value <= -17 && value >= -28)) {
                    return 1;
                }
                return 0;
            case GlkGestalt.LineInput:
                return value >= 32 && value < 127 ? 1 : 0;
            case GlkGestalt.CharOutput:
                return gestaltExt(selector, value, null);
            case GlkGestalt.MouseInput:
                return 1;
            case GlkGestalt.Timer:
                return 1;
            case GlkGestalt.Graphics:
                return 1;
            case GlkGestalt.DrawImage:
                switch (value) {
                case GlkWindow.TypeGraphics:
                case GlkWindow.TypeTextBuffer:
                    return 1;
                default:
                    return 0;
                }
            case GlkGestalt.Sound:
            case GlkGestalt.SoundVolume:
            case GlkGestalt.SoundNotify:
                return 1;
            case GlkGestalt.Hyperlinks:
            case GlkGestalt.HyperlinkInput:
                return 1;
            case GlkGestalt.SoundMusic:
                // Six.gblorb won't play sounds if this is false
                return 1;
            case GlkGestalt.GraphicsTransparency:
                return 1;
            case GlkGestalt.Unicode:
                return 1;
            case GlkGestalt.UnicodeNorm:
                return 0;
            case GlkGestalt.LineInputEcho:
                return 1;
            case GlkGestalt.LineTerminators:
            case GlkGestalt.LineTerminatorKey:
                return 0;
            case GlkGestalt.DateTime:
                return 1;
            case GlkGestalt.Sound2:
                return 0;
            case GlkGestalt.ResourceStream:
                return 1;
            default:
                return 0;
            }
        }

        @Override
        public int gestaltExt(int selector, int value, GlkIntArray array) {
            switch (selector) {
            case GlkGestalt.CharOutput:
                if (value < 256 && (value == 10 || !Character.isISOControl(value))) {
                    if (array != null) {
                        array.setIntElement(1);
                    }
                    return GlkGestalt.CharOutput_ExactPrint;
                } else {
                    if (Character.isValidCodePoint(value)) {
                        if (array != null) {
                            array.setIntElement(1);
                        }
                        return GlkGestalt.CharOutput_ApproxPrint;
                    } else {
                        if (array != null) {
                            array.setIntElement(0);
                        }
                        return GlkGestalt.CharOutput_CannotPrint;
                    }
                }
            default:
                return 0;
            }
        }


        @Override
        public GlkWindow windowGetRoot() {
            return activityState.rootWindow;
        }

        @Override
        public GlkWindow windowOpen(GlkWindow split, int method, int size, int winType, int rock) {
            Log.i(TAG, "windowOpen ");
            Window window = Window.open(GlkActivity.this, (Window) split, method, size, winType, rock);
            if (window != null && activityState.rootWindow == split) {
                activityState.rootWindow = window.parent;
                if (activityState.rootWindow == null) {
                    activityState.rootWindow = window;
                }
            }
            return glkDispatch.add(window);
        }


        @Override
        public void setWindow(GlkWindow window) {
            if (window == null) {
                activityState.currentStream = null;
            } else {
                activityState.currentStream = window.getStream();
            }
        }


        @Override
        public GlkStream streamOpenFile(GlkFile file, int mode, int rock) throws IOException {
            if (mode == GlkFile.ModeRead && !file.exists()) {
                return null;
            }
            return glkDispatch.add(new StreamFile((FileRef) file, mode, false, rock));
        }

        @Override
        public GlkStream streamOpenFileUni(GlkFile file, int mode, int rock) throws IOException {
            if (mode == GlkFile.ModeRead && !file.exists()) {
                return null;
            }
            return glkDispatch.add(new StreamFile((FileRef) file, mode, true, rock));
        }

        @Override
        public GlkStream streamOpenMemory(GlkByteArray memory, int mode, int rock) {
            return glkDispatch.add(new GlkStreamMemory(memory, rock));
        }

        @Override
        public GlkStream streamOpenMemoryUni(GlkIntArray memory, int mode, int rock) {
            return glkDispatch.add(new GlkStreamMemoryUnicode(memory, rock));
        }

        @Override
        public GlkStream streamOpenResource(int resourceId, int rock) throws IOException {
            File file = getResourceFile(resourceId);
            if (file == null) {
                return null;
            }
            return glkDispatch.add(new StreamFile(file, GlkFile.ModeRead, false, rock));
        }

        @Override
        public GlkStream streamOpenResourceUni(int resourceId, int rock) throws IOException {
            File file = getResourceFile(resourceId);
            if (file == null) {
                return null;
            }
            return glkDispatch.add(new StreamFile(file, GlkFile.ModeRead, false, rock));
        }

        @Override
        public void streamSetCurrent(GlkStream stream) {
            activityState.currentStream = stream;
        }

        @Override
        public GlkStream streamGetCurrent() {
            return activityState.currentStream;
        }

        @Override
        public void putChar(int ch) throws IOException {
            if (activityState.currentStream != null) {
                activityState.currentStream.putChar(ch);
            }
        }

        @Override
        public void putString(CharSequence string) throws IOException {
            if (activityState.currentStream != null) {
                Log.d(TAG, "FIND_OUTPUT_AA0 " + string.toString());
                activityState.currentStream.putString(string);
            }
        }

        @Override
        public void putBuffer(GlkByteArray buffer) throws IOException {
            if (activityState.currentStream != null) {
                activityState.currentStream.putBuffer(buffer);
            }
        }

        @Override
        public void putCharUni(int ch) throws IOException {
            if (activityState.currentStream != null) {
                activityState.currentStream.putCharUni(ch);
            }
        }

        @Override
        public void putStringUni(UnicodeString string) throws IOException {
            if (activityState.currentStream != null) {
                Log.d(TAG, "FIND_OUTPUT_AA " + string.toString());
                activityState.currentStream.putStringUni(string);
            }
        }

        @Override
        public void putBufferUni(GlkIntArray buffer) throws IOException {
            if (activityState.currentStream != null) {
                activityState.currentStream.putBufferUni(buffer);
            }
        }

        @Override
        public void setStyle(int style) {
            if (activityState.currentStream != null) {
                activityState.currentStream.setStyle(style);
            }
        }

        @Override
        public void setHyperlink(int linkVal) {
            if (activityState.currentStream != null) {
                activityState.currentStream.setHyperlink(linkVal);
            }
        }


        @Override
        public void styleHintSet(int winType, int style, int hint, int value) {
            switch (hint) {
            case GlkStream.StyleHintTextColor:
                activityState.foregroundColorHint.put(winType + (style << 8), value);
                break;
            case GlkStream.StyleHintBackColor:
                activityState.backgroundColorHint.put(winType + (style << 8), value);
                break;
            case GlkStream.StyleHintReverseColor:
                if (value != 0) {
                    activityState.reverseHint.add(winType + (style << 8));
                } else {
                    activityState.reverseHint.remove(winType + (style << 8));
                }
                break;
            default:
                break;
            }
        }

        @Override
        public void styleHintClear(int winType, int style, int hint) {
            switch (hint) {
            case GlkStream.StyleHintTextColor:
                activityState.foregroundColorHint.remove(winType + (style << 8));
                break;
            case GlkStream.StyleHintBackColor:
                activityState.backgroundColorHint.remove(winType + (style << 8));
                break;
            case GlkStream.StyleHintReverseColor:
                activityState.reverseHint.remove(winType + (style << 8));
                break;
            default:
                break;
            }
        }


        @Override
        public GlkFile fileCreateTemp(int usage, int rock) throws IOException {
            return glkDispatch.add(new FileRef(File.createTempFile("glk.",".tmp"), usage, GlkFile.ModeReadWrite, rock));
        }

        @Override
        public GlkFile fileCreateByName(int usage, CharSequence name, int rock) throws IOException {
            return glkDispatch.add(new FileRef(new File(main.getDir(GlkActivity.this), URLEncoder.encode("glk."+name, "UTF-8")), usage, GlkFile.ModeReadWrite, rock));
        }

        @Override
        public GlkFile fileCreateByPrompt(int usage, int mode, int rock) throws IOException {
            if (usage == GlkFile.UsageSavedGame) {
                File targetFile;
                if (mode == 2) {
                    targetFile = main.getRestoreFile(GlkActivity.this);
                    post(new Runnable() {
                        @Override public void run() {
                            Toast.makeText(GlkActivity.this, main.getRestoreFileMessage(), Toast.LENGTH_LONG).show();
                            // Toast.makeText(GlkActivity.this, "Profile " + String.valueOf(dt), Toast.LENGTH_LONG).show();
                        }
                    });
                } else {
                    targetFile = main.getSaveFile(GlkActivity.this);
                }
                Log.d(TAG, "GSF call getSaveFile C usage " + usage + " mode " + mode + " targetFile " + targetFile.getPath());
                return glkDispatch.add(new FileRef(targetFile, usage, mode, rock));
            }
            throw new RuntimeException("unimplemented");
        }

        @Override
        public GlkFile fileCreateFromFile(int usage, GlkFile file, int rock) throws IOException {
            throw new RuntimeException("unimplemented");
        }


        @Override
        public GlkSChannel sChannelCreate(int rock) throws IOException {
            Log.d(TAG,"sChannelCreate:rock="+rock);
            return glkDispatch.add(new SChannel(rock, GlkActivity.this));
        }

        @Override
        public GlkSChannel sChannelCreateExt(int rock, int volume) throws IOException {
            Log.d(TAG,"sChannelCreateExt:rock="+rock+",volume="+volume);
            SChannel schannel = new SChannel(rock, GlkActivity.this);
            schannel.setVolume(volume);
            return glkDispatch.add(schannel);
        }

        @Override
        public int sChannelPlayMulti(GlkSChannel[] channels, int[] resourceIds, int notify) {
            Log.d(TAG,"sChannelPlayMulti");
            return 0;
        }

        @Override
        public void soundLoadHint(int resourceId, boolean flag) {
            Log.w(TAG, "soundLoadHint unimplemented");
        }


        private GlkEvent timerEvent() {
            if (timerInterval <= 0) {
                return null;
            }
            if (lastTimerEvent == 0) {
                lastTimerEvent = System.currentTimeMillis();
                return null;
            }
            if (System.currentTimeMillis() < lastTimerEvent + timerInterval) {
                return null;
            }
            lastTimerEvent = System.currentTimeMillis();
            return new GlkEvent(GlkEvent.TypeTimer, null, 0, 0);
        }

        private long timeToNextTimerEvent() {
            if (timerInterval <= 0) {
                return -1;
            }
            return Math.max(1L, lastTimerEvent + timerInterval - System.currentTimeMillis());
        }

        @Override
        public GlkEvent select() {
            if (SettingsCurrent.getInterpreterProfileEnabled()) {
                if (profilerFlag) {
                    profilerFlag = false;
                    final long dt = System.currentTimeMillis() - profilerTime;
                    if (dt > 120l) {
                        EventBus.getDefault().post(new EventDebugOutput(1, 200, "profile elapsed " + dt + "ms"));
                    }
                    post(new Runnable() {
                        @Override public void run() {
                            Toast.makeText(GlkActivity.this, "Profile " + String.valueOf(dt), Toast.LENGTH_LONG).show();
                        }
                    });
                }
            }
            synchronized (ioLock) {
                outputPending = true;
                frameLayout.post(handlePendingOutput);
                while (outputPending) {
                    try {
                        ioLock.wait();
                    } catch (InterruptedException e) {
                        main.requestSuspend();
                        return new GlkEvent(GlkEvent.TypeArrange, null, 0, 0);
                    }
                }
            }
            if (activityState.rootWindow == null) {
                throw new IllegalStateException();
            }
            if (pendingArrangeEvent) {
                pendingArrangeEvent = false;
                activityState.rootWindow.clearPendingArrangeEvent();
                return new GlkEvent(GlkEvent.TypeArrange, null, 0, 0);
            }
            if (pendingRedrawEvent) {
                pendingRedrawEvent = false;
                activityState.rootWindow.clearPendingRedrawEvent();
                return new GlkEvent(GlkEvent.TypeRedraw, null, 0, 0);
            }
            for (GlkSChannel schannel : glkDispatch.sChannelList()) {
                GlkEvent event = ((SChannel) schannel).getEvent();
                if (event != null) {
                    return event;
                }
            }
            try {
                GlkEvent event = activityState.rootWindow.getEvent(0L, true);
                if (event != null) {
                    return event;
                }
            } catch (InterruptedException e) {
                main.requestSuspend();
                return new GlkEvent(GlkEvent.TypeArrange, null, 0, 0);
            }
            synchronized (ioLock) {
                outputPending = true;
                frameLayout.post(handlePendingSpeechOutput);
                while (outputPending) {
                    try {
                        ioLock.wait();
                    } catch (InterruptedException e) {
                        main.requestSuspend();
                        return new GlkEvent(GlkEvent.TypeArrange, null, 0, 0);
                    }
                }
            }
            for (;;) {
                GlkEvent event;
                try {
                    event = activityState.rootWindow.getEvent(0L, true);
                    if (event != null) {
                        return event;
                    }
                    long profileT = 0;
                    if (SettingsCurrent.getInterpreterProfileEnabled()) {
                        profileT = System.currentTimeMillis();
                    }
                    event = activityState.rootWindow.getEvent(timeToNextTimerEvent(), false);
                    if (event != null) {
                        if (SettingsCurrent.getInterpreterProfileEnabled()) {
                            profilerTime = System.currentTimeMillis();
                            profilerFlag = profilerTime - profileT > 500;
                        }
                        return event;
                    }
                    long timeToNextTimerEvent = timeToNextTimerEvent();
                    if (timeToNextTimerEvent > 1 || timeToNextTimerEvent < 0) {
                        hideProgressBar();
                        input.waitForEvent(timeToNextTimerEvent);
                        showProgressBar();
                    }
                } catch (InterruptedException e) {
                    main.requestSuspend();
                    return new GlkEvent(GlkEvent.TypeArrange, null, 0, 0);
                }
                event = timerEvent();
                if (event != null) {
                    return event;
                }
            }
        }

        @Override
        public GlkEvent selectPoll() {
            if (pendingArrangeEvent) {
                pendingArrangeEvent = false;
                if (activityState.rootWindow != null) {
                    activityState.rootWindow.clearPendingArrangeEvent();
                    return new GlkEvent(GlkEvent.TypeArrange, null, 0, 0);
                }
            }
            if (pendingRedrawEvent) {
                pendingRedrawEvent = false;
                if (activityState.rootWindow != null) {
                    activityState.rootWindow.clearPendingRedrawEvent();
                    return new GlkEvent(GlkEvent.TypeRedraw, null, 0, 0);
                }
            }
            for (GlkSChannel schannel : glkDispatch.sChannelList()) {
                GlkEvent event = ((SChannel) schannel).getEvent();
                if (event != null) {
                    return event;
                }
            }
            GlkEvent event = null;
            try {
                event = activityState.rootWindow.getEvent(0L, true);
            } catch (InterruptedException e) {
                main.requestSuspend();
                return new GlkEvent(GlkEvent.TypeNone, null, 0, 0);
            }
            if (event != null) {
                return event;
            }
            event = timerEvent();
            if (event != null) {
                return event;
            }
            return new GlkEvent(GlkEvent.TypeNone, null, 0, 0);
        }


        @Override
        public void requestTimerEvents(int millisecs) {
            timerInterval = millisecs;
            lastTimerEvent = 0;
        }


        @Override
        public boolean imageGetInfo(int resourceId, int[] size) {
            Bitmap image = getImageResource(resourceId);
            if (image == null) {
                return false;
            } else {
                size[0] = image.getWidth();
                size[1] = image.getHeight();
                return true;
            }
        }
    };

    File getResourceFile(int resourceId) {
        File file = new File(main.getDir(this), "glkres."+resourceId);
        if (file.exists()) {
            return file;
        }
        Blorb blorb = null;
        try {
            blorb = main.getBlorb(this);
            if (blorb == null) {
                return null;
            }
            for (Blorb.Resource res : blorb.resources()) {
                if (res.getNumber() == resourceId && res.getChunk() != null) {
                    File tmpFile = File.createTempFile("tmp","tmp",main.getDir(this));
                    FileOutputStream out = null;
                    try {
                        out = new FileOutputStream(tmpFile);
                        res.getChunk().write(out);
                        tmpFile.renameTo(file);
                        return file;
                    } finally {
                        if (tmpFile.exists()) {
                            tmpFile.delete();
                        }
                        if (out != null) {
                            out.close();
                        }
                    }
                }
            }
        } catch (IOException e) {
            Log.wtf(TAG,e);
        } finally {
            if (blorb != null) {
                try {
                    blorb.close();
                } catch (IOException e) {
                    Log.wtf(TAG,e);
                }
            }
        }
        return null;
    }

    File getSoundResourceFile(int resourceId) {
        File file = new File(main.getDir(this), "glkres.snd."+resourceId);
        if (file.exists()) {
            return file;
        }
        Blorb blorb = null;
        try {
            blorb = main.getBlorb(this);
            if (blorb == null) {
                return null;
            }
            for (Blorb.Resource res : blorb.resources()) {
                Log.d(TAG,"res="+res.getNumber());
                if (res.getNumber() == resourceId && res.getUsage() == Blorb.Snd && res.getChunk() != null) {
                    Log.d(TAG,String.format("chunkid=%x",res.getChunk().getId()));
                    File tmpFile = File.createTempFile("tmp","tmp",main.getDir(this));
                    File tmpFile2 = File.createTempFile("tmp","tmp",main.getDir(this));
                    FileOutputStream out = null;
                    FileOutputStream out2 = null;
                    try {
                        out = new FileOutputStream(tmpFile);
                        res.getChunk().write(out);
                        out2 = new FileOutputStream(tmpFile2);
                        switch (res.getChunk().getId()) {
                        case Blorb.OGGV: // OGGV
                            tmpFile.renameTo(file);
                            return file;
                        case Blorb.FORM: // AIFF
                            if (AudioConversion.aiffToWav(tmpFile, out2)) {
                                tmpFile2.renameTo(file);
                                return file;
                            }
                            break;
                        case Blorb.MOD: // MOD
                            if (AudioConversion.modToWav(tmpFile, out2)) {
                                tmpFile2.renameTo(file);
                                return file;
                            }
                            break;
                        case Blorb.SONG: // SONG
                            if (AudioConversion.songToWav(tmpFile, out2)) {
                                tmpFile2.renameTo(file);
                                return file;
                            }
                            break;
                        default:
                        }
                        return null;
                    } finally {
                        if (tmpFile.exists()) {
                            tmpFile.delete();
                        }
                        if (tmpFile2.exists()) {
                            tmpFile2.delete();
                        }
                        if (out != null) {
                            out.close();
                        }
                        if (out2 != null) {
                            out2.close();
                        }
                    }
                }
            }
        } catch (IOException e) {
            Log.wtf(TAG,e);
        } finally {
            if (blorb != null) {
                try {
                    blorb.close();
                } catch (IOException e) {
                    Log.wtf(TAG,e);
                }
            }
        }
        return null;
    }

    Bitmap getImageResource(int resourceId) {
        synchronized (imageResourceCache) {
            Bitmap image = imageResourceCache.get(resourceId);
            if (image != null) {
                return image;
            }
            File file = getResourceFile(resourceId);
            if (file != null) {
                image = BitmapFactory.decodeFile(file.getPath());
                if (image != null) {
                    imageResourceCache.put(resourceId, image);
                }
            }
            return image;
        }
    }

    private final Runnable handlePendingOutput = new Runnable() {
        @Override
        public void run() {
            Log.d(TAG,"handlePendingOutput:this="+GlkActivity.this);
            if (activityState.rootWindow == null) {
                frameLayout.removeAllViews();
            } else {
                if (frameLayout.getChildCount() == 0 || frameLayout.getChildAt(0) != activityState.rootWindow.getView()) {
                    frameLayout.removeAllViews();
                    if (activityState.rootWindow.getView().getParent() != null) {
                        ((ViewGroup) activityState.rootWindow.getView().getParent()).removeView(activityState.rootWindow.getView());
                    }
                    frameLayout.addView(activityState.rootWindow.getView());
                }
                if (activityState.rootWindow.updatePendingOutput(this, false)) {
                    return;
                }
            }
            synchronized (ioLock) {
                outputPending = false;
                ioLock.notifyAll();
            }
        }
    };

    private final Runnable handlePendingSpeechOutput = new Runnable() {
        @Override
        public void run() {
            Log.d(TAG,"handlePendingSpeechOutput:this="+GlkActivity.this);
            if (activityState.rootWindow == null) {
                frameLayout.removeAllViews();
            } else {
                if (frameLayout.getChildCount() == 0 || frameLayout.getChildAt(0) != activityState.rootWindow.getView()) {
                    frameLayout.removeAllViews();
                    frameLayout.addView(activityState.rootWindow.getView());
                }
                if (activityState.rootWindow.updatePendingOutput(this, false)) {
                    return;
                }
                if (activityState.rootWindow.updatePendingOutput(this, true)) {
                    return;
                }
            }
            synchronized (ioLock) {
                outputPending = false;
                ioLock.notifyAll();
            }
        }
    };

    void waitForTextMeasurer() throws InterruptedException {
        if (charWidth == 0) {
            synchronized (textMeasurer) {
                while (charWidth == 0) {
                    textMeasurer.wait();
                }
            }
        }
    }

    private final View.OnLayoutChangeListener textMeasurer = new View.OnLayoutChangeListener() {
        private int w1 = 0;
        private int h1 = 0;
        private int w2 = 0;
        private int h2 = 0;

        @Override public void onLayoutChange(View v, int left, int top, int right, int bottom, int oldLeft, int oldTop, int oldRight, int oldBottom) {
            if (v.getId() == main.getOneByOneMeasurer()) {
                w1 = right - left;
                h1 = bottom - top;
            } else if (v.getId() == main.getTwoByTwoMeasurer()) {
                w2 = right - left;
                h2 = bottom - top;
            }
            v.setVisibility(View.GONE);
            if (w1 != 0 && w2 != 0) {
                int oldw = charWidth;
                int oldh = charHeight;
                synchronized (this) {
                    charWidth = w2 - w1;
                    charHeight = h2 - h1;
                    charHMargin = w2 - 2*charWidth;
                    charVMargin = h2 - 2*charHeight;
                    this.notifyAll();
                }
                if (charWidth != oldw || charHeight != oldh) {
                    Log.d(TAG,"charSize="+charWidth+"x"+charHeight+",margin="+charHMargin+"x"+charVMargin+",1x="+w1+"x"+h1+",4x="+w2+"x"+h2);
                }
            }
        }
    };
}
