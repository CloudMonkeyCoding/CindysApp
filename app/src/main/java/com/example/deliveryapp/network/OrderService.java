package com.example.deliveryapp.network;

import android.net.Uri;
import android.os.Handler;
import android.text.TextUtils;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.example.deliveryapp.AppConfig;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

/**
 * Loads delivery orders from the Cindy's Bakeshop order API and filters the entries that still
 * require driver attention.
 */
public class OrderService {

    private static final String TAG = "OrderService";
    private static final int MAX_BODY_LOG_LENGTH = 500;

    public interface OrderFetchCallback {
        void onSuccess(@NonNull List<OrderInfo> orders, @Nullable String serverMessage);

        void onError(@NonNull String errorMessage);
    }

    private final ServerConnectionManager connectionManager;
    private final Handler mainHandler;

    public OrderService() {
        connectionManager = ServerConnectionManager.getInstance();
        mainHandler = connectionManager.getMainThreadHandler();
    }

    public void fetchUnfinishedOrders(int userId, @Nullable String email, @NonNull OrderFetchCallback callback) {
        if (userId <= 0 && TextUtils.isEmpty(email)) {
            Log.w(TAG, "Aborting delivery fetch; missing both user ID and email.");
            callback.onError("Missing or invalid staff identity.");
            return;
        }

        URL base = connectionManager.buildUrl(AppConfig.ORDER_LIST_PATH);
        if (base == null) {
            Log.e(TAG, "Order endpoint URL could not be resolved. Base URL is null.");
            callback.onError("Order endpoint URL could not be resolved.");
            return;
        }

        URL requestUrl = buildOrderListUrl(base, userId, email);
        if (requestUrl == null) {
            Log.e(TAG, "Unable to build order request URL.");
            callback.onError("Unable to build the order request URL.");
            return;
        }

        Log.d(TAG, "Fetching unfinished orders. userId=" + userId + ", email=" + email +
                ", url=" + requestUrl);

        connectionManager.getNetworkExecutor().execute(() -> {
            HttpURLConnection connection = null;
            try {
                connection = (HttpURLConnection) requestUrl.openConnection();
                connection.setConnectTimeout(10_000);
                connection.setReadTimeout(10_000);
                connection.setRequestMethod("GET");
                connection.connect();

                int statusCode = connection.getResponseCode();
                String bodyString = readResponseBody(connection, statusCode);
                Log.d(TAG, "Order response received. status=" + statusCode +
                        ", bodyLength=" + (bodyString != null ? bodyString.length() : 0));
                if (statusCode >= 400) {
                    Log.w(TAG, "Order request returned HTTP error. status=" + statusCode +
                            ", body=" + abbreviateForLog(bodyString));
                }
                if (statusCode < 200 || statusCode >= 300) {
                    postError(callback, buildHttpErrorMessage(statusCode, bodyString));
                    return;
                }

                ResponseBundle bundle = parseOrders(bodyString);
                if (bundle.errorMessage != null) {
                    Log.w(TAG, "Server indicated an error while parsing orders: " + bundle.errorMessage);
                    postError(callback, bundle.errorMessage);
                    return;
                }

                List<OrderInfo> unfinished = filterUnfinished(bundle.orders);
                Log.d(TAG, "Parsed " + (bundle.orders != null ? bundle.orders.size() : 0) +
                        " orders; unfinished count=" + unfinished.size() +
                        ", serverMessage=" + bundle.serverMessage);
                postSuccess(callback, unfinished, bundle.serverMessage);
            } catch (IOException e) {
                String message = e.getMessage();
                Log.e(TAG, "IOException while loading orders: " + message, e);
                postError(callback, message != null ? message : "Unable to load deliveries.");
            } finally {
                if (connection != null) {
                    connection.disconnect();
                }
            }
        });
    }

    @NonNull
    private String abbreviateForLog(@Nullable String value) {
        if (value == null) {
            return "null";
        }
        String trimmed = value.trim();
        if (trimmed.length() <= MAX_BODY_LOG_LENGTH) {
            return trimmed;
        }
        return trimmed.substring(0, MAX_BODY_LOG_LENGTH) + "…";
    }

    private void postSuccess(@NonNull OrderFetchCallback callback, @NonNull List<OrderInfo> orders, @Nullable String message) {
        mainHandler.post(() -> callback.onSuccess(orders, message));
    }

    private void postError(@NonNull OrderFetchCallback callback, @NonNull String message) {
        mainHandler.post(() -> callback.onError(message));
    }

    @Nullable
    private URL buildOrderListUrl(@NonNull URL base, int userId, @Nullable String email) {
        Uri.Builder builder = Uri.parse(base.toString())
                .buildUpon()
                .appendQueryParameter("action", AppConfig.ORDER_LIST_ACTION);
        if (userId > 0) {
            builder.appendQueryParameter("user_id", String.valueOf(userId));
        }
        if (!TextUtils.isEmpty(email)) {
            builder.appendQueryParameter("email", email.trim());
        }
        Uri uri = builder.build();
        try {
            return new URL(uri.toString());
        } catch (MalformedURLException e) {
            return null;
        }
    }

    @NonNull
    private ResponseBundle parseOrders(@NonNull String bodyString) {
        ResponseBundle bundle = new ResponseBundle();
        if (bodyString.trim().isEmpty()) {
            bundle.orders = Collections.emptyList();
            return bundle;
        }

        Object parsed = parseJsonValue(bodyString);
        if (parsed instanceof JSONArray) {
            bundle.orders = parseOrdersArray((JSONArray) parsed);
            return bundle;
        }
        if (parsed instanceof JSONObject) {
            JSONObject object = (JSONObject) parsed;
            JSONArray ordersArray = object.optJSONArray("orders");
            if (ordersArray == null) {
                Object data = object.opt("data");
                if (data instanceof JSONArray) {
                    ordersArray = (JSONArray) data;
                }
            }
            if (ordersArray != null) {
                bundle.orders = parseOrdersArray(ordersArray);
            } else {
                bundle.orders = Collections.emptyList();
            }
            bundle.serverMessage = extractMessage(object);
            String error = extractError(object);
            if (error != null && (bundle.orders == null || bundle.orders.isEmpty())) {
                bundle.errorMessage = error;
            }
            return bundle;
        }

        bundle.errorMessage = "Server returned an unexpected response.";
        return bundle;
    }

    @Nullable
    private Object parseJsonValue(@NonNull String value) {
        String trimmed = value.trim();
        if (trimmed.isEmpty()) {
            return new JSONArray();
        }
        try {
            return new JSONArray(trimmed);
        } catch (JSONException ignored) {
            // Try object next
        }
        try {
            return new JSONObject(trimmed);
        } catch (JSONException ignored) {
            return null;
        }
    }

    @NonNull
    private List<OrderInfo> parseOrdersArray(@NonNull JSONArray array) {
        List<OrderInfo> orders = new ArrayList<>();
        for (int i = 0; i < array.length(); i++) {
            JSONObject object = array.optJSONObject(i);
            OrderInfo info = parseOrder(object);
            if (info != null) {
                orders.add(info);
            }
        }
        return orders;
    }

    @Nullable
    private OrderInfo parseOrder(@Nullable JSONObject object) {
        if (object == null) {
            return null;
        }
        int orderId = optInt(object, -1, "Order_ID", "order_id", "id");
        int userId = optInt(object, 0, "User_ID", "user_id");
        String status = optString(object, "Status", "status");
        String orderDate = optString(object, "Order_Date", "order_date", "date");
        String fulfillmentType = optString(object, "Fulfillment_Type", "fulfillment_type");
        String source = optString(object, "Source", "source");
        int itemCount = optInt(object, 0, "Item_Count", "item_count", "items");
        double totalAmount = optDouble(object, 0.0, "Total_Amount", "total_amount", "Total");
        String itemSummary = optString(object, "Item_Summary", "item_summary");
        if (TextUtils.isEmpty(itemSummary)) {
            itemSummary = null;
        }
        String imageUrl = optString(object, "Image_Url", "image_url", "Image_Path", "image");
        String deliveryAddress = parseDeliveryAddress(object);
        if (orderId <= 0) {
            return null;
        }
        return new OrderInfo(
                orderId,
                userId,
                status,
                orderDate,
                fulfillmentType,
                source,
                itemCount,
                totalAmount,
                itemSummary,
                imageUrl,
                deliveryAddress
        );
    }

    @Nullable
    private String parseDeliveryAddress(@NonNull JSONObject object) {
        String direct = optString(
                object,
                "Delivery_Address",
                "delivery_address",
                "Address",
                "address",
                "DeliveryAddress",
                "deliveryAddress"
        );
        if (!TextUtils.isEmpty(direct)) {
            return direct;
        }

        List<String> parts = new ArrayList<>();
        collectAddressParts(parts, object);

        JSONObject nestedDelivery = object.optJSONObject("delivery_address");
        if (nestedDelivery != null) {
            collectAddressParts(parts, nestedDelivery);
        }

        JSONObject shipping = object.optJSONObject("shipping_address");
        if (shipping != null) {
            collectAddressParts(parts, shipping);
        }

        if (parts.isEmpty()) {
            return null;
        }
        return TextUtils.join(", ", parts);
    }

    private void collectAddressParts(@NonNull List<String> parts, @NonNull JSONObject source) {
        addAddressPart(parts, optString(source, "Address_Line1", "address_line1", "Address1", "address1", "Street", "street"));
        addAddressPart(parts, optString(source, "Address_Line2", "address_line2", "Address2", "address2", "Barangay", "barangay"));
        addAddressPart(parts, optString(source, "City", "city", "Municipality", "municipality"));
        addAddressPart(parts, optString(source, "Province", "province", "State", "state"));
        addAddressPart(parts, optString(source, "Postal_Code", "postal_code", "Zip_Code", "zip_code", "Zip", "zip"));
        addAddressPart(parts, optString(source, "Country", "country"));
    }

    private void addAddressPart(@NonNull List<String> parts, @Nullable String value) {
        if (TextUtils.isEmpty(value)) {
            return;
        }
        String trimmed = value.trim();
        if (trimmed.isEmpty()) {
            return;
        }
        if (!parts.contains(trimmed)) {
            parts.add(trimmed);
        }
    }

    @NonNull
    private List<OrderInfo> filterUnfinished(@Nullable List<OrderInfo> orders) {
        if (orders == null || orders.isEmpty()) {
            return Collections.emptyList();
        }
        List<OrderInfo> filtered = new ArrayList<>();
        for (OrderInfo order : orders) {
            if (order == null) {
                continue;
            }
            if (!isFinished(order.getStatus())) {
                filtered.add(order);
            }
        }
        return filtered;
    }

    private boolean isFinished(@Nullable String status) {
        if (status == null) {
            return false;
        }
        String normalized = status.trim().toLowerCase(Locale.US);
        if (normalized.isEmpty()) {
            return false;
        }
        Set<String> finishedStatuses = FINISHED_STATUSES;
        if (finishedStatuses.contains(normalized)) {
            return true;
        }
        // Treat generic success phrases as finished as well.
        return normalized.contains("delivered") || normalized.contains("completed");
    }

    @Nullable
    private String extractMessage(@NonNull JSONObject object) {
        String[] keys = new String[]{"message", "info", "detail"};
        for (String key : keys) {
            if (object.has(key) && !object.isNull(key)) {
                String value = object.optString(key, "").trim();
                if (!value.isEmpty()) {
                    return value;
                }
            }
        }
        return null;
    }

    @Nullable
    private String extractError(@NonNull JSONObject object) {
        String[] keys = new String[]{"error", "reason"};
        for (String key : keys) {
            if (object.has(key) && !object.isNull(key)) {
                String value = object.optString(key, "").trim();
                if (!value.isEmpty()) {
                    return value;
                }
            }
        }
        boolean successFlag = object.optBoolean("success", true);
        if (!successFlag) {
            return object.optString("message", "Request failed");
        }
        return null;
    }

    @NonNull
    private String buildHttpErrorMessage(int statusCode, @NonNull String body) {
        if (body.isEmpty()) {
            return "HTTP " + statusCode;
        }
        if (looksLikeHtml(body)) {
            return "HTTP " + statusCode;
        }
        return "HTTP " + statusCode + ": " + body;
    }

    private boolean looksLikeHtml(@NonNull String value) {
        String lower = value.toLowerCase(Locale.US);
        return lower.contains("<html") || lower.contains("<!doctype html");
    }

    @NonNull
    private String readResponseBody(@NonNull HttpURLConnection connection, int statusCode) throws IOException {
        InputStream stream = statusCode >= 400 ? connection.getErrorStream() : connection.getInputStream();
        if (stream == null) {
            return "";
        }
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(stream, StandardCharsets.UTF_8))) {
            StringBuilder builder = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                builder.append(line);
            }
            return builder.toString();
        }
    }

    private int optInt(@NonNull JSONObject object, int fallback, @NonNull String... keys) {
        for (String key : keys) {
            if (object.has(key) && !object.isNull(key)) {
                Object value = object.opt(key);
                if (value instanceof Number) {
                    return ((Number) value).intValue();
                }
                if (value instanceof String) {
                    try {
                        return Integer.parseInt(((String) value).trim());
                    } catch (NumberFormatException ignored) {
                        // Fall through
                    }
                }
            }
        }
        return fallback;
    }

    private double optDouble(@NonNull JSONObject object, double fallback, @NonNull String... keys) {
        for (String key : keys) {
            if (object.has(key) && !object.isNull(key)) {
                Object value = object.opt(key);
                if (value instanceof Number) {
                    return ((Number) value).doubleValue();
                }
                if (value instanceof String) {
                    try {
                        return Double.parseDouble(((String) value).trim());
                    } catch (NumberFormatException ignored) {
                        // Continue
                    }
                }
            }
        }
        return fallback;
    }

    @Nullable
    private String optString(@NonNull JSONObject object, @NonNull String... keys) {
        for (String key : keys) {
            if (object.has(key) && !object.isNull(key)) {
                String value = object.optString(key, "");
                if (!TextUtils.isEmpty(value)) {
                    return value;
                }
            }
        }
        return null;
    }

    private static class ResponseBundle {
        @Nullable
        List<OrderInfo> orders;
        @Nullable
        String serverMessage;
        @Nullable
        String errorMessage;
    }

    private static final Set<String> FINISHED_STATUSES;

    static {
        Set<String> statuses = new HashSet<>();
        statuses.add("delivered");
        statuses.add("completed");
        statuses.add("complete");
        statuses.add("cancelled");
        statuses.add("canceled");
        statuses.add("refunded");
        FINISHED_STATUSES = Collections.unmodifiableSet(statuses);
    }
}
