package com.example.android.recyclerplayground.fragments;

import android.os.Bundle;
import android.support.v7.widget.GridLayoutManager;
import android.support.v7.widget.RecyclerView;

import com.example.android.recyclerplayground.InsetDecoration;
import com.example.android.recyclerplayground.adapters.StoryBrowseAdapter;
import com.yrek.incant.StoryListSpot;

public class VerticalGridFragment extends RecyclerFragment {

    public VerticalGridFragment() {
        // Fragments should have empty https://github.com/devunwired/recyclerview-playground/issues/28
    }

    public static VerticalGridFragment newInstance() {
        VerticalGridFragment fragment = new VerticalGridFragment();
        Bundle args = new Bundle();
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    protected RecyclerView.LayoutManager getLayoutManager() {
        return new GridLayoutManager(getActivity(), StoryListSpot.listNumberOfColumns, GridLayoutManager.VERTICAL, false);
    }

    @Override
    protected RecyclerView.ItemDecoration getItemDecoration() {
        return new InsetDecoration(getActivity());
    }

    @Override
    protected int getDefaultItemCount() {
        return 100;
    }

    @Override
    protected StoryBrowseAdapter getAdapter() {
        return new StoryBrowseAdapter(getActivity(), getActivity());
    }
}
