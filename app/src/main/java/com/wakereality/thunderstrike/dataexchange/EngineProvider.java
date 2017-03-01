package com.wakereality.thunderstrike.dataexchange;

import java.util.Arrays;

/**
 * Created by Stephen A. Gutknecht on 2/18/17.
 */

public class EngineProvider {
    public String providerAppPackage = null;
    public int providerAppVersionCode = -1;
    public String[] providerEnginesAvailable = {};

    @Override
    public String toString() {
        return providerAppPackage + " " + providerAppVersionCode + " " + Arrays.toString(providerEnginesAvailable);
    }
}
