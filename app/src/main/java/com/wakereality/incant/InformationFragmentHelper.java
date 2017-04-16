package com.wakereality.incant;

import android.content.Context;
import android.content.Intent;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.os.Build;
import android.text.Html;
import android.text.method.LinkMovementMethod;
import android.util.Log;
import android.util.LruCache;
import android.view.ViewGroup;
import android.widget.TextView;

import com.example.android.recyclerplayground.EventInformationFragmentPopulate;
import com.wakereality.thunderstrike.dataexchange.EventEngineProviderChange;
import com.wakereality.thunderstrike.userinterfacehelper.PickEngineProviderHelper;
import com.yrek.incant.R;
import com.yrek.incant.StoryListSpot;
import com.yrek.incant.StoryLister;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;

/**
 * Created by Stephen A. Gutknecht on 4/16/17.
 * This design is atypical, but hey ;)
 * The fragment this is populating is low-use and performance is not a concern.
 * Instead of dynamically adding views, other ideas could be to use a ViewStub and inflate a local app layout.
 */

public class InformationFragmentHelper {
    public static final String TAG = "IncantApp";

    protected PickEngineProviderHelper pickEngineProviderHelper = new PickEngineProviderHelper();
    protected Context activityContext;


    public InformationFragmentHelper(Context appContext, Context activityContextForResources) {
        if (!EventBus.getDefault().isRegistered(this)) {
            Log.i(TAG, "EventBus register");
            EventBus.getDefault().register(this);
        }

        if (StoryListSpot.storyLister == null) {
            StoryListSpot.storyLister = new StoryLister(appContext);
        }
        if (StoryListSpot.coverImageCache == null) {
            StoryListSpot.coverImageCache = new LruCache<String, Bitmap>(10);
        }

        queryRemoteStoryEngineProviders(appContext);

        activityContext = activityContextForResources;
    }


    public static void queryRemoteStoryEngineProviders(Context context) {
        // Query for Interactive Fiction engine providers.
        Intent intent = new Intent();
        // Tell Android to start Thunderword app if not already running.
        intent.addFlags(Intent.FLAG_INCLUDE_STOPPED_PACKAGES);
        intent.setAction("interactivefiction.enginemeta.runstory");
        context.sendBroadcast(intent);
    }


    /*
    Main thread to touch GUI.
    */
    @SuppressWarnings("unused")
    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onEventMainThread(EventInformationFragmentPopulate event) {
        Log.i(TAG, "received EventInformationFragmentPopulate");

        ViewGroup rootViewGroup = (ViewGroup) event.rootView.findViewById(R.id.information_layout0);
        Context viewContext = rootViewGroup.getContext();
        Resources resourceContext = rootViewGroup.getContext().getResources();

        rootViewGroup.removeAllViews();

        // Android does not seem to have a way to getText() directly from XML, so try here in code.
        TextView main_top_intro_title0 = new TextView(viewContext);
        main_top_intro_title0.setText(activityContext.getText(R.string.main_intro_title0_styled));
        // Android does not seem to have a way to word-wrap two textviews next to each other, so just append to the same textview and it will word-wrap to fit screen width.
        //main_top_intro_title0.append(". ");
        //main_top_intro_title0.append(activityContext.getText(R.string.main_intro_message0_styled));

        main_top_intro_title0.append("\n");

        rootViewGroup.addView(main_top_intro_title0);

        engine_provider_status = new TextView(viewContext);
        rootViewGroup.addView(engine_provider_status);
        pickEngineProviderHelper.redrawEngineProvider(engine_provider_status);

        populateThunderwordInformation(rootViewGroup);
    }


    public void populateThunderwordInformation(ViewGroup viewGroup) {
        TextView info2 = new TextView(viewGroup.getContext());
        info2.setText("\n");
        String outHtml2 = viewGroup.getResources().getString(R.string.information_page_thunderword_html);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            info2.append(Html.fromHtml(outHtml2, Html.FROM_HTML_MODE_LEGACY));
        } else {
            info2.append(Html.fromHtml(outHtml2));
        }
        info2.setClickable(true);
        info2.setMovementMethod(new LinkMovementMethod());

        viewGroup.addView(info2);
    }


    protected TextView engine_provider_status;

    /*
    Main thread to touch GUI.
    */
    @SuppressWarnings("unused")
    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onEventMainThread(EventEngineProviderChange event) {
        Log.i(TAG, "EventEngineProviderChange");

        if (engine_provider_status != null) {
            pickEngineProviderHelper.redrawEngineProvider(engine_provider_status);
        }
    }
}
