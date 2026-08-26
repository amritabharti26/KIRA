package com.Amrit_a_b.kiraapp;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.text.TextUtils;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.auth.FirebaseAuth;

public class LoginActivity extends AppCompatActivity {

    private TextInputEditText etEmail;
    private TextInputEditText etPassword;

    private FirebaseAuth mAuth;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        mAuth = FirebaseAuth.getInstance();

        etEmail = findViewById(R.id.et_email);
        etPassword = findViewById(R.id.et_password);

        findViewById(R.id.btn_login)
                .setOnClickListener(v -> loginUser());

        findViewById(R.id.tv_register_link)
                .setOnClickListener(v ->
                        startActivity(
                                new Intent(
                                        LoginActivity.this,
                                        RegisterActivity.class
                                )
                        )
                );
    }

    @Override
    protected void onStart() {
        super.onStart();

        if (mAuth.getCurrentUser() != null) {
            openNextScreen();
        }
    }

    private void loginUser() {

        String email = etEmail.getText() != null
                ? etEmail.getText().toString().trim()
                : "";

        String password = etPassword.getText() != null
                ? etPassword.getText().toString().trim()
                : "";

        if (TextUtils.isEmpty(email)
                || TextUtils.isEmpty(password)) {

            Toast.makeText(
                    this,
                    "Please enter email and password",
                    Toast.LENGTH_SHORT
            ).show();

            return;
        }

        mAuth.signInWithEmailAndPassword(
                        email,
                        password
                )
                .addOnCompleteListener(task -> {

                    if (task.isSuccessful()) {

                        Toast.makeText(
                                this,
                                "Login Successful",
                                Toast.LENGTH_SHORT
                        ).show();

                        openNextScreen();

                    } else {

                        Exception exception =
                                task.getException();

                        String errorMessage =
                                exception != null
                                        ? exception.getMessage()
                                        : "Unknown error";

                        Toast.makeText(
                                this,
                                "Login failed: "
                                        + errorMessage,
                                Toast.LENGTH_LONG
                        ).show();
                    }
                });
    }

    private void openNextScreen() {

        SharedPreferences preferences =
                getSharedPreferences(
                        "safety_settings",
                        MODE_PRIVATE
                );

        boolean setupComplete =
                preferences.getBoolean(
                        "safety_setup_complete",
                        false
                );

        Intent intent;

        if (setupComplete) {

            intent = new Intent(
                    this,
                    MainActivity.class
            );

        } else {

            intent = new Intent(
                    this,
                    SafetySetupActivity.class
            );
        }

        startActivity(intent);
        finish();
    }
}