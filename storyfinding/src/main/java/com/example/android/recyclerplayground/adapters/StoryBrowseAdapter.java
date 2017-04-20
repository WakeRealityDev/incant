package com.example.android.recyclerplayground.adapters;

import android.app.Activity;
import android.content.Context;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.support.v7.widget.RecyclerView;
import android.text.Spannable;
import android.text.SpannableStringBuilder;
import android.text.style.TextAppearanceSpan;
import android.util.Log;
import android.util.SparseArray;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.wakereality.storyfinding.R;
import com.wakereality.thunderstrike.dataexchange.EngineConst;
import com.yrek.incant.Story;
import com.yrek.incant.StoryListSpot;

import java.io.File;
import java.util.ArrayList;


/*
Testing and experimenting with this vs. the original Incant LIST implementation:
The major advance of this RecyclerView in testing is that it seems to only invalidate the visible items.
  It is much smatter of detecting what is on screen at the time and only rebuilding those when dataset changes.
 */
public class StoryBrowseAdapter extends RecyclerView.Adapter<StoryBrowseAdapter.VerticalItemHolder> {

    public static final String TAG = "RecyclerViewAdap";

    private ArrayList<Story> mItems;

    private AdapterView.OnItemClickListener mOnItemClickListener;
    private AdapterView.OnItemLongClickListener mOnItemLongClickListener;
    private Resources res;

    private Activity parentActivity;

    public StoryBrowseAdapter(Context context, Activity launchParentActivity) {
        mItems = new ArrayList<>();
        headlineStyle = new TextAppearanceSpan(context, R.style.story_headline);
        parentActivity = launchParentActivity;
        res = launchParentActivity.getResources();
        buildNamesForEngines();
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
            if (StoryListSpot.storyLister != null) {
                ArrayList<Story> stories = new ArrayList<Story>();
                if (StoryListSpot.storyListAppAboveHandDown != null) {
                    stories.addAll(StoryListSpot.storyListAppAboveHandDown);
                }
                StoryListSpot.storyLister.generateStoriesListAllSortedArrayListA(stories);
                mItems.addAll(stories);
            } else {
                Log.e(TAG, "storyList is null, unable to populate");
            }
        } catch (Exception e) {
            Log.e(TAG, "Exception ", e);
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

    public SparseArray<String> engineCodeToNameCrossReference = new SparseArray<>();

    public void buildNamesForEngines() {
        engineCodeToNameCrossReference.clear();
        for (int engineIndex = 1; engineIndex < EngineConst.engineTypesFlatIndex.length; engineIndex++) {
            final int onEngineCode = EngineConst.engineTypesFlatIndex[engineIndex];
            final String onEngineName = EngineConst.engineTypesFlatNames[engineIndex];
            // ToDo: could use string resources to localize, but the names of engines are generally known in English globally
            engineCodeToNameCrossReference.put(onEngineCode, onEngineName);
        }
    }


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

        CharSequence outEngine = "?";

        boolean isDownloaded = item.isDownloaded(context);
        boolean isDownloading = item.isDownloadingNow();
        boolean isDownloadError = item.getDownloadError();
        Bitmap image = null;
        if (isDownloaded) {
            final int engineCode = item.getEngineCode();
            if (engineCode == EngineConst.ENGINE_UNKNOWN) {
                // Legacy Incant app, prep Thunderword integration.
                outEngine = res.getText(R.string.storylist_entry_engine_zmachine);
                if (item.isGlulx(context)) {
                    outEngine = res.getText(R.string.storylist_entry_engine_glulx);
                }
            } else {
                outEngine = engineCodeToNameCrossReference.get(engineCode);
            }
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
        private View externalLaunchIndicator;

        private StoryBrowseAdapter mAdapter;


        public VerticalItemHolder(View itemView, StoryBrowseAdapter adapter) {
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
            externalLaunchIndicator = itemView.findViewById(R.id.external_launch_indciator);

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
                    Log.d(TAG, "[RVdescHTML] '" + outReworkedHTML + "'");
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

            if (StoryListSpot.optionLaunchExternal) {
                if (isDownloaded) {
                    externalLaunchIndicator.setVisibility(View.VISIBLE);
                } else {
                    externalLaunchIndicator.setVisibility(View.GONE);
                }
            } else {
                externalLaunchIndicator.setVisibility(View.GONE);
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
