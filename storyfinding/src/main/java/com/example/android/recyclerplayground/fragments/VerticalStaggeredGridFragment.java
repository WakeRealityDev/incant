package com.example.android.recyclerplayground.fragments;

import android.os.Bundle;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.StaggeredGridLayoutManager;

import com.example.android.recyclerplayground.InsetDecoration;
import com.example.android.recyclerplayground.adapters.StoryBrowseAdapter;
import com.example.android.recyclerplayground.adapters.SimpleStaggeredAdapter;
import com.yrek.incant.StoryListSpot;

public class VerticalStaggeredGridFragment extends RecyclerFragment {

    public VerticalStaggeredGridFragment() {
        // Fragments should hvae empty https://github.com/devunwired/recyclerview-playground/issues/28
    }

    public static VerticalStaggeredGridFragment newInstance() {
        VerticalStaggeredGridFragment fragment = new VerticalStaggeredGridFragment();
        Bundle args = new Bundle();
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    protected RecyclerView.LayoutManager getLayoutManager() {
        return new StaggeredGridLayoutManager(StoryListSpot.listNumberOfColumns, StaggeredGridLayoutManager.VERTICAL);
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
        return new SimpleStaggeredAdapter(getActivity(), getActivity());
    }
}
