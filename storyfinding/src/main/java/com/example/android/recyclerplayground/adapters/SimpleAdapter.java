package com.example.android.recyclerplayground.adapters;

import android.content.Context;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.TextView;

import com.wakereality.storyfinding.R;
import com.yrek.incant.Story;
import com.yrek.incant.StoryListSpot;

import java.io.IOException;
import java.util.ArrayList;


public class SimpleAdapter extends RecyclerView.Adapter<SimpleAdapter.VerticalItemHolder> {

    private ArrayList<Story> mItems;

    private AdapterView.OnItemClickListener mOnItemClickListener;

    public SimpleAdapter() {
        mItems = new ArrayList<>();
    }

    /*
     * A common adapter modification or reset mechanism. As with ListAdapter,
     * calling notifyDataSetChanged() will trigger the RecyclerView to update
     * the view. However, this method will not trigger any of the RecyclerView
     * animation features.
     */
    public void setItemCount(int count, Context context) {
        mItems.clear();
        try {
            mItems.addAll(StoryListSpot.storyLister.getStories(StoryListSpot.storyLister.SortByDefault, StoryListSpot.readCommaSepValuesFile, context));
        } catch (IOException e) {
            Log.e("SimpleAdapter", "Exception ", e);
        }

        notifyDataSetChanged();
    }

    /*
     * Inserting a new item at the head of the list. This uses a specialized
     * RecyclerView method, notifyItemInserted(), to trigger any enabled item
     * animations in addition to updating the view.
     */
    public void addItem(int position) {
        if (position > mItems.size()) return;
        
        // mItems.add(position, generateDummyItem());
        notifyItemInserted(position);
    }

    /*
     * Inserting a new item at the head of the list. This uses a specialized
     * RecyclerView method, notifyItemRemoved(), to trigger any enabled item
     * animations in addition to updating the view.
     */
    public void removeItem(int position) {
        if (position >= mItems.size()) return;

        mItems.remove(position);
        notifyItemRemoved(position);
    }

    @Override
    public VerticalItemHolder onCreateViewHolder(ViewGroup container, int viewType) {
        LayoutInflater inflater = LayoutInflater.from(container.getContext());
        View root = inflater.inflate(R.layout.view_match_item, container, false);

        return new VerticalItemHolder(root, this);
    }

    @Override
    public void onBindViewHolder(VerticalItemHolder itemHolder, int position) {
        Story item = mItems.get(position);

        itemHolder.setLeftBottomNumber("p" + position);
        itemHolder.setLeftTopNumber("2.0");

        // itemHolder.setStoryDescription(item.getTitle(itemHolder.mStoryTitle.getContext()));
        itemHolder.setStoryDescription("storyDescription");

        itemHolder.setStoryAuthors(item.getAuthor(itemHolder.mStoryTitle.getContext()));
    }

    @Override
    public int getItemCount() {
        return mItems.size();
    }

    public void setOnItemClickListener(AdapterView.OnItemClickListener onItemClickListener) {
        mOnItemClickListener = onItemClickListener;
    }

    private void onItemHolderClick(VerticalItemHolder itemHolder) {
        if (mOnItemClickListener != null) {
            mOnItemClickListener.onItemClick(null, itemHolder.itemView,
                    itemHolder.getAdapterPosition(), itemHolder.getItemId());
        }
    }


    public static class VerticalItemHolder extends RecyclerView.ViewHolder implements View.OnClickListener {
        private TextView mLeftTopNumber, mLeftBottomNumber;
        private TextView mHomeName, mStoryTitle;

        private SimpleAdapter mAdapter;

        public VerticalItemHolder(View itemView, SimpleAdapter adapter) {
            super(itemView);
            itemView.setOnClickListener(this);

            mAdapter = adapter;

            mLeftTopNumber = (TextView) itemView.findViewById(R.id.text_score_home);
            mLeftBottomNumber = (TextView) itemView.findViewById(R.id.text_score_away);
            mHomeName = (TextView) itemView.findViewById(R.id.text_team_home);
            mStoryTitle = (TextView) itemView.findViewById(R.id.text_team_away);
        }

        @Override
        public void onClick(View v) {
            mAdapter.onItemHolderClick(this);
        }

        public void setLeftTopNumber(CharSequence homeScore) {
            mLeftTopNumber.setText(homeScore);
        }

        public void setLeftBottomNumber(CharSequence awayScore) {
            mLeftBottomNumber.setText(awayScore);
        }

        public void setStoryAuthors(CharSequence homeName) {
            mHomeName.setText(homeName);
        }

        public void setStoryDescription(CharSequence awayName) {
            mStoryTitle.setText(awayName);
        }
    }
}
