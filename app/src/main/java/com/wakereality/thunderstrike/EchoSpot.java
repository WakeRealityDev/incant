package com.wakereality.thunderstrike;

import com.wakereality.thunderstrike.dataexchange.EngineProvider;

import java.util.LinkedHashMap;

/**
 * Created by Stephen A. Gutknecht on 2/28/17.
 */

public class EchoSpot {
    // This is the active provider of possible multiples.
    public static EngineProvider currentEngineProvider = null;
    public static LinkedHashMap<String, EngineProvider> detectedEngineProviders = new LinkedHashMap<>(3);
    public static int currentEngineProviderIndex = 0;
}
