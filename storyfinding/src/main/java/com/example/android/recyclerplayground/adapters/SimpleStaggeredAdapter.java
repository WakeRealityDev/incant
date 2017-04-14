package com.example.android.recyclerplayground.adapters;

import android.app.Activity;
import android.content.Context;
import android.view.View;

import com.wakereality.storyfinding.R;


/*
Thoughts: we have two sizes of images to work with
 */
public class SimpleStaggeredAdapter extends SimpleAdapter {

    public SimpleStaggeredAdapter(Context context, Activity launchParentActivity) {
        super(context, launchParentActivity);
    }

    @Override
    public void onBindViewHolder(VerticalItemHolder itemHolder, int position) {
        super.onBindViewHolder(itemHolder, position);

        final View itemView = itemHolder.itemView;
        if (position % 4 == 0) {
            int height = itemView.getContext().getResources()
                    .getDimensionPixelSize(R.dimen.card_staggered_height);
            itemView.setMinimumHeight(height);
        } else {
            itemView.setMinimumHeight(0);
        }
    }
}
