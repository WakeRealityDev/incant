package com.example.android.recyclerplayground.fragments;

import android.os.Bundle;
import android.support.v7.widget.RecyclerView;

import com.example.android.recyclerplayground.InsetDecoration;
import com.example.android.recyclerplayground.adapters.StoryBrowseAdapter;
import com.example.android.recyclerplayground.layout.FixedGridLayoutManager;


public class FixedTwoWayFragment extends RecyclerFragment{

    public FixedTwoWayFragment() {
        // Fragments should hvae empty https://github.com/devunwired/recyclerview-playground/issues/28
    }

    public static FixedTwoWayFragment newInstance() {
        FixedTwoWayFragment fragment = new FixedTwoWayFragment();
        Bundle args = new Bundle();
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    protected RecyclerView.LayoutManager getLayoutManager() {
        FixedGridLayoutManager manager = new FixedGridLayoutManager();
        manager.setTotalColumnCount(10);

        return manager;
    }

    @Override
    protected RecyclerView.ItemDecoration getItemDecoration() {
        return new InsetDecoration(getActivity());
    }

    @Override
    protected int getDefaultItemCount() {
        return 5;
    }

    @Override
    protected StoryBrowseAdapter getAdapter() {
        return new StoryBrowseAdapter(getActivity(), getActivity());
    }
}
