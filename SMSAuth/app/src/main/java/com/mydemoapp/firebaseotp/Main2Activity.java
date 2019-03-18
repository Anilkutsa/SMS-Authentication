package com.mydemoapp.firebaseotp;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.TimeZone;

public class Main2Activity extends AppCompatActivity {

    private TextView totalTimeTaken;
    SharedPreferences sharedPreferences;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main2);

        totalTimeTaken = findViewById(R.id.total_time_taken);

        sharedPreferences = getSharedPreferences(MainActivity.PREF_NAME, Context.MODE_PRIVATE);

        long startTime = sharedPreferences.getLong(MainActivity.VERIFICATION_START_TIME, 0);
        long endTime = sharedPreferences.getLong(MainActivity.OTP_RECIEVED_TIME, 0);

        long timeDiff = (endTime - startTime)/1000;
        totalTimeTaken.setText(timeDiff + " Seconds");
    }
}
