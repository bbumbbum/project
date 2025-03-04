package com.dowellcomputer.myapplication;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import androidx.appcompat.app.AppCompatActivity;

import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.ImageButton;

public class MainActivity extends AppCompatActivity{

    private static final String TAG = "MainActivity";
    static Button one, two, three, four, five, six, seven, eight, nine, star, zero, sharp;
    static ImageButton start;
    private TextView textViewResult;
    private static final int MAX_TEXT_LENGTH = 15;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main_layout);

        // 버튼 ID 배열
        int[] buttonIds = {
                R.id.one, R.id.two, R.id.three, R.id.four, R.id.five,
                R.id.six, R.id.seven, R.id.eight, R.id.nine,
                R.id.star, R.id.zero, R.id.sharp
        };


        one = findViewById(R.id.one);
        two = findViewById(R.id.two);
        three = findViewById(R.id.three);
        four = findViewById(R.id.four);
        five = findViewById(R.id.five);
        six = findViewById(R.id.six);
        seven = findViewById(R.id.seven);
        eight = findViewById(R.id.eight);
        nine = findViewById(R.id.nine);
        star = findViewById(R.id.star);
        zero = findViewById(R.id.zero);
        sharp = findViewById(R.id.sharp);
        //textView 초기화
        textViewResult = findViewById(R.id.textView_result);
        //Imagebutton 초기화
        ImageButton delete = findViewById(R.id.delete);
        start = findViewById(R.id.start);

        //숫자 키패드 설정
        for (int id : buttonIds) {
            Button button = findViewById(id);
            button.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    if (v instanceof Button) {
                        Button button = (Button) v;
                        String buttonText = button.getText().toString();
                        appendTextToTextView(buttonText);
                    }
                }
            });
        }
        //키패드 지우기 버튼 설정
        delete.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                deleteLastCharacter();
            }

            private void deleteLastCharacter() {
                String currentText = textViewResult.getText().toString();
                if (currentText.length() > 0) {
                    currentText = currentText.substring(0, currentText.length() - 1);

                    // 전화번호 형식 적용
                    currentText = formatPhoneNumber(currentText.replaceAll("-", ""));
                    textViewResult.setText(currentText);
                }
            }

        });

        // 로그 메시지 추가
        Log.d(TAG, "CallRequestActivity converted");

        findViewById(R.id.start).setOnClickListener(v -> {
                textViewResult.setText(textViewResult.getText().toString());
                Intent intent = new Intent(this, CallRequestActivity.class);
                intent.putExtra("input", textViewResult.getText().toString());
                startActivity(intent);
                finish();



        });
    }

    private void appendTextToTextView(String text) {
        String currentText = textViewResult.getText().toString();
        String newText = currentText + text;

         // 길이를 제한하여 설정
        if (newText.length() > MAX_TEXT_LENGTH) {
            newText = newText.substring(0, MAX_TEXT_LENGTH);
        }

        // 전화번호 형식 적용
        newText = formatPhoneNumber(newText);

        textViewResult.setText(newText);

        }

    private String formatPhoneNumber(String phoneNumber) {
        phoneNumber = phoneNumber.replaceAll("[^\\d]", ""); // 숫자 외의 문자 제거

        StringBuilder formatted = new StringBuilder();

        int length = phoneNumber.length();
        if (length > 3) {
            if (phoneNumber.startsWith("010")) {
                formatted.append(phoneNumber.substring(0, 3)).append("-");
                if (length > 7) {
                    formatted.append(phoneNumber.substring(3, 7)).append("-");
                    formatted.append(phoneNumber.substring(7, Math.min(length, 11)));
                } else {
                    formatted.append(phoneNumber.substring(3, length));
                }
            } else {
                formatted.append(phoneNumber.substring(0, 3)).append("-");
                if (length > 6) {
                    formatted.append(phoneNumber.substring(3, 6)).append("-");
                    formatted.append(phoneNumber.substring(6, Math.min(length, 11)));
                } else {
                    formatted.append(phoneNumber.substring(3, length));
                }
            }
        } else {
            formatted.append(phoneNumber);
        }

        return formatted.toString();
    }


    @Override
    public void onBackPressed() {
        // 현재 액티비티를 종료하여 앱을 종료
        finish();
    }
}
