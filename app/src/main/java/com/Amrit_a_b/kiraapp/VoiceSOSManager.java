package com.Amrit_a_b.kiraapp;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.speech.RecognitionListener;
import android.speech.RecognizerIntent;
import android.speech.SpeechRecognizer;
import android.util.Log;

import java.util.ArrayList;

public class VoiceSOSManager {

    private SpeechRecognizer speechRecognizer;
    private Context context;

    public VoiceSOSManager(Context context) {
        this.context = context;
    }

    public void startListening() {

        speechRecognizer = SpeechRecognizer.createSpeechRecognizer(context);

        Intent intent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL,
                RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);

        speechRecognizer.setRecognitionListener(new RecognitionListener() {

            @Override
            public void onResults(Bundle results) {

                ArrayList<String> matches =
                        results.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION);

                if (matches != null) {

                    for (String text : matches) {

                        text = text.toLowerCase();

                        Log.d("VoiceSOS", "Detected: " + text);

                        if (text.contains("help")
                                || text.contains("bachao")
                                || text.contains("sos")) {

                            triggerSOS();
                            break;
                        }
                    }
                }

                restartListening();
            }

            @Override public void onReadyForSpeech(Bundle params) {}
            @Override public void onBeginningOfSpeech() {}
            @Override public void onRmsChanged(float rmsdB) {}
            @Override public void onBufferReceived(byte[] buffer) {}
            @Override public void onEndOfSpeech() {}
            @Override public void onError(int error) { restartListening(); }
            @Override public void onPartialResults(Bundle partialResults) {}
            @Override public void onEvent(int eventType, Bundle params) {}
        });

        speechRecognizer.startListening(intent);
    }

    private void restartListening() {
        if (speechRecognizer != null) {
            speechRecognizer.stopListening();
            startListening();
        }
    }

    private void triggerSOS() {

        Log.d("VoiceSOS", "SOS Triggered");

        Intent intent = new Intent(context, EmergencyAlertActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        context.startActivity(intent);
    }

    public void stopListening() {
        if (speechRecognizer != null) {
            speechRecognizer.destroy();
        }
    }
}