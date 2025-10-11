package com.example.deliveryapp;

import android.Manifest;
import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.app.ActivityCompat;

import com.example.deliveryapp.SessionManager;
import com.example.deliveryapp.tracking.OrderTrackingManager;
import com.example.deliveryapp.network.UserProfile;
import com.example.deliveryapp.network.UserService;

import java.text.DateFormat;
import java.util.Date;
import java.util.Locale;

public class MapActivity extends BottomNavActivity implements OrderTrackingManager.LocationUpdateListener {

    private static final int REQUEST_LOCATION_PERMISSIONS = 2001;
    private static final String KEY_LOCATION_PERMISSIONS_REQUESTED = "location_permissions_requested";
    public static final String EXTRA_FALLBACK_DESTINATION = "com.example.deliveryapp.extra.FALLBACK_DESTINATION";
    private static final String TAG = "MapActivity";

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
    @Nullable
    private String fallbackNavigationAddress;
    private UserService userService;

    private OrderTrackingManager orderTrackingManager;
    @Nullable
    private OrderTrackingManager.TrackedOrder activeOrder;
    private boolean hasRequestedLocationPermissions;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_customer_map);
        setupBottomNavigation(R.id.menu_map);

        String providedFallback = getIntent().getStringExtra(EXTRA_FALLBACK_DESTINATION);
        String resourceFallback = getString(R.string.map_vendor_fallback_address);
        fallbackNavigationAddress = selectFallbackAddress(providedFallback, resourceFallback);
        Log.d(TAG, "Initial fallback destination resolved. intent=" + providedFallback + ", resource=" + resourceFallback +
                ", selected=" + fallbackNavigationAddress);
        userService = new UserService();

        orderTrackingManager = OrderTrackingManager.getInstance(getApplicationContext());
        if (savedInstanceState != null) {
            hasRequestedLocationPermissions = savedInstanceState.getBoolean(
                    KEY_LOCATION_PERMISSIONS_REQUESTED,
                    false
            );
        }
        initViews();
        applyFallbackNavigationAddress(fallbackNavigationAddress);
        refreshActiveOrder();
        refreshLocationState();
        loadFallbackAddressFromProfile();
    }

    @Override
    protected void onStart() {
        super.onStart();
        orderTrackingManager.addLocationUpdateListener(this);
        if (!orderTrackingManager.ensureLocationTracking()) {
            requestLocationPermissionsIfNeeded();
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        refreshActiveOrder();
        refreshLocationState();
        if (!orderTrackingManager.canAccessLocation() && !hasRequestedLocationPermissions) {
            requestLocationPermissionsIfNeeded();
        }
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
            updateNavigationButtonState();
        }
    }

    private void refreshActiveOrder() {
        activeOrder = orderTrackingManager.getActiveOrder();
        if (activeOrder == null) {
            Log.d(TAG, "No active order available; showing fallback state.");
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

        Log.d(TAG, "Active order refreshed. id=" + activeOrder.getOrderId() + ", hasAddress="
                + !TextUtils.isEmpty(activeOrder.getAddress()));
        updateNavigationButtonState();
    }

    private void showNoActiveOrderState() {
        if (activeOrderContainer != null) {
            activeOrderContainer.setVisibility(View.GONE);
        }
        if (emptyStateView != null) {
            emptyStateView.setVisibility(View.VISIBLE);
            updateEmptyStateMessage();
        }
        updateNavigationButtonState();
    }

    private void updateEmptyStateMessage() {
        if (emptyStateView == null) {
            Log.d(TAG, "Empty state view unavailable while updating message.");
            return;
        }
        if (TextUtils.isEmpty(fallbackNavigationAddress)) {
            Log.d(TAG, "Empty state using generic message; fallback destination missing.");
            emptyStateView.setText(R.string.map_no_active_order);
        } else {
            Log.d(TAG, "Empty state using fallback destination: " + fallbackNavigationAddress);
            emptyStateView.setText(getString(R.string.map_no_active_order_vendor, fallbackNavigationAddress));
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
            Log.d(TAG, "Location unavailable; permissions not granted.");
            return;
        }

        OrderTrackingManager.TrackedLocation lastLocation = orderTrackingManager.getLastKnownLocation();
        if (lastLocation == null) {
            Log.d(TAG, "No last known location available yet.");
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
        if (activeOrder == null && TextUtils.isEmpty(fallbackNavigationAddress)) {
            Toast.makeText(this, R.string.map_no_active_order, Toast.LENGTH_SHORT).show();
            return;
        }

        String address = null;
        if (activeOrder != null) {
            address = activeOrder.getAddress();
        }
        if (TextUtils.isEmpty(address)) {
            address = fallbackNavigationAddress;
        }

        if (TextUtils.isEmpty(address)) {
            Toast.makeText(this, R.string.map_navigation_missing_destination, Toast.LENGTH_SHORT).show();
            return;
        }

        Uri uri = Uri.parse("geo:0,0?q=" + Uri.encode(address));
        Intent intent = new Intent(Intent.ACTION_VIEW, uri);
        intent.setPackage("com.google.android.apps.maps");
        try {
            startActivity(intent);
        } catch (ActivityNotFoundException primaryException) {
            Log.w(TAG, "Google Maps app unavailable, falling back to implicit intent.", primaryException);
            intent.setPackage(null);
            try {
                startActivity(intent);
            } catch (ActivityNotFoundException fallbackException) {
                Log.e(TAG, "No application available to handle map intent for address: " + address, fallbackException);
                Toast.makeText(this, R.string.map_navigation_app_missing, Toast.LENGTH_SHORT).show();
            }
        }
    }

    @Override
    public void onLocationUpdated(@NonNull OrderTrackingManager.TrackedLocation location) {
        runOnUiThread(() -> updateLocationViews(location));
    }

    @Override
    protected void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putBoolean(KEY_LOCATION_PERMISSIONS_REQUESTED, hasRequestedLocationPermissions);
    }

    private void requestLocationPermissionsIfNeeded() {
        if (orderTrackingManager.canAccessLocation()) {
            return;
        }
        hasRequestedLocationPermissions = true;
        ActivityCompat.requestPermissions(
                this,
                new String[]{Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION},
                REQUEST_LOCATION_PERMISSIONS
        );
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode != REQUEST_LOCATION_PERMISSIONS) {
            return;
        }

        boolean granted = false;
        for (int grantResult : grantResults) {
            if (grantResult == PackageManager.PERMISSION_GRANTED) {
                granted = true;
                break;
            }
        }

        if (granted) {
            orderTrackingManager.ensureLocationTracking();
            Toast.makeText(this, R.string.map_location_permission_granted, Toast.LENGTH_SHORT).show();
            return;
        }

        Toast.makeText(this, R.string.map_location_permission_denied, Toast.LENGTH_SHORT).show();
    }

    private void updateNavigationButtonState() {
        if (navigationButton == null) {
            return;
        }
        boolean hasOrderAddress = activeOrder != null && !TextUtils.isEmpty(activeOrder.getAddress());
        boolean hasFallback = !TextUtils.isEmpty(fallbackNavigationAddress);
        boolean enabled = hasOrderAddress || hasFallback;
        navigationButton.setEnabled(enabled);
        Log.d(TAG, "Navigation button state updated. hasOrderAddress=" + hasOrderAddress
                + ", hasFallback=" + hasFallback + ", enabled=" + enabled);
    }

    private void applyFallbackNavigationAddress(@Nullable String address) {
        if (address != null) {
            address = address.trim();
        }
        String sanitized = TextUtils.isEmpty(address) ? null : address;
        fallbackNavigationAddress = sanitized;
        Log.d(TAG, "Applying fallback destination: " + sanitized);
        updateNavigationButtonState();
        if (activeOrder == null) {
            updateEmptyStateMessage();
        }
    }

    private void loadFallbackAddressFromProfile() {
        if (!TextUtils.isEmpty(fallbackNavigationAddress)) {
            Log.d(TAG, "Skipping profile fetch; fallback destination already set to: " + fallbackNavigationAddress);
            return;
        }

        Integer userId = SessionManager.getUserId(getApplicationContext());
        String email = SessionManager.getEmail(getApplicationContext());
        if (!TextUtils.isEmpty(email)) {
            email = email.trim();
        }
        if (TextUtils.isEmpty(email)) {
            Log.w(TAG, "Cannot fetch profile fallback address; no email stored in session.");
            return;
        }

        Log.d(TAG, "Fetching fallback address from profile. userId=" + userId + ", email=" + email);
        userService.fetchUserProfile(userId, email, new UserService.UserProfileCallback() {
            @Override
            public void onSuccess(@NonNull UserProfile profile) {
                String profileAddress = profile.getAddress();
                if (!TextUtils.isEmpty(profileAddress)) {
                    Log.d(TAG, "Profile returned address for navigation fallback: " + profileAddress);
                    applyFallbackNavigationAddress(profileAddress);
                } else {
                    Log.w(TAG, "Profile response did not include an address field.");
                }
            }

            @Override
            public void onError(@NonNull String errorMessage) {
                Log.e(TAG, "Failed to load profile fallback address: " + errorMessage);
                // Keep silent; the static fallback string will remain in place if provided.
            }
        });
    }

    @Nullable
    private String selectFallbackAddress(@Nullable String... values) {
        if (values == null) {
            return null;
        }
        for (String value : values) {
            if (!TextUtils.isEmpty(value)) {
                return value.trim();
            }
        }
        return null;
    }
}
