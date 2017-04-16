package com.wakereality.storyfinding;

import android.content.Context;

import com.yrek.incant.StoryListSpot;

/**
 * Created by Stephen A. Gutknecht on 4/14/17.
 */

public class AddStoriesToStoryList {

    public static void processAssetsCommaSeparatedValuesList(Context appContext) {
        if (StoryListSpot.readCommaSepValuesFile == null) {
            StoryListSpot.readCommaSepValuesFile = new ReadCommaSepValuesFile();
        }
        // readCommaSepValuesFile.readComplexSetOfFilesCSV(getApplicationContext());
        StoryListSpot.readCommaSepValuesFile.readSimpleFileOneObjectCSV(appContext);
        System.gc();
    }
}
