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

        // Build CSV file from multiple sources - or use consolidated one?
        // NOTE: the current project structure doesn't have storyfinding module aware of flavors, so source files go into app level assets.
        // StoryListSpot.readCommaSepValuesFile.readComplexSetOfFilesCSV(appContext);
        StoryListSpot.readCommaSepValuesFile.readComplexSetOfFilesCSV_manyInform(appContext);

        // StoryListSpot.readCommaSepValuesFile.readSimpleFileOneObjectCSV(appContext);

        System.gc();
    }
}
