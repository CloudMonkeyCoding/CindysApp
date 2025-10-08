package com.example.deliveryapp;

import android.os.Bundle;

import androidx.annotation.Nullable;

public class DeliveriesActivity extends BottomNavActivity {

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_deliveries);
        setupBottomNavigation(R.id.menu_deliveries);
    }
}
