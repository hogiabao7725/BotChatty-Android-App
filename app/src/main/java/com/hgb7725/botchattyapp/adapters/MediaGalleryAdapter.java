package com.hgb7725.botchattyapp.adapters;

import android.content.Context;
import android.content.Intent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.hgb7725.botchattyapp.R;
import com.hgb7725.botchattyapp.activities.ImageViewerActivity;
import com.hgb7725.botchattyapp.activities.VideoPlayerActivity;
import com.hgb7725.botchattyapp.models.MediaItem;

import java.util.List;

/**
 * Adapter for displaying media items in a grid layout
 */
public class MediaGalleryAdapter extends RecyclerView.Adapter<MediaGalleryAdapter.MediaViewHolder> {

    private final Context context;
    private final List<MediaItem> mediaItems;

    public MediaGalleryAdapter(Context context, List<MediaItem> mediaItems) {
        this.context = context;
        this.mediaItems = mediaItems;
    }

    @NonNull
    @Override
    public MediaViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_media_gallery, parent, false);
        return new MediaViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull MediaViewHolder holder, int position) {
        MediaItem mediaItem = mediaItems.get(position);
        
        // Load thumbnail with Glide
        Glide.with(context)
                .load(mediaItem.getUrl())
                .centerCrop()
                .into(holder.imageMedia);
        
        // Show play icon for videos
        holder.playIcon.setVisibility(mediaItem.isVideo() ? View.VISIBLE : View.GONE);
        
        // Set click listener
        holder.itemView.setOnClickListener(v -> openMedia(mediaItem));
    }

    @Override
    public int getItemCount() {
        return mediaItems.size();
    }
    
    /**
     * Opens the media item in the appropriate viewer activity
     */
    private void openMedia(MediaItem mediaItem) {
        Intent intent;
        
        if (mediaItem.isImage()) {
            // Open image viewer
            intent = new Intent(context, ImageViewerActivity.class);
            intent.putExtra("imageUrl", mediaItem.getUrl());
        } else {
            // Open video player
            intent = new Intent(context, VideoPlayerActivity.class);
            intent.putExtra("videoUrl", mediaItem.getUrl());
        }
        
        context.startActivity(intent);
    }

    static class MediaViewHolder extends RecyclerView.ViewHolder {
        ImageView imageMedia;
        ImageView playIcon;

        MediaViewHolder(@NonNull View itemView) {
            super(itemView);
            imageMedia = itemView.findViewById(R.id.imageMedia);
            playIcon = itemView.findViewById(R.id.imagePlayIcon);
        }
    }
}