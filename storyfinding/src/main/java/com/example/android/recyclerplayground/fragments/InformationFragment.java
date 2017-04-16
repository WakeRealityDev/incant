package com.example.android.recyclerplayground.fragments;

import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.example.android.recyclerplayground.EventInformationFragmentPopulate;
import com.wakereality.storyfinding.R;

import org.greenrobot.eventbus.EventBus;

public class InformationFragment extends android.support.v4.app.Fragment {

    public InformationFragment() {
        // Fragments should have empty https://github.com/devunwired/recyclerview-playground/issues/28
    }

    public static InformationFragment newInstance() {
        InformationFragment fragment = new InformationFragment();
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
        Log.d("InformationFrag", "[IncantApp] EventInformationFragmentPopulate");
        EventBus.getDefault().post(new EventInformationFragmentPopulate(rootView, getActivity()));
    }

    @Override
    public void onResume() {
        super.onResume();
        populateViewInParentApp(getView());
    }
}
