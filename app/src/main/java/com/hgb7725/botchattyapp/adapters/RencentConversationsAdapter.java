package com.hgb7725.botchattyapp.adapters;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.firestore.FirebaseFirestore;
import com.hgb7725.botchattyapp.R;
import com.hgb7725.botchattyapp.databinding.ItemContainerRecentConversionBinding;
import com.hgb7725.botchattyapp.listeners.ConversionListener;
import com.hgb7725.botchattyapp.models.ChatMessage;
import com.hgb7725.botchattyapp.models.User;
import com.hgb7725.botchattyapp.utilities.Constants;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Objects;

public class RencentConversationsAdapter extends RecyclerView.Adapter<RencentConversationsAdapter.ConversionViewHolder> {

    private final List<ChatMessage> chatMessages;
    private final ConversionListener conversionListener;
    private final FirebaseFirestore database;

    public RencentConversationsAdapter(List<ChatMessage> chatMessages, ConversionListener conversionListener) {
        this.chatMessages = chatMessages;
        this.conversionListener = conversionListener;
        this.database = FirebaseFirestore.getInstance();
    }

    @NonNull
    @Override
    public ConversionViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        return new ConversionViewHolder(
                ItemContainerRecentConversionBinding.inflate(
                        LayoutInflater.from(parent.getContext()),
                        parent,
                        false
                )
        );
    }

    @Override
    public void onBindViewHolder(@NonNull ConversionViewHolder holder, int position) {
        holder.setData(chatMessages.get(position));
    }

    @Override
    public int getItemCount() {
        return chatMessages.size();
    }

    class ConversionViewHolder extends RecyclerView.ViewHolder {
        ItemContainerRecentConversionBinding binding;

        ConversionViewHolder(ItemContainerRecentConversionBinding itemContainerRecentConversionBinding) {
            super(itemContainerRecentConversionBinding.getRoot());
            binding = itemContainerRecentConversionBinding;
        }

        void setData(ChatMessage chatMessage) {
            // Get user data from users collection if not already loaded
            if (chatMessage.getConversionName() == null || chatMessage.getConversionImage() == null) {
                database.collection(Constants.KEY_COLLECTION_USERS)
                    .document(chatMessage.getConversionId())
                    .get()
                    .addOnSuccessListener(userDocument -> {
                        if (userDocument.exists()) {
                            chatMessage.setConversionName(userDocument.getString(Constants.KEY_NAME));
                            chatMessage.setConversionImage(userDocument.getString(Constants.KEY_IMAGE));
                            
                            // Update the UI with the loaded data
                            if (chatMessage.getConversionImage() != null) {
                                binding.imageProfile.setImageBitmap(getConversionImage(chatMessage.getConversionImage()));
                            }
                            if (chatMessage.getConversionName() != null) {
                                binding.textName.setText(chatMessage.getConversionName());
                            }
                        }
                    });
            } else {
                // Use existing data if available
                binding.imageProfile.setImageBitmap(getConversionImage(chatMessage.getConversionImage()));
                binding.textName.setText(chatMessage.getConversionName());
            }
            
            binding.textRecentMessage.setText(chatMessage.getMessage());
            binding.textDateTime.setText(getReadableDateTime(chatMessage.getDateObject()));
            
            // Display unread message count if any
            if (chatMessage.getUnreadCount() > 0) {
                binding.textUnreadCount.setVisibility(View.VISIBLE);
                binding.textUnreadCount.setText(String.valueOf(chatMessage.getUnreadCount()));
            } else {
                binding.textUnreadCount.setVisibility(View.GONE);
            }
            
            binding.getRoot().setOnClickListener(v -> {
                User user = new User();
                user.setId(chatMessage.getConversionId());
                user.setName(chatMessage.getConversionName());
                user.setImage(chatMessage.getConversionImage());
                conversionListener.onConversionClicked(user);
            });

            // Check availability status
            database.collection(Constants.KEY_COLLECTION_USERS).document(
                    chatMessage.getConversionId()
            ).addSnapshotListener((value, error) -> {
                if (error != null) {
                    return;
                }
                if (value != null) {
                    // Check if online status should be visible
                    Boolean isOnlineStatusVisible = value.getBoolean(Constants.KEY_ONLINE_STATUS_VISIBLE);
                    if (isOnlineStatusVisible == null) {
                        isOnlineStatusVisible = true;
                    }
                    
                    if (value.getLong(Constants.KEY_AVAILABILITY) != null) {
                        int availability = Objects.requireNonNull(
                                value.getLong(Constants.KEY_AVAILABILITY)
                        ).intValue();
                        boolean isOnline = availability == 1 && isOnlineStatusVisible;
                        binding.viewOnlineStatus.setBackgroundResource(
                                isOnline ? R.drawable.background_online_status : R.drawable.background_offline_status
                        );
                    }
                }
            });
        }

        private Bitmap getConversionImage(String encodedImage) {
            byte[] bytes = android.util.Base64.decode(encodedImage, android.util.Base64.DEFAULT);
            return BitmapFactory.decodeByteArray(bytes, 0, bytes.length);
        }

        private String getReadableDateTime(Date date) {
            return new SimpleDateFormat("MMMM dd, yyyy - hh:mm a", Locale.getDefault()).format(date);
        }
    }
}
