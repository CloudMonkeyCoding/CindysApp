package com.example.deliveryapp;

import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.example.deliveryapp.tracking.OrderTrackingManager;

import java.text.DateFormat;
import java.util.Date;
import java.util.Locale;

public class MapActivity extends BottomNavActivity implements OrderTrackingManager.LocationUpdateListener {

    private View activeOrderContainer;
    private TextView emptyStateView;
    private TextView orderNumberView;
    private TextView orderStatusView;
    private TextView orderSummaryView;
    private TextView orderAddressView;
    private View locationContainer;
    private TextView locationDetailsView;
    private TextView locationTimestampView;
    private Button navigationButton;

    private OrderTrackingManager orderTrackingManager;
    @Nullable
    private OrderTrackingManager.TrackedOrder activeOrder;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_customer_map);
        setupBottomNavigation(R.id.menu_map);

        orderTrackingManager = OrderTrackingManager.getInstance(getApplicationContext());
        initViews();
        refreshActiveOrder();
        refreshLocationState();
    }

    @Override
    protected void onStart() {
        super.onStart();
        orderTrackingManager.addLocationUpdateListener(this);
        orderTrackingManager.ensureLocationTracking();
    }

    @Override
    protected void onResume() {
        super.onResume();
        refreshActiveOrder();
        refreshLocationState();
    }

    @Override
    protected void onStop() {
        orderTrackingManager.removeLocationUpdateListener(this);
        super.onStop();
    }

    private void initViews() {
        emptyStateView = findViewById(R.id.mapEmptyState);
        activeOrderContainer = findViewById(R.id.mapActiveOrderContainer);
        orderNumberView = findViewById(R.id.mapOrderNumber);
        orderStatusView = findViewById(R.id.mapOrderStatus);
        orderSummaryView = findViewById(R.id.mapOrderSummary);
        orderAddressView = findViewById(R.id.mapOrderAddress);
        locationContainer = findViewById(R.id.mapLocationContainer);
        locationDetailsView = findViewById(R.id.mapLocationDetails);
        locationTimestampView = findViewById(R.id.mapLocationTimestamp);
        navigationButton = findViewById(R.id.btnArrivedCustomer);

        if (locationDetailsView != null) {
            locationDetailsView.setText(R.string.map_location_unavailable);
        }

        if (navigationButton != null) {
            navigationButton.setOnClickListener(v -> launchNavigation());
        }
    }

    private void refreshActiveOrder() {
        activeOrder = orderTrackingManager.getActiveOrder();
        if (activeOrder == null) {
            showNoActiveOrderState();
            return;
        }

        if (emptyStateView != null) {
            emptyStateView.setVisibility(View.GONE);
        }
        if (activeOrderContainer != null) {
            activeOrderContainer.setVisibility(View.VISIBLE);
        }

        if (orderNumberView != null) {
            orderNumberView.setText(getString(R.string.map_order_number, activeOrder.getOrderId()));
        }

        if (orderStatusView != null) {
            String status = activeOrder.getStatus();
            if (TextUtils.isEmpty(status)) {
                status = getString(R.string.map_order_status_placeholder);
            }
            orderStatusView.setText(getString(R.string.map_order_status, status));
        }

        if (orderSummaryView != null) {
            String summary = activeOrder.getSummary();
            if (TextUtils.isEmpty(summary)) {
                summary = getString(R.string.map_order_summary_placeholder);
            }
            orderSummaryView.setText(summary);
        }

        if (orderAddressView != null) {
            String address = activeOrder.getAddress();
            if (TextUtils.isEmpty(address)) {
                orderAddressView.setText(R.string.map_order_address_missing);
            } else {
                orderAddressView.setText(getString(R.string.map_order_address, address));
            }
        }

        if (navigationButton != null) {
            navigationButton.setEnabled(!TextUtils.isEmpty(activeOrder.getAddress()));
        }
    }

    private void showNoActiveOrderState() {
        if (activeOrderContainer != null) {
            activeOrderContainer.setVisibility(View.GONE);
        }
        if (navigationButton != null) {
            navigationButton.setEnabled(false);
        }
        if (emptyStateView != null) {
            emptyStateView.setVisibility(View.VISIBLE);
            emptyStateView.setText(R.string.map_no_active_order);
        }
    }

    private void refreshLocationState() {
        if (locationContainer == null || locationDetailsView == null || locationTimestampView == null) {
            return;
        }

        if (!orderTrackingManager.canAccessLocation()) {
            locationContainer.setVisibility(View.VISIBLE);
            locationDetailsView.setText(R.string.map_location_permission_missing);
            locationTimestampView.setVisibility(View.GONE);
            return;
        }

        OrderTrackingManager.TrackedLocation lastLocation = orderTrackingManager.getLastKnownLocation();
        if (lastLocation == null) {
            locationContainer.setVisibility(View.VISIBLE);
            locationDetailsView.setText(R.string.map_location_unavailable);
            locationTimestampView.setVisibility(View.GONE);
            return;
        }

        updateLocationViews(lastLocation);
    }

    private void updateLocationViews(@NonNull OrderTrackingManager.TrackedLocation location) {
        if (locationContainer == null || locationDetailsView == null || locationTimestampView == null) {
            return;
        }

        locationContainer.setVisibility(View.VISIBLE);

        String provider = location.getProvider();
        if (TextUtils.isEmpty(provider)) {
            provider = getString(R.string.map_location_provider_unknown);
        }

        String details = getString(
                R.string.map_location_coordinates,
                location.getLatitude(),
                location.getLongitude(),
                provider
        );
        locationDetailsView.setText(details);

        long timestamp = location.getTimestampMillis();
        if (timestamp > 0L) {
            DateFormat timeFormat = DateFormat.getTimeInstance(DateFormat.SHORT, Locale.getDefault());
            String formattedTime = timeFormat.format(new Date(timestamp));
            locationTimestampView.setVisibility(View.VISIBLE);
            locationTimestampView.setText(getString(R.string.map_location_timestamp, formattedTime));
        } else {
            locationTimestampView.setVisibility(View.GONE);
        }
    }

    private void launchNavigation() {
        if (activeOrder == null) {
            Toast.makeText(this, R.string.map_no_active_order, Toast.LENGTH_SHORT).show();
            return;
        }

        String address = activeOrder.getAddress();
        if (TextUtils.isEmpty(address)) {
            Toast.makeText(this, R.string.map_navigation_missing_address, Toast.LENGTH_SHORT).show();
            return;
        }

        Uri uri = Uri.parse("geo:0,0?q=" + Uri.encode(address));
        Intent intent = new Intent(Intent.ACTION_VIEW, uri);
        intent.setPackage("com.google.android.apps.maps");
        try {
            startActivity(intent);
        } catch (ActivityNotFoundException primaryException) {
            intent.setPackage(null);
            try {
                startActivity(intent);
            } catch (ActivityNotFoundException fallbackException) {
                Toast.makeText(this, R.string.map_navigation_app_missing, Toast.LENGTH_SHORT).show();
            }
        }
    }

    @Override
    public void onLocationUpdated(@NonNull OrderTrackingManager.TrackedLocation location) {
        runOnUiThread(() -> updateLocationViews(location));
    }
}
