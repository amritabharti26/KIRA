package com.Amrit_a_b.kiraapp;

import android.Manifest;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.location.Location;
import android.media.MediaRecorder;
import android.media.Ringtone;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.telephony.SmsManager;
import android.util.Log;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
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
    private FusedLocationProviderClient fusedLocationClient;
    private DatabaseReference mDatabase;
    private FirebaseAuth mAuth;

    private List<String> contacts;
    private int currentContactIndex = 0;

    private final Handler handler = new Handler(Looper.getMainLooper());

    private MediaRecorder mediaRecorder;
    private boolean isRecording = false;

    private Ringtone siren;

    private boolean isAlertRunning = true;
    private boolean isTriggered = false;

    private int countdown = 5;

    private TextView tvMessage;

    private static final String DB_URL =
            "https://ai-powered-women-safety-ca54a-default-rtdb.firebaseio.com/";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_emergency_alert);

        mAuth = FirebaseAuth.getInstance();

        try {
            mDatabase = FirebaseDatabase.getInstance(DB_URL).getReference();
        } catch (Exception e) {
            Log.e(TAG, "Firebase init error: " + e.getMessage());
            mDatabase = FirebaseDatabase.getInstance().getReference();
        }

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);

        tvMessage = findViewById(R.id.tv_alert_message);

        checkPermissions();

        DangerZoneDetector.loadDangerZones();

        startCountdown();

        findViewById(R.id.btn_stop_alert).setOnClickListener(v -> stopAlert());
    }

    private void startCountdown() {
        if (countdown > 0 && isAlertRunning) {
            tvMessage.setText("Alert will trigger in " + countdown + "s\nTap STOP to cancel");
            handler.postDelayed(() -> {
                countdown--;
                startCountdown();
            }, 1000);
        } else if (!isTriggered && isAlertRunning) {
            triggerEmergencyActions();
        }
    }

    private void triggerEmergencyActions() {
        isTriggered = true;
        if (tvMessage != null) tvMessage.setText("Alert Sent to Emergency Contacts");

        startSiren();
        sendQuickSMS();
        sendSOSLocationAndSave();
        startLiveTracking();
        startPriorityCalling();
        startEmergencyRecording();
    }

    private void startSiren() {
        try {
            Uri alert = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM);
            siren = RingtoneManager.getRingtone(getApplicationContext(), alert);
            if (siren != null) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                    siren.setLooping(true);
                }
                siren.play();
            }
        } catch (Exception e) {
            Log.e(TAG, "Siren error: " + e.getMessage());
        }
    }

    private void sendQuickSMS() {
        List<String> numbers = getEmergencyContacts();
        if (numbers.isEmpty()) return;

        SmsManager smsManager = getSmsManager();
        if (smsManager == null) return;

        for (String number : numbers) {
            try {
                smsManager.sendTextMessage(number, null, "🚨 EMERGENCY! Please track my location.", null, null);
            } catch (Exception e) {
                Log.e(TAG, "SMS error: " + e.getMessage());
            }
        }
    }

    private SmsManager getSmsManager() {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                return getSystemService(SmsManager.class);
            } else {
                return SmsManager.getDefault();
            }
        } catch (Exception e) {
            return null;
        }
    }

    private void sendSOSLocationAndSave() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) return;

        fusedLocationClient.getLastLocation().addOnSuccessListener(location -> {
            String locationLink = "Location not available";
            if (location != null) {
                double lat = location.getLatitude();
                double lng = location.getLongitude();

                if (DangerZoneDetector.isDangerZone(lat, lng)) {
                    Toast.makeText(this, "⚠ High risk area detected", Toast.LENGTH_LONG).show();
                }
                locationLink = "https://www.google.com/maps?q=" + lat + "," + lng;
            }

            List<String> numbers = getEmergencyContacts();
            SmsManager smsManager = getSmsManager();
            if (smsManager != null) {
                for (String number : numbers) {
                    try {
                        smsManager.sendTextMessage(number, null, "📍 My Location: " + locationLink, null, null);
                    } catch (Exception e) {
                        Log.e(TAG, "Location SMS error: " + e.getMessage());
                    }
                }
            }

            String timeStamp = new SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale.getDefault()).format(new Date());
            saveAlertLocally(timeStamp, locationLink);
            saveAlertToFirebase(timeStamp, locationLink);
        }).addOnFailureListener(e -> Log.e(TAG, "Location fetch failed: " + e.getMessage()));
    }

    private void startLiveTracking() {
        handler.postDelayed(new Runnable() {
            @Override
            public void run() {
                if (!isAlertRunning) return;
                if (ActivityCompat.checkSelfPermission(EmergencyAlertActivity.this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) return;

                fusedLocationClient.getLastLocation().addOnSuccessListener(location -> {
                    if (location != null && mAuth.getCurrentUser() != null) {
                        Map<String, Object> data = new HashMap<>();
                        data.put("lat", location.getLatitude());
                        data.put("lng", location.getLongitude());
                        data.put("time", System.currentTimeMillis());

                        mDatabase.child("live_tracking")
                                .child(mAuth.getCurrentUser().getUid())
                                .setValue(data);
                    }
                });
                handler.postDelayed(this, 5000);
            }
        }, 5000);
    }

    private void saveAlertLocally(String time, String loc) {
        SharedPreferences prefs = getSharedPreferences("local_data", MODE_PRIVATE);
        String existing = prefs.getString("local_alerts", "[]");
        try {
            JSONArray array = new JSONArray(existing);
            JSONObject obj = new JSONObject();
            obj.put("timestamp", time);
            obj.put("location", loc);
            obj.put("status", "Emergency Triggered");
            array.put(obj);
            prefs.edit().putString("local_alerts", array.toString()).apply();
        } catch (Exception e) {
            Log.e(TAG, "Local save error: " + e.getMessage());
        }
    }

    private void saveAlertToFirebase(String time, String loc) {
        if (mAuth.getCurrentUser() == null) return;
        String userId = mAuth.getCurrentUser().getUid();
        Map<String, Object> alert = new HashMap<>();
        alert.put("timestamp", time);
        alert.put("location", loc);
        alert.put("status", "Emergency Triggered");

        mDatabase.child("alerts").child(userId).push().setValue(alert)
                .addOnFailureListener(e -> Log.e(TAG, "Firebase save error: " + e.getMessage()));
    }

    private void checkPermissions() {
        String[] permissions = {
                Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.SEND_SMS,
                Manifest.permission.CALL_PHONE,
                Manifest.permission.RECORD_AUDIO,
                Manifest.permission.CAMERA
        };
        List<String> missing = new ArrayList<>();
        for (String p : permissions) {
            if (ActivityCompat.checkSelfPermission(this, p) != PackageManager.PERMISSION_GRANTED) {
                missing.add(p);
            }
        }
        if (!missing.isEmpty()) {
            ActivityCompat.requestPermissions(this, missing.toArray(new String[0]), 101);
        }
    }

    private List<String> getEmergencyContacts() {
        List<String> numbers = new ArrayList<>();
        SharedPreferences prefs = getSharedPreferences("contacts", MODE_PRIVATE);
        String data = prefs.getString("contact_list", "[]");
        try {
            JSONArray array = new JSONArray(data);
            for (int i = 0; i < array.length(); i++) {
                numbers.add(array.getJSONObject(i).getString("phone"));
            }
        } catch (Exception e) {
            Log.e(TAG, "Get contacts error: " + e.getMessage());
        }
        return numbers;
    }

    private void startPriorityCalling() {
        contacts = getEmergencyContacts();
        if (!contacts.isEmpty()) callNextContact();
    }

    private void callNextContact() {
        if (!isAlertRunning || currentContactIndex >= contacts.size()) return;

        String number = contacts.get(currentContactIndex);
        Intent callIntent = new Intent(Intent.ACTION_CALL);
        callIntent.setData(Uri.parse("tel:" + number));

        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.CALL_PHONE) == PackageManager.PERMISSION_GRANTED) {
            try {
                startActivity(callIntent);
                currentContactIndex++;
                handler.postDelayed(this::callNextContact, 20000);
            } catch (Exception e) {
                Log.e(TAG, "Call error: " + e.getMessage());
            }
        }
    }

    private void startEmergencyRecording() {
        // Record video (which includes audio) to avoid MIC conflict
        try {
            String videoFilePath = getExternalFilesDir(null).getAbsolutePath() + "/emergency_video.mp4";
            
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                mediaRecorder = new MediaRecorder(this);
            } else {
                mediaRecorder = new MediaRecorder();
            }

            mediaRecorder.setAudioSource(MediaRecorder.AudioSource.MIC);
            mediaRecorder.setVideoSource(MediaRecorder.VideoSource.CAMERA);
            mediaRecorder.setOutputFormat(MediaRecorder.OutputFormat.MPEG_4);
            mediaRecorder.setOutputFile(videoFilePath);
            mediaRecorder.setVideoEncoder(MediaRecorder.VideoEncoder.H264);
            mediaRecorder.setAudioEncoder(MediaRecorder.AudioEncoder.AAC);
            mediaRecorder.setVideoSize(640, 480);
            mediaRecorder.setVideoFrameRate(30);

            mediaRecorder.prepare();
            mediaRecorder.start();
            isRecording = true;
        } catch (Exception e) {
            Log.e(TAG, "Recording error: " + e.getMessage());
            // If video fails, try audio only
            startAudioOnlyRecording();
        }
    }

    private void startAudioOnlyRecording() {
        try {
            String audioFilePath = getExternalFilesDir(null).getAbsolutePath() + "/emergency_audio.3gp";
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                mediaRecorder = new MediaRecorder(this);
            } else {
                mediaRecorder = new MediaRecorder();
            }
            mediaRecorder.setAudioSource(MediaRecorder.AudioSource.MIC);
            mediaRecorder.setOutputFormat(MediaRecorder.OutputFormat.THREE_GPP);
            mediaRecorder.setOutputFile(audioFilePath);
            mediaRecorder.setAudioEncoder(MediaRecorder.AudioEncoder.AMR_NB);
            mediaRecorder.prepare();
            mediaRecorder.start();
            isRecording = true;
        } catch (Exception e) {
            Log.e(TAG, "Audio only recording error: " + e.getMessage());
        }
    }

    private void stopAlert() {
        isAlertRunning = false;
        handler.removeCallbacksAndMessages(null);

        if (siren != null) {
            try {
                if (siren.isPlaying()) siren.stop();
            } catch (Exception e) {
                Log.e(TAG, "Siren stop error: " + e.getMessage());
            }
        }

        if (isRecording && mediaRecorder != null) {
            try {
                mediaRecorder.stop();
            } catch (Exception e) {
                Log.e(TAG, "MediaRecorder stop error: " + e.getMessage());
            } finally {
                mediaRecorder.release();
                mediaRecorder = null;
                isRecording = false;
            }
        }

        if (!isFinishing()) finish();
    }

    @Override
    protected void onDestroy() {
        stopAlert();
        super.onDestroy();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == 101) {
            for (int res : grantResults) {
                if (res != PackageManager.PERMISSION_GRANTED) {
                    Toast.makeText(this, "Permissions required for full safety features", Toast.LENGTH_SHORT).show();
                    break;
                }
            }
        }
    }
}