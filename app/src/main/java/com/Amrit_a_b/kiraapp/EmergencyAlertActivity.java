package com.Amrit_a_b.kiraapp;

import android.Manifest;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.media.Ringtone;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.telephony.SmsManager;
import android.util.Log;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import org.json.JSONArray;
import org.json.JSONObject;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class EmergencyAlertActivity extends AppCompatActivity {

    private static final String TAG = "EmergencyAlert";

    private static final String DB_URL =
            "https://ai-powered-women-safety-ca54a-default-rtdb.firebaseio.com/";

    private FusedLocationProviderClient fusedLocationClient;
    private DatabaseReference mDatabase;
    private FirebaseAuth mAuth;

    private List<String> contacts;
    private int currentContactIndex = 0;

    private final Handler handler =
            new Handler(Looper.getMainLooper());

    private enum RecordingMode {
        AUDIO,
        VIDEO
    }

    private RecordingMode recordingMode =
            RecordingMode.VIDEO;

    private Ringtone siren;

    private boolean isAlertRunning = true;
    private boolean isTriggered = false;

    private int countdown = 5;

    private TextView tvMessage;


    // =========================================================
    // CREATE
    // =========================================================

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);

        setContentView(
                R.layout.activity_emergency_alert
        );

        mAuth =
                FirebaseAuth.getInstance();


        try {

            mDatabase =
                    FirebaseDatabase
                            .getInstance(DB_URL)
                            .getReference();

        } catch (Exception e) {

            Log.e(
                    TAG,
                    "Firebase init error: "
                            + e.getMessage(),
                    e
            );

            mDatabase =
                    FirebaseDatabase
                            .getInstance()
                            .getReference();
        }


        fusedLocationClient =
                LocationServices
                        .getFusedLocationProviderClient(this);


        tvMessage =
                findViewById(
                        R.id.tv_alert_message
                );


        /*
         * Recording mode was selected during
         * Safety Setup.
         */

        loadRecordingMode();


        /*
         * Danger zones can load independently.
         */

        try {

            DangerZoneDetector.loadDangerZones();

        } catch (Exception e) {

            Log.e(
                    TAG,
                    "Danger zone loading failed",
                    e
            );
        }


        findViewById(
                R.id.btn_stop_alert
        ).setOnClickListener(
                v -> stopAlert()
        );


        /*
         * IMPORTANT:
         *
         * There is NO permission request here.
         *
         * Permissions must already have been
         * granted during Safety Setup.
         */

        startCountdown();
    }


    // =========================================================
    // RECORDING MODE
    // =========================================================

    private void loadRecordingMode() {

        SharedPreferences preferences =
                getSharedPreferences(
                        "safety_settings",
                        MODE_PRIVATE
                );


        String mode =
                preferences.getString(
                        "recording_mode",
                        "VIDEO"
                );


        if ("AUDIO".equalsIgnoreCase(mode)) {

            recordingMode =
                    RecordingMode.AUDIO;

        } else {

            recordingMode =
                    RecordingMode.VIDEO;
        }
    }


    // =========================================================
    // COUNTDOWN
    // =========================================================

    private void startCountdown() {

        if (
                countdown > 0
                        && isAlertRunning
        ) {

            if (tvMessage != null) {

                tvMessage.setText(
                        getString(
                                R.string.alert_countdown,
                                countdown
                        )
                );
            }


            handler.postDelayed(
                    () -> {

                        countdown--;

                        startCountdown();

                    },
                    1000
            );


        } else if (
                !isTriggered
                        && isAlertRunning
        ) {

            triggerEmergencyActions();
        }
    }


    // =========================================================
    // MAIN SOS ORCHESTRATOR
    // =========================================================

    private void triggerEmergencyActions() {

        isTriggered = true;


        if (tvMessage != null) {

            tvMessage.setText(
                    R.string.alert_sent
            );
        }


        /*
         * IMPORTANT:
         *
         * Every emergency action is independent.
         *
         * If one fails, the others continue.
         */


        // =====================================================
        // SIREN
        // =====================================================

        runEmergencyAction(
                "Siren",
                this::startSiren
        );


        // =====================================================
        // SMS
        // =====================================================

        runEmergencyAction(
                "SMS",
                this::sendQuickSMS
        );


        // =====================================================
        // LOCATION
        // =====================================================

        runEmergencyAction(
                "Location",
                this::sendSOSLocationAndSave
        );


        // =====================================================
        // LIVE TRACKING
        // =====================================================

        runEmergencyAction(
                "Live tracking",
                this::startLiveTracking
        );


        // =====================================================
        // EVIDENCE RECORDING
        // =====================================================

        /*
         * Evidence recording is completely independent.
         *
         * It does NOT depend on:
         * - SMS
         * - Calling
         * - Location
         * - Live tracking
         * - Siren
         *
         * The recording is saved locally on the device.
         */

        runEmergencyAction(
                "Evidence recording",
                this::startEvidenceRecordingService
        );


        // =====================================================
        // EMERGENCY CALLING
        // =====================================================

        /*
         * Calling is completely independent.
         *
         * Evidence recording is already started before
         * this action is executed.
         */

        runEmergencyAction(
                "Emergency calling",
                this::startPriorityCalling
        );
    }


    // =========================================================
    // INDEPENDENT EMERGENCY ACTION
    // =========================================================

    private void runEmergencyAction(
            String actionName,
            Runnable action
    ) {

        try {

            action.run();

        } catch (Exception e) {

            Log.e(
                    TAG,
                    actionName
                            + " failed",
                    e
            );
        }
    }


    // =========================================================
    // EVIDENCE RECORDING SERVICE
    // =========================================================

    private void startEvidenceRecordingService() {

        Intent intent =
                new Intent(
                        this,
                        EvidenceRecordingService.class
                );


        intent.setAction(
                EvidenceRecordingService.ACTION_START
        );


        String mode;


        if (
                recordingMode
                        == RecordingMode.AUDIO
        ) {

            mode = "AUDIO";

        } else {

            mode = "VIDEO";
        }


        intent.putExtra(
                EvidenceRecordingService.EXTRA_MODE,
                mode
        );


        try {

            /*
             * Android 8+ requires foreground-service
             * startup through startForegroundService().
             */

            if (
                    android.os.Build.VERSION.SDK_INT
                            >= android.os.Build.VERSION_CODES.O
            ) {

                startForegroundService(intent);

            } else {

                startService(intent);
            }


            Log.d(
                    TAG,
                    "Evidence recording started: "
                            + mode
            );


        } catch (Exception e) {

            /*
             * Recording failure must NEVER stop
             * SMS, location, calling or siren.
             */

            Log.e(
                    TAG,
                    "Could not start evidence service",
                    e
            );
        }
    }


    // =========================================================
    // SIREN
    // =========================================================

    private void startSiren() {

        try {

            Uri alert =
                    RingtoneManager.getDefaultUri(
                            RingtoneManager.TYPE_ALARM
                    );


            siren =
                    RingtoneManager.getRingtone(
                            getApplicationContext(),
                            alert
                    );


            if (siren != null) {

                if (
                        android.os.Build.VERSION.SDK_INT
                                >= android.os.Build.VERSION_CODES.P
                ) {

                    siren.setLooping(true);
                }

                siren.play();
            }

        } catch (Exception e) {

            Log.e(
                    TAG,
                    "Siren error",
                    e
            );
        }
    }


    // =========================================================
    // QUICK SMS
    // =========================================================

    private void sendQuickSMS() {

        List<String> numbers =
                getEmergencyContacts();


        if (numbers.isEmpty()) {
            return;
        }


        SmsManager smsManager =
                getSmsManager();


        if (smsManager == null) {
            return;
        }


        for (String number : numbers) {

            try {

                smsManager.sendTextMessage(
                        number,
                        null,
                        "🚨 EMERGENCY! Please track my location.",
                        null,
                        null
                );

            } catch (Exception e) {

                Log.e(
                        TAG,
                        "SMS error",
                        e
                );
            }
        }
    }


    private SmsManager getSmsManager() {

        try {

            if (
                    android.os.Build.VERSION.SDK_INT
                            >= android.os.Build.VERSION_CODES.S
            ) {

                return getSystemService(
                        SmsManager.class
                );

            } else {

                //noinspection deprecation
                return SmsManager.getDefault();
            }

        } catch (Exception e) {

            Log.e(
                    TAG,
                    "SmsManager error",
                    e
            );

            return null;
        }
    }


    // =========================================================
    // LOCATION + FIREBASE
    // =========================================================

    private void sendSOSLocationAndSave() {

        if (
                ActivityCompat.checkSelfPermission(
                        this,
                        Manifest.permission.ACCESS_FINE_LOCATION
                )
                        != PackageManager.PERMISSION_GRANTED
        ) {

            Log.e(
                    TAG,
                    "Location permission unavailable"
            );

            return;
        }


        fusedLocationClient
                .getLastLocation()
                .addOnSuccessListener(
                        location -> {

                            String locationLink =
                                    "Location not available";


                            if (location != null) {

                                double lat =
                                        location.getLatitude();

                                double lng =
                                        location.getLongitude();


                                try {

                                    if (
                                            DangerZoneDetector
                                                    .isDangerZone(
                                                            lat,
                                                            lng
                                                    )
                                    ) {

                                        Toast.makeText(
                                                this,
                                                "⚠ High risk area detected",
                                                Toast.LENGTH_LONG
                                        ).show();
                                    }

                                } catch (Exception e) {

                                    Log.e(
                                            TAG,
                                            "Danger zone check failed",
                                            e
                                    );
                                }


                                locationLink =
                                        "https://www.google.com/maps?q="
                                                + lat
                                                + ","
                                                + lng;
                            }


                            /*
                             * Location SMS contains ONLY
                             * location information.
                             *
                             * No audio/video is attached.
                             */

                            sendLocationSMS(
                                    locationLink
                            );


                            String timeStamp =
                                    new SimpleDateFormat(
                                            "dd MMM yyyy, hh:mm a",
                                            Locale.getDefault()
                                    ).format(
                                            new Date()
                                    );


                            saveAlertLocally(
                                    timeStamp,
                                    locationLink
                            );


                            /*
                             * Firebase stores alert/location
                             * metadata ONLY.
                             *
                             * No evidence files.
                             */

                            saveAlertToFirebase(
                                    timeStamp,
                                    locationLink
                            );
                        }
                )
                .addOnFailureListener(
                        e -> Log.e(
                                TAG,
                                "Location fetch failed",
                                e
                        )
                );
    }


    private void sendLocationSMS(
            String locationLink
    ) {

        List<String> numbers =
                getEmergencyContacts();


        SmsManager smsManager =
                getSmsManager();


        if (smsManager == null) {
            return;
        }


        for (String number : numbers) {

            try {

                smsManager.sendTextMessage(
                        number,
                        null,
                        "📍 My Location: "
                                + locationLink,
                        null,
                        null
                );

            } catch (Exception e) {

                Log.e(
                        TAG,
                        "Location SMS error",
                        e
                );
            }
        }
    }


    // =========================================================
    // LIVE TRACKING
    // =========================================================

    private void startLiveTracking() {

        handler.postDelayed(
                new Runnable() {

                    @Override
                    public void run() {

                        if (!isAlertRunning) {
                            return;
                        }


                        if (
                                ActivityCompat.checkSelfPermission(
                                        EmergencyAlertActivity.this,
                                        Manifest.permission
                                                .ACCESS_FINE_LOCATION
                                )
                                        != PackageManager.PERMISSION_GRANTED
                        ) {

                            return;
                        }


                        fusedLocationClient
                                .getLastLocation()
                                .addOnSuccessListener(
                                        location -> {

                                            if (
                                                    location != null
                                                            && mAuth
                                                            .getCurrentUser()
                                                            != null
                                            ) {

                                                Map<String, Object>
                                                        data =
                                                        new HashMap<>();


                                                data.put(
                                                        "lat",
                                                        location
                                                                .getLatitude()
                                                );


                                                data.put(
                                                        "lng",
                                                        location
                                                                .getLongitude()
                                                );


                                                data.put(
                                                        "time",
                                                        System
                                                                .currentTimeMillis()
                                                );


                                                try {

                                                    mDatabase
                                                            .child(
                                                                    "live_tracking"
                                                            )
                                                            .child(
                                                                    mAuth
                                                                            .getCurrentUser()
                                                                            .getUid()
                                                            )
                                                            .setValue(
                                                                    data
                                                            );

                                                } catch (Exception e) {

                                                    Log.e(
                                                            TAG,
                                                            "Live tracking save failed",
                                                            e
                                                    );
                                                }
                                            }
                                        }
                                );


                        handler.postDelayed(
                                this,
                                5000
                        );
                    }
                },
                5000
        );
    }


    // =========================================================
    // LOCAL ALERT HISTORY
    // =========================================================

    private void saveAlertLocally(
            String time,
            String loc
    ) {

        SharedPreferences prefs =
                getSharedPreferences(
                        "local_data",
                        MODE_PRIVATE
                );


        String existing =
                prefs.getString(
                        "local_alerts",
                        "[]"
                );


        try {

            JSONArray array =
                    new JSONArray(existing);


            JSONObject obj =
                    new JSONObject();


            obj.put(
                    "timestamp",
                    time
            );


            obj.put(
                    "location",
                    loc
            );


            obj.put(
                    "status",
                    "Emergency Triggered"
            );


            array.put(obj);


            prefs.edit()
                    .putString(
                            "local_alerts",
                            array.toString()
                    )
                    .apply();


        } catch (Exception e) {

            Log.e(
                    TAG,
                    "Local save error",
                    e
            );
        }
    }


    // =========================================================
    // FIREBASE ALERT METADATA ONLY
    // =========================================================

    private void saveAlertToFirebase(
            String time,
            String loc
    ) {

        if (
                mAuth.getCurrentUser() == null
        ) {

            return;
        }


        String userId =
                mAuth
                        .getCurrentUser()
                        .getUid();


        Map<String, Object> alert =
                new HashMap<>();


        alert.put(
                "timestamp",
                time
        );


        alert.put(
                "location",
                loc
        );


        alert.put(
                "status",
                "Emergency Triggered"
        );


        try {

            mDatabase
                    .child("alerts")
                    .child(userId)
                    .push()
                    .setValue(alert)
                    .addOnFailureListener(
                            e -> Log.e(
                                    TAG,
                                    "Firebase save error",
                                    e
                            )
                    );

        } catch (Exception e) {

            Log.e(
                    TAG,
                    "Firebase error",
                    e
            );
        }
    }


    // =========================================================
    // CONTACTS
    // =========================================================

    private List<String> getEmergencyContacts() {

        List<String> numbers =
                new ArrayList<>();


        SharedPreferences prefs =
                getSharedPreferences(
                        "contacts",
                        MODE_PRIVATE
                );


        String data =
                prefs.getString(
                        "contact_list",
                        "[]"
                );


        try {

            JSONArray array =
                    new JSONArray(data);


            for (
                    int i = 0;
                    i < array.length();
                    i++
            ) {

                numbers.add(
                        array
                                .getJSONObject(i)
                                .getString("phone")
                );
            }

        } catch (Exception e) {

            Log.e(
                    TAG,
                    "Get contacts error",
                    e
            );
        }


        return numbers;
    }


    // =========================================================
    // PRIORITY CALLING
    // =========================================================

    private void startPriorityCalling() {

        contacts =
                getEmergencyContacts();


        currentContactIndex = 0;


        if (!contacts.isEmpty()) {

            callNextContact();
        }
    }


    private void callNextContact() {

        if (
                !isAlertRunning
                        || currentContactIndex
                        >= contacts.size()
        ) {

            return;
        }


        String number =
                contacts.get(
                        currentContactIndex
                );


        Intent callIntent =
                new Intent(
                        Intent.ACTION_CALL
                );


        callIntent.setData(
                Uri.parse(
                        "tel:" + number
                )
        );


        if (
                ActivityCompat.checkSelfPermission(
                        this,
                        Manifest.permission.CALL_PHONE
                )
                        != PackageManager.PERMISSION_GRANTED
        ) {

            Log.e(
                    TAG,
                    "CALL_PHONE permission unavailable"
            );

            return;
        }


        try {

            /*
             * Recording service has already been
             * started before this point.
             */

            startActivity(callIntent);


            currentContactIndex++;


            handler.postDelayed(
                    this::callNextContact,
                    20000
            );


        } catch (Exception e) {

            Log.e(
                    TAG,
                    "Call error",
                    e
            );
        }
    }


    // =========================================================
    // STOP SOS
    // =========================================================

    private void stopAlert() {

        isAlertRunning = false;


        handler.removeCallbacksAndMessages(
                null
        );


        /*
         * Stop siren.
         */

        if (siren != null) {

            try {

                if (siren.isPlaying()) {

                    siren.stop();
                }

            } catch (Exception e) {

                Log.e(
                        TAG,
                        "Siren stop error",
                        e
                );
            }
        }


        // =====================================================
        // STOP INDEPENDENT EVIDENCE RECORDING
        // =====================================================

        try {

            Intent intent =
                    new Intent(
                            this,
                            EvidenceRecordingService.class
                    );


            intent.setAction(
                    EvidenceRecordingService.ACTION_STOP
            );


            /*
             * Send STOP command to the evidence service.
             *
             * The service itself will:
             *
             * 1. Stop MediaRecorder
             * 2. Finalize the recording file
             * 3. Stop foreground mode
             * 4. Stop itself
             *
             * SMS, calling and location are NOT affected.
             */

            startService(intent);


        } catch (Exception e) {

            Log.e(
                    TAG,
                    "Could not stop evidence service",
                    e
            );
        }


        // =====================================================
        // CLEAR CURRENT LIVE TRACKING
        // =====================================================

        try {

            if (
                    mAuth.getCurrentUser()
                            != null
            ) {

                mDatabase
                        .child("live_tracking")
                        .child(
                                mAuth
                                        .getCurrentUser()
                                        .getUid()
                        )
                        .removeValue();
            }

        } catch (Exception e) {

            Log.e(
                    TAG,
                    "Live tracking cleanup failed",
                    e
            );
        }


        /*
         * Finish the emergency screen.
         *
         * MainActivity.onResume() will automatically
         * restart Voice SOS listening.
         */

        if (!isFinishing()) {

            finish();
        }
    }


    // =========================================================
    // ACTIVITY DESTROY
    // =========================================================

    @Override
    protected void onDestroy() {

        /*
         * IMPORTANT:
         *
         * DO NOT stop the EvidenceRecordingService here.
         *
         * The service must remain independent of
         * this Activity.
         */

        handler.removeCallbacksAndMessages(
                null
        );


        super.onDestroy();
    }
}