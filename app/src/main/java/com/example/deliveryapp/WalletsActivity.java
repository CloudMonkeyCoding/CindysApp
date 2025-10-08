package com.example.deliveryapp;

import android.os.Bundle;

import androidx.annotation.Nullable;

public class WalletsActivity extends BottomNavActivity {

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activitywallet);
        setupBottomNavigation(R.id.menu_wallets);
    }
}
