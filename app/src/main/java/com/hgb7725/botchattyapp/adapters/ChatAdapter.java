package com.hgb7725.botchattyapp.adapters;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.view.LayoutInflater;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.hgb7725.botchattyapp.activities.ImageViewerActivity;
import com.hgb7725.botchattyapp.databinding.ItemContainerReceivedFileBinding;
import com.hgb7725.botchattyapp.databinding.ItemContainerReceivedImageBinding;
import com.hgb7725.botchattyapp.databinding.ItemContainerReceivedMessageBinding;
import com.hgb7725.botchattyapp.databinding.ItemContainerSentFileBinding;
import com.hgb7725.botchattyapp.databinding.ItemContainerSentImageBinding;
import com.hgb7725.botchattyapp.databinding.ItemContainerSentMessageBinding;
import com.hgb7725.botchattyapp.models.ChatMessage;
import com.hgb7725.botchattyapp.utilities.FileUtils;

import java.util.List;

public class ChatAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

    private final List<ChatMessage> chatMessageList;
    private final Bitmap receiverProfileImage;
    private final String senderId;

    public static final int VIEW_TYPE_SENT_TEXT = 1;
    public static final int VIEW_TYPE_RECEIVED_TEXT = 2;
    public static final int VIEW_TYPE_SENT_IMAGE = 3;
    public static final int VIEW_TYPE_RECEIVED_IMAGE = 4;
    public static final int VIEW_TYPE_SENT_FILE = 5;
    public static final int VIEW_TYPE_RECEIVED_FILE = 6;

    public ChatAdapter(List<ChatMessage> chatMessageList, Bitmap receiverProfileImage, String senderId) {
        this.chatMessageList = chatMessageList;
        this.receiverProfileImage = receiverProfileImage;
        this.senderId = senderId;
    }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        LayoutInflater inflater = LayoutInflater.from(parent.getContext());
        switch (viewType) {
            case VIEW_TYPE_SENT_TEXT:
                return new SentTextViewHolder(ItemContainerSentMessageBinding.inflate(inflater, parent, false));
            case VIEW_TYPE_RECEIVED_TEXT:
                return new ReceivedTextViewHolder(ItemContainerReceivedMessageBinding.inflate(inflater, parent, false));
            case VIEW_TYPE_SENT_IMAGE:
                return new SentImageViewHolder(ItemContainerSentImageBinding.inflate(inflater, parent, false));
            case VIEW_TYPE_RECEIVED_IMAGE:
                return new ReceivedImageViewHolder(ItemContainerReceivedImageBinding.inflate(inflater, parent, false));
            case VIEW_TYPE_SENT_FILE:
                return new SentFileViewHolder(ItemContainerSentFileBinding.inflate(inflater, parent, false));
            case VIEW_TYPE_RECEIVED_FILE:
                return new ReceivedFileViewHolder(ItemContainerReceivedFileBinding.inflate(inflater, parent, false));
            default:
                throw new IllegalArgumentException("Unknown view type: " + viewType);
        }
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
        ChatMessage chatMessage = chatMessageList.get(position);
        switch (getItemViewType(position)) {
            case VIEW_TYPE_SENT_TEXT:
                ((SentTextViewHolder) holder).setData(chatMessage);
                break;
            case VIEW_TYPE_RECEIVED_TEXT:
                ((ReceivedTextViewHolder) holder).setData(chatMessage, receiverProfileImage);
                break;
            case VIEW_TYPE_SENT_IMAGE:
                ((SentImageViewHolder) holder).setData(chatMessage);
                break;
            case VIEW_TYPE_RECEIVED_IMAGE:
                ((ReceivedImageViewHolder) holder).setData(chatMessage, receiverProfileImage);
                break;
            case VIEW_TYPE_SENT_FILE:
                ((SentFileViewHolder) holder).setData(chatMessage);
                break;
            case VIEW_TYPE_RECEIVED_FILE:
                ((ReceivedFileViewHolder) holder).setData(chatMessage, receiverProfileImage);
                break;
        }
    }

    @Override
    public int getItemCount() {
        return chatMessageList.size();
    }

    @Override
    public int getItemViewType(int position) {
        ChatMessage message = chatMessageList.get(position);
        boolean isSender = message.getSenderId().equals(senderId);
        boolean isImage = "image".equals(message.getType());
        boolean isFile = "file".equals(message.getType());

        if (isSender && isImage) return VIEW_TYPE_SENT_IMAGE;
        if (!isSender && isImage) return VIEW_TYPE_RECEIVED_IMAGE;
        if (isSender && isFile) return VIEW_TYPE_SENT_FILE;
        if (!isSender && isFile) return VIEW_TYPE_RECEIVED_FILE;
        return isSender ? VIEW_TYPE_SENT_TEXT : VIEW_TYPE_RECEIVED_TEXT;
    }

    static class SentTextViewHolder extends RecyclerView.ViewHolder {
        private final ItemContainerSentMessageBinding binding;

        SentTextViewHolder(ItemContainerSentMessageBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }

        void setData(ChatMessage chatMessage) {
            binding.textMessage.setText(chatMessage.getMessage());
            binding.textDateTime.setText(chatMessage.getDateTime());
            binding.textMessage.setOnLongClickListener(v -> {
                ClipboardManager clipboard = (ClipboardManager) v.getContext().getSystemService(Context.CLIPBOARD_SERVICE);
                ClipData clip = ClipData.newPlainText("Copied Text", chatMessage.getMessage());
                clipboard.setPrimaryClip(clip);
                Toast.makeText(v.getContext(), "Text copied!", Toast.LENGTH_SHORT).show();
                return true;
            });
        }
    }

    static class ReceivedTextViewHolder extends RecyclerView.ViewHolder {
        private final ItemContainerReceivedMessageBinding binding;

        ReceivedTextViewHolder(ItemContainerReceivedMessageBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }

        void setData(ChatMessage chatMessage, Bitmap receiverProfileImage) {
            binding.textMessage.setText(chatMessage.getMessage());
            binding.textDateTime.setText(chatMessage.getDateTime());
            binding.imageProfile.setImageBitmap(receiverProfileImage);
            binding.textMessage.setOnLongClickListener(v -> {
                ClipboardManager clipboard = (ClipboardManager) v.getContext().getSystemService(Context.CLIPBOARD_SERVICE);
                ClipData clip = ClipData.newPlainText("Copied Text", chatMessage.getMessage());
                clipboard.setPrimaryClip(clip);
                Toast.makeText(v.getContext(), "Text copied!", Toast.LENGTH_SHORT).show();
                return true;
            });
        }
    }

    static class SentImageViewHolder extends RecyclerView.ViewHolder {
        private final ItemContainerSentImageBinding binding;

        SentImageViewHolder(ItemContainerSentImageBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }

        void setData(ChatMessage chatMessage) {
            Glide.with(binding.imageMessage.getContext())
                    .load(chatMessage.getMessage())
                    .into(binding.imageMessage);
            binding.textDateTime.setText(chatMessage.getDateTime());
            
            // Add click listener to open the full-screen image viewer
            binding.cardImage.setOnClickListener(v -> {
                Context context = v.getContext();
                Intent intent = new Intent(context, ImageViewerActivity.class);
                intent.putExtra("imageUrl", chatMessage.getMessage());
                context.startActivity(intent);
            });
        }
    }

    static class ReceivedImageViewHolder extends RecyclerView.ViewHolder {
        private final ItemContainerReceivedImageBinding binding;

        ReceivedImageViewHolder(ItemContainerReceivedImageBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }

        void setData(ChatMessage chatMessage, Bitmap receiverProfileImage) {
            Glide.with(binding.imageMessage.getContext())
                    .load(chatMessage.getMessage())
                    .into(binding.imageMessage);
            binding.textDateTime.setText(chatMessage.getDateTime());
            binding.imageProfile.setImageBitmap(receiverProfileImage);
            
            // Add click listener to open the full-screen image viewer
            binding.cardImage.setOnClickListener(v -> {
                Context context = v.getContext();
                Intent intent = new Intent(context, ImageViewerActivity.class);
                intent.putExtra("imageUrl", chatMessage.getMessage());
                context.startActivity(intent);
            });
        }
    }

    static class SentFileViewHolder extends RecyclerView.ViewHolder {
        private final ItemContainerSentFileBinding binding;

        SentFileViewHolder(ItemContainerSentFileBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }

        void setData(ChatMessage chatMessage) {
            binding.textFileName.setText(
                    FileUtils.shortenMiddleToFit(chatMessage.getFileName(), 15)
            );
            binding.textDateTime.setText(chatMessage.getDateTime());
            binding.iconFile.setImageResource(FileUtils.getFileIconRes(chatMessage.getFileName()));

            binding.fileContainer.setOnClickListener(v -> {
                FileUtils.confirmAndDownloadFile(v.getContext(), chatMessage.getMessage(), chatMessage.getFileName());
            });
        }
    }

    static class ReceivedFileViewHolder extends RecyclerView.ViewHolder {
        private final ItemContainerReceivedFileBinding binding;

        ReceivedFileViewHolder(ItemContainerReceivedFileBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }

        void setData(ChatMessage chatMessage, Bitmap receiverProfileImage) {
            binding.textFileName.setText(
                    FileUtils.shortenMiddleToFit(chatMessage.getFileName(), 15)
            );
            binding.textDateTime.setText(chatMessage.getDateTime());
            binding.iconFile.setImageResource(FileUtils.getFileIconRes(chatMessage.getFileName()));
            binding.imageProfile.setImageBitmap(receiverProfileImage);

            binding.fileContainer.setOnClickListener(v -> {
                FileUtils.confirmAndDownloadFile(v.getContext(), chatMessage.getMessage(), chatMessage.getFileName());
            });
        }
    }
}