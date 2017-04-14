package com.example.android.recyclerplayground.adapters;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
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

import com.wakereality.storyfinding.R;
import com.yrek.incant.DownloadSpot;
import com.yrek.incant.EventLocalStoryLaunch;
import com.yrek.incant.ParamConst;
import com.yrek.incant.Story;
import com.yrek.incant.StoryDetails;
import com.yrek.incant.StoryListSpot;

import org.greenrobot.eventbus.EventBus;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;


public class SimpleAdapter extends RecyclerView.Adapter<SimpleAdapter.VerticalItemHolder> {

    public static final String TAG = "RecylerViewAdap";

    private ArrayList<Story> mItems;

    private AdapterView.OnItemClickListener mOnItemClickListener;
    private AdapterView.OnItemLongClickListener mOnItemLongClickListener;

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
    public void setAdapterContent(Context context) {
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
        final Story item = mItems.get(position);

        Context context = parentActivity;
        final String storyName = item.getName(context);
        SpannableStringBuilder sb = new SpannableStringBuilder(storyName);
        String storyHeadline = item.getHeadline(context);
        if (storyHeadline != null) {
            if (storyHeadline.length() > 0) {
                sb.append(" (");
                int start = sb.length();
                sb.append(storyHeadline);
                // sb.setSpan(new StyleSpan(android.graphics.Typeface.ITALIC), start, sb.length(), Spannable.SPAN_INCLUSIVE_INCLUSIVE);
                sb.setSpan(headlineStyle, start, sb.length(), Spannable.SPAN_INCLUSIVE_INCLUSIVE);
                sb.append(")");
            }
        }
        itemHolder.setStoryTitle(sb);

        itemHolder.setStoryAuthors(item.getAuthor(context));

        boolean isDownloaded = item.isDownloaded(context);
        boolean isDownloading = item.isDownloadingNow();
        boolean isDownloadError = item.getDownloadError();
        Bitmap image = null;
        if (isDownloaded) {
            final File coverImage = item.getCoverImageFile(context);
            if (coverImage.exists()) {
                image = StoryListSpot.coverImageCache.get(storyName);
                if (image == null) {
                    image = item.getCoverImageBitmap(context);
                    if (image != null) {
                        StoryListSpot.coverImageCache.put(storyName, image);
                    }
                }
            }
        }

        itemHolder.setDownloadProgress(item.isDownloadingNow());

        // null is good, it will remove image from recycled views
        itemHolder.setCoverImage(image, isDownloaded, isDownloading, isDownloadError);

        itemHolder.setStoryDescription(item.getDescription(context));

        String outEngine = "Z";
        if (item.isGlulx(context)) {
            outEngine = "G";
        }
        itemHolder.setStoryEngine(outEngine);
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

    public void setOnItemLongClickListener(AdapterView.OnItemLongClickListener onItemLongClickListener) {
        mOnItemLongClickListener = onItemLongClickListener;
    }

    private boolean onItemHolderLongClick(VerticalItemHolder itemHolder) {
        Log.d(TAG, "[RVlongClick] onItemHolderLongClick");
        if (mOnItemLongClickListener != null) {
            return mOnItemLongClickListener.onItemLongClick(null, itemHolder.itemView,
                    itemHolder.getAdapterPosition(), itemHolder.getItemId());
        }
        return false;
    }

    public Story getStoryForPosition(int position) {
        return mItems.get(position);
    }


    public class VerticalItemHolder extends RecyclerView.ViewHolder implements View.OnClickListener, View.OnLongClickListener {
        private TextView mStoryAuthors, mStoryTitle;
        private ImageView cover;
        private TextView download;
        private ProgressBar progressBar;
        private TextView play;
        private ViewGroup buttons;
        private View itemViewContainer;
        private String storyName = "";
        private TextView storyDescription;
        private TextView storyEngine;

        private SimpleAdapter mAdapter;


        public VerticalItemHolder(View itemView, SimpleAdapter adapter) {
            super(itemView);

            itemView.setOnClickListener(this);
            itemView.setOnLongClickListener(this);
            mAdapter = adapter;

            mStoryAuthors = (TextView) itemView.findViewById(R.id.text_story_authors);
            mStoryTitle = (TextView) itemView.findViewById(R.id.text_story_title);
            storyDescription = (TextView) itemView.findViewById(R.id.text_story_description);
            cover = (ImageView) itemView.findViewById(R.id.cover);
            download = (TextView) itemView.findViewById(R.id.download);
            play = (TextView) itemView.findViewById(R.id.play);
            progressBar = (ProgressBar) itemView.findViewById(R.id.progressbar);
            buttons = (ViewGroup) itemView.findViewById(R.id.buttons);
            storyEngine = (TextView) itemView.findViewById(R.id.engine_detail);

            itemViewContainer = itemView;
        }

        @Override
        public void onClick(View v) {
            mAdapter.onItemHolderClick(this);
        }

        @Override
        public boolean onLongClick(View v) {
            return mAdapter.onItemHolderLongClick(this);
        }


        public void setStoryAuthors(CharSequence storyAuthors) {
            mStoryAuthors.setText(storyAuthors);
        }

        public void setStoryTitle(CharSequence storyTitle) {
            mStoryTitle.setText(storyTitle);
        }

        // No spans are sent, so parma is not CharSequence - it is always string, as often this needs trim
        public void setStoryDescription(String storyDescriptionValue) {
            // ToDo: CSV file indicate if HTML, static prep-time check instead of runtime check
            if (storyDescriptionValue == null) {
                storyDescription.setText("");
            } else {
                // Simple HTML can be found, the easiest way to detect is a close tag like "</b>" as that pattern isn't likely to happen in non-HTML. See ToDo: above.
                if (storyDescriptionValue.contains("</")) {
                    // Going to HTML will strip whitespace behavior, so re-add newlines.  description of story "Gaucho" is a good test.
                    // Trim leading and trailing too, we only want inner newlines.
                    // ToDo: prep of CSV needs to trim. story example: "bsifhw1ik8524evd","Under the Bed"
                    String outReworkedHTML = storyDescriptionValue.trim().replace("<p>", "\n").replace("\n\n", "\n").replace("\n", "<br />");
                    Log.i(TAG, "[RVdescHTML] '" + outReworkedHTML + "'");
                    if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.N) {
                        storyDescription.setText(android.text.Html.fromHtml(outReworkedHTML, android.text.Html.FROM_HTML_MODE_LEGACY));
                    } else {
                        storyDescription.setText(android.text.Html.fromHtml(outReworkedHTML));
                    }
                } else {
                    storyDescription.setText(storyDescriptionValue.trim().replace("\n\n", "\n"));
                }
            }
        }

        public void setCoverImage(Bitmap image, boolean isDownloaded, boolean isDownloading, boolean isDownloadError) {
            cover.setImageBitmap(image);

                if (isDownloaded) {
                    // Items without image get this TextView
                    if (image == null) {
                        play.setVisibility(View.VISIBLE);
                    } else {
                        play.setVisibility(View.GONE);
                    }
                    download.setVisibility(View.GONE);
                    progressBar.setVisibility(View.GONE);
                } else {
                    play.setVisibility(View.GONE);
                    if (isDownloading) {
                        download.setVisibility(View.VISIBLE);
                        download.setText("Downloading");
                        download.setBackgroundColor(Color.parseColor("#E1BEE7"));
                        progressBar.setVisibility(View.VISIBLE);
                    } else {
                        download.setVisibility(View.VISIBLE);
                        download.setBackgroundColor(Color.parseColor("#BBDEFB"));
                        download.setText("Download");
                        if (isDownloadError) {
                            download.append("\n(Retry)");
                            download.setBackgroundColor(Color.parseColor("#FFF9C4"));
                        }
                        progressBar.setVisibility(View.GONE);
                    }
                }
        }

        /*
        ToDo: all this ImageCache is based on story name - would be better to use SHA256 hash of story.
         */
        public void setButtonAreaBehavior(final Story story, final int onPosition) {

            // Set non-visible so if all conditions aren't right there will be nothing.
            cover.setVisibility(View.GONE);
            final Context context = cover.getContext();
            storyName = story == null ? null : story.getName(context);

            View.OnClickListener launchStoryClickListener = new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    Log.d(TAG, "OnClick SPOT_C");
                    EventBus.getDefault().post(new EventLocalStoryLaunch(parentActivity, story));
                }
            };

            itemViewContainer.setOnLongClickListener(new View.OnLongClickListener() {
                @Override public boolean onLongClick(View v) {
                    Log.d(TAG, "OnLongClick SPOT_A");
                    Intent intent = new Intent(parentActivity, StoryDetails.class);
                    intent.putExtra(ParamConst.SERIALIZE_KEY_STORY, story);
                    parentActivity.startActivity(intent);
                    return true;
                }
            });

            if (story.isDownloaded(context)) {
                download.setVisibility(View.GONE);

                // Make the entire section, left button layout, clickable
                buttons.setOnClickListener(launchStoryClickListener);
                Log.i(TAG, "[RVlist][RVlistClick] setting launch click listner " + storyName);

                if (story.getCoverImageFile(context).exists()) {
                    cover.setTag(story);
                    cover.post(new Runnable() {
                        @Override
                        public void run() {
                            Bitmap image = StoryListSpot.coverImageCache.get(storyName);
                            if (image == null) {
                                image = story.getCoverImageBitmap(context);
                                if (image == null) {
                                    Log.i(TAG, "[RVlist][coverImage] imageLost " + storyName);
                                    if (play != null) {
                                        play.setVisibility(View.VISIBLE);
                                    }
                                    return;
                                }
                                StoryListSpot.coverImageCache.put(storyName, image);
                            }
                            final Bitmap bitmap = image;
                            cover.post(new Runnable() {
                                @Override
                                public void run() {
                                    if (story == cover.getTag()) {
                                        cover.setVisibility(View.VISIBLE);
                                        cover.setImageBitmap(bitmap);
                                        cover.setTag(null);
                                        if (play != null) {
                                            play.setVisibility(View.GONE);
                                        }
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
                //  final int saveOnPositon = onPosition;
                final String storyNameSaved = storyName;
                play.setVisibility(View.GONE);
                cover.setVisibility(View.GONE);
                synchronized (DownloadSpot.downloading) {
                    if (DownloadSpot.downloading.contains(storyName)) {
                        download.setVisibility(View.GONE);
                        progressBar.setVisibility(View.VISIBLE);
                        itemViewContainer.setOnClickListener(null);
                    } else {
                        progressBar.setTag(R.id.downloadPositionTag, onPosition);
                        download.setVisibility(View.VISIBLE);
                        download.setText(R.string.download_story);
                        Log.i(TAG, "[RVlist][RVlistClick] setting download listner " + storyName);
                        buttons.setOnClickListener(new View.OnClickListener() {
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
                                                error = context.getString(R.string.download_invalid, storyNameSaved);
                                            }
                                        } catch (Exception e) {
                                            Log.wtf(TAG,e);
                                            error = context.getString(R.string.download_failed, storyNameSaved);
                                        }
                                        synchronized (DownloadSpot.downloading) {
                                            DownloadSpot.downloading.remove(storyName);
                                            DownloadSpot.downloading.notifyAll();
                                            //if (progressBar != null) {
                                                progressBar.post(new Runnable() {
                                                    @Override
                                                    public void run() {
                                                        int progressBarTag = -1;
                                                        Object getTag =  progressBar.getTag(R.id.downloadPositionTag);
                                                        if (getTag != null) {
                                                            progressBarTag = (int) getTag;
                                                        }
                                                        if (progressBarTag == onPosition) {
                                                            //if (progressBar != null) {
                                                                progressBar.setVisibility(View.GONE);
                                                                // call self to show icon?
                                                                // Not really recursive, as we are on a runnable.
                                                                setButtonAreaBehavior(story, onPosition);
                                                            Log.d(TAG, "[RVlist][coverImage][RVrecycled] calling setCoverImage after download " + progressBarTag + " vs " + onPosition + " story " + storyName);
                                                            //}
                                                        } else {
                                                            Log.w(TAG, "[RVlist][coverImage][RVrecycled] detected change of reuseViewCount, " + progressBarTag + " vs " + onPosition + " skipping update. story " + storyName + " ==? " + storyNameSaved);
                                                        }
                                                    }
                                                });
                                            //}
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

        public void setStoryDescriptionMaxLines(int maxTextViewwWrappedLines) {
            storyDescription.setMaxLines(maxTextViewwWrappedLines);
        }

        public void setDownloadProgress(boolean downloadingNow) {
            if (downloadingNow) {
                progressBar.setVisibility(View.VISIBLE);
            } else {
                progressBar.setVisibility(View.GONE);
            }
        }

        public void setStoryEngine(CharSequence engineString) {
            storyEngine.setText(engineString);
        }
    }
}
