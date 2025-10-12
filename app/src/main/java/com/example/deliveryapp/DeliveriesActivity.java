package com.example.deliveryapp;

import android.Manifest;
import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.text.TextUtils;
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

import com.example.deliveryapp.AppConfig;
import com.example.deliveryapp.network.OrderInfo;
import com.example.deliveryapp.network.OrderService;
import com.example.deliveryapp.network.UserService;
import com.example.deliveryapp.tracking.OrderTrackingManager;

import java.text.NumberFormat;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class DeliveriesActivity extends BottomNavActivity {

    private static final int REQUEST_LOCATION_PERMISSIONS = 1001;

    private View refreshView;
    private View logoutView;
    private ProgressBar deliveriesLoading;
    private TextView deliveriesMessage;
    private LinearLayout deliveriesListContainer;

    private final OrderService orderService = new OrderService();
    private final UserService userService = new UserService();
    private OrderTrackingManager orderTrackingManager;
    @Nullable
    private Integer resolvedUserId;
    @Nullable
    private String resolvedEmail;
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
        showMessage(getString(R.string.deliveries_loading));
        showLoading(true);
        resolveStaffIdentity(false);
    }

    private void initViews() {
        deliveriesLoading = findViewById(R.id.deliveriesLoading);
        deliveriesMessage = findViewById(R.id.deliveriesMessage);
        deliveriesListContainer = findViewById(R.id.deliveriesListContainer);
        refreshView = findViewById(R.id.deliveriesRefresh);
        logoutView = findViewById(R.id.deliveriesLogout);

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

        if (logoutView != null) {
            logoutView.setOnClickListener(v -> handleLogout());
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

        String sessionEmail = SessionManager.getEmail(this);
        if (!TextUtils.isEmpty(sessionEmail)) {
            resolvedEmail = sessionEmail;
        }

        Integer sessionUserId = SessionManager.getUserId(this);
        if (sessionUserId != null) {
            resolvedUserId = sessionUserId;
            loadOrders(userRequestedRefresh);
            return;
        }

        if (AppConfig.DEFAULT_STAFF_USER_ID > 0) {
            resolvedUserId = AppConfig.DEFAULT_STAFF_USER_ID;
            if (TextUtils.isEmpty(resolvedEmail)) {
                resolvedEmail = null;
            }
            loadOrders(userRequestedRefresh);
            return;
        }

        String email = sessionEmail;
        if (TextUtils.isEmpty(email)) {
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

        final String lookupEmail = email;
        userService.fetchUserIdByEmail(lookupEmail, new UserService.UserIdCallback() {
            @Override
            public void onSuccess(int userId) {
                isResolvingUserId = false;
                resolvedUserId = userId;
                resolvedEmail = lookupEmail;
                SessionManager.storeSession(getApplicationContext(), lookupEmail, userId);
                loadOrders(userRequestedRefresh);
            }

            @Override
            public void onError(@NonNull String errorMessage) {
                isResolvingUserId = false;
                resolvedUserId = null;
                resolvedEmail = null;
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

        int currentUserId = resolvedUserId != null ? resolvedUserId : 0;
        orderService.fetchUnfinishedOrders(currentUserId, resolvedEmail, new OrderService.OrderFetchCallback() {
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

            Button viewInMapsButton = itemView.findViewById(R.id.viewInMapsButton);
            if (viewInMapsButton != null) {
                if (!TextUtils.isEmpty(address)) {
                    viewInMapsButton.setVisibility(View.VISIBLE);
                    viewInMapsButton.setOnClickListener(v -> openAddressInMaps(address));
                } else {
                    viewInMapsButton.setVisibility(View.GONE);
                    viewInMapsButton.setOnClickListener(null);
                }
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

    @Override
    protected void onStart() {
        super.onStart();
        if (orderTrackingManager == null) {
            orderTrackingManager = OrderTrackingManager.getInstance(getApplicationContext());
        }
        orderTrackingManager.ensureLocationTracking();
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
            return;
        }

        if (result.isTrackingStarted()) {
            showToast(getString(R.string.deliveries_tracking_started, order.getOrderId()));
        } else {
            showToast(getString(R.string.deliveries_tracking_unavailable, order.getOrderId()));
        }
    }

    private void openAddressInMaps(@NonNull String address) {
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
                showToast(getString(R.string.map_navigation_app_missing));
            }
        }
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
            return;
        }

        if (pendingPermissionOrder != null) {
            showToast(getString(R.string.deliveries_tracking_permission_denied, pendingPermissionOrder.getOrderId()));
            pendingPermissionOrder = null;
        } else {
            showToast(getString(R.string.deliveries_tracking_permission_denied_generic));
        }
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

    private void handleLogout() {
        SessionManager.clearSession(getApplicationContext());
        resolvedUserId = null;
        resolvedEmail = null;
        isLoading = false;
        isResolvingUserId = false;
        Toast.makeText(this, getString(R.string.deliveries_logout_toast), Toast.LENGTH_SHORT).show();

        Intent intent = new Intent(this, LoginActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }
}
