package com.wakereality.thunderstrike;

import com.wakereality.thunderstrike.dataexchange.EngineProvider;

import java.util.LinkedHashMap;

/**
 * Created by Stephen A. Gutknecht on 2/28/17.
 * NOTE: There are two variations of EchoSpot, one in ThunderEcho and one in ThunderStrike
 */

public class EchoSpot {
    // This is the active provider of possible multiples.
    public static EngineProvider currentEngineProvider = null;
    public static LinkedHashMap<String, EngineProvider> detectedEngineProviders = new LinkedHashMap<>(3);
    public static int currentEngineProviderIndex = 0;

    // The app can tell the library which app ID to use on broadcast sending to Thunderword / Engine Providers.
    public static String sending_APPLICATION_ID = "";

    // BUG hack workaround: 1) start story Activity from storyList. 2) back button returns to WRONG place, to mainactivity, 3) start story again, returns to correct place.
    // The consequence to this workaround is that this sequence fails: 1) Start app, 2) Browse stories, 3)
    public static int backButtonHackStoryStartFlag = 0;
}
