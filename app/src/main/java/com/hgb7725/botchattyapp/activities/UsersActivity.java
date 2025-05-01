package com.hgb7725.botchattyapp.activities;

import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;

import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.hgb7725.botchattyapp.adapters.UsersAdapter;
import com.hgb7725.botchattyapp.databinding.ActivityUsersBinding;
import com.hgb7725.botchattyapp.listeners.UserListener;
import com.hgb7725.botchattyapp.models.User;
import com.hgb7725.botchattyapp.utilities.Constants;
import com.hgb7725.botchattyapp.utilities.PreferenceManager;

import java.util.ArrayList;
import java.util.List;

public class UsersActivity extends BaseActivity implements UserListener {

    private ActivityUsersBinding binding;
    private PreferenceManager preferenceManager;
    private List<User> allUsers;
    private UsersAdapter usersAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityUsersBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        preferenceManager = new PreferenceManager(getApplicationContext());
        allUsers = new ArrayList<>();
        getUsers();
        setListeners();
    }

    private void setListeners() {
        binding.imageBack.setOnClickListener(v -> getOnBackPressedDispatcher().onBackPressed());
        
        // Set up text change listener for search field
        binding.inputSearch.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
                // Not needed
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                // Not needed
                filterUsers(s.toString());
            }

            @Override
            public void afterTextChanged(Editable s) {
                // Not needed
            }
        });
    }

    private void filterUsers(String query) {
        if (allUsers == null || allUsers.isEmpty()) {
            return;
        }
        
        query = query.toLowerCase().trim();
        List<User> filteredList = new ArrayList<>();
        
        if (query.isEmpty()) {
            filteredList.addAll(allUsers);
        } else {
            for (User user : allUsers) {
                if (user.getName().toLowerCase().contains(query) || 
                        user.getEmail().toLowerCase().contains(query)) {
                    filteredList.add(user);
                }
            }
        }

        if (filteredList.isEmpty()) {
            binding.textErrorMessage.setText("No matching users found");
            binding.textErrorMessage.setVisibility(View.VISIBLE);
            binding.usersRecyclerView.setVisibility(View.GONE);
        } else {
            binding.textErrorMessage.setVisibility(View.GONE);
            binding.usersRecyclerView.setVisibility(View.VISIBLE);
            usersAdapter = new UsersAdapter(filteredList, this);
            binding.usersRecyclerView.setAdapter(usersAdapter);
        }
    }

    private void getUsers() {
        loading(true);
        FirebaseFirestore database = FirebaseFirestore.getInstance();
        database.collection(Constants.KEY_COLLECTION_USERS)
                .get()
                .addOnCompleteListener(task -> {
                    loading(false);
                    String currentUserId = preferenceManager.getString(Constants.KEY_USER_ID);
                    if (task.isSuccessful() && task.getResult() != null) {
                        allUsers = new ArrayList<>();
                        for (QueryDocumentSnapshot queryDocumentSnapshot : task.getResult()) {
                            if (currentUserId.equals(queryDocumentSnapshot.getId())) {
                                continue;
                            }
                            User user = new User();
                            user.setName(queryDocumentSnapshot.getString(Constants.KEY_NAME));
                            user.setEmail(queryDocumentSnapshot.getString(Constants.KEY_EMAIL));
                            user.setImage(queryDocumentSnapshot.getString(Constants.KEY_IMAGE));
                            user.setToken(queryDocumentSnapshot.getString(Constants.KEY_FCM_TOKEN));
                            user.setId(queryDocumentSnapshot.getId());
                            allUsers.add(user);
                        }
                        if (allUsers.size() > 0) {
                            usersAdapter = new UsersAdapter(allUsers, this);
                            // attach adapter to RecyclerView and display list of users
                            binding.usersRecyclerView.setAdapter(usersAdapter);
                            binding.usersRecyclerView.setVisibility(View.VISIBLE);
                        } else {
                            showErrorMessage();
                        }
                    } else {
                        showErrorMessage();
                    }
                });
    }

    private void showErrorMessage() {
        binding.textErrorMessage.setText("No users (Problem)");
        binding.textErrorMessage.setVisibility(View.VISIBLE);
    }

    private void loading(Boolean isLoading) {
        if (isLoading) {
            binding.progressBar.setVisibility(View.VISIBLE);
        } else {
            binding.progressBar.setVisibility(View.INVISIBLE);
        }
    }

    @Override
    public void onUserClicked(User user) {
        Intent intent = new Intent(getApplicationContext(), ChatActivity.class);
        intent.putExtra(Constants.KEY_USER, user);
        startActivity(intent);
        finish();
    }
}