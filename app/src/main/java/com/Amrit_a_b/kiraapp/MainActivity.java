package com.Amrit_a_b.kiraapp;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.hardware.Sensor;
import android.hardware.SensorManager;
import android.os.Bundle;
import android.os.Handler;
import android.speech.RecognizerIntent;
import android.speech.SpeechRecognizer;
import android.speech.RecognitionListener;
import android.util.Log;
import android.widget.TextView;

import java.util.ArrayList;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.card.MaterialCardView;

public class MainActivity extends AppCompatActivity {

    private SensorManager sensorManager;
    private Sensor accelerometer;
    private ShakeDetector shakeDetector;

    private SpeechRecognizer speechRecognizer;
    private Intent speechIntent;
    private boolean isVoiceListening = false;

    private TextView tvVoiceStatus;
    private final Handler handlerRestart = new Handler();


    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_main);


        // =====================================================
        // VOICE STATUS
        // =====================================================

        tvVoiceStatus =
                findViewById(R.id.tv_voice_sos_label);


        // =====================================================
        // DASHBOARD CARDS
        // =====================================================

        MaterialCardView cardContacts =
                findViewById(R.id.card_contacts);

        MaterialCardView cardLocation =
                findViewById(R.id.card_location);

        MaterialCardView cardDangerZones =
                findViewById(R.id.card_danger_zones);

        MaterialCardView cardHistory =
                findViewById(R.id.card_history);

        MaterialCardView cardTips =
                findViewById(R.id.card_tips);

        MaterialCardView cardEvidence =
                findViewById(R.id.card_evidence);


        // =====================================================
        // CONTACTS
        // =====================================================

        if (cardContacts != null) {

            cardContacts.setOnClickListener(v ->
                    startWithTransition(
                            EmergencyContactsActivity.class
                    )
            );
        }


        // =====================================================
        // LIVE LOCATION
        // =====================================================

        if (cardLocation != null) {

            cardLocation.setOnClickListener(v ->
                    startWithTransition(
                            LiveLocationActivity.class
                    )
            );
        }


        // =====================================================
        // DANGER ZONES
        // =====================================================

        if (cardDangerZones != null) {

            cardDangerZones.setOnClickListener(v ->
                    startWithTransition(
                            DangerZonesActivity.class
                    )
            );
        }


        // =====================================================
        // ALERT HISTORY
        // =====================================================

        if (cardHistory != null) {

            cardHistory.setOnClickListener(v ->
                    startWithTransition(
                            AlertHistoryActivity.class
                    )
            );
        }


        // =====================================================
        // SAFETY TIPS
        // =====================================================

        if (cardTips != null) {

            cardTips.setOnClickListener(v ->
                    startWithTransition(
                            SafetyTipsActivity.class
                    )
            );
        }


        // =====================================================
        // EMERGENCY EVIDENCE
        // =====================================================

        if (cardEvidence != null) {

            cardEvidence.setOnClickListener(v ->
                    startWithTransition(
                            EvidenceActivity.class
                    )
            );
        }


        // =====================================================
        // SOS BUTTON
        // =====================================================

        findViewById(R.id.sos_button)
                .setOnClickListener(v -> {

                    stopVoiceListeningForSOS();

                    startWithTransition(
                            EmergencyAlertActivity.class
                    );
                });


        // =====================================================
        // BOTTOM NAVIGATION
        // =====================================================

        BottomNavigationView bottomNav =
                findViewById(
                        R.id.bottom_navigation
                );


        if (bottomNav != null) {

            bottomNav.setSelectedItemId(
                    R.id.nav_home
            );


            bottomNav.setOnItemSelectedListener(
                    item -> {

                        int id =
                                item.getItemId();


                        if (id == R.id.nav_home) {

                            return true;
                        }


                        else if (
                                id == R.id.nav_contacts
                        ) {

                            startWithTransition(
                                    EmergencyContactsActivity.class
                            );

                            return true;
                        }


                        else if (
                                id == R.id.nav_history
                        ) {

                            startWithTransition(
                                    AlertHistoryActivity.class
                            );

                            return true;
                        }


                        else if (
                                id == R.id.nav_settings
                        ) {

                            startWithTransition(
                                    ProfileActivity.class
                            );

                            return true;
                        }


                        return false;
                    }
            );
        }


        // =====================================================
        // SHAKE DETECTION
        // =====================================================

        sensorManager =
                (SensorManager)
                        getSystemService(
                                SENSOR_SERVICE
                        );


        if (sensorManager != null) {

            accelerometer =
                    sensorManager.getDefaultSensor(
                            Sensor.TYPE_ACCELEROMETER
                    );


            shakeDetector =
                    new ShakeDetector(() -> {

                        if (!isFinishing()) {

                            // Stop Voice SOS before starting emergency recording
                            stopVoiceListeningForSOS();

                            startWithTransition(
                                    EmergencyAlertActivity.class
                            );
                        }
                    });
        }


        // =====================================================
        // VOICE SOS
        // =====================================================

        initVoiceTrigger();
    }


    // =========================================================
    // SCREEN TRANSITION
    // =========================================================

    private void startWithTransition(
            Class<?> cls
    ) {

        Intent intent =
                new Intent(
                        this,
                        cls
                );

        startActivity(intent);


        overridePendingTransition(
                android.R.anim.fade_in,
                android.R.anim.fade_out
        );
    }


    // =========================================================
    // VOICE SOS
    // =========================================================

    private void initVoiceTrigger() {

        if (!SpeechRecognizer
                .isRecognitionAvailable(this)) {

            if (tvVoiceStatus != null) {

                tvVoiceStatus.setText(
                        R.string.voice_sos_not_available
                );
            }

            return;
        }


        speechRecognizer =
                SpeechRecognizer
                        .createSpeechRecognizer(this);


        speechIntent =
                new Intent(
                        RecognizerIntent
                                .ACTION_RECOGNIZE_SPEECH
                );


        speechIntent.putExtra(
                RecognizerIntent
                        .EXTRA_LANGUAGE_MODEL,

                RecognizerIntent
                        .LANGUAGE_MODEL_FREE_FORM
        );


        speechIntent.putExtra(
                RecognizerIntent
                        .EXTRA_PARTIAL_RESULTS,

                true
        );


        speechRecognizer
                .setRecognitionListener(
                        new RecognitionListener() {

                            @Override
                            public void onResults(
                                    Bundle results
                            ) {

                                isVoiceListening =
                                        false;


                                ArrayList<String>
                                        matches =
                                        results
                                                .getStringArrayList(
                                                        SpeechRecognizer
                                                                .RESULTS_RECOGNITION
                                                );


                                if (matches != null) {

                                    for (
                                            String word
                                            : matches
                                    ) {

                                        String w =
                                                word.toLowerCase();


                                        if (
                                                w.contains("help")
                                                        || w.contains("sos")
                                                        || w.contains("bachao")
                                                        || w.contains("save me")
                                        ) {

                                            stopVoiceListeningForSOS();

                                            startWithTransition(
                                                    EmergencyAlertActivity.class
                                            );

                                            return;
                                        }
                                    }
                                }


                                startListening();
                            }


                            @Override
                            public void onPartialResults(
                                    Bundle partialResults
                            ) {

                                ArrayList<String>
                                        matches =
                                        partialResults
                                                .getStringArrayList(
                                                        SpeechRecognizer
                                                                .RESULTS_RECOGNITION
                                                );


                                if (
                                        matches != null
                                                && !matches.isEmpty()
                                ) {

                                    String text =
                                            matches
                                                    .get(0)
                                                    .toLowerCase();


                                    if (
                                            tvVoiceStatus
                                                    != null
                                    ) {

                                        tvVoiceStatus
                                                .setText(
                                                        getString(
                                                                R.string.hearing_text,
                                                                text
                                                        )
                                                );
                                    }


                                    if (
                                            text.contains("help")
                                                    || text.contains("sos")
                                                    || text.contains("bachao")
                                                    || text.contains("save me")
                                    ) {

                                        stopVoiceListeningForSOS();

                                        startWithTransition(
                                                EmergencyAlertActivity.class
                                        );
                                    }
                                }
                            }


                            @Override
                            public void onReadyForSpeech(
                                    Bundle params
                            ) {

                                isVoiceListening =
                                        true;


                                if (
                                        tvVoiceStatus
                                                != null
                                ) {

                                    tvVoiceStatus
                                            .setText(
                                                    R.string.voice_sos_listening
                                            );
                                }
                            }


                            @Override
                            public void onError(
                                    int error
                            ) {

                                isVoiceListening =
                                        false;


                                handlerRestart
                                        .postDelayed(
                                                () ->
                                                        startListening(),
                                                1000
                                        );
                            }


                            @Override
                            public void onBeginningOfSpeech() {
                            }


                            @Override
                            public void onRmsChanged(
                                    float rmsdB
                            ) {
                            }


                            @Override
                            public void onBufferReceived(
                                    byte[] buffer
                            ) {
                            }


                            @Override
                            public void onEndOfSpeech() {

                                isVoiceListening =
                                        false;


                                handlerRestart
                                        .postDelayed(
                                                () ->
                                                        startListening(),
                                                500
                                        );
                            }


                            @Override
                            public void onEvent(
                                    int eventType,
                                    Bundle params
                            ) {
                            }
                        }
                );
    }


    // =========================================================
    // START VOICE LISTENING
    // =========================================================

    private void startListening() {

        if (
                isFinishing()
                        || isDestroyed()
        ) {

            return;
        }


        if (
                speechRecognizer != null
                        && !isVoiceListening
        ) {

            if (
                    ActivityCompat.checkSelfPermission(
                            this,
                            Manifest.permission.RECORD_AUDIO
                    )
                            == PackageManager.PERMISSION_GRANTED
            ) {

                try {

                    speechRecognizer
                            .startListening(
                                    speechIntent
                            );

                } catch (Exception e) {

                    Log.e(
                            "Voice",
                            "Error: "
                                    + e.getMessage()
                    );
                }
            }
        }
    }


    // =========================================================
    // RESUME
    // =========================================================

    @Override
    protected void onResume() {

        super.onResume();


        if (
                sensorManager != null
                        && accelerometer != null
        ) {

            sensorManager.registerListener(
                    shakeDetector,
                    accelerometer,
                    SensorManager.SENSOR_DELAY_UI
            );
        }


        startListening();
    }


    // =========================================================
    // PAUSE
    // =========================================================

    @Override
    protected void onPause() {

        super.onPause();


        if (sensorManager != null) {

            sensorManager.unregisterListener(
                    shakeDetector
            );
        }


        if (speechRecognizer != null) {

            speechRecognizer.stopListening();

            isVoiceListening = false;
        }
    }


    // =========================================================
    // STOP VOICE SOS FOR EMERGENCY RECORDING
    // =========================================================

    private void stopVoiceListeningForSOS() {

        if (speechRecognizer != null) {

            try {
                speechRecognizer.cancel();
            } catch (Exception e) {
                Log.e(
                        "Voice",
                        "Error stopping voice SOS",
                        e
                );
            }

            isVoiceListening = false;
        }

        handlerRestart.removeCallbacksAndMessages(null);
    }



    // =========================================================
    // DESTROY
    // =========================================================

    @Override
    protected void onDestroy() {

        super.onDestroy();


        if (speechRecognizer != null) {

            speechRecognizer.cancel();

            speechRecognizer.destroy();
        }


        handlerRestart
                .removeCallbacksAndMessages(null);
    }
}