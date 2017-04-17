package com.wakereality.incant;

import android.Manifest;
import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.util.Log;
import android.view.View;
import android.widget.Toast;

import com.example.android.recyclerplayground.BrowseStoriesActivity;
import com.yrek.incant.Incant;
import com.yrek.incant.R;
import com.yrek.incant.Story;
import com.yrek.incant.StoryListSpot;

import java.io.File;


/**
 * Created by Stephen A. Gutknecht on 4/16/17.
 */

public class LandingActivity extends Activity {

    public static final String TAG = "LandingActivity";

    protected boolean startActivityOnce = false;
    protected static InformationFragmentHelper informationFragmentHelper;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        confirmPermissionToUseStorage();
        confirmPermissionToUseSpeechListener();
        // getPermissionToUseWindow();

        openTargetActivity();
    }

    public void openTargetActivity() {
        if (StoryListSpot.storagePermissionReady) {
            // We may need to create paths
            createDiskPathsOnce();

            if (!startActivityOnce) {
                startActivityOnce = true;

                if (informationFragmentHelper == null) {
                    informationFragmentHelper = new InformationFragmentHelper(getApplicationContext(), this);
                }

                if (1 == 1) {
                    startActivity(new Intent(this, BrowseStoriesActivity.class));
                    finish();
                } else {
                    startActivity(new Intent(this, Incant.class));
                    finish();
                }
            }
        }
    }


    public void setContentViewForPermissionRequest() {
        setContentView(R.layout.landing_activity);
        findViewById(R.id.main_top_error_storage_try).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                confirmPermissionToUseStorage();
            }
        });
    }


    // Identifier for the permission request
    private static final int WRITE_STORAGE_PERMISSIONS_REQUEST = 1;
    private static final int RECORD_AUDIO_PERMISSION_REQUEST = 2;
    private static final int SYSTEM_ALERT_WINDOW_PERMISSION_REQUEST = 3;
    private static final int SYSTEM_OVERLAY_WINDOW_PERMISSION_REQUEST = 4;

    public void confirmPermissionToUseStorage() {
        if (ContextCompat.checkSelfPermission(this,
                Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {

            StoryListSpot.storagePermissionReady = false;

            // Should we show an explanation?
            if (ActivityCompat.shouldShowRequestPermissionRationale(this,
                    Manifest.permission.WRITE_EXTERNAL_STORAGE)) {

                // Show an explanation to the user *asynchronously* -- don't
                // block
                // this thread waiting for the user's response! After the user
                // sees the explanation, try again to request the permission.

                ActivityCompat.requestPermissions(this,
                        new String[] { Manifest.permission.WRITE_EXTERNAL_STORAGE },
                        WRITE_STORAGE_PERMISSIONS_REQUEST);
            } else {

                // No explanation needed, we can request the permission.
                ActivityCompat.requestPermissions(this,
                        new String[] { Manifest.permission.WRITE_EXTERNAL_STORAGE },
                        WRITE_STORAGE_PERMISSIONS_REQUEST);

            }
        } else {
            StoryListSpot.storagePermissionReady = true;
        }
    }

    public void confirmPermissionToUseSpeechListener() {
        if (ContextCompat.checkSelfPermission(this,
                Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {

            StoryListSpot.recordAudioPermissionReady = false;

            // Should we show an explanation?
            if (ActivityCompat.shouldShowRequestPermissionRationale(this, Manifest.permission.RECORD_AUDIO)) {

                // Show an explanation to the user *asynchronously* -- don't
                // block
                // this thread waiting for the user's response! After the user
                // sees the explanation, try again to request the permission.

                StoryListSpot.recordAudioPermissionReady = false;

                ActivityCompat.requestPermissions(this,
                        new String[] { Manifest.permission.RECORD_AUDIO },
                        RECORD_AUDIO_PERMISSION_REQUEST);
            } else {

                // No explanation needed, we can request the permission.
                ActivityCompat.requestPermissions(this,
                        new String[] { Manifest.permission.RECORD_AUDIO },
                        RECORD_AUDIO_PERMISSION_REQUEST);
            }
        } else {
            StoryListSpot.recordAudioPermissionReady = true;
        }
    }

    public void checkDrawOverlayPermission() {
        /** check if we already  have permission to draw over other apps */
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (!Settings.canDrawOverlays(this)) {
                /** if not construct intent to request permission */
                Intent intent = new Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                        Uri.parse("package:" + getPackageName()));
                /** request permission via start activity for result */
                startActivityForResult(intent, SYSTEM_OVERLAY_WINDOW_PERMISSION_REQUEST);
            }
        }
    }


    public void getPermissionToUseWindow() {
        checkDrawOverlayPermission();

        if (ContextCompat.checkSelfPermission(this,
                Manifest.permission.SYSTEM_ALERT_WINDOW) != PackageManager.PERMISSION_GRANTED) {

            Log.w("Incant", "Permission not granted SYSTEM_ALERT_WINDOW");

            // Should we show an explanation?
            if (ActivityCompat.shouldShowRequestPermissionRationale(this, Manifest.permission.SYSTEM_ALERT_WINDOW)) {

                // Show an explanation to the user *asynchronously* -- don't
                // block
                // this thread waiting for the user's response! After the user
                // sees the explanation, try again to request the permission.

            } else {

                // No explanation needed, we can request the permission.
                ActivityCompat.requestPermissions(this,
                        new String[] { Manifest.permission.SYSTEM_ALERT_WINDOW },
                        SYSTEM_ALERT_WINDOW_PERMISSION_REQUEST);

            }
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String permissions[], int[] grantResults) {
        switch (requestCode) {
            case WRITE_STORAGE_PERMISSIONS_REQUEST: {
                // If request is cancelled, the result arrays are empty.
                if (grantResults.length > 0
                        && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    // Toast.makeText(this, "Write Storage permission granted", Toast.LENGTH_SHORT).show();
                    // permission was granted, yay! Do the
                    // contacts-related task you need to do.
                    StoryListSpot.storagePermissionReady = true;
                    openTargetActivity();
                } else {
                    Toast.makeText(this, "Write Storage permission denied",
                            Toast.LENGTH_SHORT).show();
                    // permission denied, boo! Disable the
                    // functionality that depends on this permission.
                    StoryListSpot.storagePermissionReady = false;

                    setContentViewForPermissionRequest();
                }
                return;
            }
            case RECORD_AUDIO_PERMISSION_REQUEST:
                // If request is cancelled, the result arrays are empty.
                if (grantResults.length > 0
                        && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    // Toast.makeText(this, "Write Storage permission granted", Toast.LENGTH_SHORT).show();
                    // permission was granted, yay! Do the
                    // contacts-related task you need to do.
                    StoryListSpot.recordAudioPermissionReady = true;
                    openTargetActivity();
                } else {
                    Toast.makeText(this, "Write Storage permission denied",
                            Toast.LENGTH_SHORT).show();
                    // permission denied, boo! Disable the
                    // functionality that depends on this permission.
                    StoryListSpot.recordAudioPermissionReady = false;
                }
                return;
            case SYSTEM_ALERT_WINDOW_PERMISSION_REQUEST:
                return;

            // other 'case' lines to check for other
            // permissions this app might request
        }
    }


    public void createDiskPathsOnce()
    {
        File rootPath = Story.getRootDir(getApplicationContext());
        if (! rootPath.exists())
        {
            rootPath.mkdirs();
        }
        Log.i(TAG, "exists? " + rootPath.exists() + " " + rootPath + " free " + rootPath.getFreeSpace() + " writable " + rootPath.canWrite());
    }
}
