package com.Amrit_a_b.kiraapp;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

public class RegisterActivity extends AppCompatActivity {

    private TextInputEditText etName, etEmail, etPassword;

    private FirebaseAuth mAuth;
    private DatabaseReference mDatabase;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register);

        mAuth = FirebaseAuth.getInstance();
        mDatabase = FirebaseDatabase.getInstance().getReference("users");

        etName = findViewById(R.id.et_name);
        etEmail = findViewById(R.id.et_email);
        etPassword = findViewById(R.id.et_password);

        findViewById(R.id.btn_register).setOnClickListener(v -> registerUser());

        findViewById(R.id.tv_login_link).setOnClickListener(v -> finish());
    }

    private void registerUser() {

        String name = etName.getText() != null
                ? etName.getText().toString().trim()
                : "";

        String email = etEmail.getText() != null
                ? etEmail.getText().toString().trim()
                : "";

        String password = etPassword.getText() != null
                ? etPassword.getText().toString().trim()
                : "";

        if (TextUtils.isEmpty(name) || TextUtils.isEmpty(email) || TextUtils.isEmpty(password)) {
            Toast.makeText(this, "All fields are required", Toast.LENGTH_SHORT).show();
            return;
        }

        if (password.length() < 6) {
            Toast.makeText(this, "Password must be at least 6 characters", Toast.LENGTH_SHORT).show();
            return;
        }

        mAuth.createUserWithEmailAndPassword(email, password)
                .addOnCompleteListener(task -> {

                    if (task.isSuccessful()) {

                        FirebaseUser currentUser = mAuth.getCurrentUser();

                        if (currentUser == null) {
                            Toast.makeText(
                                    this,
                                    "Registration completed, but user information was unavailable.",
                                    Toast.LENGTH_LONG
                            ).show();
                            return;
                        }

                        String userId = currentUser.getUid();

                        User user = new User(name, email);

                        mDatabase.child(userId).setValue(user);

                        Toast.makeText(this, "Registration Successful", Toast.LENGTH_SHORT).show();

                        startActivity(new Intent(RegisterActivity.this, MainActivity.class));
                        finish();

                    } else {

                        Exception exception = task.getException();

                        String errorMessage = exception != null
                                ? exception.getMessage()
                                : "Unknown error";

                        Toast.makeText(
                                this,
                                "Registration Failed: " + errorMessage,
                                Toast.LENGTH_LONG
                        ).show();
                    }
                });
    }
}