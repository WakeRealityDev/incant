package com.yrek.incant;

import android.app.Activity;
import android.content.Context;
import android.util.AttributeSet;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.widget.ScrollView;

public class SubactivityView extends ScrollView {
    private GestureDetector gestureDetector;

    public SubactivityView(Context context) {
        super(context);
    }

    public SubactivityView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public SubactivityView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
    }

    public void setActivity(final Activity activity) {
        gestureDetector = new GestureDetector(activity, new GestureDetector.SimpleOnGestureListener() {
            @Override public boolean onFling(MotionEvent e1, MotionEvent e2, float velocityX, float velocityY) {
                if (activity != null && Math.abs(3.5f*velocityY) < Math.abs(velocityX)) {
                    activity.finish();
                }
                return false;
            }
        });
    }

    @Override
    public boolean onTouchEvent(MotionEvent motionEvent) {
        if (gestureDetector != null) {
            gestureDetector.onTouchEvent(motionEvent);
        }
        return super.onTouchEvent(motionEvent);
    }
}
