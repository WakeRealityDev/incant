package com.wakereality.incant;

import android.app.Application;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;

import com.wakereality.thunderstrike.EchoSpot;
import com.yrek.incant.BuildConfig;
import com.yrek.incant.StoryListSpot;
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

        EchoSpot.sending_APPLICATION_ID = BuildConfig.APPLICATION_ID;

        SettingsCurrent.setSpeechEnabled(spref.getBoolean("speech_enabled", false));
        SettingsCurrent.setInterpreterProfileEnabled(spref.getBoolean("profile_enabled", false));
        SettingsCurrent.setSpeechRecognizerEnabled(spref.getBoolean("recognition_enabled", false));
        SettingsCurrent.setSpeechRecognizerMute(spref.getBoolean("recognition_mute_enabled", false));

        StoryListSpot.optionLaunchExternal = spref.getBoolean("storylist_launchexternal", false);

        // true, show by default, negate the hide preference.
        StoryListSpot.showHeadingExpanded = ! spref.getBoolean("storylist_expand_default", false);
        // This is default on opening of the app
        // false, do not hide by default
        StoryListSpot.showHeadingExpandedHideByDefault = spref.getBoolean("storylist_expand_default", false);

        StoryListSpot.showInterfaceTipsA = spref.getBoolean("storylist_intro_tips", true);

        StoryEngineLaunchHelper storyEngineLaunchHelper = new StoryEngineLaunchHelper();
    }
}
