package com.example.deliveryapp.network;

import android.net.Uri;
import android.os.Handler;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.example.deliveryapp.AppConfig;

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

    private final Handler mainHandler;
    private final ServerConnectionManager connectionManager;

    public UserService() {
        connectionManager = ServerConnectionManager.getInstance();
        mainHandler = connectionManager.getMainThreadHandler();
    }

    public void fetchUserIdByEmail(@NonNull String email, @NonNull UserIdCallback callback) {
        URL endpoint = connectionManager.buildUrl(AppConfig.USER_PROFILE_PATH);
        if (endpoint == null) {
            postError(callback, "User profile endpoint URL could not be resolved.");
            return;
        }

        URL requestUrl = buildProfileUrl(endpoint, email);
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

                int userId = extractUserId(body);
                if (userId > 0) {
                    postSuccess(callback, userId);
                    return;
                }

                String message = extractErrorFromBody(body);
                postError(callback, message != null ? message : "User profile did not include an ID.");
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
    private URL buildProfileUrl(@NonNull URL endpoint, @NonNull String email) {
        Uri uri = Uri.parse(endpoint.toString())
                .buildUpon()
                .appendQueryParameter("action", AppConfig.USER_PROFILE_ACTION)
                .appendQueryParameter("email", email)
                .build();
        try {
            return new URL(uri.toString());
        } catch (MalformedURLException e) {
            return null;
        }
    }

    private void postSuccess(@NonNull UserIdCallback callback, int userId) {
        mainHandler.post(() -> callback.onSuccess(userId));
    }

    private void postError(@NonNull UserIdCallback callback, @NonNull String message) {
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

    private int extractUserId(@NonNull JSONObject body) {
        if (body.has("user_id")) {
            return body.optInt("user_id", -1);
        }
        if (body.has("User_ID")) {
            return body.optInt("User_ID", -1);
        }
        JSONObject nestedUser = body.optJSONObject("user");
        if (nestedUser != null) {
            int nestedId = nestedUser.optInt("user_id", -1);
            if (nestedId <= 0) {
                nestedId = nestedUser.optInt("User_ID", -1);
            }
            return nestedId;
        }
        return -1;
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
}
