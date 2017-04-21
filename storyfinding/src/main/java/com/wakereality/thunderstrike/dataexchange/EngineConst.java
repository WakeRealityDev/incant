package com.wakereality.thunderstrike.dataexchange;

import android.util.SparseArray;

/**
* Created by Stephen A. Gutknecht on 1/23/17.
 * ToDo: support a "engineLibraryInstalled" state, as may distribute versions of the app
 *    with only a subset of engines.
 * ToDo: app startup check if all the library filenames are located in the runtime listing.
 */

public class EngineConst {
    public static final int ENGINE_UNKNOWN = 0;
    public static final int ENGINE_GLULX_GLULXE = 1;
    public static final int ENGINE_GLULX_GIT = 2;
    // FyreVM-web in WebView
    public static final int ENGINE_GLULX_GLULX_TYPESCRIPT = 3;
    // Could be Glulx or Z-machine, file extension does not indicate
    public static final int ENGINE_BLORB_UNKNOWN = 98;
    public static final int ENGINE_GLULX_DEFAULT = 99;
    public static final int ENGINE_Z_BOCFEL = 100;
    public static final int ENGINE_Z_FROTZ = 101;
    public static final int ENGINE_Z_NITFOL = 102;
    public static final int ENGINE_Z_DEFAULT = 199;
    public static final int ENGINE_TADS_ANY = 200;
    public static final int ENGINE_TADS2 = 201;
    public static final int ENGINE_TADS3 = 202;
    public static final int ENGINE_SCOTT_ADAMS = 300;
    public static final int ENGINE_ADVSYS = 400;
    public static final int ENGINE_LEVEL9 = 500;
    public static final int ENGINE_ADRIFT = 600;
    public static final int ENGINE_SQUIFFY = 700;
    public static final int ENGINE_HUGO = 800;
    public static final int ENGINE_MAGNETIC_SCROLLS = 900;
    public static final int ENGINE_ALAN2 = 1000;
    public static final int ENGINE_ALAN3 = 1001;
    public static final int ENGINE_QUEST = 1100;



    public static final SparseArray<String> engineLibraryName = new SparseArray<>();

    static {
        engineLibraryName.put(ENGINE_GLULX_GLULXE,     "glulxe");
        engineLibraryName.put(ENGINE_GLULX_GIT,        "git");
        engineLibraryName.put(ENGINE_GLULX_DEFAULT,    "git");
        // Git at least returns a useful error on such ambiguous files
        engineLibraryName.put(ENGINE_BLORB_UNKNOWN,    "git");
        engineLibraryName.put(ENGINE_Z_BOCFEL,         "bocfel");
        engineLibraryName.put(ENGINE_Z_FROTZ,          "frotz");
        engineLibraryName.put(ENGINE_Z_NITFOL,         "nitfol");
        // NOTE on default choice - nitfol can't take zblorb, only .z files? It presents help info on .zblorb
        engineLibraryName.put(ENGINE_Z_DEFAULT,        "bocfel");
        engineLibraryName.put(ENGINE_TADS_ANY,         "tads");
        engineLibraryName.put(ENGINE_TADS2,            "tads");
        engineLibraryName.put(ENGINE_TADS3,            "tads");
        engineLibraryName.put(ENGINE_SCOTT_ADAMS,      "scott");
        engineLibraryName.put(ENGINE_ADVSYS,           "advsys");
        engineLibraryName.put(ENGINE_LEVEL9,           "level9");
        engineLibraryName.put(ENGINE_ADRIFT,           "scare");
        engineLibraryName.put(ENGINE_SQUIFFY,          "webview");
        engineLibraryName.put(ENGINE_HUGO,             "hugo");
        engineLibraryName.put(ENGINE_MAGNETIC_SCROLLS, "magnetic");
        engineLibraryName.put(ENGINE_ALAN2,            "alan2");
        engineLibraryName.put(ENGINE_ALAN3,            "alan3");
        engineLibraryName.put(ENGINE_QUEST,            "geas");
    }

    public static final int[] engineFlatIndex = new int[] {
            ENGINE_UNKNOWN,
            ENGINE_GLULX_GLULXE,
            ENGINE_GLULX_GIT,
            ENGINE_GLULX_GLULX_TYPESCRIPT,
            ENGINE_BLORB_UNKNOWN,
            ENGINE_GLULX_DEFAULT,
            ENGINE_Z_BOCFEL,
            ENGINE_Z_FROTZ,
            ENGINE_Z_NITFOL,
            ENGINE_Z_DEFAULT,
            ENGINE_TADS_ANY,
            ENGINE_TADS2,
            ENGINE_TADS3,
            ENGINE_SCOTT_ADAMS,
            ENGINE_ADVSYS,
            ENGINE_LEVEL9,
            ENGINE_ADRIFT,
            ENGINE_SQUIFFY,
            ENGINE_HUGO,
            ENGINE_MAGNETIC_SCROLLS,
            ENGINE_ALAN2,
            ENGINE_ALAN3,
            ENGINE_QUEST,
    };


    public static final String LAUNCH_PARAM_KEY_LAUNCH_INITIATED_WHEN = "launchInitiatedWhen";
    public static final String LAUNCH_PARAM_KEY_LAUNCHCODE   = "launchCode";
    public static final String LAUNCH_PARAM_KEY_ENGINECODE   = "enginecode";
    public static final String LAUNCH_PARAM_KEY_LAUNCHSTORY  = "launch";
    public static final String LAUNCH_PARAM_KEY_ACTIVITYCODE = "activitycode";
    public static final String LAUNCH_PARAM_KEY_HASH_STORY   = "datapick";
    public static final String LAUNCH_PARAM_KEY_FILE_STORY_PATH = "path";

    public static final int LAUNCH_PARAM_LAUNCHCODE_AUTO = 1;
    public static final int LAUNCH_PARAM_UNKNOWN_VALUE = -1;
    public static final int LAUNCH_PARAM_LAUNCHSTORY_YES = 1;

    public static final String ENGINE_FRIENDLYNAME_UNKNOWN = "Unknown";
    public static final String ENGINE_FRIENDLYNAME_ZMACHINE = "Z-Machine";
    public static final String ENGINE_FRIENDLYNAME_GLULX = "Glulx";
    public static final String ENGINE_FRIENDLYNAME_SQUIFFY = "Squiffy";
    public static final String ENGINE_FRIENDLYNAME_LEVEL9 = "Level9";
    public static final String ENGINE_FRIENDLYNAME_HUGO = "Hugo";
    public static final String ENGINE_FRIENDLYNAME_MAGNETIC_SCROLLS = "Magnetic Scrolls";
    public static final String ENGINE_FRIENDLYNAME_ALAN2 = "Alan 2";
    public static final String ENGINE_FRIENDLYNAME_ALAN3 = "Alan 3";
    public static final String ENGINE_FRIENDLYNAME_TADS2 = "TADS 2";
    public static final String ENGINE_FRIENDLYNAME_TADS3 = "TADS 3";
    public static final String ENGINE_FRIENDLYNAME_UNKNOWN_BLORB = "blorb";
    public static final String ENGINE_FRIENDLYNAME_SCOTT_ADAMS = "Scott Adams";
    public static final String ENGINE_FRIENDLYNAME_ADVSYS = "AdvSys";
    public static final String ENGINE_FRIENDLYNAME_ADRIFT = "Adrift";
    public static final String ENGINE_FRIENDLYNAME_QUEST = "Quest";

    public static final int PAYLOAD_TO_ENGINE_USE_GENERAL_PURPOSE = 0;
    public static final int PAYLOAD_TO_ENGINE_USE_REMGLK_INIT = 1;
    public static final int PAYLOAD_TO_ENGINE_USE_REMGLK_STOP_ENGINE = 2;


    /*
    Per-interpreter engine, no competing interpreters
     */
    public static final int[] engineTypesFlatIndex = new int[] {
            ENGINE_UNKNOWN,
            ENGINE_BLORB_UNKNOWN,
            ENGINE_GLULX_DEFAULT,
            ENGINE_Z_DEFAULT,
            ENGINE_TADS2,
            ENGINE_TADS3,
            ENGINE_SCOTT_ADAMS,
            ENGINE_ADVSYS,
            ENGINE_LEVEL9,
            ENGINE_ADRIFT,
            ENGINE_HUGO,
            ENGINE_MAGNETIC_SCROLLS,
            ENGINE_ALAN2,
            ENGINE_ALAN3,
            ENGINE_QUEST,
    };

    public static final String[] engineTypesFlatNames = new String[] {
            ENGINE_FRIENDLYNAME_UNKNOWN,
            ENGINE_FRIENDLYNAME_UNKNOWN_BLORB,
            ENGINE_FRIENDLYNAME_GLULX,
            ENGINE_FRIENDLYNAME_ZMACHINE,
            ENGINE_FRIENDLYNAME_TADS2,
            ENGINE_FRIENDLYNAME_TADS3,
            ENGINE_FRIENDLYNAME_SCOTT_ADAMS,
            ENGINE_FRIENDLYNAME_ADVSYS,
            ENGINE_FRIENDLYNAME_LEVEL9,
            ENGINE_FRIENDLYNAME_ADRIFT,
            ENGINE_FRIENDLYNAME_HUGO,
            ENGINE_FRIENDLYNAME_MAGNETIC_SCROLLS,
            ENGINE_FRIENDLYNAME_ALAN2,
            ENGINE_FRIENDLYNAME_ALAN3,
            ENGINE_FRIENDLYNAME_QUEST,
    };

}
