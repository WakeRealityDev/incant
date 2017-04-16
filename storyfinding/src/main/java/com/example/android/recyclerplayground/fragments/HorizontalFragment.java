package com.example.android.recyclerplayground.fragments;

import android.os.Bundle;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;

import com.example.android.recyclerplayground.InsetDecoration;
import com.example.android.recyclerplayground.adapters.StoryBrowseAdapter;

public class HorizontalFragment extends RecyclerFragment {

    public HorizontalFragment() {
        // Fragments should have empty https://github.com/devunwired/recyclerview-playground/issues/28
    }

    public static HorizontalFragment newInstance() {
        HorizontalFragment fragment = new HorizontalFragment();
        Bundle args = new Bundle();
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    protected RecyclerView.LayoutManager getLayoutManager() {
        return new LinearLayoutManager(getActivity(), LinearLayoutManager.HORIZONTAL, false);
    }

    @Override
    protected RecyclerView.ItemDecoration getItemDecoration() {
        return new InsetDecoration(getActivity());
    }

    @Override
    protected int getDefaultItemCount() {
        return 40;
    }

    @Override
    protected StoryBrowseAdapter getAdapter() {
        return new StoryBrowseAdapter(getActivity(), getActivity());
    }
}
