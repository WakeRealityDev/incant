package com.example.android.recyclerplayground.adapters;

import android.app.Activity;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.support.v7.widget.RecyclerView;
import android.text.Spannable;
import android.text.SpannableStringBuilder;
import android.text.style.TextAppearanceSpan;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.example.android.recyclerplayground.MainActivity;
import com.wakereality.storyfinding.R;
import com.yrek.incant.DownloadSpot;
import com.yrek.incant.EventLocalStoryLaunch;
import com.yrek.incant.Story;
import com.yrek.incant.StoryListSpot;

import org.greenrobot.eventbus.EventBus;

import java.io.IOException;
import java.util.ArrayList;


public class SimpleAdapter extends RecyclerView.Adapter<SimpleAdapter.VerticalItemHolder> {

    public static final String TAG = "RecylerViewAdap";

    private ArrayList<Story> mItems;

    private AdapterView.OnItemClickListener mOnItemClickListener;

    private Activity parentActivity;

    public SimpleAdapter(Context context, Activity launchParentActivity) {
        mItems = new ArrayList<>();
        headlineStyle = new TextAppearanceSpan(context, R.style.story_headline);
        parentActivity = launchParentActivity;
    }


    private Thread downloadingObserver = null;


    private void setDownloadingObserver() {
        synchronized (DownloadSpot.downloading) {
            if (downloadingObserver == null && ! DownloadSpot.downloading.isEmpty()) {
                downloadingObserver = new Thread() {
                    @Override
                    public void run() {
                        Log.d(TAG, "setDownloadingObserver run() " + Thread.currentThread());
                        synchronized (DownloadSpot.downloading) {
                            try {
                                DownloadSpot.downloading.wait();
                            } catch (Exception e) {
                                Log.wtf(TAG, e);
                            }
                            downloadingObserver = null;
                            // ToDo: how do we invalidate the ONE item on RecyclerView list to update, not the entire list?
                            // storyList.post(refreshStoryListRunnable);
                        }
                    }
                };
                downloadingObserver.setName("downloadingObserver");
                downloadingObserver.start();
            }
        }
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
        View root = inflater.inflate(R.layout.list_story_item, container, false);

        return new VerticalItemHolder(root, this);
    }

    private TextAppearanceSpan headlineStyle;

    @Override
    public void onBindViewHolder(VerticalItemHolder itemHolder, int position) {
        Story item = mItems.get(position);

        itemHolder.setLeftBottomNumber("p" + position);
        itemHolder.setLeftTopNumber("2.0");

        Context context = itemHolder.mStoryTitle.getContext();
        SpannableStringBuilder sb = new SpannableStringBuilder(item.getName(context));
        String storyHeadline = item.getHeadline(context);
        if (storyHeadline.length() > 0) {
            sb.append(" (");
            int start = sb.length();
            sb.append(storyHeadline);
            // sb.setSpan(new StyleSpan(android.graphics.Typeface.ITALIC), start, sb.length(), Spannable.SPAN_INCLUSIVE_INCLUSIVE);
            sb.setSpan(headlineStyle, start, sb.length(), Spannable.SPAN_INCLUSIVE_INCLUSIVE);
            sb.append(")");
        }
        itemHolder.setStoryDescription(sb);
        // itemHolder.setStoryDescription("storyDescription");

        itemHolder.setStoryAuthors(item.getAuthor(itemHolder.mStoryTitle.getContext()));

        itemHolder.setCoverImage(item);
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


    public class VerticalItemHolder extends RecyclerView.ViewHolder implements View.OnClickListener {
        private TextView mLeftTopNumber, mLeftBottomNumber;
        private TextView mHomeName, mStoryTitle;
        private ImageView cover;
        private TextView download;
        private ProgressBar progressBar;
        private TextView play;
        private View itemViewContainer;

        private SimpleAdapter mAdapter;

        public VerticalItemHolder(View itemView, SimpleAdapter adapter) {
            super(itemView);
            itemView.setOnClickListener(this);

            mAdapter = adapter;

            mLeftTopNumber = (TextView) itemView.findViewById(R.id.text_score_home);
            mLeftBottomNumber = (TextView) itemView.findViewById(R.id.text_score_away);
            mHomeName = (TextView) itemView.findViewById(R.id.text_team_home);
            mStoryTitle = (TextView) itemView.findViewById(R.id.text_team_away);
            cover = (ImageView) itemView.findViewById(R.id.cover);
            download = (TextView) itemView.findViewById(R.id.download);
            play = (TextView) itemView.findViewById(R.id.play);
            play.setVisibility(View.GONE);
            progressBar = (ProgressBar) itemView.findViewById(R.id.progressbar);
            progressBar.setVisibility(View.GONE);
            itemViewContainer = itemView;
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

        public void setCoverImage(final Story story) {

            // Set non-visible so if all conditions aren't right there will be nothing.
            cover.setVisibility(View.GONE);
            final Context context = cover.getContext();
            final String storyName = story == null ? null : story.getName(context);

            View.OnClickListener launchStoryClickListener = new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    Log.d(TAG, "OnClick SPOT_C");
                    EventBus.getDefault().post(new EventLocalStoryLaunch(parentActivity, story));
                }
            };

            if (story.isDownloaded(context)) {
                download.setVisibility(View.GONE);

                // Make the entire section, left button layout, clickable
                itemView.findViewById(R.id.buttons).setOnClickListener(launchStoryClickListener);

                if (story.getCoverImageFile(context).exists()) {
                    cover.setTag(story);
                    cover.post(new Runnable() {
                        @Override
                        public void run() {
                            Bitmap image = StoryListSpot.coverImageCache.get(storyName);
                            if (image == null) {
                                image = story.getCoverImageBitmap(context);
                                if (image == null) {
                                    return;
                                }
                                StoryListSpot.coverImageCache.put(storyName, image);
                            }
                            final Bitmap bitmap = image;
                            cover.post(new Runnable() {
                                @Override
                                public void run() {
                                    if (story == cover.getTag()) {
                                        // play.setVisibility(View.GONE);
                                        cover.setVisibility(View.VISIBLE);
                                        cover.setImageBitmap(bitmap);
                                        cover.setTag(null);
                                    }
                                }
                            });
                        }
                    });
                    //cover.setOnClickListener(launchStoryClickListener);
                } else {
                    play.setVisibility(View.VISIBLE);
                    //play.setOnClickListener(launchStoryClickListener);
                }
            } else {
                play.setVisibility(View.GONE);
                cover.setVisibility(View.GONE);
                synchronized (DownloadSpot.downloading) {
                    if (DownloadSpot.downloading.contains(storyName)) {
                        download.setVisibility(View.GONE);
                        progressBar.setVisibility(View.VISIBLE);
                        itemViewContainer.setOnClickListener(null);
                    } else {
                        download.setVisibility(View.VISIBLE);
                        download.setText(R.string.download_story);
                        itemViewContainer.setOnClickListener(new View.OnClickListener() {
                            @Override public void onClick(final View v) {
                                Log.d(TAG, "[downloadStory] OnClick SPOT_D download");
                                download.setVisibility(View.GONE);
                                progressBar.setVisibility(View.VISIBLE);
                                progressBar.setBackgroundColor(Color.parseColor("#E1BEE7"));
                                synchronized (DownloadSpot.downloading) {
                                    DownloadSpot.downloading.add(storyName);
                                    setDownloadingObserver();
                                }
                                Thread downloadThreadA = new Thread() {
                                    @Override public void run() {
                                        Log.d(TAG, "run() " + Thread.currentThread());
                                        String error = null;
                                        try {
                                            if (!story.download(context)) {
                                                Log.w(TAG, "download_invalid " + story.getName(context));
                                                error = context.getString(R.string.download_invalid, story.getName(context));
                                            }
                                        } catch (Exception e) {
                                            Log.wtf(TAG,e);
                                            error = context.getString(R.string.download_failed, story.getName(context));
                                        }
                                        synchronized (DownloadSpot.downloading) {
                                            DownloadSpot.downloading.remove(storyName);
                                            DownloadSpot.downloading.notifyAll();
                                            if (progressBar != null) {
                                                progressBar.post(new Runnable() {
                                                    @Override
                                                    public void run() {
                                                        if (progressBar != null) {
                                                            progressBar.setVisibility(View.GONE);
                                                            // call self to show icon?
                                                            // Not really recursive, as we are on a runnable.
                                                            setCoverImage(story);
                                                        }
                                                    }
                                                });
                                            }
                                        }
                                        if (error != null) {
                                            Log.w(TAG, "download error " + error);
                                            final String msg = error;
                                            v.post(new Runnable() {
                                                @Override public void run() {
                                                    Toast.makeText(context, msg, Toast.LENGTH_SHORT).show();
                                                }
                                            });
                                        }
                                    }
                                };
                                downloadThreadA.setName("downloadA");
                                downloadThreadA.start();
                            }
                        });
                    }
                }
            }
        }
    }
}
