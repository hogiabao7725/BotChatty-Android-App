package com.hgb7725.botchattyapp.fragments;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.hgb7725.botchattyapp.R;
import com.hgb7725.botchattyapp.adapters.MediaGalleryAdapter;
import com.hgb7725.botchattyapp.models.MediaItem;

import java.util.ArrayList;
import java.util.List;

public class MediaGalleryFragment extends Fragment {

    private static final String ARG_MEDIA_ITEMS = "mediaItems";
    private List<MediaItem> mediaItems;
    private RecyclerView recyclerView;
    private TextView emptyStateTextView;

    public static MediaGalleryFragment newInstance(ArrayList<MediaItem> mediaItems) {
        MediaGalleryFragment fragment = new MediaGalleryFragment();
        Bundle args = new Bundle();
        args.putSerializable(ARG_MEDIA_ITEMS, mediaItems);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            mediaItems = (List<MediaItem>) getArguments().getSerializable(ARG_MEDIA_ITEMS);
        } else {
            mediaItems = new ArrayList<>();
        }
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_media_gallery, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        recyclerView = view.findViewById(R.id.recyclerViewMedia);
        emptyStateTextView = view.findViewById(R.id.textEmptyState);

        // Set up RecyclerView with grid layout
        int spanCount = 3; // Number of columns
        GridLayoutManager layoutManager = new GridLayoutManager(getContext(), spanCount);
        recyclerView.setLayoutManager(layoutManager);

        // Set adapter
        if (mediaItems.isEmpty()) {
            recyclerView.setVisibility(View.GONE);
            emptyStateTextView.setVisibility(View.VISIBLE);
            
            // Set appropriate empty state message based on media type
            if (getTag() != null && getTag().equals("VIDEOS")) {
                emptyStateTextView.setText(R.string.no_shared_videos);
            } else {
                emptyStateTextView.setText(R.string.no_shared_images);
            }
        } else {
            recyclerView.setVisibility(View.VISIBLE);
            emptyStateTextView.setVisibility(View.GONE);
            
            MediaGalleryAdapter adapter = new MediaGalleryAdapter(getContext(), mediaItems);
            recyclerView.setAdapter(adapter);
        }
    }
}