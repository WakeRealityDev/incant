package com.wakereality.thunderstrike.dataexchange;

/**
 * Created by Stephen A. Gutknecht on 2/18/17.
 */

public class EventEngineProviderChange {
    public final EngineProvider engineProvider;

    public EventEngineProviderChange(EngineProvider detectedProvider) {
        engineProvider = detectedProvider;
    }
}
