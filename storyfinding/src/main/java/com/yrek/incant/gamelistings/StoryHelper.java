package com.yrek.incant.gamelistings;

import android.content.Context;
import android.util.Log;

import com.yrek.incant.Story;

import java.net.URL;
import java.util.ArrayList;
import java.util.Locale;

/**
 * Created by Stephen A. Gutknecht on 9/14/16.
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


    public static String getUsefulFileExtensionFromURL(URL inUrl) {
        if (inUrl == null)
            return "tmp";

        String simpleString = inUrl.toString();
        return getUsefulFileExtensionFromURL(simpleString);
    }

    public static String getUsefulFileExtensionFromURL(String inUrl) {
        if (inUrl == null)
            return "tmp";

        String simpleString = inUrl.toLowerCase(Locale.US);

        if (simpleString.endsWith(".zblorb")) {
            return "zblorb";
        } else if (simpleString.endsWith(".gblorb")) {
            return "gblorb";
        } else if (simpleString.endsWith(".ulx")) {
            return "ulx";
        } else if (simpleString.endsWith(".z8")) {
            return "z8";
        } else if (simpleString.endsWith(".z7")) {
            return "z7";
        } else if (simpleString.endsWith(".z6")) {
            return "z6";
        } else if (simpleString.endsWith(".z5")) {
            return "z5";
        } else if (simpleString.endsWith(".z4")) {
            return "z4";
        } else if (simpleString.endsWith(".z3")) {
            return "z3";
        } else if (simpleString.endsWith(".blb")) {
            return "blorb";
        } else if (simpleString.endsWith(".zip")) {
            return "zip";
        }

        return "unknown";
    }
}


