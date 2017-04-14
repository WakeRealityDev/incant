package com.yrek.incant;

import android.graphics.Bitmap;
import android.util.LruCache;

import com.wakereality.storyfinding.ReadCommaSepValuesFile;

/**
 * Created by adminsag on 4/13/17.
 */

public class StoryListSpot {
    public static StoryLister storyLister;
    public static ReadCommaSepValuesFile readCommaSepValuesFile;
    public static LruCache<String,Bitmap> coverImageCache;

}
