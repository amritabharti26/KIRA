package com.Amrit_a_b.kiraapp;

import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.appbar.MaterialToolbar;

public class AboutActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);

        setContentView(
                R.layout.activity_about
        );


        // =====================================================
        // TOOLBAR
        // =====================================================

        MaterialToolbar toolbar =
                findViewById(
                        R.id.toolbar
                );

        if (toolbar != null) {

            toolbar.setNavigationOnClickListener(
                    v -> finish()
            );
        }
    }
}