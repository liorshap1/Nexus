package com.example.nexus.utils;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.speech.RecognitionListener;
import android.speech.RecognizerIntent;
import android.speech.SpeechRecognizer;
import android.util.Log;

import java.util.Objects;

public class SpeechRecognizerUtils {
    private SpeechRecognizer speechRecognizer;
    private RecognitionListener recognitionListener;
    private final Context context;
    private final Callback callback;

    public SpeechRecognizerUtils(Context context, Callback callback) {
        this.callback = callback;
        this.context = context;

        initSpeechRecognizer();
    }

    private void initSpeechRecognizer() {
        this.speechRecognizer = SpeechRecognizer.createSpeechRecognizer(context);
        this.recognitionListener = new RecognitionListener() {
            @Override
            public void onReadyForSpeech(Bundle params) {

            }

            @Override
            public void onBeginningOfSpeech() {

            }

            @Override
            public void onRmsChanged(float rmsdB) {

            }

            @Override
            public void onBufferReceived(byte[] buffer) {

            }

            @Override
            public void onEndOfSpeech() {

            }

            @Override
            public void onError(int error) {
                Log.d("SpeechRecognizerUtils", "Error on speech " + error);
                callback.onSpeechError(error);
            }

            @Override
            public void onResults(Bundle results) {
                String recognizedText = Objects.requireNonNull(results.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)).get(0);
                callback.onSpeechResult(recognizedText);
            }

            @Override
            public void onPartialResults(Bundle partialResults) {

            }

            @Override
            public void onEvent(int eventType, Bundle params) {

            }
        };

        assert this.speechRecognizer != null;
        this.speechRecognizer.setRecognitionListener(this.recognitionListener);
    }

    public void startListening() {
        Intent intent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
        speechRecognizer.startListening(intent);
    }

    public void stopListening() {
        if (speechRecognizer != null) {
            speechRecognizer.stopListening();
        }
    }

    public void destroy() {
        if (speechRecognizer != null) {
            speechRecognizer.destroy();
        }
    }

    public interface Callback {
        void onSpeechResult(String text);

        void onSpeechError(int error);
    }
}

