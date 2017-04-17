package com.example.android.recyclerplayground.adapters;

import android.content.Context;
import android.util.AttributeSet;
import android.util.Log;
import android.widget.GridLayout;
import android.widget.TextView;

import com.wakereality.storyfinding.R;


public class StoryItemView extends GridLayout {

    private TextView mStoryTitle, mStoryAuthor;

    public StoryItemView(Context context) {
        super(context);
    }

    public StoryItemView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public StoryItemView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    @Override
    protected void onFinishInflate() {
        super.onFinishInflate();

        mStoryTitle = (TextView) findViewById(R.id.text_story_title);
        mStoryAuthor = (TextView) findViewById(R.id.text_story_authors);
    }

    @Override
    public String toString() {
        try {
            return mStoryAuthor.getText() + "v" + mStoryTitle.getText()
                    + ": " + getLeft() + "," + getTop()
                    + ": " + getMeasuredWidth() + "x" + getMeasuredHeight();
        } catch (Exception e0) {
            Log.e("StoryItem", "Exception in StoryItemView toString", e0);
            return "ERROR_STORY_ITEMVIEW_0";
        }
    }
}
