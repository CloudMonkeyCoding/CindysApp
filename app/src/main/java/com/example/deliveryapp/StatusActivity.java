package com.example.deliveryapp;

import android.os.Bundle;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;

import com.example.deliveryapp.BuildConfig;
import com.example.deliveryapp.network.ServerConnectionManager;

public class StatusActivity extends BottomNavActivity {

    private TextView statusBanner;
    private ProgressBar connectionProgress;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_status);
        setupBottomNavigation(R.id.menu_status);

        statusBanner = findViewById(R.id.statusBanner);
        connectionProgress = findViewById(R.id.connectionProgress);

        checkServerConnection();
    }

    private void checkServerConnection() {
        if (statusBanner == null || connectionProgress == null) {
            return;
        }

        connectionProgress.setVisibility(View.VISIBLE);
        updateStatusUi(StatusState.CHECKING, getString(R.string.status_checking_message));

        ServerConnectionManager.getInstance().checkConnection(
                getString(R.string.server_health_path),
                (isConnected, errorMessage) -> {
                    connectionProgress.setVisibility(View.GONE);

                    if (isConnected) {
                        updateStatusUi(
                                StatusState.CONNECTED,
                                getString(R.string.status_connected, BuildConfig.API_BASE_URL)
                        );
                    } else {
                        String errorText = getString(R.string.status_connection_failed);
                        if (errorMessage != null && !errorMessage.isEmpty()) {
                            errorText = errorText + " (" + errorMessage + ")";
                        }
                        updateStatusUi(StatusState.ERROR, errorText);
                    }
                }
        );
    }

    private void updateStatusUi(StatusState state, String message) {
        if (statusBanner == null) {
            return;
        }

        statusBanner.setText(message);

        int backgroundColorRes;
        int textColorRes;

        if (state == StatusState.CONNECTED) {
            backgroundColorRes = R.color.status_connected_bg;
            textColorRes = R.color.status_connected_text;
        } else if (state == StatusState.ERROR) {
            backgroundColorRes = R.color.status_error_bg;
            textColorRes = R.color.status_error_text;
        } else {
            backgroundColorRes = R.color.status_checking_bg;
            textColorRes = R.color.status_checking_text;
        }

        statusBanner.setBackgroundColor(ContextCompat.getColor(this, backgroundColorRes));
        statusBanner.setTextColor(ContextCompat.getColor(this, textColorRes));
    }

    private enum StatusState {
        CHECKING,
        CONNECTED,
        ERROR
    }
}
