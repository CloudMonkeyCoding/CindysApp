package com.example.deliveryapp.tracking;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.os.Looper;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;

import com.example.deliveryapp.network.OrderInfo;

import java.lang.ref.WeakReference;

/**
 * Stores the driver's active order and coordinates ongoing GPS tracking for it.
 */
public final class OrderTrackingManager {

    private static final String TAG = "OrderTrackingManager";

    private static final String PREFS_NAME = "order_tracking";
    private static final String KEY_ACTIVE_ORDER_ID = "active_order_id";
    private static final String KEY_ACTIVE_ORDER_USER_ID = "active_order_user_id";
    private static final String KEY_ACTIVE_ORDER_STATUS = "active_order_status";
    private static final String KEY_ACTIVE_ORDER_SUMMARY = "active_order_summary";
    private static final String KEY_ACTIVE_ORDER_ADDRESS = "active_order_address";
    private static final String KEY_LAST_LATITUDE = "last_latitude";
    private static final String KEY_LAST_LONGITUDE = "last_longitude";
    private static final String KEY_LAST_PROVIDER = "last_provider";
    private static final String KEY_LAST_LOCATION_TIME = "last_location_time";

    private static final long MIN_UPDATE_INTERVAL_MS = 15_000L;
    private static final float MIN_UPDATE_DISTANCE_METERS = 25f;

    private static volatile OrderTrackingManager instance;

    private final Context applicationContext;
    private final SharedPreferences preferences;
    @Nullable
    private final LocationManager locationManager;
    @Nullable
    private TrackingLocationListener locationListener;

    private OrderTrackingManager(@NonNull Context context) {
        applicationContext = context.getApplicationContext();
        preferences = applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        locationManager = (LocationManager) applicationContext.getSystemService(Context.LOCATION_SERVICE);
    }

    @NonNull
    public static OrderTrackingManager getInstance(@NonNull Context context) {
        if (instance == null) {
            synchronized (OrderTrackingManager.class) {
                if (instance == null) {
                    instance = new OrderTrackingManager(context);
                }
            }
        }
        return instance;
    }

    @NonNull
    public ActivationResult activateOrder(@NonNull OrderInfo order) {
        storeActiveOrder(order);

        boolean permissionGranted = hasLocationPermission();
        boolean trackingStarted = false;
        if (permissionGranted) {
            trackingStarted = startLocationTracking();
        }

        return new ActivationResult(true, permissionGranted, trackingStarted);
    }

    private void storeActiveOrder(@NonNull OrderInfo order) {
        preferences.edit()
                .putInt(KEY_ACTIVE_ORDER_ID, order.getOrderId())
                .putInt(KEY_ACTIVE_ORDER_USER_ID, order.getUserId())
                .putString(KEY_ACTIVE_ORDER_STATUS, safeString(order.getStatus()))
                .putString(KEY_ACTIVE_ORDER_SUMMARY, safeString(order.getItemSummary()))
                .putString(KEY_ACTIVE_ORDER_ADDRESS, safeString(order.getDeliveryAddress()))
                .apply();
    }

    private boolean hasLocationPermission() {
        int fine = ContextCompat.checkSelfPermission(applicationContext, Manifest.permission.ACCESS_FINE_LOCATION);
        int coarse = ContextCompat.checkSelfPermission(applicationContext, Manifest.permission.ACCESS_COARSE_LOCATION);
        return fine == PackageManager.PERMISSION_GRANTED || coarse == PackageManager.PERMISSION_GRANTED;
    }

    @SuppressLint("MissingPermission")
    private boolean startLocationTracking() {
        if (locationManager == null) {
            Log.w(TAG, "LocationManager not available; cannot start tracking.");
            return false;
        }

        if (locationListener == null) {
            locationListener = new TrackingLocationListener(this);
        }

        try {
            locationManager.removeUpdates(locationListener);
        } catch (SecurityException exception) {
            Log.w(TAG, "Unable to remove previous location updates", exception);
        }

        boolean gpsRequested = requestUpdatesForProvider(LocationManager.GPS_PROVIDER);
        if (gpsRequested) {
            return true;
        }
        return requestUpdatesForProvider(LocationManager.NETWORK_PROVIDER);
    }

    @SuppressLint("MissingPermission")
    private boolean requestUpdatesForProvider(@Nullable String provider) {
        if (provider == null || locationManager == null) {
            return false;
        }
        try {
            locationManager.requestLocationUpdates(
                    provider,
                    MIN_UPDATE_INTERVAL_MS,
                    MIN_UPDATE_DISTANCE_METERS,
                    getOrCreateListener(),
                    Looper.getMainLooper()
            );
            return true;
        } catch (SecurityException | IllegalArgumentException exception) {
            Log.w(TAG, "Failed to request updates for provider " + provider, exception);
            return false;
        }
    }

    @SuppressLint("MissingPermission")
    private TrackingLocationListener getOrCreateListener() {
        if (locationListener == null) {
            locationListener = new TrackingLocationListener(this);
        }
        return locationListener;
    }

    private void handleLocationUpdate(@NonNull Location location) {
        preferences.edit()
                .putString(KEY_LAST_LATITUDE, String.valueOf(location.getLatitude()))
                .putString(KEY_LAST_LONGITUDE, String.valueOf(location.getLongitude()))
                .putString(KEY_LAST_PROVIDER, safeString(location.getProvider()))
                .putLong(KEY_LAST_LOCATION_TIME, location.getTime())
                .apply();
    }

    @NonNull
    private static String safeString(@Nullable String value) {
        return value != null ? value : "";
    }

    public static final class ActivationResult {
        private final boolean orderStored;
        private final boolean locationPermissionGranted;
        private final boolean trackingStarted;

        private ActivationResult(boolean orderStored, boolean locationPermissionGranted, boolean trackingStarted) {
            this.orderStored = orderStored;
            this.locationPermissionGranted = locationPermissionGranted;
            this.trackingStarted = trackingStarted;
        }

        public boolean isOrderStored() {
            return orderStored;
        }

        public boolean isLocationPermissionGranted() {
            return locationPermissionGranted;
        }

        public boolean isTrackingStarted() {
            return trackingStarted;
        }
    }

    private static final class TrackingLocationListener implements LocationListener {

        private final WeakReference<OrderTrackingManager> managerRef;

        TrackingLocationListener(@NonNull OrderTrackingManager manager) {
            managerRef = new WeakReference<>(manager);
        }

        @Override
        public void onLocationChanged(@NonNull Location location) {
            OrderTrackingManager manager = managerRef.get();
            if (manager != null) {
                manager.handleLocationUpdate(location);
            }
        }

        @Override
        public void onProviderEnabled(@NonNull String provider) {
            // No-op
        }

        @Override
        public void onProviderDisabled(@NonNull String provider) {
            // No-op
        }

        @Override
        public void onStatusChanged(String provider, int status, Bundle extras) {
            // Deprecated callback required for older API levels
        }
    }
}
