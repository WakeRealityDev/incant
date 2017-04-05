package com.wakereality.thunderstrike.dataexchange;

/**
 */

public class EventEngineRunningStatus {
    public static final int STATE_UNKNOWN = 0;
    public static final int STATE_WAS_RUNNING_EXIT_NORMAL = 1;
    public static final int STATE_WAS_RUNNING_EXIT_ERRORED = 2;
    // Abnormal indicates we have no idea why it exited. This should not be encountered if RemGlk / Loader C code is consistent with Java code expectations.
    public static final int STATE_WAS_RUNNING_EXIT_ABNORMAL = 10;
    public static final int STATE_STARTING_NOW = 20;


    public int stateCode = STATE_UNKNOWN;
    public boolean clearIfPresent = true;

    public EventEngineRunningStatus(int engineStateCode) {
        stateCode = engineStateCode;
    }
}
