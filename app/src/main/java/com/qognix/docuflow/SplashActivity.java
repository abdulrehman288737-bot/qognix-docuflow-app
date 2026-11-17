package com.qognix.docuflow;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;

import android.app.Activity;

/**
 * Simple branded splash screen (Midnight Navy #0B1020, violet accent).
 * After a short fixed delay we move to MainActivity.
 * (User decision 2026-11-17: no full-screen ads anywhere in this app -
 * the splash simply opens the website.)
 */
public class SplashActivity extends Activity {

    private static final long SPLASH_MS = 1600L;
    private boolean moved = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_splash);
        new Handler(Looper.getMainLooper()).postDelayed(this::goToMain, SPLASH_MS);
    }

    private void goToMain() {
        if (moved || isFinishing()) return;
        moved = true;
        startActivity(new Intent(this, MainActivity.class));
        finish();
    }
}
