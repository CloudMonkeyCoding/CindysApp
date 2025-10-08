package com.example.deliveryapp.network;

import android.os.Handler;
import android.os.Looper;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.example.deliveryapp.BuildConfig;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.HttpUrl;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

/**
 * Resolves delivery user metadata from the Cindy's Bakeshop PHP APIs.
 */
public class UserService {

    public interface UserIdCallback {
        void onSuccess(int userId);

        void onError(@NonNull String errorMessage);
    }

    private final OkHttpClient okHttpClient;
    private final Handler mainHandler;
    private final ServerConnectionManager connectionManager;

    public UserService() {
        connectionManager = ServerConnectionManager.getInstance();
        okHttpClient = connectionManager.getHttpClient();
        mainHandler = new Handler(Looper.getMainLooper());
    }

    public void fetchUserIdByEmail(@NonNull String email, @NonNull UserIdCallback callback) {
        HttpUrl endpoint = connectionManager.buildUrl(BuildConfig.USER_PROFILE_PATH);
        if (endpoint == null) {
            postError(callback, "User profile endpoint URL could not be resolved.");
            return;
        }

        HttpUrl requestUrl = endpoint.newBuilder()
                .addQueryParameter("action", BuildConfig.USER_PROFILE_ACTION)
                .addQueryParameter("email", email)
                .build();

        Request request = new Request.Builder()
                .url(requestUrl)
                .get()
                .build();

        okHttpClient.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(@NonNull Call call, @NonNull IOException e) {
                postError(callback, e.getMessage() != null ? e.getMessage() : "Unable to reach user service.");
            }

            @Override
            public void onResponse(@NonNull Call call, @NonNull Response response) {
                try (Response res = response) {
                    String bodyString = res.body() != null ? res.body().string() : "";
                    if (!res.isSuccessful()) {
                        String message = extractErrorMessage(bodyString);
                        if (message == null) {
                            message = "HTTP " + res.code();
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
                    postError(callback, e.getMessage() != null ? e.getMessage() : "Unable to read response.");
                }
            }
        });
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
}

