package com.hgb7725.botchattyapp.activities;

import android.os.Bundle;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.viewpager2.adapter.FragmentStateAdapter;
import androidx.viewpager2.widget.ViewPager2;

import com.google.android.material.tabs.TabLayout;
import com.google.android.material.tabs.TabLayoutMediator;
import com.hgb7725.botchattyapp.databinding.ActivitySharedMediaBinding;
import com.hgb7725.botchattyapp.firebase.MediaFirebaseService;
import com.hgb7725.botchattyapp.fragments.MediaGalleryFragment;
import com.hgb7725.botchattyapp.models.MediaItem;
import com.hgb7725.botchattyapp.utilities.Constants;
import com.hgb7725.botchattyapp.utilities.PreferenceManager;

import java.util.ArrayList;
import java.util.List;

public class SharedMediaActivity extends AppCompatActivity {

    private ActivitySharedMediaBinding binding;
    private PreferenceManager preferenceManager;
    private MediaFirebaseService mediaFirebaseService;
    private String userId;
    private List<MediaItem> allMediaItems;
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivitySharedMediaBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        
        preferenceManager = new PreferenceManager(getApplicationContext());
        mediaFirebaseService = new MediaFirebaseService(this, preferenceManager);
        
        // Get user ID from intent
        userId = getIntent().getStringExtra(Constants.KEY_USER_ID);
        if (userId == null) {
            finish();
            return;
        }
        
        // Setup back button
        binding.imageBack.setOnClickListener(v -> onBackPressed());
        
        // Load all media
        loadAllMedia();
    }
    
    private void loadAllMedia() {
        showLoading(true);
        mediaFirebaseService.fetchSharedMedia(userId, new MediaFirebaseService.MediaFetchListener() {
            @Override
            public void onMediaFetched(List<MediaItem> mediaItems) {
                allMediaItems = mediaItems;
                
                // Check if we have any media
                if (mediaItems.isEmpty()) {
                    showEmptyState();
                } else {
                    setupViewPager();
                }
                
                showLoading(false);
            }

            @Override
            public void onError(String errorMessage) {
                showLoading(false);
                showEmptyState();
            }
        });
    }
    
    private void setupViewPager() {
        // Create separate lists for images and videos
        List<MediaItem> images = new ArrayList<>();
        List<MediaItem> videos = new ArrayList<>();
        
        for (MediaItem item : allMediaItems) {
            if (item.isImage()) {
                images.add(item);
            } else if (item.isVideo()) {
                videos.add(item);
            }
        }
        
        // Setup ViewPager adapter
        MediaTabPagerAdapter adapter = new MediaTabPagerAdapter(this, images, videos);
        binding.viewPager.setAdapter(adapter);
        
        // Connect TabLayout with ViewPager
        new TabLayoutMediator(binding.tabLayout, binding.viewPager, (tab, position) -> {
            if (position == 0) {
                tab.setText("Images (" + images.size() + ")");
            } else {
                tab.setText("Videos (" + videos.size() + ")");
            }
        }).attach();
        
        // Show the ViewPager
        binding.viewPager.setVisibility(View.VISIBLE);
        binding.textEmptyState.setVisibility(View.GONE);
    }
    
    private void showEmptyState() {
        binding.viewPager.setVisibility(View.GONE);
        binding.textEmptyState.setVisibility(View.VISIBLE);
        binding.tabLayout.setVisibility(View.GONE);
    }
    
    private void showLoading(boolean isLoading) {
        if (isLoading) {
            binding.progressBar.setVisibility(View.VISIBLE);
        } else {
            binding.progressBar.setVisibility(View.GONE);
        }
    }
    
    /**
     * ViewPager adapter for media tabs
     */
    private static class MediaTabPagerAdapter extends FragmentStateAdapter {
        private final List<MediaItem> images;
        private final List<MediaItem> videos;
        
        public MediaTabPagerAdapter(@NonNull FragmentActivity fragmentActivity, List<MediaItem> images, List<MediaItem> videos) {
            super(fragmentActivity);
            this.images = images;
            this.videos = videos;
        }

        @NonNull
        @Override
        public Fragment createFragment(int position) {
            if (position == 0) {
                // Images tab
                return MediaGalleryFragment.newInstance(new ArrayList<>(images));
            } else {
                // Videos tab
                return MediaGalleryFragment.newInstance(new ArrayList<>(videos));
            }
        }

        @Override
        public int getItemCount() {
            return 2; // Always 2 tabs (Images and Videos)
        }
    }
}