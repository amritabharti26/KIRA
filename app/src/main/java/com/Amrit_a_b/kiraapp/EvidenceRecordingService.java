package com.Amrit_a_b.kiraapp;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ServiceInfo;
import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraCaptureSession;
import android.hardware.camera2.CameraCharacteristics;
import android.hardware.camera2.CameraDevice;
import android.hardware.camera2.CameraManager;
import android.hardware.camera2.CaptureRequest;
import android.hardware.camera2.CaptureFailure;
import android.hardware.camera2.params.StreamConfigurationMap;
import android.media.MediaRecorder;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.util.Log;
import android.util.Size;
import android.view.Surface;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;
import androidx.core.content.ContextCompat;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Date;
import java.util.List;
import java.util.Locale;


// =========================================================
// EVIDENCE RECORDING SERVICE
// =========================================================
//
// Handles ONLY emergency evidence recording.
//
// VIDEO + AUDIO
//      ↓
// Camera2
//      ↓
// MediaRecorder
//      ↓
// Local MP4
//
// AUDIO ONLY
//      ↓
// MediaRecorder
//      ↓
// Local 3GP
//
// This service does NOT handle:
//      - SMS
//      - Phone calls
//      - Location
//      - SOS trigger logic
//
// Those actions remain independent.
// =========================================================

public class EvidenceRecordingService extends Service {

    // =========================================================
    // CONSTANTS
    // =========================================================

    private static final String TAG =
            "EvidenceRecording";

    private static final String CHANNEL_ID =
            "kira_evidence_recording";

    private static final int NOTIFICATION_ID =
            3001;

    public static final String ACTION_START =
            "com.Amrit_a_b.kiraapp.START_RECORDING";

    public static final String ACTION_STOP =
            "com.Amrit_a_b.kiraapp.STOP_RECORDING";

    public static final String EXTRA_MODE =
            "recording_mode";


    // =========================================================
    // MEDIA RECORDER
    // =========================================================

    private MediaRecorder mediaRecorder;

    private File currentOutputFile;

    private boolean isRecording = false;

    private boolean isVideoStarting = false;


    // =========================================================
    // CAMERA2
    // =========================================================

    private CameraManager cameraManager;

    private CameraDevice cameraDevice;

    private CameraCaptureSession captureSession;

    private Surface recorderSurface;

    private String cameraId;

    private Size videoSize;


    // =========================================================
    // HANDLER
    // =========================================================

    private final Handler mainHandler =
            new Handler(
                    Looper.getMainLooper()
            );


    // =========================================================
    // CAMERA CAPTURE CALLBACK
    // =========================================================
    //
    // This lets us verify that the camera is actually
    // producing frames for MediaRecorder.
    // =========================================================

    private final CameraCaptureSession.CaptureCallback
            captureCallback =
            new CameraCaptureSession.CaptureCallback() {

                @Override
                public void onCaptureStarted(
                        @NonNull CameraCaptureSession session,
                        @NonNull CaptureRequest request,
                        long timestamp,
                        long frameNumber
                ) {

                    Log.d(
                            TAG,
                            "Camera frame captured: "
                                    + frameNumber
                    );
                }

                @Override
                public void onCaptureFailed(
                        @NonNull CameraCaptureSession session,
                        @NonNull CaptureRequest request,
                        @NonNull CaptureFailure failure
                ) {

                    Log.e(
                            TAG,
                            "Camera frame FAILED. reason="
                                    + failure.getReason()
                    );
                }
            };


    // =========================================================
    // CREATE
    // =========================================================

    @Override
    public void onCreate() {

        super.onCreate();

        createNotificationChannel();

        cameraManager =
                (CameraManager)
                        getSystemService(
                                Context.CAMERA_SERVICE
                        );

        Log.d(
                TAG,
                "EvidenceRecordingService created"
        );
    }


    // =========================================================
    // START COMMAND
    // =========================================================

    @Override
    public int onStartCommand(
            Intent intent,
            int flags,
            int startId
    ) {

        if (intent == null) {

            stopSelf();

            return START_NOT_STICKY;
        }


        String action =
                intent.getAction();


        // =====================================================
        // STOP RECORDING
        // =====================================================

        if (ACTION_STOP.equals(action)) {

            stopRecording();

            stopForegroundService();

            return START_NOT_STICKY;
        }


        // =====================================================
        // START RECORDING
        // =====================================================

        if (ACTION_START.equals(action)) {

            String mode =
                    intent.getStringExtra(
                            EXTRA_MODE
                    );


            if (mode == null) {

                mode = "VIDEO";
            }


            // -------------------------------------------------
            // ENTER FOREGROUND
            // -------------------------------------------------

            startRecordingForeground(mode);


            // -------------------------------------------------
            // START ACTUAL RECORDING
            // -------------------------------------------------

            startRecording(mode);
        }


        return START_NOT_STICKY;
    }


    // =========================================================
    // FOREGROUND SERVICE
    // =========================================================

    private void startRecordingForeground(
            String mode
    ) {

        Notification notification =
                buildNotification();


        if (
                Build.VERSION.SDK_INT
                        >= Build.VERSION_CODES.R
        ) {

            if (
                    "VIDEO".equalsIgnoreCase(mode)
            ) {

                startForeground(
                        NOTIFICATION_ID,
                        notification,
                        ServiceInfo
                                .FOREGROUND_SERVICE_TYPE_CAMERA
                                |
                                ServiceInfo
                                        .FOREGROUND_SERVICE_TYPE_MICROPHONE
                );

            } else {

                startForeground(
                        NOTIFICATION_ID,
                        notification,
                        ServiceInfo
                                .FOREGROUND_SERVICE_TYPE_MICROPHONE
                );
            }

        } else {

            startForeground(
                    NOTIFICATION_ID,
                    notification
            );
        }
    }


    // =========================================================
    // START RECORDING
    // =========================================================

    private void startRecording(
            String mode
    ) {

        if (isRecording || isVideoStarting) {

            Log.d(
                    TAG,
                    "Recording already active"
            );

            return;
        }


        // =====================================================
        // MICROPHONE PERMISSION
        // =====================================================

        boolean audioGranted =
                ContextCompat.checkSelfPermission(
                        this,
                        Manifest.permission.RECORD_AUDIO
                )
                        == PackageManager.PERMISSION_GRANTED;


        if (!audioGranted) {

            Log.e(
                    TAG,
                    "Microphone permission not granted"
            );

            stopServiceSafely();

            return;
        }


        // =====================================================
        // VIDEO MODE
        // =====================================================

        if (
                "VIDEO".equalsIgnoreCase(mode)
        ) {

            boolean cameraGranted =
                    ContextCompat.checkSelfPermission(
                            this,
                            Manifest.permission.CAMERA
                    )
                            == PackageManager.PERMISSION_GRANTED;


            if (!cameraGranted) {

                Log.e(
                        TAG,
                        "Camera permission not granted"
                );

                // Video unavailable.
                // Continue with independent audio evidence.

                startAudioRecording();

                return;
            }


            startVideoRecording();


        } else {

            // =================================================
            // AUDIO ONLY
            // =================================================

            startAudioRecording();
        }
    }


    // =========================================================
    // START CAMERA2 VIDEO + AUDIO
    // =========================================================

    @SuppressLint("MissingPermission")
    private void startVideoRecording() {

        if (cameraManager == null) {

            Log.e(
                    TAG,
                    "CameraManager unavailable"
            );

            startAudioRecording();

            return;
        }


        isVideoStarting = true;


        try {

            // =================================================
            // EVIDENCE DIRECTORY
            // =================================================

            File directory =
                    getEvidenceDirectory();


            if (directory == null) {

                throw new Exception(
                        "Evidence directory unavailable"
                );
            }


            // =================================================
            // FILE NAME
            // =================================================

            String timeStamp =
                    new SimpleDateFormat(
                            "yyyyMMdd_HHmmss",
                            Locale.getDefault()
                    ).format(
                            new Date()
                    );


            currentOutputFile =
                    new File(
                            directory,
                            "KIRA_VIDEO_"
                                    + timeStamp
                                    + ".mp4"
                    );


            // =================================================
            // FIND BACK CAMERA
            // =================================================

            cameraId =
                    findBackCamera();


            if (cameraId == null) {

                throw new Exception(
                        "No usable camera found"
                );
            }


            // =================================================
            // FIND SUPPORTED VIDEO SIZE
            // =================================================

            videoSize =
                    findBestVideoSize(
                            cameraId
                    );


            if (videoSize == null) {

                throw new Exception(
                        "No supported MediaRecorder "
                                + "video size found"
                );
            }


            Log.d(
                    TAG,
                    "Camera2 selected: "
                            + cameraId
            );


            Log.d(
                    TAG,
                    "Video size: "
                            + videoSize.getWidth()
                            + "x"
                            + videoSize.getHeight()
            );


            // =================================================
            // CREATE MEDIA RECORDER
            // =================================================
            //
            // Use the no-argument constructor for compatibility
            // with minSdk 24.
            // =================================================

            mediaRecorder =
                    new MediaRecorder();


            // =================================================
            // AUDIO SOURCE
            // =================================================

            mediaRecorder.setAudioSource(
                    MediaRecorder.AudioSource.CAMCORDER
            );


            // =================================================
            // CAMERA2 VIDEO SOURCE
            // =================================================
            //
            // Camera2 sends frames through a Surface.
            // =================================================

            mediaRecorder.setVideoSource(
                    MediaRecorder.VideoSource.SURFACE
            );


            // =================================================
            // OUTPUT FORMAT
            // =================================================

            mediaRecorder.setOutputFormat(
                    MediaRecorder.OutputFormat.MPEG_4
            );


            // =================================================
            // VIDEO SIZE
            // =================================================

            mediaRecorder.setVideoSize(
                    videoSize.getWidth(),
                    videoSize.getHeight()
            );


            // =================================================
            // FRAME RATE
            // =================================================

            mediaRecorder.setVideoFrameRate(
                    30
            );


            // =================================================
            // VIDEO BITRATE
            // =================================================

            mediaRecorder.setVideoEncodingBitRate(
                    4_000_000
            );


            // =================================================
            // VIDEO ENCODER
            // =================================================

            mediaRecorder.setVideoEncoder(
                    MediaRecorder.VideoEncoder.H264
            );


            // =================================================
            // AUDIO ENCODER
            // =================================================

            mediaRecorder.setAudioEncoder(
                    MediaRecorder.AudioEncoder.AAC
            );


            // =================================================
            // AUDIO BITRATE
            // =================================================

            mediaRecorder.setAudioEncodingBitRate(
                    128_000
            );


            // =================================================
            // AUDIO SAMPLE RATE
            // =================================================

            mediaRecorder.setAudioSamplingRate(
                    44_100
            );


            // =================================================
            // AUDIO CHANNELS
            // =================================================

            mediaRecorder.setAudioChannels(
                    1
            );


            // =================================================
            // OUTPUT FILE
            // =================================================

            mediaRecorder.setOutputFile(
                    currentOutputFile
                            .getAbsolutePath()
            );


            // =================================================
            // PREPARE MEDIA RECORDER
            // =================================================

            mediaRecorder.prepare();


            Log.d(
                    TAG,
                    "Camera2 MediaRecorder prepared"
            );


            // =================================================
            // GET RECORDER SURFACE
            // =================================================

            recorderSurface =
                    mediaRecorder.getSurface();


            if (recorderSurface == null) {

                throw new Exception(
                        "MediaRecorder surface is null"
                );
            }


            Log.d(
                    TAG,
                    "MediaRecorder surface obtained"
            );


            // =================================================
            // OPEN CAMERA2
            // =================================================

            cameraManager.openCamera(
                    cameraId,
                    cameraStateCallback,
                    mainHandler
            );


        } catch (Exception e) {

            Log.e(
                    TAG,
                    "Camera2 video setup failed",
                    e
            );


            isVideoStarting = false;


            cleanupCameraResources();

            deleteIncompleteOutputFile();


            // =================================================
            // IMPORTANT
            // Video failure must NEVER break SOS.
            // Continue with independent audio evidence.
            // =================================================

            startAudioRecording();
        }
    }


    // =========================================================
    // FIND BACK CAMERA
    // =========================================================

    private String findBackCamera()
            throws CameraAccessException {

        String fallbackCamera = null;


        for (
                String id :
                cameraManager.getCameraIdList()
        ) {

            CameraCharacteristics characteristics =
                    cameraManager.getCameraCharacteristics(
                            id
                    );


            Integer facing =
                    characteristics.get(
                            CameraCharacteristics
                                    .LENS_FACING
                    );


            if (
                    facing != null
                            &&
                            facing
                                    == CameraCharacteristics
                                    .LENS_FACING_BACK
            ) {

                return id;
            }


            if (fallbackCamera == null) {

                fallbackCamera = id;
            }
        }


        return fallbackCamera;
    }


    // =========================================================
    // FIND SUPPORTED VIDEO SIZE
    // =========================================================

    private Size findBestVideoSize(
            String id
    ) throws CameraAccessException {

        CameraCharacteristics characteristics =
                cameraManager
                        .getCameraCharacteristics(id);


        StreamConfigurationMap map =
                characteristics.get(
                        CameraCharacteristics
                                .SCALER_STREAM_CONFIGURATION_MAP
                );


        if (map == null) {

            return null;
        }


        Size[] sizes =
                map.getOutputSizes(
                        MediaRecorder.class
                );


        if (
                sizes == null
                        || sizes.length == 0
        ) {

            return null;
        }


        // =====================================================
        // TARGET
        // =====================================================
        //
        // Prefer 1280x720 or smaller.
        // =====================================================

        Size best = null;

        double bestScore =
                Double.MAX_VALUE;


        for (Size size : sizes) {

            int width =
                    size.getWidth();

            int height =
                    size.getHeight();


            if (
                    width > 1280
                            ||
                            height > 720
            ) {

                continue;
            }


            double ratio =
                    (double) width
                            / height;


            double ratioDifference =
                    Math.abs(
                            ratio
                                    - (16.0 / 9.0)
                    );


            double areaDifference =
                    Math.abs(
                            (width * height)
                                    - (1280 * 720)
                    );


            double score =
                    ratioDifference * 1_000_000
                            + areaDifference;


            if (
                    best == null
                            ||
                            score < bestScore
            ) {

                best = size;

                bestScore = score;
            }
        }


        // =====================================================
        // FALLBACK
        // =====================================================

        if (best == null) {

            best =
                    Arrays.stream(sizes)
                            .min(
                                    Comparator.comparingInt(a -> a.getWidth()
                                            * a.getHeight())
                            )
                            .orElse(null);
        }


        return best;
    }


    // =========================================================
    // CAMERA2 STATE CALLBACK
    // =========================================================

    private final CameraDevice.StateCallback
            cameraStateCallback =
            new CameraDevice.StateCallback() {

                @Override
                public void onOpened(
                        @NonNull CameraDevice camera
                ) {

                    Log.d(
                            TAG,
                            "Camera2 opened"
                    );


                    cameraDevice =
                            camera;


                    try {

                        createRecordingSession();

                    } catch (Exception e) {

                        Log.e(
                                TAG,
                                "Could not create camera session",
                                e
                        );


                        fallbackToAudio();
                    }
                }


                @Override
                public void onDisconnected(
                        @NonNull CameraDevice camera
                ) {

                    Log.e(
                            TAG,
                            "Camera2 disconnected"
                    );


                    camera.close();


                    if (!isRecording) {

                        fallbackToAudio();
                    }
                }


                @Override
                public void onError(
                        @NonNull CameraDevice camera,
                        int error
                ) {

                    Log.e(
                            TAG,
                            "Camera2 error: "
                                    + error
                    );


                    camera.close();


                    if (!isRecording) {

                        fallbackToAudio();
                    }
                }
            };


    // =========================================================
    // CREATE CAMERA2 RECORDING SESSION
    // =========================================================

    private void createRecordingSession()
            throws CameraAccessException {

        if (
                cameraDevice == null
                        ||
                        recorderSurface == null
        ) {

            throw new IllegalStateException(
                    "Camera device or recorder surface missing"
            );
        }


        // =====================================================
        // IMPORTANT
        // The MediaRecorder surface MUST be included in the
        // Camera2 capture session.
        // =====================================================

        cameraDevice.createCaptureSession(
                List.of(
                        recorderSurface
                ),
                new CameraCaptureSession.StateCallback() {

                    @Override
                    public void onConfigured(
                            @NonNull CameraCaptureSession session
                    ) {

                        Log.d(
                                TAG,
                                "Camera capture session configured"
                        );


                        captureSession =
                                session;


                        try {

                            startCameraCapture();

                        } catch (Exception e) {

                            Log.e(
                                    TAG,
                                    "Camera capture failed",
                                    e
                            );


                            fallbackToAudio();
                        }
                    }


                    @Override
                    public void onConfigureFailed(
                            @NonNull CameraCaptureSession session
                    ) {

                        Log.e(
                                TAG,
                                "Camera capture session configuration failed"
                        );


                        fallbackToAudio();
                    }
                },
                mainHandler
        );
    }


    // =========================================================
    // START CAMERA CAPTURE
    // =========================================================

    private void startCameraCapture()
            throws CameraAccessException {

        if (
                cameraDevice == null
                        ||
                        captureSession == null
                        ||
                        recorderSurface == null
                        ||
                        mediaRecorder == null
        ) {

            throw new IllegalStateException(
                    "Camera recording resources missing"
            );
        }


        try {

            // =================================================
            // CREATE RECORD REQUEST
            // =================================================

            CaptureRequest.Builder builder =
                    cameraDevice.createCaptureRequest(
                            CameraDevice.TEMPLATE_RECORD
                    );


            // =================================================
            // TARGET MEDIARECORDER SURFACE
            // =================================================

            builder.addTarget(
                    recorderSurface
            );


            // =================================================
            // AUTO CONTROL
            // =================================================

            builder.set(
                    CaptureRequest.CONTROL_MODE,
                    CaptureRequest.CONTROL_MODE_AUTO
            );


            // =================================================
            // CONTINUOUS AUTO FOCUS
            // =================================================

            try {

                builder.set(
                        CaptureRequest.CONTROL_AF_MODE,
                        CaptureRequest.CONTROL_AF_MODE_CONTINUOUS_VIDEO
                );

            } catch (Exception e) {

                Log.w(
                        TAG,
                        "Continuous autofocus not supported",
                        e
                );
            }


            // =================================================
            // START MEDIA RECORDER
            // =================================================

            mediaRecorder.start();

            Log.d(
                    TAG,
                    "MediaRecorder started"
            );


            // =================================================
            // START CONTINUOUS CAMERA CAPTURE
            // =================================================

            captureSession.setRepeatingRequest(
                    builder.build(),
                    captureCallback,
                    mainHandler
            );


            // =================================================
            // RECORDING STATE
            // =================================================

            isRecording = true;

            isVideoStarting = false;


            Log.d(
                    TAG,
                    "Camera2 repeating capture started"
            );


            Log.d(
                    TAG,
                    "VIDEO + AUDIO recording started successfully: "
                            + currentOutputFile
                            .getAbsolutePath()
            );


        } catch (Exception e) {

            Log.e(
                    TAG,
                    "Failed to start Camera2 capture",
                    e
            );


            isRecording = false;

            isVideoStarting = false;


            try {

                if (captureSession != null) {

                    captureSession.stopRepeating();
                }

            } catch (Exception ignored) {
            }


            try {

                if (mediaRecorder != null) {

                    mediaRecorder.reset();
                }

            } catch (Exception ignored) {
            }


            throw e;
        }
    }


    // =========================================================
    // FALLBACK TO AUDIO
    // =========================================================

    private void fallbackToAudio() {

        if (isRecording) {

            return;
        }


        isVideoStarting = false;


        Log.d(
                TAG,
                "Falling back to audio evidence"
        );


        cleanupCameraResources();

        deleteIncompleteOutputFile();


        // =====================================================
        // IMPORTANT
        // Audio continues independently.
        // =====================================================

        startAudioRecording();
    }


    // =========================================================
    // START AUDIO RECORDING
    // =========================================================

    @SuppressLint("NewApi")
    private void startAudioRecording() {

        if (isRecording) {

            return;
        }


        try {

            File directory =
                    getEvidenceDirectory();


            if (directory == null) {

                Log.e(
                        TAG,
                        "Evidence directory unavailable"
                );


                stopServiceSafely();

                return;
            }


            // =================================================
            // FILE NAME
            // =================================================

            String timeStamp =
                    new SimpleDateFormat(
                            "yyyyMMdd_HHmmss",
                            Locale.getDefault()
                    ).format(
                            new Date()
                    );


            currentOutputFile =
                    new File(
                            directory,
                            "KIRA_AUDIO_"
                                    + timeStamp
                                    + ".3gp"
                    );


            // =================================================
            // CREATE AUDIO RECORDER
            // =================================================

            mediaRecorder =
                    new MediaRecorder();


            // =================================================
            // AUDIO SOURCE
            // =================================================

            mediaRecorder.setAudioSource(
                    MediaRecorder.AudioSource.MIC
            );


            // =================================================
            // OUTPUT FORMAT
            // =================================================

            mediaRecorder.setOutputFormat(
                    MediaRecorder.OutputFormat.THREE_GPP
            );


            // =================================================
            // OUTPUT FILE
            // =================================================

            mediaRecorder.setOutputFile(
                    currentOutputFile
                            .getAbsolutePath()
            );


            // =================================================
            // AUDIO ENCODER
            // =================================================

            mediaRecorder.setAudioEncoder(
                    MediaRecorder.AudioEncoder.AMR_NB
            );


            // =================================================
            // PREPARE
            // =================================================

            mediaRecorder.prepare();


            // =================================================
            // START
            // =================================================

            mediaRecorder.start();


            isRecording = true;

            isVideoStarting = false;


            Log.d(
                    TAG,
                    "AUDIO recording started: "
                            + currentOutputFile
                            .getAbsolutePath()
            );


        } catch (Exception e) {

            Log.e(
                    TAG,
                    "Audio recording failed",
                    e
            );


            releaseRecorder();

            deleteIncompleteOutputFile();

            stopServiceSafely();
        }
    }


    // =========================================================
    // EVIDENCE DIRECTORY
    // =========================================================

    private File getEvidenceDirectory() {

        File baseDirectory =
                getExternalFilesDir(null);


        if (baseDirectory == null) {

            return null;
        }


        File evidenceDirectory =
                new File(
                        baseDirectory,
                        "Evidence"
                );


        if (!evidenceDirectory.exists()) {

            if (!evidenceDirectory.mkdirs()) {

                Log.e(
                        TAG,
                        "Could not create Evidence directory"
                );

                return null;
            }
        }


        return evidenceDirectory;
    }


    // =========================================================
    // STOP RECORDING
    // =========================================================

    private void stopRecording() {

        isVideoStarting = false;


        // =====================================================
        // STOP CAMERA CAPTURE
        // =====================================================

        if (captureSession != null) {

            try {

                captureSession.stopRepeating();

            } catch (Exception e) {

                Log.w(
                        TAG,
                        "Could not stop camera repeating request",
                        e
                );
            }


            try {

                captureSession.abortCaptures();

            } catch (Exception e) {

                Log.w(
                        TAG,
                        "Could not abort camera captures",
                        e
                );
            }
        }


        // =====================================================
        // STOP MEDIA RECORDER
        // =====================================================

        if (
                mediaRecorder != null
                        &&
                        isRecording
        ) {

            try {

                mediaRecorder.stop();


                Log.d(
                        TAG,
                        "Evidence recording stopped successfully"
                );


            } catch (RuntimeException e) {

                Log.e(
                        TAG,
                        "Recording could not be finalized",
                        e
                );


                deleteIncompleteOutputFile();
            }
        }


        // =====================================================
        // RELEASE EVERYTHING
        // =====================================================

        releaseRecorder();

        cleanupCameraResources();


        currentOutputFile = null;
    }


    // =========================================================
    // RELEASE MEDIA RECORDER
    // =========================================================

    private void releaseRecorder() {

        if (mediaRecorder != null) {

            try {

                mediaRecorder.reset();

            } catch (Exception e) {

                Log.w(
                        TAG,
                        "Recorder reset failed",
                        e
                );
            }


            try {

                mediaRecorder.release();

            } catch (Exception e) {

                Log.w(
                        TAG,
                        "Recorder release failed",
                        e
                );
            }


            mediaRecorder = null;
        }


        isRecording = false;
    }


    // =========================================================
    // CLEAN CAMERA2 RESOURCES
    // =========================================================

    private void cleanupCameraResources() {

        if (captureSession != null) {

            try {

                captureSession.close();

            } catch (Exception e) {

                Log.w(
                        TAG,
                        "Capture session close failed",
                        e
                );
            }


            captureSession = null;
        }


        if (cameraDevice != null) {

            try {

                cameraDevice.close();

            } catch (Exception e) {

                Log.w(
                        TAG,
                        "Camera close failed",
                        e
                );
            }


            cameraDevice = null;
        }


        recorderSurface = null;

        cameraId = null;

        videoSize = null;
    }


    // =========================================================
    // DELETE INCOMPLETE FILE
    // =========================================================

    private void deleteIncompleteOutputFile() {

        if (
                currentOutputFile != null
                        &&
                        currentOutputFile.exists()
        ) {

            try {

                boolean deleted =
                        currentOutputFile.delete();


                Log.d(
                        TAG,
                        "Incomplete file deleted: "
                                + deleted
                );


            } catch (Exception e) {

                Log.w(
                        TAG,
                        "Could not delete incomplete file",
                        e
                );
            }
        }


        currentOutputFile = null;
    }


    // =========================================================
    // STOP SERVICE SAFELY
    // =========================================================

    private void stopServiceSafely() {

        releaseRecorder();

        cleanupCameraResources();

        deleteIncompleteOutputFile();

        stopForegroundService();

        stopSelf();
    }


    // =========================================================
    // STOP FOREGROUND SERVICE
    // =========================================================

    private void stopForegroundService() {

        stopForeground(
                STOP_FOREGROUND_REMOVE
        );


        stopSelf();
    }


    // =========================================================
    // NOTIFICATION CHANNEL
    // =========================================================

    private void createNotificationChannel() {

        if (
                Build.VERSION.SDK_INT
                        >= Build.VERSION_CODES.O
        ) {

            NotificationChannel channel =
                    new NotificationChannel(
                            CHANNEL_ID,
                            "KIRA Evidence Recording",
                            NotificationManager
                                    .IMPORTANCE_LOW
                    );


            channel.setDescription(
                    "Shows when KIRA emergency evidence recording is active."
            );


            NotificationManager manager =
                    getSystemService(
                            NotificationManager.class
                    );


            if (manager != null) {

                manager.createNotificationChannel(
                        channel
                );
            }
        }
    }


    // =========================================================
    // NOTIFICATION
    // =========================================================

    private Notification buildNotification() {

        return new NotificationCompat.Builder(
                this,
                CHANNEL_ID
        )
                .setContentTitle(
                        "KIRA Emergency Recording"
                )
                .setContentText(
                        "Evidence recording is active"
                )
                .setSmallIcon(
                        android.R.drawable
                                .ic_btn_speak_now
                )
                .setOngoing(true)
                .setPriority(
                        NotificationCompat
                                .PRIORITY_LOW
                )
                .build();
    }


    // =========================================================
    // DESTROY
    // =========================================================

    @Override
    public void onDestroy() {

        mainHandler.removeCallbacksAndMessages(
                null
        );


        releaseRecorder();

        cleanupCameraResources();


        Log.d(
                TAG,
                "EvidenceRecordingService destroyed"
        );


        super.onDestroy();
    }


    // =========================================================
    // NOT BOUND
    // =========================================================

    @Nullable
    @Override
    public IBinder onBind(
            Intent intent
    ) {

        return null;
    }
}