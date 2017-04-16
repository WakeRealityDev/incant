package com.wakereality.incant;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;

import com.example.android.recyclerplayground.BrowseStoriesActivity;
import com.yrek.incant.Incant;


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
        if (! startActivityOnce) {
            startActivityOnce = true;

            if (informationFragmentHelper == null) {
                informationFragmentHelper = new InformationFragmentHelper(getApplicationContext(), this);
            }

            if (1==1) {
                startActivity(new Intent(this, BrowseStoriesActivity.class));
            } else {
                startActivity(new Intent(this, Incant.class));
            }
        }
    }
}
