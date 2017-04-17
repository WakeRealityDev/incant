package com.wakereality.storyfinding;

import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.util.LruCache;

import com.yrek.incant.StoryListSpot;
import com.yrek.incant.StoryLister;

/**
 * Created by Stephen A. Gutknecht on 4/16/17.
 */

public class CommonSetup {

    public static void prepareList(Context appContext) {
        if (StoryListSpot.storyLister == null) {
            StoryListSpot.storyLister = new StoryLister(appContext);
        }
        if (StoryListSpot.coverImageCache == null) {
            StoryListSpot.coverImageCache = new LruCache<String, Bitmap>(10);
        }

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

}
