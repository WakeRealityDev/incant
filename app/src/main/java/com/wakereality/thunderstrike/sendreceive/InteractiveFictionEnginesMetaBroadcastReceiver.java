package com.wakereality.thunderstrike.sendreceive;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

import java.util.Arrays;

/**
 * This is a useful way to know that Thunderword app is installed and available on the device
 *   before blindly launching stories. Will also inform outside apps if multiple providers are
 *   installed, the app name in the "sender" field and multiple incoming responses can be
 *   received.
 *
 */

public class InteractiveFictionEnginesMetaBroadcastReceiver extends BroadcastReceiver {

    @Override
    public void onReceive(Context context, Intent intent) {
        switch (intent.getAction()) {
            case "interactivefiction.enginemeta.storyengines":
                String sender = intent.getStringExtra("sender");
                int senderVersionCode = intent.getIntExtra("sender_versioncode", -1);
                String[] enginesAvailable = intent.getStringArrayExtra("engines_available");

                Log.i("IFEngineMeta", "sender " + sender + " versionCode " + senderVersionCode + " engines: " + Arrays.toString(enginesAvailable));
                break;
            default:
                Log.w("IFEngineMeta", "unmatched action: " + intent.getAction());
                break;
        }
    }
}
