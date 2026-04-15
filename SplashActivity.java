package com.example.letsgo;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.view.animation.AlphaAnimation;
import android.view.animation.Animation;
import android.widget.ImageView;
import android.widget.TextView;

public class SplashActivity extends Activity {

    private static final int SPLASH_TIME = 4000; // 4 seconds
    private static final int LETTER_DELAY = 150; // speed of typing

    private final String appNameText = "Let's Go";
    private final String appCaptionText = "Your Ride, Your Way";

    private TextView appName, appCaption;
    private ImageView logo;
    private int nameIndex = 0, captionIndex = 0;
    private final Handler handler = new Handler();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_splash);

        appName = findViewById(R.id.appName);
        appCaption = findViewById(R.id.appCaption);
        logo = findViewById(R.id.logo);

        // 🌀 Fade-in animation for logo
        Animation fadeIn = new AlphaAnimation(0, 1);
        fadeIn.setDuration(1500);
        logo.startAnimation(fadeIn);

        // ✨ Start typing animations simultaneously
        handler.postDelayed(typeAppName, LETTER_DELAY);
        handler.postDelayed(typeCaption, LETTER_DELAY);

        // ⏳ Move to next screen after splash
        handler.postDelayed(() -> {
            Intent intent = new Intent(SplashActivity.this, WelcomeActivity.class);
            startActivity(intent);
            finish();
        }, SPLASH_TIME);
    }

    // Typing animation for app name
    private final Runnable typeAppName = new Runnable() {
        @Override
        public void run() {
            if (nameIndex < appNameText.length()) {
                appName.setText(appName.getText().toString() + appNameText.charAt(nameIndex));
                nameIndex++;
                handler.postDelayed(this, LETTER_DELAY);
            }
        }
    };

    // Typing animation for caption
    private final Runnable typeCaption = new Runnable() {
        @Override
        public void run() {
            if (captionIndex < appCaptionText.length()) {
                appCaption.setText(appCaption.getText().toString() + appCaptionText.charAt(captionIndex));
                captionIndex++;
                handler.postDelayed(this, LETTER_DELAY);
            }
        }
    };

    @Override
    protected void onDestroy() {
        super.onDestroy();
        handler.removeCallbacks(typeAppName);
        handler.removeCallbacks(typeCaption);
    }
}
