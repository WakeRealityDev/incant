package com.wakereality.incant;

import android.content.Intent;

import com.yrek.incant.EventLocalStoryLaunch;
import com.yrek.incant.GlulxStory;
import com.yrek.incant.ZCodeStory;
import com.yrek.incant.glk.GlkActivity;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;

/**
 * Created by adminsag on 4/5/17.
 */

public class LocalEngineLaunchHelper {
    public LocalEngineLaunchHelper() {
        EventBus.getDefault().register(this);
    }

    @SuppressWarnings("unused")
    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onEventMainThread(EventLocalStoryLaunch event) {
        Intent intent = new Intent(event.callingActivity, GlkActivity.class);
        if (event.story.isZcode(event.callingActivity)) {
            intent.putExtra(GlkActivity.GLK_MAIN, new ZCodeStory(event.story, event.story.getName(event.callingActivity)));
        } else {
            intent.putExtra(GlkActivity.GLK_MAIN, new GlulxStory(event.story, event.story.getName(event.callingActivity)));
        }
        event.callingActivity.startActivity(intent);
    }
}
