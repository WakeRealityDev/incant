package com.example.android.recyclerplayground.fragments;

import android.content.Context;
import android.content.res.Resources;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.TextView;

import com.wakereality.storyfinding.R;
import com.wakereality.thunderstrike.dataexchange.EngineConst;

public class StoriesSourceFragment extends android.support.v4.app.Fragment {

    public StoriesSourceFragment() {
        // Fragments should have empty https://github.com/devunwired/recyclerview-playground/issues/28
    }

    public static StoriesSourceFragment newInstance() {
        StoriesSourceFragment fragment = new StoriesSourceFragment();
        Bundle args = new Bundle();
        fragment.setArguments(args);
        return fragment;
    }

    // NOTE: not overriding onCreate to         setHasOptionsMenu(true);
    // There will be no options menu, this is intentional, as they apply to active list

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.fragment_information, container, false);

        // Reusing same layout, change title.
        TextView pageTitle0 = (TextView) rootView.findViewById(R.id.information_pagetitle0);
        pageTitle0.setText("Get More stories: Story Listing source selection");

        populateViewInParentApp(rootView);

        return rootView;
    }

    /*
    The objective here is multi-fold:
       1. to have all the bells and wistles of runtime detection of strings, language, etc.
       2. Only invoke if the Navigation Drawer actually visits this page
       3. Allow the parent app (parent to this library) to hold content and multiple apps
     */

    public void populateViewInParentApp(View rootView) {
        ViewGroup rootViewGroup = (ViewGroup) rootView.findViewById(R.id.information_layout0);
        Context viewContext = rootViewGroup.getContext();
        Resources resourceContext = rootViewGroup.getContext().getResources();

        rootViewGroup.removeAllViews();

        for (int i = 1; i < EngineConst.engineTypesFlatNames.length; i++) {
            CheckBox checkBox = new CheckBox(viewContext);
            String engineName = EngineConst.engineTypesFlatNames[i];
            checkBox.setText(engineName);
            rootViewGroup.addView(checkBox);
        }

        TextView sectionHeadDownload = new TextView(viewContext);
        sectionHeadDownload.setText("Filter entries by");
        rootViewGroup.addView(sectionHeadDownload);

        CheckBox notDownloaded = new CheckBox(viewContext);
        notDownloaded.setText("Not yet downloaded");
        rootViewGroup.addView(notDownloaded);

        CheckBox incantExpandedDownloaded = new CheckBox(viewContext);
        incantExpandedDownloaded.setText("Incant! expanded downloaded");
        rootViewGroup.addView(incantExpandedDownloaded);

        CheckBox foundByFileExtension = new CheckBox(viewContext);
        foundByFileExtension.setText("Found by file extension");
        rootViewGroup.addView(foundByFileExtension);

        TextView sectionHeadExportMyDatabase = new TextView(viewContext);
        sectionHeadExportMyDatabase.setText("Export listing of story files and SHA-256 file integrity checksums");
        rootViewGroup.addView(sectionHeadExportMyDatabase);

        TextView exportMyDatabaseA = new TextView(viewContext);
        exportMyDatabaseA.setText("1. Export my Database to community");
        rootViewGroup.addView(exportMyDatabaseA);

        TextView exportMyDatabaseB = new TextView(viewContext);
        exportMyDatabaseB.setText("2. Export my Database to Andriod clipboard");
        rootViewGroup.addView(exportMyDatabaseB);
    }

    @Override
    public void onResume() {
        super.onResume();
        populateViewInParentApp(getView());
    }
}
