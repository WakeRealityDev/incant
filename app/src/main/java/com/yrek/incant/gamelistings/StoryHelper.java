package com.yrek.incant.gamelistings;

import android.content.Context;
import android.util.Log;

import com.yrek.incant.Story;

import java.util.ArrayList;

/**
 * Created by adminsag on 9/14/16.
 */
public class StoryHelper {

    public static void addStory(Context context, Story story, ArrayList<Story> stories, int category0) {
        String name = story.getName(context);
        if (name == null || name.indexOf('/') >= 0 || name.equals(".") || name.equals("..")) {
            Log.d("StoryHelper", "addStory skip " + name);
            return;
        }
        for (Story s : stories) {
            if (name.equals(s.getName(context))) {
                Log.i("StoryHelper", "story already in system " + name);
                return;
            }
        }
        story.setListingExtras(category0);
        stories.add(story);
    }
}


