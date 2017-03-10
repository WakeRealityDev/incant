package com.wakereality.incant;

import android.app.Application;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;

import com.yrek.runconfig.SettingsCurrent;

/**
 * Created by Stephen A. Gutknecht on 2/23/17.
 */

public class IncantApplication extends Application {

    @Override
    public void onCreate() {
        super.onCreate();
        populatedQuickPreferences();
    }

    public void populatedQuickPreferences() {
        SharedPreferences spref = PreferenceManager.getDefaultSharedPreferences(this);

        SettingsCurrent.setSpeechEnabled(spref.getBoolean("speech_enabled", false));
        SettingsCurrent.setInterpreterProfileEnabled(spref.getBoolean("profile_enabled", false));
        SettingsCurrent.setSpeechRecognizerEnabled(spref.getBoolean("recognition_enabled", false));
        SettingsCurrent.setSpeechRecognizerMute(spref.getBoolean("recognition_mute_enabled", false));
    }
}
