package com.wakereality.storyfinding;

import android.content.Context;

import com.yrek.incant.Story;

/**
 * Created by adminsag on 4/14/17.
 */

public class EventExternalEngineStoryLaunch {
    public Context context;
    public Story story;
    public int targetActivity = 0;
    public boolean interruptEngineIfRunning = false;

    public EventExternalEngineStoryLaunch(Context launchContext, Story launchStory, int selectedLaunchActivity, boolean interruptEngine) {
        context = launchContext;
        story = launchStory;
        targetActivity = selectedLaunchActivity;
        interruptEngineIfRunning = interruptEngine;
    }
}
