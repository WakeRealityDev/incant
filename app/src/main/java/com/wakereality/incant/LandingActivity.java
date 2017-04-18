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
import com.example.android.recyclerplayground.BrowseStoriesNewDrawerActivity;
import com.yrek.incant.Incant;
import com.yrek.incant.R;
import com.yrek.incant.StoryListSpot;


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
            if (!startActivityOnce) {
                startActivityOnce = true;

                if (informationFragmentHelper == null) {
                    informationFragmentHelper = new InformationFragmentHelper(getApplicationContext(), this);
                }

                if (1 == 1) {
                    // startActivity(new Intent(this, BrowseStoriesActivity.class));
                    startActivity(new Intent(this, BrowseStoriesNewDrawerActivity.class));
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

            // other 'case' lines to check for other
            // permissions this app might request
        }
    }

}
