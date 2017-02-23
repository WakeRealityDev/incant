package com.wakereality.thunderstrike.sendreceive;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

import com.wakereality.thunderstrike.dataexchange.EventEngineRunningStatus;

import org.greenrobot.eventbus.EventBus;

/**
 */

public class ThunderwordEngineRunningStatusBroadcastReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        int stateCode = intent.getIntExtra("stateCode", -1);
        if (stateCode != -1) {
            int index = intent.getIntExtra("index", -1);
            boolean clearIfPresent = intent.getBooleanExtra("clearIfPresent", false);
            Log.v("EngineRunStatus", "[ThunderClap][shareRemGlk] #" + index + " stateCode " + stateCode + " clearIfPresent " + clearIfPresent);
            EventBus.getDefault().post(new EventEngineRunningStatus(stateCode));
        }
    }
}
