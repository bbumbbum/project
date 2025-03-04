package com.dowellcomputer.myapplication;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.media.AudioAttributes;
import android.media.AudioFormat;
import android.media.AudioManager;
import android.media.AudioTrack;
import android.media.audiofx.AcousticEchoCanceler;
import android.os.Bundle;
import android.os.Handler;
import android.speech.RecognizerIntent;
import android.speech.RecognitionListener;
import android.speech.SpeechRecognizer;
import android.util.Log;
import android.view.View;
import android.widget.TextView;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import java.io.InputStream;
import java.io.IOException;
import java.io.EOFException;
import java.util.ArrayList;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class Client extends AppCompatActivity {

    private static final int REQUEST_CODE = 1234;
    private static final String TAG = "ClientActivity";
    private SpeechRecognizer speechRecognizer;
    private boolean isListening = false;
    private TextView getText, calling;
    private Handler handler = new Handler();
    private static final String TEXT_SERVER_URL = "http://192.168.35.243:8765/receive";
    private static final String AUDIO_STREAM_URL = "http://192.168.35.243:8766/stream";
    private ExecutorService executorService = Executors.newSingleThreadExecutor();
    private AudioTrack audioTrack;
    //에코 제거 설정
    private AcousticEchoCanceler echoCanceler;
    //타이머 설정
    private long startTime = 0L;
    private boolean isRunning = false;
    private Runnable updateTimerThread = new Runnable() {
        //스탑워치 설정하기
        @Override
        public void run() {
            long timeInMilliseconds = System.currentTimeMillis() - startTime;
            int secs = (int) (timeInMilliseconds / 1000);
            int mins = secs / 60;
            secs = secs % 60;
            calling.setText(String.format("%02d:%02d", mins, secs));
            handler.postDelayed(this, 1000);
        }
    };

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.calling);

//        start = findViewById(R.id.start);
        calling = findViewById(R.id.calling);
        getText = findViewById(R.id.calling_to);
        //입력받은 번호 넣기
        Intent intent = getIntent();
        String inputText = intent.getStringExtra("input");

        if (inputText != null)
            getText.setText(inputText);
        //스탑워치 돌리기
        calling.setText("00:00");
        startStopwatch();

        speechRecognizer = SpeechRecognizer.createSpeechRecognizer(this);

        findViewById(R.id.finish).setOnClickListener(v -> {
            stopStopwatch();
            stopListening();
            Intent backtomain = new Intent(this, MainActivity.class);
            startActivity(backtomain);
            finish();
        });

        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.RECORD_AUDIO}, REQUEST_CODE);
        } else {
            // Start listening and streaming audio automatically when the activity is created
            isListening = true;
            startListening();
            startStreamingAudio();
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQUEST_CODE && grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            // Start listening and streaming audio if permission is granted
            isListening = true;
            startListening();
            startStreamingAudio();
        }
    }

    private void startListening() {
        Intent intent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault());
        intent.putExtra(RecognizerIntent.EXTRA_PROMPT, "Speak now...");
        speechRecognizer.setRecognitionListener(new RecognitionListener() {
            @Override
            public void onReadyForSpeech(Bundle params) {
                Log.d(TAG, "Ready for speech");
            }

            @Override
            public void onBeginningOfSpeech() {
                Log.d(TAG, "Beginning of speech");
            }

            @Override
            public void onRmsChanged(float rmsdB) {
                // No implementation needed
            }

            @Override
            public void onBufferReceived(byte[] buffer) {
                // No implementation needed
            }

            @Override
            public void onEndOfSpeech() {
                Log.d(TAG, "End of speech");
            }

            @Override
            public void onError(int error) {
                Log.d(TAG, "Error: " + error);
                if (isListening) {
                    startListening();  // Restart listening on error
                }
            }

            @Override
            public void onResults(Bundle results) {
                ArrayList<String> matches = results.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION);
                if (matches != null && !matches.isEmpty()) {
                    String recognizedText = matches.get(0);
                    Log.d(TAG, "Recognized Text: " + recognizedText);
                    sendTextToServer(recognizedText);  // 서버로 텍스트 전송
                }
                if (isListening) {
                    startListening();  // Restart listening after results
                }
            }

            @Override
            public void onPartialResults(Bundle partialResults) {
                // No implementation needed
            }

            @Override
            public void onEvent(int eventType, Bundle params) {
                // No implementation needed
            }
        });
        speechRecognizer.startListening(intent);
    }

    private void sendTextToServer(String text) {
        OkHttpClient client = new OkHttpClient.Builder()
                .connectTimeout(30, TimeUnit.SECONDS)
                .writeTimeout(30, TimeUnit.SECONDS)
                .readTimeout(30, TimeUnit.SECONDS)
                .build();

        MediaType JSON = MediaType.parse("application/json; charset=utf-8");
        String json = "{\"text\":\"" + text + "\"}";
        RequestBody body = RequestBody.create(JSON, json);
        Request request = new Request.Builder()
                .url(TEXT_SERVER_URL)
                .post(body)
                .build();

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                Log.e(TAG, "Failed to send text to server", e);
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                if (!response.isSuccessful()) {
                    Log.e(TAG, "Unexpected code " + response);
                } else {
                    Log.d(TAG, "Successfully sent text to server");
                }
            }
        });
    }

    private void startStreamingAudio() {
        executorService.execute(() -> {
            OkHttpClient client = new OkHttpClient.Builder()
                    .connectTimeout(60,TimeUnit.SECONDS)
                    .writeTimeout(60, TimeUnit.SECONDS)
                    .readTimeout(60, TimeUnit.SECONDS)
                    .build();

            Request request = new Request.Builder()
                    .url(AUDIO_STREAM_URL)
                    .build();

            client.newCall(request).enqueue(new Callback() {
                @Override
                public void onFailure(Call call, IOException e) {
                    Log.e(TAG, "Failed to stream audio", e);
                }

                @Override
                public void onResponse(Call call, Response response) throws IOException {
                    if (!response.isSuccessful()) {
                        Log.e(TAG, "Unexpected code " + response);
                        return;
                    }

                    try (InputStream inputStream = response.body().byteStream()) {
                        playAudio(inputStream);
                    }
                }
            });
        });
    }

    private void playAudio(InputStream inputStream) {
        int sampleRate = 16000;  // Adjust as needed
        int bufferSize = AudioTrack.getMinBufferSize(sampleRate, AudioFormat.CHANNEL_OUT_MONO, AudioFormat.ENCODING_PCM_16BIT);

        audioTrack = new AudioTrack(
                new AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_MEDIA)
                        .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                        .build(),
                new AudioFormat.Builder()
                        .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                        .setSampleRate(sampleRate)
                        .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                        .build(),
                bufferSize,
                AudioTrack.MODE_STREAM,
                AudioManager.AUDIO_SESSION_ID_GENERATE
        );

        // 에코 캔슬러 적용
        if (AcousticEchoCanceler.isAvailable()) {
            echoCanceler = AcousticEchoCanceler.create(audioTrack.getAudioSessionId());
            if (echoCanceler != null) {
                echoCanceler.setEnabled(true);
            }
        }

        audioTrack.play();

        byte[] buffer = new byte[bufferSize];
        int bytesRead;
        try {
            while ((bytesRead = inputStream.read(buffer)) != -1 && isListening) {
                audioTrack.write(buffer, 0, bytesRead);
            }
        }catch (EOFException e) {
            Log.e(TAG, "Stream ended", e);
        }catch (IOException e) {
            Log.e(TAG, "Error playing audio", e);
        }
        finally {
            audioTrack.stop();
            audioTrack.release();
        }
    }

    private void stopListening() {
        isListening = false;
        if(speechRecognizer != null){
            speechRecognizer.stopListening();
            speechRecognizer.cancel();
            speechRecognizer.destroy();
        }
        executorService.shutdownNow();

        if (audioTrack != null) {
            if (audioTrack.getState() == AudioTrack.STATE_INITIALIZED) {
                audioTrack.stop();
            }
            audioTrack.release();
            audioTrack = null;
        }

        if (echoCanceler != null) {
            echoCanceler.release();
            echoCanceler = null;
        }


    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (speechRecognizer != null) {
            speechRecognizer.destroy();
        }

        if (audioTrack != null) {
            audioTrack.release();
        }

        if (echoCanceler != null) {
            echoCanceler.release();
        }

        executorService.shutdownNow();
    }

    private void startStopwatch() {
        if (!isRunning) {
            startTime = System.currentTimeMillis();
            handler.postDelayed(updateTimerThread, 0);
            isRunning = true;
        }
    }

    private void stopStopwatch() {
        if (isRunning) {
            handler.removeCallbacks(updateTimerThread);
            isRunning = false;
            Log.d(TAG, "ended");
        }
    }

    private void backMain() {
        Intent intent = new Intent(Client.this, CallRequestActivity.class);
        startActivity(intent);
        finish();
    }

    @Override
    public void onBackPressed() {
        // 뒤로가기 버튼을 비활성화하려면 이 메서드를 비워둡니다.
    }

}