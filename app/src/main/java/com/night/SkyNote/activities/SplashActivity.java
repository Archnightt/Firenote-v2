package com.night.SkyNote.activities;

import android.content.Intent;
import android.os.Handler;
import android.os.Looper;
import android.os.Bundle;
import androidx.appcompat.app.AppCompatActivity;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.night.SkyNote.R;

public class SplashActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_splash);

        // Add a delay of 2 seconds (2000ms) before navigating
        new Handler(Looper.getMainLooper()).postDelayed(() -> {

            // Check if the user is logged in
            FirebaseAuth mAuth = FirebaseAuth.getInstance();
            FirebaseUser currentUser = mAuth.getCurrentUser();

            if (currentUser != null) {
                // User is logged in, navigate to MainActivity
                Intent intent = new Intent(this, MainActivity.class);
                startActivity(intent);
            } else {
                // User is not logged in, navigate to LoginActivity
                Intent intent = new Intent(this, LoginActivity.class);
                startActivity(intent);
            }

            // Close SplashActivity
            finish();
        }, 1000);
    }
}
