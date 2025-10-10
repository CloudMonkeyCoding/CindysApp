package com.example.deliveryapp;

import android.Manifest;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.app.ActivityCompat;
import androidx.fragment.app.FragmentManager;

import com.example.deliveryapp.network.OrderInfo;
import com.example.deliveryapp.network.OrderService;
import com.example.deliveryapp.network.UserService;
import com.example.deliveryapp.tracking.OrderTrackingManager;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.MapsInitializer;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

import java.text.NumberFormat;
import java.text.DateFormat;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class DeliveriesActivity extends BottomNavActivity implements OnMapReadyCallback, OrderTrackingManager.LocationUpdateListener {

    private static final String TAG = "DeliveriesActivity";
    private static boolean hasConfiguredMapsSdk;

    private static final int REQUEST_LOCATION_PERMISSIONS = 1001;
    private static final float MAP_DEFAULT_ZOOM = 15f;

    private View refreshView;
    private ProgressBar deliveriesLoading;
    private TextView deliveriesMessage;
    private LinearLayout deliveriesListContainer;
    private TextView mapStatusView;

    @Nullable
    private GoogleMap googleMap;
    @Nullable
    private Marker driverMarker;
    @Nullable
    private OrderTrackingManager.TrackedLocation lastKnownLocation;
    private boolean hasCenteredMap;
    private boolean isMapReady;
    private boolean isMissingMapsApiKey;
    private boolean mapInitializationFailed;

    private final OrderService orderService = new OrderService();
    private final UserService userService = new UserService();
    private OrderTrackingManager orderTrackingManager;
    @Nullable
    private Integer resolvedUserId;
    @Nullable
    private OrderInfo pendingPermissionOrder;
    private boolean isResolvingUserId;
    private boolean isLoading;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_deliveries);
        setupBottomNavigation(R.id.menu_deliveries);

        initViews();
        orderTrackingManager = OrderTrackingManager.getInstance(getApplicationContext());
        lastKnownLocation = orderTrackingManager.getLastKnownLocation();
        initMap();
        showMessage(getString(R.string.deliveries_loading));
        showLoading(true);
        resolveStaffIdentity(false);
    }

    private void initViews() {
        deliveriesLoading = findViewById(R.id.deliveriesLoading);
        deliveriesMessage = findViewById(R.id.deliveriesMessage);
        deliveriesListContainer = findViewById(R.id.deliveriesListContainer);
        refreshView = findViewById(R.id.deliveriesRefresh);
        mapStatusView = findViewById(R.id.deliveriesMapStatus);

        if (mapStatusView != null) {
            mapStatusView.setText(R.string.deliveries_map_loading);
            mapStatusView.setVisibility(View.VISIBLE);
        }

        if (refreshView != null) {
            refreshView.setOnClickListener(v -> {
                if (isResolvingUserId) {
                    showToast(getString(R.string.deliveries_resolving_user_id_toast));
                    return;
                }
                if (isLoading) {
                    showToast(getString(R.string.deliveries_loading_in_progress));
                    return;
                }
                if (resolvedUserId != null) {
                    loadOrders(true);
                } else {
                    resolveStaffIdentity(true);
                }
            });
        }
    }

    private void initMap() {
        isMissingMapsApiKey = TextUtils.isEmpty(AppConfig.GOOGLE_MAPS_API_KEY);

        if (isMissingMapsApiKey) {
            if (mapStatusView != null) {
                mapStatusView.setText(R.string.deliveries_map_missing_api_key);
                mapStatusView.setVisibility(View.VISIBLE);
            }
            return;
        }

        if (!configureMapsSdkIfNeeded()) {
            if (mapStatusView != null) {
                mapStatusView.setText(R.string.deliveries_map_initialization_failed);
                mapStatusView.setVisibility(View.VISIBLE);
            }
            return;
        }

        FragmentManager fragmentManager = getSupportFragmentManager();
        SupportMapFragment mapFragment = (SupportMapFragment) fragmentManager.findFragmentById(R.id.deliveriesMapFragment);
        if (mapFragment == null) {
            mapFragment = SupportMapFragment.newInstance();
            try {
                fragmentManager.beginTransaction()
                        .replace(R.id.deliveriesMapFragment, mapFragment)
                        .commitNow();
            } catch (IllegalStateException exception) {
                fragmentManager.beginTransaction()
                        .replace(R.id.deliveriesMapFragment, mapFragment)
                        .commitAllowingStateLoss();
                fragmentManager.executePendingTransactions();
            }
        }
        if (mapFragment != null) {
            mapFragment.getMapAsync(this);
        } else if (mapStatusView != null) {
            mapStatusView.setText(R.string.deliveries_map_unavailable);
            mapStatusView.setVisibility(View.VISIBLE);
        }
    }

    private void resolveStaffIdentity(boolean userRequestedRefresh) {
        if (isResolvingUserId) {
            if (userRequestedRefresh) {
                showToast(getString(R.string.deliveries_resolving_user_id_toast));
            }
            return;
        }

        if (resolvedUserId != null) {
            loadOrders(userRequestedRefresh);
            return;
        }

        if (AppConfig.DEFAULT_STAFF_USER_ID > 0) {
            resolvedUserId = AppConfig.DEFAULT_STAFF_USER_ID;
            loadOrders(userRequestedRefresh);
            return;
        }

        FirebaseUser firebaseUser = FirebaseAuth.getInstance().getCurrentUser();
        if (firebaseUser == null || TextUtils.isEmpty(firebaseUser.getEmail())) {
            showLoading(false);
            showMessage(getString(R.string.deliveries_missing_user));
            if (userRequestedRefresh) {
                showToast(getString(R.string.deliveries_missing_user));
            }
            return;
        }

        isResolvingUserId = true;
        showLoading(true);
        showMessage(getString(R.string.deliveries_resolving_user_id));

        String email = firebaseUser.getEmail();
        userService.fetchUserIdByEmail(email, new UserService.UserIdCallback() {
            @Override
            public void onSuccess(int userId) {
                isResolvingUserId = false;
                resolvedUserId = userId;
                loadOrders(userRequestedRefresh);
            }

            @Override
            public void onError(@NonNull String errorMessage) {
                isResolvingUserId = false;
                resolvedUserId = null;
                showLoading(false);
                String message = !TextUtils.isEmpty(errorMessage)
                        ? errorMessage
                        : getString(R.string.deliveries_error);
                showMessage(message);
                if (userRequestedRefresh) {
                    showToast(message);
                }
            }
        });
    }

    private void loadOrders(boolean userRequestedRefresh) {
        if (resolvedUserId == null) {
            resolveStaffIdentity(userRequestedRefresh);
            return;
        }

        if (isLoading) {
            if (userRequestedRefresh) {
                showToast(getString(R.string.deliveries_loading_in_progress));
            }
            return;
        }

        isLoading = true;
        showLoading(true);
        showMessage(getString(R.string.deliveries_loading));

        orderService.fetchUnfinishedOrders(resolvedUserId, new OrderService.OrderFetchCallback() {
            @Override
            public void onSuccess(@NonNull List<OrderInfo> orders, @Nullable String serverMessage) {
                isLoading = false;
                showLoading(false);
                if (orders.isEmpty()) {
                    String message = !TextUtils.isEmpty(serverMessage)
                            ? serverMessage
                            : getString(R.string.deliveries_empty);
                    showMessage(message);
                } else {
                    hideMessage();
                    renderOrders(orders);
                    if (!TextUtils.isEmpty(serverMessage) && userRequestedRefresh) {
                        showToast(serverMessage);
                    }
                }
            }

            @Override
            public void onError(@NonNull String errorMessage) {
                isLoading = false;
                showLoading(false);
                String message = !TextUtils.isEmpty(errorMessage)
                        ? errorMessage
                        : getString(R.string.deliveries_error);
                showMessage(message);
                if (userRequestedRefresh) {
                    showToast(message);
                }
            }
        });
    }

    @Override
    protected void onStart() {
        super.onStart();
        if (orderTrackingManager == null) {
            orderTrackingManager = OrderTrackingManager.getInstance(getApplicationContext());
        }
        orderTrackingManager.addLocationUpdateListener(this);
        if (lastKnownLocation == null) {
            lastKnownLocation = orderTrackingManager.getLastKnownLocation();
        }
        orderTrackingManager.ensureLocationTracking();
        updateMapUiState();
    }

    @Override
    protected void onResume() {
        super.onResume();
        updateMapUiState();
    }

    @Override
    protected void onStop() {
        if (orderTrackingManager != null) {
            orderTrackingManager.removeLocationUpdateListener(this);
        }
        super.onStop();
    }

    @Override
    public void onMapReady(@NonNull GoogleMap map) {
        googleMap = map;
        isMapReady = true;
        googleMap.getUiSettings().setMapToolbarEnabled(false);
        googleMap.getUiSettings().setCompassEnabled(true);
        googleMap.getUiSettings().setZoomControlsEnabled(false);
        googleMap.getUiSettings().setMyLocationButtonEnabled(true);
        updateMapUiState();
    }

    @Override
    public void onLocationUpdated(@NonNull OrderTrackingManager.TrackedLocation location) {
        lastKnownLocation = location;
        runOnUiThread(this::updateMapUiState);
    }

    private void updateMapUiState() {
        if (mapStatusView == null) {
            return;
        }
        if (isMissingMapsApiKey) {
            mapStatusView.setText(R.string.deliveries_map_missing_api_key);
            mapStatusView.setVisibility(View.VISIBLE);
            clearDriverMarker();
            setMyLocationLayerEnabled(false);
            return;
        }
        if (mapInitializationFailed) {
            mapStatusView.setText(R.string.deliveries_map_initialization_failed);
            mapStatusView.setVisibility(View.VISIBLE);
            clearDriverMarker();
            setMyLocationLayerEnabled(false);
            return;
        }
        if (!isMapReady || googleMap == null) {
            mapStatusView.setText(R.string.deliveries_map_loading);
            mapStatusView.setVisibility(View.VISIBLE);
            return;
        }
        if (orderTrackingManager == null) {
            mapStatusView.setText(R.string.deliveries_map_unavailable);
            mapStatusView.setVisibility(View.VISIBLE);
            clearDriverMarker();
            setMyLocationLayerEnabled(false);
            return;
        }
        boolean hasPermission = orderTrackingManager.canAccessLocation();
        if (!hasPermission) {
            setMyLocationLayerEnabled(false);
            clearDriverMarker();
            hasCenteredMap = false;
            mapStatusView.setText(R.string.deliveries_map_permission_required);
            mapStatusView.setVisibility(View.VISIBLE);
            return;
        }
        setMyLocationLayerEnabled(true);
        OrderTrackingManager.TrackedLocation location = lastKnownLocation;
        if (location == null) {
            location = orderTrackingManager.getLastKnownLocation();
            lastKnownLocation = location;
        }
        if (location == null) {
            clearDriverMarker();
            hasCenteredMap = false;
            mapStatusView.setText(R.string.deliveries_map_waiting_location);
            mapStatusView.setVisibility(View.VISIBLE);
            return;
        }
        updateDriverMarker(location);
        mapStatusView.setText(formatLastUpdated(location.getTimestampMillis()));
        mapStatusView.setVisibility(View.VISIBLE);
    }

    private boolean configureMapsSdkIfNeeded() {
        if (hasConfiguredMapsSdk) {
            mapInitializationFailed = false;
            return true;
        }

        try {
            String mapsApiKey = AppConfig.GOOGLE_MAPS_API_KEY;
            if (!TextUtils.isEmpty(mapsApiKey)) {
                MapsInitializer.setApiKey(mapsApiKey);
            }
            MapsInitializer.initialize(getApplicationContext(), MapsInitializer.Renderer.LATEST, renderer -> {});
            hasConfiguredMapsSdk = true;
            mapInitializationFailed = false;
            return true;
        } catch (Exception exception) {
            mapInitializationFailed = true;
            Log.e(TAG, "Failed to initialize Google Maps", exception);
            return false;
        }
    }

    private void updateDriverMarker(@NonNull OrderTrackingManager.TrackedLocation location) {
        if (googleMap == null) {
            return;
        }
        LatLng position = new LatLng(location.getLatitude(), location.getLongitude());
        if (driverMarker == null) {
            driverMarker = googleMap.addMarker(new MarkerOptions()
                    .position(position)
                    .title(getString(R.string.deliveries_map_driver_marker_title)));
        } else {
            driverMarker.setPosition(position);
        }
        if (!hasCenteredMap) {
            googleMap.moveCamera(CameraUpdateFactory.newLatLngZoom(position, MAP_DEFAULT_ZOOM));
            hasCenteredMap = true;
        }
    }

    private void clearDriverMarker() {
        if (driverMarker != null) {
            driverMarker.remove();
            driverMarker = null;
        }
    }

    private void setMyLocationLayerEnabled(boolean enabled) {
        if (googleMap == null) {
            return;
        }
        try {
            googleMap.setMyLocationEnabled(enabled);
        } catch (SecurityException exception) {
            if (enabled) {
                googleMap.setMyLocationEnabled(false);
            }
        }
    }

    @NonNull
    private String formatLastUpdated(long timestampMillis) {
        if (timestampMillis <= 0L) {
            return getString(R.string.deliveries_map_waiting_location);
        }
        DateFormat timeFormat = DateFormat.getTimeInstance(DateFormat.SHORT, Locale.getDefault());
        return getString(R.string.deliveries_map_last_updated, timeFormat.format(new Date(timestampMillis)));
    }

    private void renderOrders(@NonNull List<OrderInfo> orders) {
        LayoutInflater inflater = LayoutInflater.from(this);
        deliveriesListContainer.removeAllViews();

        NumberFormat currencyFormat = NumberFormat.getCurrencyInstance(Locale.getDefault());

        for (OrderInfo order : orders) {
            View itemView = inflater.inflate(R.layout.item_delivery_order, deliveriesListContainer, false);
            TextView orderNumber = itemView.findViewById(R.id.orderNumber);
            TextView orderStatus = itemView.findViewById(R.id.orderStatus);
            TextView orderSummary = itemView.findViewById(R.id.orderSummary);
            TextView orderAddress = itemView.findViewById(R.id.orderAddress);
            TextView orderMeta = itemView.findViewById(R.id.orderMeta);
            TextView orderTotal = itemView.findViewById(R.id.orderTotal);
            Button deliverButton = itemView.findViewById(R.id.deliverButton);

            orderNumber.setText(getString(R.string.deliveries_order_number, order.getOrderId()));

            String statusValue = order.getStatus();
            if (TextUtils.isEmpty(statusValue)) {
                statusValue = getString(R.string.deliveries_order_status_unknown);
            }
            orderStatus.setText(getString(R.string.deliveries_order_status, statusValue));

            String summary = order.getItemSummary();
            if (TextUtils.isEmpty(summary)) {
                int count = Math.max(order.getItemCount(), 0);
                summary = getResources().getQuantityString(R.plurals.deliveries_item_count, count == 0 ? 0 : count, count);
            }
            orderSummary.setText(summary);

            String address = order.getDeliveryAddress();
            if (!TextUtils.isEmpty(address)) {
                orderAddress.setText(getString(R.string.deliveries_order_address, address));
                orderAddress.setVisibility(View.VISIBLE);
            } else {
                orderAddress.setVisibility(View.GONE);
            }

            String meta = buildMetaLine(order);
            if (TextUtils.isEmpty(meta)) {
                meta = getString(R.string.deliveries_order_meta_fallback);
            }
            orderMeta.setText(meta);

            double totalAmount = Math.max(order.getTotalAmount(), 0.0);
            String formattedTotal = currencyFormat.format(totalAmount);
            orderTotal.setText(getString(R.string.deliveries_order_total, formattedTotal));

            if (deliverButton != null) {
                deliverButton.setOnClickListener(v -> onDeliverClicked(order));
            }

            deliveriesListContainer.addView(itemView);
        }
        deliveriesListContainer.setVisibility(View.VISIBLE);
    }

    private void onDeliverClicked(@NonNull OrderInfo order) {
        if (orderTrackingManager == null) {
            orderTrackingManager = OrderTrackingManager.getInstance(getApplicationContext());
        }
        OrderTrackingManager.ActivationResult result = orderTrackingManager.activateOrder(order);

        if (!result.isLocationPermissionGranted()) {
            pendingPermissionOrder = order;
            requestLocationPermissions();
            showToast(getString(R.string.deliveries_tracking_permission_request, order.getOrderId()));
            updateMapUiState();
            return;
        }

        if (result.isTrackingStarted()) {
            showToast(getString(R.string.deliveries_tracking_started, order.getOrderId()));
        } else {
            showToast(getString(R.string.deliveries_tracking_unavailable, order.getOrderId()));
        }
        updateMapUiState();
    }

    private void requestLocationPermissions() {
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
            boolean trackingStarted = orderTrackingManager.ensureLocationTracking();
            if (pendingPermissionOrder != null) {
                int orderId = pendingPermissionOrder.getOrderId();
                pendingPermissionOrder = null;
                if (trackingStarted) {
                    showToast(getString(R.string.deliveries_tracking_permission_granted, orderId));
                } else {
                    showToast(getString(R.string.deliveries_tracking_unavailable, orderId));
                }
            } else {
                if (trackingStarted) {
                    showToast(getString(R.string.deliveries_tracking_permission_granted_generic));
                } else {
                    showToast(getString(R.string.deliveries_tracking_permission_granted_unavailable));
                }
            }
            updateMapUiState();
            return;
        }

        if (pendingPermissionOrder != null) {
            showToast(getString(R.string.deliveries_tracking_permission_denied, pendingPermissionOrder.getOrderId()));
            pendingPermissionOrder = null;
        } else {
            showToast(getString(R.string.deliveries_tracking_permission_denied_generic));
        }
        updateMapUiState();
    }

    @NonNull
    private String buildMetaLine(@NonNull OrderInfo order) {
        List<String> parts = new ArrayList<>();
        if (!TextUtils.isEmpty(order.getFulfillmentType())) {
            parts.add(order.getFulfillmentType());
        }
        if (order.getItemCount() > 0) {
            parts.add(getResources().getQuantityString(R.plurals.deliveries_item_count, order.getItemCount(), order.getItemCount()));
        }
        String formattedDate = formatDate(order.getOrderDate());
        if (!TextUtils.isEmpty(formattedDate)) {
            parts.add(formattedDate);
        }
        if (!TextUtils.isEmpty(order.getSource())) {
            parts.add(order.getSource());
        }
        return TextUtils.join(" • ", parts);
    }

    @Nullable
    private String formatDate(@Nullable String rawValue) {
        if (TextUtils.isEmpty(rawValue)) {
            return null;
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            try {
                LocalDate date = LocalDate.parse(rawValue);
                DateTimeFormatter formatter = DateTimeFormatter.ofPattern("MMM d, yyyy", Locale.getDefault());
                return date.format(formatter);
            } catch (DateTimeParseException ignored) {
                // Fall back to raw string
            }
        }
        return rawValue;
    }

    private void showLoading(boolean show) {
        if (deliveriesLoading != null) {
            deliveriesLoading.setVisibility(show ? View.VISIBLE : View.GONE);
        }
        if (show) {
            deliveriesListContainer.setVisibility(View.GONE);
        }
    }

    private void showMessage(@NonNull String message) {
        if (deliveriesMessage != null) {
            deliveriesMessage.setText(message);
            deliveriesMessage.setVisibility(View.VISIBLE);
        }
        deliveriesListContainer.setVisibility(View.GONE);
    }

    private void hideMessage() {
        if (deliveriesMessage != null) {
            deliveriesMessage.setVisibility(View.GONE);
        }
    }

    private void showToast(@NonNull String message) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
    }
}
