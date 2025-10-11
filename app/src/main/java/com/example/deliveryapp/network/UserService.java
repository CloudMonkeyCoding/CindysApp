package com.example.deliveryapp.network;

import android.net.Uri;
import android.os.Handler;
import android.text.TextUtils;

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

/**
 * Resolves delivery user metadata from the Cindy's Bakeshop PHP APIs.
 */
public class UserService {

    public interface UserIdCallback {
        void onSuccess(int userId);

        void onError(@NonNull String errorMessage);
    }

    public interface UserProfileCallback {
        void onSuccess(@NonNull UserProfile profile);

        void onError(@NonNull String errorMessage);
    }

    private final Handler mainHandler;
    private final ServerConnectionManager connectionManager;

    public UserService() {
        connectionManager = ServerConnectionManager.getInstance();
        mainHandler = connectionManager.getMainThreadHandler();
    }

    public void fetchUserIdByEmail(@NonNull String email, @NonNull UserIdCallback callback) {
        fetchUserProfile(null, email, new UserProfileCallback() {
            @Override
            public void onSuccess(@NonNull UserProfile profile) {
                if (profile.getUserId() > 0) {
                    postSuccess(callback, profile.getUserId());
                } else {
                    postError(callback, "User profile did not include an ID.");
                }
            }

            @Override
            public void onError(@NonNull String errorMessage) {
                postError(callback, errorMessage);
            }
        });
    }

    public void fetchUserProfile(@Nullable Integer userId, @Nullable String email, @NonNull UserProfileCallback callback) {
        if ((userId == null || userId <= 0) && TextUtils.isEmpty(email)) {
            postError(callback, "User lookup requires a user ID or email address.");
            return;
        }

        URL endpoint = connectionManager.buildUrl(AppConfig.USER_PROFILE_PATH);
        if (endpoint == null) {
            postError(callback, "User profile endpoint URL could not be resolved.");
            return;
        }

        URL requestUrl = buildProfileUrl(endpoint, userId, email);
        if (requestUrl == null) {
            postError(callback, "Failed to build user profile request URL.");
            return;
        }

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
                if (statusCode < 200 || statusCode >= 300) {
                    String message = extractErrorMessage(bodyString);
                    if (message == null) {
                        message = "HTTP " + statusCode;
                    }
                    postError(callback, message);
                    return;
                }

                JSONObject body = parseJson(bodyString);
                if (body == null) {
                    postError(callback, "Server returned an unexpected response.");
                    return;
                }

                UserProfile profile = extractProfile(body, userId, email);
                if (profile != null) {
                    postSuccess(callback, profile);
                    return;
                }

                String message = extractErrorFromBody(body);
                postError(callback, message != null ? message : "User profile did not include any details.");
            } catch (IOException e) {
                postError(callback, e.getMessage() != null ? e.getMessage() : "Unable to reach user service.");
            } finally {
                if (connection != null) {
                    connection.disconnect();
                }
            }
        });
    }

    @Nullable
    private URL buildProfileUrl(@NonNull URL endpoint, @Nullable Integer userId, @Nullable String email) {
        Uri.Builder builder = Uri.parse(endpoint.toString())
                .buildUpon()
                .appendQueryParameter("action", AppConfig.USER_PROFILE_ACTION);
        if (userId != null && userId > 0) {
            builder.appendQueryParameter("user_id", String.valueOf(userId));
        } else if (!TextUtils.isEmpty(email)) {
            builder.appendQueryParameter("email", email);
        }
        Uri uri = builder.build();
        try {
            return new URL(uri.toString());
        } catch (MalformedURLException e) {
            return null;
        }
    }

    private void postSuccess(@NonNull UserIdCallback callback, int userId) {
        mainHandler.post(() -> callback.onSuccess(userId));
    }

    private void postSuccess(@NonNull UserProfileCallback callback, @NonNull UserProfile profile) {
        mainHandler.post(() -> callback.onSuccess(profile));
    }

    private void postError(@NonNull UserIdCallback callback, @NonNull String message) {
        mainHandler.post(() -> callback.onError(message));
    }

    private void postError(@NonNull UserProfileCallback callback, @NonNull String message) {
        mainHandler.post(() -> callback.onError(message));
    }

    @Nullable
    private JSONObject parseJson(@NonNull String value) {
        if (value.trim().isEmpty()) {
            return new JSONObject();
        }
        try {
            return new JSONObject(value);
        } catch (JSONException e) {
            return null;
        }
    }

    @Nullable
    private String extractErrorFromBody(@NonNull JSONObject body) {
        if (body.has("error")) {
            String value = body.optString("error", "").trim();
            if (!value.isEmpty()) {
                return value;
            }
        }
        if (body.has("message")) {
            String value = body.optString("message", "").trim();
            if (!value.isEmpty()) {
                return value;
            }
        }
        return null;
    }

    @Nullable
    private String extractErrorMessage(@NonNull String rawBody) {
        JSONObject json = parseJson(rawBody);
        if (json != null) {
            return extractErrorFromBody(json);
        }
        return rawBody.isEmpty() ? null : rawBody;
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

    @Nullable
    private UserProfile extractProfile(@NonNull JSONObject body, @Nullable Integer requestedUserId, @Nullable String requestedEmail) {
        JSONObject candidate = locateProfileObject(body);
        int userId = optInt(candidate, requestedUserId != null ? requestedUserId : -1,
                "user_id", "User_ID", "id", "ID", "staff_id", "Staff_ID", "Store_Staff_ID");
        if (userId <= 0) {
            userId = optInt(body, requestedUserId != null ? requestedUserId : -1,
                    "user_id", "User_ID", "id", "ID", "staff_id", "Staff_ID", "Store_Staff_ID");
        }
        String email = optString(candidate, requestedEmail,
                "email", "Email", "user_email", "User_Email");
        if (TextUtils.isEmpty(email)) {
            email = optString(body, requestedEmail,
                    "email", "Email", "user_email", "User_Email");
        }
        String name = optString(candidate, null,
                "name", "Name", "full_name", "Full_Name", "username", "Username", "display_name", "Display_Name");
        if (TextUtils.isEmpty(name)) {
            name = optString(body, null,
                    "name", "Name", "full_name", "Full_Name", "username", "Username", "display_name", "Display_Name");
        }
        String address = optString(candidate, null,
                "address", "Address", "store_address", "Store_Address", "vendor_address", "Vendor_Address",
                "business_address", "Business_Address", "location", "Location", "street", "Street",
                "full_address", "Full_Address", "delivery_address", "Delivery_Address");
        if (TextUtils.isEmpty(address)) {
            address = optString(body, null,
                    "address", "Address", "store_address", "Store_Address", "vendor_address", "Vendor_Address",
                    "business_address", "Business_Address", "location", "Location", "street", "Street",
                    "full_address", "Full_Address", "delivery_address", "Delivery_Address");
        }

        if (userId <= 0 && requestedUserId != null && requestedUserId > 0) {
            userId = requestedUserId;
        }
        if (TextUtils.isEmpty(email) && !TextUtils.isEmpty(requestedEmail)) {
            email = requestedEmail;
        }

        if (userId <= 0 && TextUtils.isEmpty(email) && TextUtils.isEmpty(name) && TextUtils.isEmpty(address)) {
            return null;
        }

        return new UserProfile(
                userId,
                TextUtils.isEmpty(email) ? null : email.trim(),
                TextUtils.isEmpty(name) ? null : name.trim(),
                TextUtils.isEmpty(address) ? null : address.trim()
        );
    }

    @Nullable
    private JSONObject locateProfileObject(@NonNull JSONObject body) {
        if (looksLikeProfile(body)) {
            return body;
        }

        String[] objectKeys = {"user", "profile", "data", "result", "payload", "users"};
        for (String key : objectKeys) {
            Object value = body.opt(key);
            if (value instanceof JSONObject) {
                JSONObject candidate = (JSONObject) value;
                if (looksLikeProfile(candidate)) {
                    return candidate;
                }
            } else if (value instanceof JSONArray) {
                JSONArray array = (JSONArray) value;
                for (int i = 0; i < array.length(); i++) {
                    JSONObject candidate = array.optJSONObject(i);
                    if (looksLikeProfile(candidate)) {
                        return candidate;
                    }
                }
            }
        }

        JSONObject nestedUser = body.optJSONObject("user");
        if (nestedUser != null) {
            return nestedUser;
        }
        return body;
    }

    private boolean looksLikeProfile(@Nullable JSONObject object) {
        if (object == null) {
            return false;
        }
        return object.has("user_id")
                || object.has("User_ID")
                || object.has("email")
                || object.has("Email")
                || object.has("address")
                || object.has("Address");
    }

    private int optInt(@Nullable JSONObject object, int fallback, @NonNull String... keys) {
        if (object == null) {
            return fallback;
        }
        for (String key : keys) {
            if (object.has(key)) {
                int value = object.optInt(key, fallback);
                if (value > 0) {
                    return value;
                }
            }
        }
        return fallback;
    }

    @Nullable
    private String optString(@Nullable JSONObject object, @Nullable String fallback, @NonNull String... keys) {
        if (object == null) {
            return fallback;
        }
        for (String key : keys) {
            if (object.has(key)) {
                String value = object.optString(key, "").trim();
                if (!value.isEmpty()) {
                    return value;
                }
            }
        }
        return fallback;
    }
}
