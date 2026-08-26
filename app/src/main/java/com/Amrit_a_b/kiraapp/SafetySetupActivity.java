package com.Amrit_a_b.kiraapp;

import android.Manifest;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.google.android.material.button.MaterialButton;

import java.util.ArrayList;
import java.util.List;

public class SafetySetupActivity extends AppCompatActivity {

    private static final int PERMISSION_REQUEST_CODE = 200;

    private TextView tvPermissionStatus;

    private String selectedRecordingMode = "VIDEO";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_safety_setup);

        RadioGroup recordingModeGroup = findViewById(R.id.rg_recording_mode);

        MaterialButton btnContinue = findViewById(R.id.btn_continue_setup);

        tvPermissionStatus =
                findViewById(R.id.tv_permission_status);

        recordingModeGroup.setOnCheckedChangeListener(
                (group, checkedId) -> {

                    if (checkedId == R.id.rb_video_audio) {

                        selectedRecordingMode = "VIDEO";

                    } else if (checkedId == R.id.rb_audio_only) {

                        selectedRecordingMode = "AUDIO";
                    }
                }
        );

        btnContinue.setOnClickListener(v -> {

            saveRecordingPreference();

            requestRequiredPermissions();
        });
    }

    /**
     * Save the user's selected evidence recording mode.
     */
    private void saveRecordingPreference() {

        SharedPreferences preferences =
                getSharedPreferences(
                        "safety_settings",
                        MODE_PRIVATE
                );

        preferences.edit()
                .putString(
                        "recording_mode",
                        selectedRecordingMode
                )
                .apply();
    }

    /**
     * Return permissions required by KIRA.
     */
    private List<String> getRequiredPermissions() {

        List<String> permissions =
                new ArrayList<>();

        // Location for emergency location sharing
        permissions.add(
                Manifest.permission.ACCESS_FINE_LOCATION
        );

        // Emergency SMS
        permissions.add(
                Manifest.permission.SEND_SMS
        );

        // Emergency phone call
        permissions.add(
                Manifest.permission.CALL_PHONE
        );

        // Audio recording
        permissions.add(
                Manifest.permission.RECORD_AUDIO
        );

        // Camera is required only for Video + Audio
        if ("VIDEO".equals(selectedRecordingMode)) {

            permissions.add(
                    Manifest.permission.CAMERA
            );
        }

        // Notifications for Android 13+
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {

            permissions.add(
                    Manifest.permission.POST_NOTIFICATIONS
            );
        }

        return permissions;
    }

    /**
     * Check which required permissions are still missing.
     */
    private List<String> getMissingPermissions() {

        List<String> missingPermissions =
                new ArrayList<>();

        for (String permission : getRequiredPermissions()) {

            if (ContextCompat.checkSelfPermission(
                    this,
                    permission
            ) != PackageManager.PERMISSION_GRANTED) {

                missingPermissions.add(permission);
            }
        }

        return missingPermissions;
    }

    /**
     * Request only permissions that have not already been granted.
     */
    private void requestRequiredPermissions() {

        List<String> missingPermissions =
                getMissingPermissions();

        if (missingPermissions.isEmpty()) {

            completeSetup();

            return;
        }

        tvPermissionStatus.setText(
                R.string.permission_request_message
        );

        ActivityCompat.requestPermissions(
                this,
                missingPermissions.toArray(
                        new String[0]
                ),
                PERMISSION_REQUEST_CODE
        );
    }

    /**
     * Called after Android finishes the permission request.
     */
    @Override
    public void onRequestPermissionsResult(
            int requestCode,
            @NonNull String[] permissions,
            @NonNull int[] grantResults
    ) {

        super.onRequestPermissionsResult(
                requestCode,
                permissions,
                grantResults
        );

        if (requestCode != PERMISSION_REQUEST_CODE) {
            return;
        }

        /*
         * Check the actual current permission state again.
         * This is safer than relying only on grantResults.
         */
        List<String> missingPermissions =
                getMissingPermissions();

        if (missingPermissions.isEmpty()) {

            completeSetup();

        } else {

            tvPermissionStatus.setText(
                    R.string.permission_required_message
            );

            Toast.makeText(
                    this,
                    "Please allow all required permissions to complete safety setup.",
                    Toast.LENGTH_LONG
            ).show();
        }
    }

    /**
     * Mark safety setup as completed.
     */
    private void completeSetup() {

        SharedPreferences preferences =
                getSharedPreferences(
                        "safety_settings",
                        MODE_PRIVATE
                );

        preferences.edit()
                .putBoolean(
                        "safety_setup_complete",
                        true
                )
                .apply();

        Toast.makeText(
                this,
                R.string.safety_setup_complete,
                Toast.LENGTH_SHORT
        ).show();

        openMainActivity();
    }

    /**
     * Open the main dashboard.
     */
    private void openMainActivity() {

        Intent intent =
                new Intent(
                        SafetySetupActivity.this,
                        MainActivity.class
                );

        startActivity(intent);
        finish();
    }
}