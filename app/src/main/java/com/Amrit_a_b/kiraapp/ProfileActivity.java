package com.Amrit_a_b.kiraapp;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.appbar.MaterialToolbar;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

import java.util.Objects;

public class ProfileActivity extends AppCompatActivity {

    // =========================================================
    // FIREBASE
    // =========================================================

    private FirebaseAuth mAuth;


    // =========================================================
    // ADMIN UID
    // =========================================================
    //
    // IMPORTANT:
    // Replace this with YOUR actual Firebase Authentication UID.
    //
    // Firebase Console
    // → Authentication
    // → Users
    // → Your account
    // → User UID
    // =========================================================

    private static final String ADMIN_UID =
            "7FPNbKvbyeTEfhSSlW2cs70J5fR2";


    // =========================================================
    // CREATE
    // =========================================================

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);

        setContentView(
                R.layout.activity_profile
        );


        // =====================================================
        // FIREBASE AUTHENTICATION
        // =====================================================

        mAuth =
                FirebaseAuth.getInstance();

        FirebaseUser currentUser =
                mAuth.getCurrentUser();


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
        // PROFILE INFORMATION
        // =====================================================

        TextView tvEmail =
                findViewById(
                        R.id.tv_profile_email
                );

        TextView tvName =
                findViewById(
                        R.id.tv_profile_name
                );


        if (currentUser != null) {

            // -------------------------------------------------
            // EMAIL
            // -------------------------------------------------

            if (tvEmail != null) {

                String email =
                        currentUser.getEmail();

                tvEmail.setText(
                        Objects.requireNonNullElse(
                                email,
                                "Email not available"
                        )
                );
            }


            // -------------------------------------------------
            // NAME
            // -------------------------------------------------

            if (tvName != null) {

                String name =
                        currentUser.getDisplayName();

                if (
                        name != null
                                &&
                                !name.trim().isEmpty()
                ) {

                    tvName.setText(
                            name
                    );

                } else {

                    tvName.setText(
                            "Kira User"
                    );
                }
            }
        }


        // =====================================================
        // GIVE FEEDBACK
        // =====================================================
        //
        // Opens the overall Kira Guardian feedback screen.
        // =====================================================

        Button feedbackBtn =
                findViewById(
                        R.id.btn_give_feedback
                );


        if (feedbackBtn != null) {

            feedbackBtn.setOnClickListener(
                    v -> {

                        Intent intent =
                                new Intent(
                                        ProfileActivity.this,
                                        FeedbackActivity.class
                                );

                        startActivity(intent);
                    }
            );
        }


        // =====================================================
        // ADMIN FEEDBACK
        // =====================================================
        //
        // This button is visible only for the configured
        // Firebase admin account.
        // =====================================================

        Button viewFeedbackBtn =
                findViewById(
                        R.id.btn_view_feedback
                );


        if (viewFeedbackBtn != null) {

            // -----------------------------------------------
            // Keep hidden by default
            // -----------------------------------------------

            viewFeedbackBtn.setVisibility(
                    View.GONE
            );


            // -----------------------------------------------
            // Check logged-in user
            // -----------------------------------------------

            if (currentUser != null) {

                String currentUserId =
                        currentUser.getUid();


                // -------------------------------------------
                // ADMIN CHECK
                // -------------------------------------------

                if (
                        ADMIN_UID.equals(
                                currentUserId
                        )
                ) {

                    viewFeedbackBtn.setVisibility(
                            View.VISIBLE
                    );


                    viewFeedbackBtn.setOnClickListener(
                            v -> {

                                Intent intent =
                                        new Intent(
                                                ProfileActivity.this,
                                                AdminFeedbackActivity.class
                                        );

                                startActivity(intent);
                            }
                    );
                }
            }
        }


        // =========================================================
// ABOUT KIRA GUARDIAN
// =========================================================

        Button aboutBtn =
                findViewById(
                        R.id.btn_about
                );

        if (aboutBtn != null) {

            aboutBtn.setOnClickListener(
                    v -> {

                        Intent intent =
                                new Intent(
                                        ProfileActivity.this,
                                        AboutActivity.class
                                );

                        startActivity(intent);
                    }
            );
        }




        // =====================================================
        // LOGOUT
        // =====================================================

        Button logoutBtn =
                findViewById(
                        R.id.btn_logout
                );


        if (logoutBtn != null) {

            logoutBtn.setOnClickListener(
                    v -> {

                        // -------------------------------------
                        // SIGN OUT
                        // -------------------------------------

                        mAuth.signOut();


                        // -------------------------------------
                        // OPEN LOGIN
                        // -------------------------------------

                        Intent intent =
                                new Intent(
                                        ProfileActivity.this,
                                        LoginActivity.class
                                );


                        intent.setFlags(
                                Intent.FLAG_ACTIVITY_NEW_TASK
                                        |
                                        Intent.FLAG_ACTIVITY_CLEAR_TASK
                        );


                        startActivity(
                                intent
                        );


                        finish();
                    }
            );
        }
    }
}