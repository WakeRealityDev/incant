package com.yrek.runconfig;

import java.io.File;

/**
 * NOTE, these are not currently persisted, app restart will put them to defaults.
 */
public class SettingsCurrent {

    public static boolean getSpeechEnabled()
    {
        return speechEnabled;
    }

    public static boolean getDownloadSkipCachefile() {
        return false;
    }

    public static boolean getFileRootDirInside() {
        return false;
    }

    public static boolean getFileCacheDirInside() {
        return false;
    }

    private static boolean listingShowLocal = false;

    public static boolean flipListingShowLocal() {
        listingShowLocal = ! listingShowLocal;
        return listingShowLocal;
    }

    public static boolean getListingShowLocal()
    {
        return listingShowLocal;
    }


    private static boolean speechRecognizerEnabled = false;
    private static boolean speechEnabled = false;
    private static boolean speechRecognizerMute = false;
    private static boolean interpreterProfileEnabled = true;
    private static boolean enableAutoEnterOnGlkCharInput = true;
    private static boolean scrapeIFArchive = false;

    public static boolean getSpeechRecognizerEnabled() {
        return speechRecognizerEnabled;
    }

    public static boolean getSpeechRecognizerMute() {
        return speechRecognizerMute;
    }

    /*
    ToDo: Even though we write to disk and do not read, the suspend app and resume app seems to work?!
     */
    public static int getSaveInstanceToDiskB() {
        return 4;
    }

    public static File getSaveInstanceToDiskFileA() {
        return new File("/sdcard/Incant_Stories/pause_save.bin");
    }

    public static boolean getGameLayoutPaddingA() {
        return true;
    }

    public static boolean getGameLayoutInputColorA() {
        return true;
    }

    public static void flipSpeechRecognizerEnabled() {
        speechRecognizerEnabled = ! speechRecognizerEnabled;
    }

    public static void flipSpeechEnabled() {
        speechEnabled = ! speechEnabled;
    }

    public static void flipSpeechRecognizerMute() {
        speechRecognizerMute = ! speechRecognizerMute;
    }

    public static void flipEnableAutoEnterOnGlkCharInput() {
        enableAutoEnterOnGlkCharInput = ! enableAutoEnterOnGlkCharInput;
    }

    public static boolean getInterpreterProfileEnabled() {
        return interpreterProfileEnabled;
    }

    public static void flipInterpreterProfileEnabled() {
        interpreterProfileEnabled = ! interpreterProfileEnabled;
    }

    public static void setSpeechEnabled(boolean setSpeechEnabled) {
        speechEnabled = setSpeechEnabled;
    }

    public static void setInterpreterProfileEnabled(boolean setInterpreterProfileEnabled) {
        interpreterProfileEnabled = setInterpreterProfileEnabled;
    }

    public static void setSpeechRecognizerEnabled(boolean setSpeechRecognizerEnabled) {
        speechRecognizerEnabled = setSpeechRecognizerEnabled;
    }

    public static void setSpeechRecognizerMute(boolean setSpeechRecognizerMute) {
        speechRecognizerMute = setSpeechRecognizerMute;
    }

    public static boolean getEnableAutoEnterOnGlkCharInput() {
        return enableAutoEnterOnGlkCharInput;
    }

    public static boolean getScrapeIFArchive() {
        return scrapeIFArchive;
    }
}
