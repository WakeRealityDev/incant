package com.yrek.incant.gamemeta;

/**
 * Created by adminsag on 10/3/16.
 */

public class EventDebugOutput {
    public int placeCode = 0;
    public int logLevel = 0;
    public String payload0 = "";

    public EventDebugOutput(int debugLogLevel, int debugPlaceCode, String payloadFirst) {
        logLevel = debugLogLevel;
        placeCode = debugPlaceCode;
        payload0 = payloadFirst;
    }

    @Override
    public String toString()
    {
        return logLevel + "," + placeCode + ":" + payload0;
    }
}
