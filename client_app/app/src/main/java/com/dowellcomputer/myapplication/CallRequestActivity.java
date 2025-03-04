package com.dowellcomputer.myapplication;

import android.content.Intent;
import android.media.MediaPlayer;
import android.os.Bundle;
import android.util.Log;
import androidx.appcompat.app.AppCompatActivity;
import org.json.JSONObject;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;


import android.view.View;
import android.widget.TextView;
import android.widget.ImageButton;

public class CallRequestActivity extends AppCompatActivity {

    private static final String TAG = "CallRequestActivity";
    private static final String SERVER_URL = "http://192.168.35.243:8767/request"; // 클라이언트 PC의 주소
//    static ImageButton finish;
    private TextView calling, getText;
    private static final int MAX_TEXT_LENGTH = 11;
    private boolean isListening = false;
    private MediaPlayer mediaPlayer;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.calling);

        if (!isListening) {
            isListening = true;
            startDialTone();
            sendCallRequest();
        }
        calling = findViewById(R.id.calling);
        getText = findViewById(R.id.calling_to);
        // 로그 메시지 추가
        Log.d(TAG, "CallRequestActivity started");

        //입력받은 번호 넣기
        Intent intent = getIntent();
        String inputText = intent.getStringExtra("input");

        if (inputText != null)
            getText.setText(inputText);


        findViewById(R.id.finish).setOnClickListener(v -> {
            stopListening();
            Intent backtomain = new Intent(this, MainActivity.class);
            startActivity(backtomain);
            finish();
        });
    }


    private void startDialTone() {
        mediaPlayer = MediaPlayer.create(this, R.raw.dial_tone);
        mediaPlayer.setLooping(true);
        mediaPlayer.start();
    }

    private void stopDialTone() {
        if (mediaPlayer != null) {
            mediaPlayer.stop();
            mediaPlayer.release();
            mediaPlayer = null;
        }
    }

    private void sendCallRequest() {
        new Thread(() -> {
            try {
                URL url = new URL(SERVER_URL);
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setRequestMethod("POST");
                conn.setRequestProperty("Content-Type", "application/json; utf-8");
                conn.setRequestProperty("Accept", "application/json");
                conn.setDoOutput(true);

                JSONObject jsonParam = new JSONObject();
                jsonParam.put("type", "call_request");

                try (OutputStream os = conn.getOutputStream()) {
                    byte[] input = jsonParam.toString().getBytes("utf-8");
                    os.write(input, 0, input.length);
                }

                try (InputStream in = conn.getInputStream()) {
                    byte[] response = new byte[1024];
                    int length = in.read(response);
                    String responseStr = new String(response, 0, length);

                    // 응답을 로그에 출력
                    Log.d(TAG, "Response from server: " + responseStr);

                    JSONObject jsonResponse = new JSONObject(responseStr);
                    if (jsonResponse.getString("status").equals("accepted")) {
                        runOnUiThread(() -> {
                            // 다이얼 톤 중지
                            stopDialTone();
                            // 통화 시작을 위한 기존 ClientActivity 호출
                            Log.d(TAG, "Response from server: okok " );
                            startCall();
                        });
                    } else {
                        runOnUiThread(() -> {
                            stopDialTone();
                            Log.d(TAG, "Call declined");
                        });
                    }
                }

            } catch (Exception e) {
                e.printStackTrace();
                stopDialTone();
            }
        }).start();
    }

    private void startCall() {
        Intent intent = new Intent(this, Client.class);
        intent.putExtra("input", getText.getText().toString());
        startActivity(intent);
        finish();
    }

    private void stopListening() {
        isListening = false;
        stopDialTone();
        // 추가적인 통화 종료 처리가 필요하면 여기서 수행
    }

    @Override
    public void onBackPressed() {
        // 뒤로가기 버튼을 비활성화하려면 이 메서드를 비워둡니다.
    }
}
