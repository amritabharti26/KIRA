package com.Amrit_a_b.kiraapp;

import android.content.pm.PackageInfo;
import android.os.Bundle;
import android.text.TextUtils;
import android.widget.EditText;
import android.widget.RatingBar;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.button.MaterialButton;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import java.util.HashMap;
import java.util.Map;

public class FeedbackActivity extends AppCompatActivity {

    // =========================================================
    // UI
    // =========================================================

    private RatingBar ratingBar;

    private EditText etFeedback;

    private MaterialButton btnSubmit;


    // =========================================================
    // FIREBASE
    // =========================================================

    private FirebaseAuth mAuth;

    private DatabaseReference feedbackReference;


    // =========================================================
    // CREATE
    // =========================================================

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);

        setContentView(
                R.layout.activity_feedback
        );


        // =====================================================
        // INITIALIZE UI
        // =====================================================

        ratingBar =
                findViewById(
                        R.id.rating_bar
                );

        etFeedback =
                findViewById(
                        R.id.et_feedback
                );

        btnSubmit =
                findViewById(
                        R.id.btn_submit_feedback
                );


        // =====================================================
        // FIREBASE INITIALIZATION
        // =====================================================

        mAuth =
                FirebaseAuth.getInstance();

        feedbackReference =
                FirebaseDatabase
                        .getInstance()
                        .getReference("feedback");


        // =====================================================
        // SUBMIT BUTTON
        // =====================================================

        btnSubmit.setOnClickListener(
                v -> submitFeedback()
        );
    }


    // =========================================================
    // SUBMIT FEEDBACK
    // =========================================================

    private void submitFeedback() {

        // =====================================================
        // RATING
        // =====================================================

        float rating =
                ratingBar.getRating();


        if (rating <= 0) {

            Toast.makeText(
                    this,
                    "Please select a star rating.",
                    Toast.LENGTH_SHORT
            ).show();

            return;
        }


        // =====================================================
        // COMMENT
        // =====================================================

        String comment =
                etFeedback
                        .getText()
                        .toString()
                        .trim();


        if (TextUtils.isEmpty(comment)) {

            Toast.makeText(
                    this,
                    "Please enter your feedback.",
                    Toast.LENGTH_SHORT
            ).show();

            return;
        }


        // =====================================================
        // CURRENT USER
        // =====================================================

        FirebaseUser currentUser =
                mAuth.getCurrentUser();


        String userId =
                "anonymous";


        if (currentUser != null) {

            userId =
                    currentUser.getUid();
        }


        // =====================================================
        // CREATE UNIQUE FEEDBACK ID
        // =====================================================

        String feedbackId =
                feedbackReference
                        .push()
                        .getKey();


        if (feedbackId == null) {

            Toast.makeText(
                    this,
                    "Unable to create feedback record.",
                    Toast.LENGTH_SHORT
            ).show();

            return;
        }


        // =====================================================
        // APP VERSION
        // =====================================================

        String appVersion =
                "unknown";

        try {

            PackageInfo packageInfo =
                    getPackageManager()
                            .getPackageInfo(
                                    getPackageName(),
                                    0
                            );

            appVersion =
                    packageInfo.versionName;

        } catch (Exception e) {

            e.printStackTrace();
        }


        // =====================================================
        // FEEDBACK DATA
        // =====================================================

        Map<String, Object> feedback =
                new HashMap<>();


        feedback.put(
                "userId",
                userId
        );


        feedback.put(
                "rating",
                rating
        );


        feedback.put(
                "comment",
                comment
        );


        feedback.put(
                "timestamp",
                System.currentTimeMillis()
        );


        feedback.put(
                "appVersion",
                appVersion
        );


        // =====================================================
        // DISABLE BUTTON
        // =====================================================

        btnSubmit.setEnabled(false);

        btnSubmit.setText(
                "Submitting..."
        );


        // =====================================================
        // SAVE TO FIREBASE RTDB
        // =====================================================

        feedbackReference
                .child(feedbackId)
                .setValue(feedback)
                .addOnSuccessListener(
                        unused -> {

                            Toast.makeText(
                                    FeedbackActivity.this,
                                    "Thank you for your feedback! ❤️",
                                    Toast.LENGTH_LONG
                            ).show();


                            // -----------------------------
                            // CLOSE FEEDBACK SCREEN
                            // -----------------------------

                            finish();
                        }
                )
                .addOnFailureListener(
                        error -> {

                            btnSubmit.setEnabled(
                                    true
                            );

                            btnSubmit.setText(
                                    "Submit Feedback"
                            );


                            Toast.makeText(
                                    FeedbackActivity.this,
                                    "Could not submit feedback. Please try again.",
                                    Toast.LENGTH_LONG
                            ).show();
                        }
                );
    }
}