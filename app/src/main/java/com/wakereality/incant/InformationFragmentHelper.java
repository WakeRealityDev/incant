package com.wakereality.incant;

import android.content.Context;
import android.content.Intent;
import android.content.res.Resources;
import android.os.Build;
import android.text.Html;
import android.text.method.LinkMovementMethod;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.TextView;

import com.example.android.recyclerplayground.BrowseStoriesActivity;
import com.example.android.recyclerplayground.BrowseStoriesNewDrawerActivity;
import com.example.android.recyclerplayground.EventInformationFragmentPopulate;
import com.wakereality.storyfinding.CommonAppSetup;
import com.wakereality.thunderstrike.dataexchange.EventEngineProviderChange;
import com.wakereality.thunderstrike.userinterfacehelper.PickEngineProviderHelper;
import com.yrek.incant.Incant;
import com.yrek.incant.R;
import com.yrek.incant.StoryListSpot;
import com.yrek.runconfig.SettingsCurrent;

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

        CommonAppSetup.prepareList(appContext);

        activityContext = activityContextForResources;
    }




    /*
    Main thread to touch GUI.
    */
    @SuppressWarnings("unused")
    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onEventMainThread(final EventInformationFragmentPopulate event) {
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
        pickEngineProviderHelper.redrawEngineProvider(engine_provider_status, resourceContext.getText(R.string.information_providerchange_prefix));


        TextView legacyList = new TextView(viewContext);
        legacyList.setText("2. Legacy story list");
        legacyList.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                event.holdingActivity.startActivity(new Intent(event.holdingActivity, Incant.class));
            }
        });
        rootViewGroup.addView(legacyList);

        TextView aboutApp = new TextView(viewContext);
        aboutApp.setText("3. About Incant! for Thunderword app, licensing and other information");
        aboutApp.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                event.holdingActivity.startActivity(new Intent(event.holdingActivity, AboutAppActivity.class));
            }
        });
        rootViewGroup.addView(aboutApp);

        TextView incantSpeechLabel = new TextView(viewContext);
        incantSpeechLabel.setText("4. The Incant! app you are currently using has local engines with speech recognition and output options:");
        rootViewGroup.addView(incantSpeechLabel);


        final CheckBox incantSpeechA = new CheckBox(viewContext);
        incantSpeechA.setText("Incant! app speech output enable");
        incantSpeechA.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                SettingsCurrent.flipSpeechEnabled();
                incantSpeechA.setChecked(SettingsCurrent.getSpeechEnabled());
            }
        });
        incantSpeechA.setChecked(SettingsCurrent.getSpeechEnabled());
        rootViewGroup.addView(incantSpeechA);

        final CheckBox incantSpeechB = new CheckBox(viewContext);
        incantSpeechB.setText("Incant! app speech recognition enable");
        incantSpeechB.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                SettingsCurrent.flipSpeechRecognizerEnabled();
                incantSpeechB.setChecked(SettingsCurrent.getSpeechRecognizerEnabled());
            }
        });
        incantSpeechB.setChecked(SettingsCurrent.getSpeechRecognizerEnabled());
        rootViewGroup.addView(incantSpeechB);

        final CheckBox incantSpeechC = new CheckBox(viewContext);
        incantSpeechC.setText("Incant! app speech recognition squelch beep");
        incantSpeechC.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                SettingsCurrent.flipSpeechRecognizerMute();
                incantSpeechC.setChecked(SettingsCurrent.getSpeechRecognizerMute());
            }
        });
        incantSpeechC.setChecked(SettingsCurrent.getSpeechRecognizerMute());
        rootViewGroup.addView(incantSpeechC);

        final CheckBox incantAutoKey = new CheckBox(viewContext);
        incantAutoKey.setText("Incant! app automatic keystroke entry");
        incantAutoKey.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                SettingsCurrent.flipEnableAutoEnterOnGlkCharInput();
                incantAutoKey.setChecked(SettingsCurrent.getEnableAutoEnterOnGlkCharInput());
            }
        });
        incantAutoKey.setChecked(SettingsCurrent.getEnableAutoEnterOnGlkCharInput());
        rootViewGroup.addView(incantAutoKey);

        final CheckBox incantProfilePerformance = new CheckBox(viewContext);
        incantProfilePerformance.setText("Incant! app profile engine performance");
        incantProfilePerformance.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                SettingsCurrent.flipInterpreterProfileEnabled();
                incantProfilePerformance.setChecked(SettingsCurrent.getInterpreterProfileEnabled());
            }
        });
        incantProfilePerformance.setChecked(SettingsCurrent.getInterpreterProfileEnabled());
        rootViewGroup.addView(incantProfilePerformance);

        TextView storyListNewStyle = new TextView(viewContext);
        storyListNewStyle.setText("5. Story List alternate style (testing)");
        storyListNewStyle.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                event.holdingActivity.startActivity(new Intent(event.holdingActivity, BrowseStoriesNewDrawerActivity.class));
            }
        });
        rootViewGroup.addView(storyListNewStyle);

        if (! StoryListSpot.recordAudioPermissionReady) {
            TextView incantAudioPermission = new TextView(viewContext);
            incantAudioPermission.setText(R.string.information_audio_permission_warn0);
            rootViewGroup.addView(incantAudioPermission);
        }

        // ToDo: add Thunderword spinner from other fragments for picking Activity?

        populateThunderwordInformation(rootViewGroup);

        int marginTop = (int)(4 * resourceContext.getDisplayMetrics().density);
        View[] viewsList = new View[] { legacyList, aboutApp, incantSpeechLabel };
        setMargnsForList(viewsList, marginTop);
    }


    /*
    Assumption?: Using padding instead of margins gives more touch surface.
     */
    public void setMargnsForList(View[] viewsList, int pixels) {
        for (int i = 0; i < viewsList.length; i++) {
            View targetView = viewsList[i];
            ViewGroup.MarginLayoutParams lp = (ViewGroup.MarginLayoutParams) viewsList[i].getLayoutParams();
            // int left, int top, int right, int bottom
            viewsList[i].setPadding(targetView.getPaddingLeft(), pixels, targetView.getPaddingRight(), targetView.getPaddingBottom());
        }
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
            pickEngineProviderHelper.redrawEngineProvider(engine_provider_status, engine_provider_status.getResources().getText(R.string.information_providerchange_prefix));
        }
    }
}
