package com.hgb7725.botchattyapp.activities;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.widget.LinearLayout;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.AppCompatButton;
import androidx.appcompat.widget.AppCompatImageView;

import com.hgb7725.botchattyapp.R;

public class HelpSupportActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_help_support);
        
        // Set up back button
        AppCompatImageView imageBack = findViewById(R.id.imageBack);
        imageBack.setOnClickListener(v -> onBackPressed());
        
        // Set up email link
        LinearLayout layoutEmail = findViewById(R.id.layoutEmail);
        layoutEmail.setOnClickListener(v -> {
            Intent intent = new Intent(Intent.ACTION_SENDTO);
            intent.setData(Uri.parse("mailto:support@botchatty.com"));
            try {
                startActivity(intent);
            } catch (Exception e) {
                Toast.makeText(HelpSupportActivity.this, "No email app found", Toast.LENGTH_SHORT).show();
            }
        });
        
        // Set up website link
        LinearLayout layoutWebsite = findViewById(R.id.layoutWebsite);
        layoutWebsite.setOnClickListener(v -> {
            Intent intent = new Intent(Intent.ACTION_VIEW);
            intent.setData(Uri.parse("https://www.botchatty.com/support"));
            try {
                startActivity(intent);
            } catch (Exception e) {
                Toast.makeText(HelpSupportActivity.this, "No web browser found", Toast.LENGTH_SHORT).show();
            }
        });
        
        // Set up contact support button
        AppCompatButton buttonContactSupport = findViewById(R.id.buttonContactSupport);
        buttonContactSupport.setOnClickListener(v -> {
            Intent intent = new Intent(Intent.ACTION_SENDTO);
            intent.setData(Uri.parse("mailto:support@botchatty.com"));
            intent.putExtra(Intent.EXTRA_SUBJECT, "BotChatty Support Request");
            try {
                startActivity(intent);
            } catch (Exception e) {
                Toast.makeText(HelpSupportActivity.this, "No email app found", Toast.LENGTH_SHORT).show();
            }
        });
        
        // Set up tutorial video links
        LinearLayout layoutTutorial1 = findViewById(R.id.layoutTutorial1);
        layoutTutorial1.setOnClickListener(v -> {
            Intent intent = new Intent(Intent.ACTION_VIEW);
            intent.setData(Uri.parse("https://www.botchatty.com/tutorials/getting-started"));
            try {
                startActivity(intent);
            } catch (Exception e) {
                Toast.makeText(HelpSupportActivity.this, "No web browser found", Toast.LENGTH_SHORT).show();
            }
        });
        
        LinearLayout layoutTutorial2 = findViewById(R.id.layoutTutorial2);
        layoutTutorial2.setOnClickListener(v -> {
            Intent intent = new Intent(Intent.ACTION_VIEW);
            intent.setData(Uri.parse("https://www.botchatty.com/tutorials/advanced-features"));
            try {
                startActivity(intent);
            } catch (Exception e) {
                Toast.makeText(HelpSupportActivity.this, "No web browser found", Toast.LENGTH_SHORT).show();
            }
        });
    }
}