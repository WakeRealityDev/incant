package com.yrek.incant;

import android.graphics.Bitmap;
import android.util.LruCache;
import android.view.animation.Animation;

import com.wakereality.storyfinding.ReadCommaSepValuesFile;

import java.util.ArrayList;

/**
 * Created by Stephen A. Gutknecht on 4/13/17.
 */

public class StoryListSpot {
    public static StoryLister storyLister;
    public static ReadCommaSepValuesFile readCommaSepValuesFile;
    public static LruCache<String, Bitmap> coverImageCache;

    public static Story storyDetailStory0 = null;

    public static boolean optionLaunchExternal = false;
    public static int optionLaunchExternalActivityCode = 0;
    public static boolean optionaLaunchInterruptEngine = true;

    public static boolean showHeadingExpanded = true;
    public static boolean showHeadingExpandedHideByDefault = true;

    public static boolean showInterfaceTipsA = true;

    public static int listNumberOfColumns = 2;

    public static boolean storagePermissionReady = false;
    public static boolean recordAudioPermissionReady = false;

    // Will override. put here to allow a 'owning app' of the library to stuff down content.
    // null means leave original content. empty means remove view. content more than empty will be shown.
    public static CharSequence storyListHeaderInterfaceTipReplacement0 = null;
    public static boolean storyListHeaderInterfaceTipAppendHide = true;

    public static ArrayList<Story> storyListAppAboveHandDown = null;

    public static Animation launchStoryLocalAnimation = null;

    public static int storyListTotalCount = 0;
}
