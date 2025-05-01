package com.hgb7725.botchattyapp.activities;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.widget.LinearLayout;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.AppCompatImageView;

import com.hgb7725.botchattyapp.R;

public class AboutActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_about);
        
        // Set up back button
        AppCompatImageView imageBack = findViewById(R.id.imageBack);
        imageBack.setOnClickListener(v -> onBackPressed());
        
        // Set up website link
        LinearLayout layoutWebsite = findViewById(R.id.layoutWebsite);
        layoutWebsite.setOnClickListener(v -> {
            openUrl("https://www.botchatty.com");
        });
        
        // Set up email link
        LinearLayout layoutEmail = findViewById(R.id.layoutEmail);
        layoutEmail.setOnClickListener(v -> {
            Intent intent = new Intent(Intent.ACTION_SENDTO);
            intent.setData(Uri.parse("mailto:info@botchatty.com"));
            try {
                startActivity(intent);
            } catch (Exception e) {
                Toast.makeText(AboutActivity.this, "No email app found", Toast.LENGTH_SHORT).show();
            }
        });
        
        // Set up location
        LinearLayout layoutLocation = findViewById(R.id.layoutLocation);
        layoutLocation.setOnClickListener(v -> {
            Uri gmmIntentUri = Uri.parse("geo:0,0?q=123 Tech Avenue, San Francisco, CA 94107");
            Intent mapIntent = new Intent(Intent.ACTION_VIEW, gmmIntentUri);
            mapIntent.setPackage("com.google.android.apps.maps");
            try {
                startActivity(mapIntent);
            } catch (Exception e) {
                Toast.makeText(AboutActivity.this, "No map app found", Toast.LENGTH_SHORT).show();
            }
        });
        
        // Set up social media icons
        AppCompatImageView imageFacebook = findViewById(R.id.imageFacebook);
        imageFacebook.setOnClickListener(v -> {
            openUrl("https://www.facebook.com/botchatty");
        });
        
        AppCompatImageView imageTwitter = findViewById(R.id.imageTwitter);
        imageTwitter.setOnClickListener(v -> {
            openUrl("https://www.twitter.com/botchatty");
        });
        
        AppCompatImageView imageInstagram = findViewById(R.id.imageInstagram);
        imageInstagram.setOnClickListener(v -> {
            openUrl("https://www.instagram.com/botchatty");
        });
        
        AppCompatImageView imageLinkedin = findViewById(R.id.imageLinkedin);
        imageLinkedin.setOnClickListener(v -> {
            openUrl("https://www.linkedin.com/company/botchatty");
        });
    }
    
    /**
     * Helper method to open URLs
     */
    private void openUrl(String url) {
        Intent intent = new Intent(Intent.ACTION_VIEW);
        intent.setData(Uri.parse(url));
        try {
            startActivity(intent);
        } catch (Exception e) {
            Toast.makeText(AboutActivity.this, "No web browser found", Toast.LENGTH_SHORT).show();
        }
    }
}