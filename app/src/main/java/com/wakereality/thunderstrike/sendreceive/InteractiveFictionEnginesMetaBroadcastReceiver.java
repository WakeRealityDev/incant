package com.wakereality.thunderstrike.sendreceive;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

import com.wakereality.thunderstrike.EchoSpot;
import com.wakereality.thunderstrike.dataexchange.EngineProvider;
import com.wakereality.thunderstrike.dataexchange.EventEngineProviderChange;

import org.greenrobot.eventbus.EventBus;

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
        EngineProvider engineProvider = new EngineProvider();

        switch (intent.getAction()) {
            case "interactivefiction.enginemeta.storyengines":
                engineProvider.providerAppPackage = intent.getStringExtra("sender");
                engineProvider.providerAppVersionCode = intent.getIntExtra("sender_versioncode", -1);
                engineProvider.providerEnginesAvailable = intent.getStringArrayExtra("engines_available");
                // SHA-256 hash of story file.
                engineProvider.providerStoriesBuiltIn = intent.getStringArrayExtra("stories_built_in");
                // Not using this data for now, but the field is documented here. "_EN" for English.
                // Array is same length and order as "stories_built_in", one to one relationship.
                intent.getStringArrayExtra("stories_built_in_description_EN");
                intent.getIntArrayExtra("stories_built_in_engine_code");

                // LIFO logic for setting current provider. User controls can pick.
                EchoSpot.currentEngineProvider = engineProvider;
                EchoSpot.detectedEngineProviders.put(engineProvider.providerAppPackage, engineProvider);

                EventBus.getDefault().post(new EventEngineProviderChange(engineProvider));

                Log.i("IFEngineMeta", "[engineProvider] app responded: " + engineProvider.toString());
                break;
            default:
                Log.w("IFEngineMeta", "unmatched action: " + intent.getAction());
                break;
        }
    }
}
