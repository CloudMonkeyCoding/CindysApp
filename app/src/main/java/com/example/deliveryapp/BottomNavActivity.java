package com.example.deliveryapp;

import android.content.Intent;

import androidx.annotation.IdRes;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.bottomnavigation.BottomNavigationView;

public abstract class BottomNavActivity extends AppCompatActivity {

    protected void setupBottomNavigation(@IdRes int selectedItemId) {
        BottomNavigationView bottomNavigationView = findViewById(R.id.bottomNavigation);
        if (bottomNavigationView == null) {
            return;
        }

        bottomNavigationView.setOnItemSelectedListener(item -> {
            if (item.getItemId() == selectedItemId) {
                return true;
            }

            Intent intent = null;
            int itemId = item.getItemId();
            if (itemId == R.id.menu_deliveries) {
                intent = new Intent(this, DeliveriesActivity.class);
            } else if (itemId == R.id.menu_map) {
                intent = new Intent(this, MapActivity.class);
            } else if (itemId == R.id.menu_history) {
                intent = new Intent(this, HistoryActivity.class);
            }

            if (intent != null) {
                intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                startActivity(intent);
                overridePendingTransition(0, 0);
                finish();
                return true;
            }

            return false;
        });

        bottomNavigationView.setSelectedItemId(selectedItemId);
    }
}
