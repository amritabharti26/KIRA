package com.Amrit_a_b.kiraapp;

import android.os.Bundle;
import android.text.TextUtils;
import android.view.Gravity;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.card.MaterialCardView;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.List;
import java.util.Locale;


// =========================================================
// KIRA GUARDIAN - ADMIN FEEDBACK
// =========================================================
//
// Displays feedback submitted by Kira users.
//
// Data shown:
// - Rating
// - Comment
// - User ID
// - Date and time
// - App version
//
// Only the configured admin UID should be allowed to access
// this screen.
// =========================================================

public class AdminFeedbackActivity extends AppCompatActivity {

    // =========================================================
    // ADMIN UID
    // =========================================================
    //
    // IMPORTANT:
    // Replace this with YOUR Firebase Authentication UID.
    //
    // Firebase Console:
    // Authentication -> Users -> select your account -> User UID
    // =========================================================

    private static final String ADMIN_UID =
            "7FPNbKvbyeTEfhSSlW2cs70J5fR2";


    // =========================================================
    // FIREBASE
    // =========================================================

    private DatabaseReference feedbackReference;


    // =========================================================
    // UI
    // =========================================================

    private LinearLayout feedbackContainer;

    private TextView tvEmpty;


    // =========================================================
    // CREATE
    // =========================================================

    @Override
    protected void onCreate(
            Bundle savedInstanceState
    ) {

        super.onCreate(savedInstanceState);

        setContentView(
                R.layout.activity_admin_feedback
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


        // =====================================================
        // UI
        // =====================================================

        feedbackContainer =
                findViewById(
                        R.id.feedback_container
                );

        tvEmpty =
                findViewById(
                        R.id.tv_empty_feedback
                );


        // =====================================================
        // FIREBASE
        // =====================================================

        feedbackReference =
                FirebaseDatabase
                        .getInstance()
                        .getReference("feedback");


        // =====================================================
        // CHECK ADMIN
        // =====================================================

        String currentUserId =
                com.google.firebase.auth.FirebaseAuth
                        .getInstance()
                        .getCurrentUser() != null
                        ?
                        com.google.firebase.auth.FirebaseAuth
                                .getInstance()
                                .getCurrentUser()
                                .getUid()
                        :
                        null;


        if (
                currentUserId == null
                        ||
                        !ADMIN_UID.equals(
                                currentUserId
                        )
        ) {

            Toast.makeText(
                    this,
                    "Access denied.",
                    Toast.LENGTH_LONG
            ).show();

            finish();

            return;
        }


        // =====================================================
        // LOAD FEEDBACK
        // =====================================================

        loadFeedback();
    }


    // =========================================================
    // LOAD FEEDBACK
    // =========================================================

    private void loadFeedback() {

        feedbackReference
                .addListenerForSingleValueEvent(
                        new ValueEventListener() {

                            @Override
                            public void onDataChange(
                                    DataSnapshot snapshot
                            ) {

                                feedbackContainer
                                        .removeAllViews();


                                List<DataSnapshot>
                                        feedbackList =
                                        new ArrayList<>();


                                // ---------------------------------
                                // COLLECT RECORDS
                                // ---------------------------------

                                for (
                                        DataSnapshot item :
                                        snapshot.getChildren()
                                ) {

                                    feedbackList.add(
                                            item
                                    );
                                }


                                // ---------------------------------
                                // NEWEST FIRST
                                // ---------------------------------

                                Collections.sort(
                                        feedbackList,
                                        new Comparator<DataSnapshot>() {

                                            @Override
                                            public int compare(
                                                    DataSnapshot first,
                                                    DataSnapshot second
                                            ) {

                                                Long firstTime =
                                                        first.child(
                                                                "timestamp"
                                                        ).getValue(
                                                                Long.class
                                                        );


                                                Long secondTime =
                                                        second.child(
                                                                "timestamp"
                                                        ).getValue(
                                                                Long.class
                                                        );


                                                if (
                                                        firstTime == null
                                                ) {

                                                    firstTime = 0L;
                                                }


                                                if (
                                                        secondTime == null
                                                ) {

                                                    secondTime = 0L;
                                                }


                                                return Long.compare(
                                                        secondTime,
                                                        firstTime
                                                );
                                            }
                                        }
                                );


                                // ---------------------------------
                                // NO FEEDBACK
                                // ---------------------------------

                                if (
                                        feedbackList.isEmpty()
                                ) {

                                    tvEmpty.setVisibility(
                                            android.view.View.VISIBLE
                                    );

                                    return;
                                }


                                tvEmpty.setVisibility(
                                        android.view.View.GONE
                                );


                                // ---------------------------------
                                // DISPLAY
                                // ---------------------------------

                                for (
                                        DataSnapshot item :
                                        feedbackList
                                ) {

                                    addFeedbackCard(
                                            item
                                    );
                                }
                            }


                            @Override
                            public void onCancelled(
                                    DatabaseError error
                            ) {

                                Toast.makeText(
                                        AdminFeedbackActivity.this,
                                        "Unable to load feedback: "
                                                + error.getMessage(),
                                        Toast.LENGTH_LONG
                                ).show();
                            }
                        }
                );
    }


    // =========================================================
    // ADD FEEDBACK CARD
    // =========================================================

    private void addFeedbackCard(
            DataSnapshot item
    ) {

        // =====================================================
        // DATA
        // =====================================================

        Double rating =
                item.child("rating")
                        .getValue(
                                Double.class
                        );


        String comment =
                item.child("comment")
                        .getValue(
                                String.class
                        );


        String userId =
                item.child("userId")
                        .getValue(
                                String.class
                        );


        String appVersion =
                item.child("appVersion")
                        .getValue(
                                String.class
                        );


        Long timestamp =
                item.child("timestamp")
                        .getValue(
                                Long.class
                        );


        // =====================================================
        // DEFAULT VALUES
        // =====================================================

        if (rating == null) {

            rating = 0.0;
        }


        if (TextUtils.isEmpty(comment)) {

            comment =
                    "No written feedback.";
        }


        if (TextUtils.isEmpty(userId)) {

            userId =
                    "Unknown user";
        }


        if (TextUtils.isEmpty(appVersion)) {

            appVersion =
                    "Unknown";
        }


        // =====================================================
        // DATE
        // =====================================================

        String dateText =
                "Unknown date";


        if (timestamp != null) {

            SimpleDateFormat formatter =
                    new SimpleDateFormat(
                            "dd MMM yyyy, hh:mm a",
                            Locale.getDefault()
                    );


            dateText =
                    formatter.format(
                            new Date(timestamp)
                    );
        }


        // =====================================================
        // CARD
        // =====================================================

        MaterialCardView card =
                new MaterialCardView(this);


        LinearLayout.LayoutParams
                cardParams =
                new LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT,
                        LinearLayout.LayoutParams.WRAP_CONTENT
                );


        cardParams.setMargins(
                0,
                0,
                0,
                20
        );


        card.setLayoutParams(
                cardParams
        );


        card.setRadius(
                20f
        );


        card.setCardElevation(
                4f
        );


        card.setContentPadding(
                20,
                20,
                20,
                20
        );


        // =====================================================
        // CARD CONTENT
        // =====================================================

        LinearLayout content =
                new LinearLayout(this);


        content.setOrientation(
                LinearLayout.VERTICAL
        );


        // =====================================================
        // RATING
        // =====================================================

        TextView ratingView =
                new TextView(this);


        ratingView.setText(
                createStars(
                        rating
                )
                        + "  "
                        + String.format(
                        Locale.getDefault(),
                        "%.0f/5",
                        rating
                )
        );


        ratingView.setTextSize(
                22f
        );


        ratingView.setGravity(
                Gravity.CENTER_VERTICAL
        );


        // =====================================================
        // COMMENT
        // =====================================================

        TextView commentView =
                new TextView(this);


        commentView.setText(
                comment
        );


        commentView.setTextSize(
                17f
        );


        commentView.setTextColor(
                getColor(
                        android.R.color.black
                )
        );


        commentView.setPadding(
                0,
                14,
                0,
                14
        );


        // =====================================================
        // DATE
        // =====================================================

        TextView dateView =
                new TextView(this);


        dateView.setText(
                dateText
        );


        dateView.setTextSize(
                14f
        );


        dateView.setTextColor(
                0xFF777777
        );


        // =====================================================
        // USER
        // =====================================================

        TextView userView =
                new TextView(this);


        userView.setText(
                "User: "
                        + userId
        );


        userView.setTextSize(
                12f
        );


        userView.setTextColor(
                0xFF888888
        );


        userView.setPadding(
                0,
                8,
                0,
                0
        );


        // =====================================================
        // APP VERSION
        // =====================================================

        TextView versionView =
                new TextView(this);


        versionView.setText(
                "App version: "
                        + appVersion
        );


        versionView.setTextSize(
                12f
        );


        versionView.setTextColor(
                0xFF888888
        );


        // =====================================================
        // ADD VIEWS
        // =====================================================

        content.addView(
                ratingView
        );


        content.addView(
                commentView
        );


        content.addView(
                dateView
        );


        content.addView(
                userView
        );


        content.addView(
                versionView
        );


        card.addView(
                content
        );


        feedbackContainer.addView(
                card
        );
    }


    // =========================================================
    // CREATE STAR STRING
    // =========================================================

    private String createStars(
            double rating
    ) {

        StringBuilder stars =
                new StringBuilder();


        int roundedRating =
                (int) Math.round(
                        rating
                );


        for (
                int i = 1;
                i <= 5;
                i++
        ) {

            if (
                    i <= roundedRating
            ) {

                stars.append("★");

            } else {

                stars.append("☆");
            }
        }


        return stars.toString();
    }
}