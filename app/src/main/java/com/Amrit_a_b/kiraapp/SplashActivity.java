package com.Amrit_a_b.kiraapp;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import androidx.appcompat.app.AppCompatActivity;
import com.google.firebase.auth.FirebaseAuth;

public class SplashActivity extends AppCompatActivity {

    private final Handler handler = new Handler(Looper.getMainLooper());
    private Runnable runnable;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        // Removed legacy full screen flags that can cause black screens on modern Android
        setContentView(R.layout.activity_splash);

        runnable = () -> {
            if (isFinishing() || isDestroyed()) return;
            
            try {
                FirebaseAuth auth = FirebaseAuth.getInstance();
                Intent intent;
                if (auth.getCurrentUser() != null) {
                    intent = new Intent(SplashActivity.this, MainActivity.class);
                } else {
                    intent = new Intent(SplashActivity.this, LoginActivity.class);
                }
                startActivity(intent);
                finish();
            } catch (Exception e) {
                Log.e("Splash", "Navigation error: " + e.getMessage());
                if (!isFinishing()) {
                    startActivity(new Intent(SplashActivity.this, LoginActivity.class));
                    finish();
                }
            }
        };

        handler.postDelayed(runnable, 2000);
    }

    @Override
    protected void onDestroy() {
        if (handler != null && runnable != null) {
            handler.removeCallbacks(runnable);
        }
        super.onDestroy();
    }
}