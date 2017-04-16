package com.example.android.recyclerplayground;

import android.app.Activity;
import android.view.View;

/**
 * Created by adminsag on 4/16/17.
 */

public class EventInformationFragmentPopulate {
    public View rootView = null;
    public Activity holdingActivity = null;

    public EventInformationFragmentPopulate(View viewToPopulate, Activity parentActivity) {
        rootView = viewToPopulate;
        holdingActivity = parentActivity;
    }
}
