package com.example.deliveryapp;

import android.os.Bundle;
import android.widget.Button;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.example.deliveryapp.R;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        Button startButton = findViewById(R.id.btnStart);
        if (startButton != null) {
            startButton.setOnClickListener(v ->
                    Toast.makeText(this, R.string.app_name, Toast.LENGTH_SHORT).show()
            );
        }
    }
}
