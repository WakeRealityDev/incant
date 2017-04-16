package com.example.android.recyclerplayground.adapters;

import android.app.Activity;
import android.content.Context;
import android.view.View;

import com.wakereality.storyfinding.R;


/*
Thoughts: we have two sizes of images to work with
// ToDo: fast scrolling and download, and it gets confused on target
 */
public class SimpleStaggeredAdapter extends StoryBrowseAdapter {

    public SimpleStaggeredAdapter(Context context, Activity launchParentActivity) {
        super(context, launchParentActivity);
    }

    @Override
    public void onBindViewHolder(VerticalItemHolder itemHolder, int position) {
        super.onBindViewHolder(itemHolder, position);

        final View itemView = itemHolder.itemView;

        // Since now they no longer need to align, can be staggered, give more room for text
        itemHolder.setStoryDescriptionMaxLines(16);

        /*
        The original sample code picked every 4th one to artificially make higher.
         */
        if (1==2) {
            if (position % 4 == 0) {
                int height = itemView.getContext().getResources()
                        .getDimensionPixelSize(R.dimen.card_staggered_height);
                itemView.setMinimumHeight(height);
            } else {
                itemView.setMinimumHeight(0);
            }
        }
    }
}
