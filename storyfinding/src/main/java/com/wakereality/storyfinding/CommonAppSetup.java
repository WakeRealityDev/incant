package com.wakereality.storyfinding;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.preference.PreferenceManager;
import android.util.Log;
import android.util.LruCache;

import com.yrek.incant.Story;
import com.yrek.incant.StoryListSpot;
import com.yrek.incant.StoryLister;

import java.io.File;

/**
 * Created by Stephen A. Gutknecht on 4/16/17.
 */

public class CommonAppSetup {

    public static void prepareList(Context appContext) {
        if (StoryListSpot.storyLister == null) {
            StoryListSpot.storyLister = new StoryLister(appContext);
        }
        if (StoryListSpot.coverImageCache == null) {
            StoryListSpot.coverImageCache = new LruCache<String, Bitmap>(16);
        }

        createDiskPathsOnce(appContext);
        queryRemoteStoryEngineProviders(appContext);
    }

    public static void queryRemoteStoryEngineProviders(Context context) {
        // Query for Interactive Fiction engine providers.
        Intent intent = new Intent();
        // Tell Android to start Thunderword app if not already running.
        intent.addFlags(Intent.FLAG_INCLUDE_STOPPED_PACKAGES);
        intent.setAction("interactivefiction.enginemeta.runstory");
        context.sendBroadcast(intent);
    }



    public static void createDiskPathsOnce(Context appContext)
    {
        File rootPath = Story.getRootDir(appContext);
        if (! rootPath.exists())
        {
            rootPath.mkdirs();
        }
        Log.i("StoryFinding", "[rootPath] exists? " + rootPath.exists() + " " + rootPath + " free " + rootPath.getFreeSpace() + " writable " + rootPath.canWrite());
    }

    public static void appStartupInitializeDefaults(Context appContext) {
        SharedPreferences spref = PreferenceManager.getDefaultSharedPreferences(appContext);

        StoryListSpot.optionLaunchExternal = spref.getBoolean("storylist_launchexternal", false);

        // true, show by default, negate the hide preference.
        StoryListSpot.showHeadingExpanded = ! spref.getBoolean("storylist_expand_default", false);
        // This is default on opening of the app
        // false, do not hide by default
        StoryListSpot.showHeadingExpandedHideByDefault = spref.getBoolean("storylist_expand_default", false);

        StoryListSpot.showInterfaceTipsA = spref.getBoolean("storylist_intro_tips", true);
    }
}
