package com.example.android.recyclerplayground.adapters;

import android.content.Context;
import android.util.AttributeSet;
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

        mStoryTitle = (TextView) findViewById(R.id.text_score_home);
        mStoryAuthor = (TextView) findViewById(R.id.text_score_away);
    }

    @Override
    public String toString() {
        return mStoryAuthor.getText() + "v" + mStoryTitle.getText()
                + ": " + getLeft() + "," + getTop()
                + ": " + getMeasuredWidth() + "x" + getMeasuredHeight();
    }
}
