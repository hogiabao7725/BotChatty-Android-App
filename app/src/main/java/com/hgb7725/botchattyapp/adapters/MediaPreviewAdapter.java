package com.hgb7725.botchattyapp.adapters;

import android.content.Context;
import android.content.Intent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.hgb7725.botchattyapp.R;
import com.hgb7725.botchattyapp.activities.ImageViewerActivity;
import com.hgb7725.botchattyapp.activities.VideoPlayerActivity;
import com.hgb7725.botchattyapp.models.MediaItem;

import java.util.List;

/**
 * Adapter for displaying media thumbnails in the shared media preview section
 */
public class MediaPreviewAdapter extends RecyclerView.Adapter<MediaPreviewAdapter.MediaViewHolder> {

    private final List<MediaItem> mediaItems;
    private final Context context;
    private final boolean showMoreButton;
    private final int totalCount;
    private OnMoreButtonClickListener moreButtonListener;

    public interface OnMoreButtonClickListener {
        void onMoreButtonClick();
    }

    public MediaPreviewAdapter(Context context, List<MediaItem> mediaItems, boolean showMoreButton, int totalCount) {
        this.context = context;
        this.mediaItems = mediaItems;
        this.showMoreButton = showMoreButton;
        this.totalCount = totalCount;
    }

    public void setOnMoreButtonClickListener(OnMoreButtonClickListener listener) {
        this.moreButtonListener = listener;
    }

    @NonNull
    @Override
    public MediaViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_media_preview, parent, false);
        return new MediaViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull MediaViewHolder holder, int position) {
        if (position == mediaItems.size() - 1 && showMoreButton) {
            // This is the "View More" button
            Glide.with(context)
                    .load(mediaItems.get(position).getUrl())
                    .centerCrop()
                    .into(holder.imageMedia);

            // Add overlay and text showing remaining count
            holder.overlayView.setVisibility(View.VISIBLE);
            int remainingCount = totalCount - mediaItems.size() + 1;
            holder.textMoreCount.setVisibility(View.VISIBLE);
            holder.textMoreCount.setText("+" + remainingCount);
            holder.playIcon.setVisibility(View.GONE);

            // Set click listener for "View More" button
            holder.itemView.setOnClickListener(v -> {
                if (moreButtonListener != null) {
                    moreButtonListener.onMoreButtonClick();
                }
            });
        } else {
            // Regular media item
            MediaItem mediaItem = mediaItems.get(position);
            
            Glide.with(context)
                    .load(mediaItem.getUrl())
                    .centerCrop()
                    .into(holder.imageMedia);
            
            // Show play icon for videos
            holder.playIcon.setVisibility(mediaItem.isVideo() ? View.VISIBLE : View.GONE);
            
            // Hide overlay and count for regular items
            holder.overlayView.setVisibility(View.GONE);
            holder.textMoreCount.setVisibility(View.GONE);
            
            // Set click listener to open media
            holder.itemView.setOnClickListener(v -> openMedia(mediaItem));
        }
    }

    @Override
    public int getItemCount() {
        return mediaItems.size();
    }

    /**
     * Opens the selected media item in appropriate viewer
     */
    private void openMedia(MediaItem mediaItem) {
        Intent intent;
        if (mediaItem.isImage()) {
            intent = new Intent(context, ImageViewerActivity.class);
            intent.putExtra("imageUrl", mediaItem.getUrl());
        } else {
            intent = new Intent(context, VideoPlayerActivity.class);
            intent.putExtra("videoUrl", mediaItem.getUrl());
        }
        context.startActivity(intent);
    }

    static class MediaViewHolder extends RecyclerView.ViewHolder {
        ImageView imageMedia;
        View overlayView;
        TextView textMoreCount;
        ImageView playIcon;

        MediaViewHolder(@NonNull View itemView) {
            super(itemView);
            imageMedia = itemView.findViewById(R.id.imageMedia);
            overlayView = itemView.findViewById(R.id.viewOverlay);
            textMoreCount = itemView.findViewById(R.id.textMoreCount);
            playIcon = itemView.findViewById(R.id.imagePlayIcon);
        }
    }
}