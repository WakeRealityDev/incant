package com.wakereality.storyfinding;

import com.yrek.incant.Story;

/**
 * Created by adminsag on 4/14/17.
 */

public class EventStoryListDownloadResult {
    public boolean downloadResultError = false;
    public Story downloadStory;

    public EventStoryListDownloadResult(boolean downloadError, Story story) {
        downloadResultError = downloadError;
        downloadStory = story;
    }
}
