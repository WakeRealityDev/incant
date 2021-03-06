package com.wakereality.incant;

import android.content.Context;
import android.content.Intent;
import android.util.Log;

import com.wakereality.storyfinding.EventExternalEngineStoryLaunch;
import com.wakereality.thunderstrike.EchoSpot;
import com.wakereality.storyfinding.EventLocalStoryLaunch;
import com.wakereality.thunderstrike.dataexchange.EngineConst;
import com.yrek.incant.GlulxStory;
import com.yrek.incant.Story;
import com.yrek.incant.StoryDetails;
import com.yrek.incant.ZCodeStory;
import com.yrek.incant.glk.GlkActivity;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;

import java.io.File;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Created by Stephen A. Gutknecht on 4/5/17.
 */

public class StoryEngineLaunchHelper {
    public static final String TAG = "LaunchHelper";
    protected static AtomicInteger launchToken = new AtomicInteger(0);

    public StoryEngineLaunchHelper() {
        EventBus.getDefault().register(this);
    }

    /*
    This is on BACKGROUND thread for possibly downloading of cover art
     */
    @SuppressWarnings("unused")
    @Subscribe(threadMode = ThreadMode.BACKGROUND)
    public void onEventMainThread(EventLocalStoryLaunch event) {
        if (event.story.prepForIncantEngineLaunch(event.callingActivity)) {
            Intent intent = new Intent(event.callingActivity, GlkActivity.class);
            // use getStorageName as if there are two with same name, still need to distinguish
            if (event.story.isZcode(event.callingActivity)) {
                intent.putExtra(GlkActivity.GLK_MAIN, new ZCodeStory(event.story, event.story.getStorageName(event.callingActivity)));
            } else {
                intent.putExtra(GlkActivity.GLK_MAIN, new GlulxStory(event.story, event.story.getStorageName(event.callingActivity)));
            }
            // need to switch to main thread?
            Log.d(TAG, "[playClick] startActivity for launch. Story: " + event.story.getName(event.callingActivity));
            event.callingActivity.startActivity(intent);
        } else {
            Log.e(TAG, "[playClick] fail to prepare for launch. Story: " + event.story.getName(event.callingActivity));
        }
    }

    @SuppressWarnings("unused")
    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onEventMainThread(EventExternalEngineStoryLaunch event) {
        Story story = event.story;
        Context context = event.context;
        Intent intent = new Intent();
        // Tell Android to start Thunderword app if not already running.
        intent.addFlags(Intent.FLAG_INCLUDE_STOPPED_PACKAGES);

        // Inform the Engine Provider who to call back.
        intent.putExtra("sender", EchoSpot.sending_APPLICATION_ID);

        String targetPackage = "";
        // Send to specific selected Engine Provider.
        if (EchoSpot.currentEngineProvider != null) {
            intent.setPackage(EchoSpot.currentEngineProvider.providerAppPackage);
            targetPackage = EchoSpot.currentEngineProvider.providerAppPackage;
        }

        long launchWhen = System.currentTimeMillis();
        intent.putExtra("sentwhen", launchWhen);

        String launchInfo = "??";
        String setAction;
        if (story.isExtractedForIncantEngine(context)) {
            if (story.isZcode(context)) {
                setAction = "interactivefiction.engine.zmachine";
            } else {
                setAction = "interactivefiction.engine.glulx";
            }
            // Not all stories come in Blorb packages, check first, but if missing go for the data file.
            File exportStoryDataFile = story.getBlorbFile(context);
            if (!exportStoryDataFile.exists()) {
                if (story.isZcode(context)) {
                    exportStoryDataFile = story.getZcodeFile(context);
                } else {
                    exportStoryDataFile = story.getGlulxFile(context);
                }
            }
            // NOTE: This assumes that the database on the Engine Prover has the SHA256 to path mapping - another option
            //   is to send path to the file instead of SHA256.
            launchInfo = EngineConst.LAUNCH_PARAM_KEY_FILE_STORY_PATH + " " + exportStoryDataFile.getPath();
            intent.putExtra(EngineConst.LAUNCH_PARAM_KEY_FILE_STORY_PATH, exportStoryDataFile.getPath());
        } else {
            setAction = "interactivefiction.engine.automatch";
            String storyHashSHA256 = story.getStoryHashSHA256(context);
            launchInfo = EngineConst.LAUNCH_PARAM_KEY_HASH_STORY + " " + storyHashSHA256;
            intent.putExtra(EngineConst.LAUNCH_PARAM_KEY_HASH_STORY, storyHashSHA256);
        }

        intent.setAction(setAction);

        int myLaunchToken = launchToken.incrementAndGet();
        // Set default value.
        int selectedLaunchActivity = event.targetActivity;
        if (selectedLaunchActivity <= 0) {
            selectedLaunchActivity = 1;   /* Bidirectional Scrolling Activity */
        }

        if (selectedLaunchActivity == 5) {
            // NOTE: This naming assumes there is only ONE instance of TextFiction for Thunderword app: not Experiential and Standard installed at same time.
            // ToDo: Getting file not found on Text Fiction side, generate a SHA-256 hash here always?
            intent.setAction("interactivefiction.userinterface.textfictionremotedata");
            intent.setPackage(null);
        } else {
            intent.putExtra(EngineConst.LAUNCH_PARAM_KEY_ACTIVITYCODE, selectedLaunchActivity);
        }

        Log.i(TAG, "[engineLaunch] " + launchInfo  + " setAction " + setAction + " sender " + EchoSpot.sending_APPLICATION_ID + " launchToken " + myLaunchToken + " selectedLaunchActivity " + selectedLaunchActivity + " when " + launchWhen + " target " + targetPackage);

        intent.putExtra("interrupt", event.interruptEngineIfRunning);
        intent.putExtra("launchtoken", "A" + myLaunchToken);

        context.sendBroadcast(intent);
    }
}
