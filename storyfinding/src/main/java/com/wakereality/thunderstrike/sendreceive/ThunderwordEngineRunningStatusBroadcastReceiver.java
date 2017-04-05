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
        // appStartWhen can tell outside apps if there is a break in continuity of information due to unexpected engine provider restart.
        long appStartTime = intent.getLongExtra("appStartWhen", 0L);
        int stateCode = intent.getIntExtra("stateCode", -1);
        String sender = intent.getStringExtra("sender");
        if (stateCode != -1) {
            int index = intent.getIntExtra("index", -1);
            boolean clearIfPresent = intent.getBooleanExtra("clearIfPresent", false);
            Log.v("EngineRunStatus", "[ThunderClap][RUNNING_STATUS][shareEngineStatus] #" + index + " stateCode " + stateCode + " clearIfPresent " + clearIfPresent + " appStartTime " + appStartTime + " sender " + sender);
            EventBus.getDefault().post(new EventEngineRunningStatus(stateCode));
        } else {
            Log.w("EngineRunStatus", "[ThunderClap][RUNNING_STATUS][shareEngineStatus] stateCode -1, skipping internal processing appStartTime " + appStartTime + " sender " + sender);
        }
    }
}
