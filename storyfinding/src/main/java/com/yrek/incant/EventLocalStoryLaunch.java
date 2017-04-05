package com.yrek.incant;

import android.app.Activity;

/**
 * Created by Stephen A. Gutknecht on 4/5/17.
 */

public class EventLocalStoryLaunch {
    public Story story;
    public Activity callingActivity;

    public EventLocalStoryLaunch(Activity startActivityCallingActivity, Story storyToLaunch) {
        callingActivity = startActivityCallingActivity;
        story = storyToLaunch;
    }
}
