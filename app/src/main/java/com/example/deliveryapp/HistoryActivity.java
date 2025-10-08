package com.example.deliveryapp;

import android.os.Bundle;

import androidx.annotation.Nullable;

public class HistoryActivity extends BottomNavActivity {

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activityhistory);
        setupBottomNavigation(R.id.menu_history);
    }
}
