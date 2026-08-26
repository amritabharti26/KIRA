package com.Amrit_a_b.kiraapp;

import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;

public class ShakeDetector implements SensorEventListener {

    private static final float SHAKE_THRESHOLD = 25f; // Strong shake
    private static final int SHAKE_WINDOW_MS = 2000; // 2 seconds window
    private static final int MIN_SHAKES = 3; // 3 shakes to trigger

    private long firstShakeTime = 0;
    private int shakeCount = 0;

    public interface OnShakeListener {
        void onShake();
    }

    private final OnShakeListener listener;

    public ShakeDetector(OnShakeListener listener) {
        this.listener = listener;
    }

    @Override
    public void onSensorChanged(SensorEvent event) {
        if (event.sensor.getType() == Sensor.TYPE_ACCELEROMETER) {
            float x = event.values[0];
            float y = event.values[1];
            float z = event.values[2];

            float acceleration = (float) Math.sqrt(x * x + y * y + z * z);

            if (acceleration > SHAKE_THRESHOLD) {
                long now = System.currentTimeMillis();

                if (shakeCount == 0) {
                    firstShakeTime = now;
                    shakeCount++;
                } else if (now - firstShakeTime < SHAKE_WINDOW_MS) {
                    shakeCount++;
                    if (shakeCount >= MIN_SHAKES) {
                        shakeCount = 0;
                        if (listener != null) {
                            listener.onShake();
                        }
                    }
                } else {
                    // Reset if window expired
                    firstShakeTime = now;
                    shakeCount = 1;
                }
            }
        }
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {
    }
}