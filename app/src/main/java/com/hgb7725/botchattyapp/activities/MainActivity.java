package com.hgb7725.botchattyapp.activities;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.util.Base64;
import android.view.View;
import android.widget.Toast;

import com.hgb7725.botchattyapp.adapters.RencentConversationsAdapter;
import com.hgb7725.botchattyapp.databinding.ActivityMainBinding;
import com.hgb7725.botchattyapp.firebase.ConversationFirebaseService;
import com.hgb7725.botchattyapp.firebase.UserFirebaseService;
import com.hgb7725.botchattyapp.listeners.ConversionListener;
import com.hgb7725.botchattyapp.models.ChatMessage;
import com.hgb7725.botchattyapp.models.User;
import com.hgb7725.botchattyapp.utilities.Constants;
import com.hgb7725.botchattyapp.utilities.PreferenceManager;

import java.util.ArrayList;
import java.util.List;
import java.util.Collections;

public class MainActivity extends BaseActivity implements ConversionListener {

    private ActivityMainBinding binding;
    // Initialize these variables to avoid "might not have been initialized" warnings
    private PreferenceManager preferenceManager = null;
    private List<ChatMessage> conversations = new ArrayList<>();
    private RencentConversationsAdapter conversationsAdapter;
    private ConversationFirebaseService conversationService;
    private UserFirebaseService userService;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        
        // Initialize preferenceManager properly
        preferenceManager = new PreferenceManager(getApplicationContext());
        
        // Initialize Firebase services
        conversationService = new ConversationFirebaseService(getApplicationContext(), preferenceManager);
        userService = new UserFirebaseService(getApplicationContext(), preferenceManager);
        
        init();
        loadUserDetails();
        setListeners();
        setupConversationListener();
        userService.getToken();
    }

    private void init() {
        // No need to initialize conversations again since we did it at the class level
        conversationsAdapter = new RencentConversationsAdapter(conversations, this);
        binding.conversationsRecyclerView.setAdapter(conversationsAdapter);
    }

    private void setListeners() {
        binding.imageSignOut.setOnClickListener(v -> performSignOut());
        binding.fabNewChat.setOnClickListener(v ->
                startActivity(new Intent(getApplicationContext(), UsersActivity.class)));
    }

    private void loadUserDetails() {
        binding.textName.setText(preferenceManager.getString(Constants.KEY_NAME));
        byte[] bytes = Base64.decode(preferenceManager.getString(Constants.KEY_IMAGE), Base64.DEFAULT);
        Bitmap bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.length);
        binding.imageProfile.setImageBitmap(bitmap);
    }

    private void showToast(String message) {
        Toast.makeText(getApplicationContext(), message, Toast.LENGTH_SHORT).show();
    }

    private void setupConversationListener() {
        conversationService.setConversationListener(new ConversationFirebaseService.ConversationListener() {
            @Override
            public void onConversationsLoaded(List<ChatMessage> conversationList) {
                // Updated conversation list and UI
                conversations.clear();
                conversations.addAll(conversationList);
                conversationsAdapter.notifyDataSetChanged();
                binding.conversationsRecyclerView.smoothScrollToPosition(0);
                binding.conversationsRecyclerView.setVisibility(View.VISIBLE);
                binding.progressBar.setVisibility(View.GONE);
            }

            @Override
            public void onConversationsUpdated() {
                // Update adapter when there is a change
                conversationsAdapter.notifyDataSetChanged();
            }
        });


        // Start listening to conversations
        conversationService.listenConversations();
    }

    private void performSignOut() {
        showToast("Signing Out ...");
        userService.signOut(new UserFirebaseService.SignOutListener() {
            @Override
            public void onSignOutSuccess() {
                startActivity(new Intent(getApplicationContext(), SignInActivity.class));
                finish();
            }

            @Override
            public void onSignOutFailure(String errorMessage) {
                showToast(errorMessage);
            }
        });
    }

    @Override
    public void onConversionClicked(User user) {
        // Reset unread message count when user clicks on conversation
        conversationService.resetUnreadCountForSender(user.getId());
        
        Intent intent = new Intent(getApplicationContext(), ChatActivity.class);
        intent.putExtra(Constants.KEY_USER, user);
        startActivity(intent);
    }

    @Override
    protected void onResume() {
        super.onResume();
        // Refresh the conversation list when returning to MainActivity
        if (conversationService != null) {
            // Force a refresh of conversations from Firebase
            conversationService.refreshConversations();
            
            // Also sort the current list
            Collections.sort(conversations, (obj1, obj2) ->
                    obj2.getDateObject().compareTo(obj1.getDateObject()));
            conversationsAdapter.notifyDataSetChanged();
        }
    }
}