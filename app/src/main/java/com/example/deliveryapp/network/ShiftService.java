package com.example.deliveryapp.network;

import android.os.Handler;
import android.os.Looper;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.example.deliveryapp.BuildConfig;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.util.Locale;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.FormBody;
import okhttp3.HttpUrl;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

/**
 * Handles loading and updating shift information via the Cindy's Bakeshop PHP endpoints.
 */
public class ShiftService {

    public interface ShiftFetchCallback {
        void onSuccess(@Nullable ShiftInfo shiftInfo, @Nullable String serverMessage);

        void onError(@NonNull String errorMessage);
    }

    public interface ShiftActionCallback {
        void onSuccess(@Nullable ShiftInfo updatedShift, @Nullable String serverMessage);

        void onError(@NonNull String errorMessage);
    }

    private final OkHttpClient okHttpClient;
    private final Handler mainHandler;
    private final ServerConnectionManager connectionManager;

    public ShiftService() {
        connectionManager = ServerConnectionManager.getInstance();
        okHttpClient = connectionManager.getHttpClient();
        mainHandler = new Handler(Looper.getMainLooper());
    }

    public void fetchUpcomingShift(int userId, @NonNull ShiftFetchCallback callback) {
        if (userId <= 0) {
            callback.onError("Missing or invalid staff user ID.");
            return;
        }

        HttpUrl baseUrl = connectionManager.buildUrl(BuildConfig.SHIFT_SCHEDULE_PATH);
        if (baseUrl == null) {
            callback.onError("Shift endpoint URL could not be resolved.");
            return;
        }

        RequestBody requestBody = new FormBody.Builder()
                .add("action", BuildConfig.SHIFT_FETCH_ACTION)
                .add("user_id", String.valueOf(userId))
                .build();

        Request request = new Request.Builder()
                .url(baseUrl)
                .post(requestBody)
                .build();

        executeRequest(request, new JsonResponseHandler() {
            @Override
            public void onSuccess(@NonNull JSONObject body) {
                boolean success = isSuccess(body);
                String message = extractMessage(body);
                if (!success) {
                    callback.onError(message != null ? message : "Failed to load shift data.");
                    return;
                }

                ShiftInfo shift = extractShift(body);
                callback.onSuccess(shift, message);
            }

            @Override
            public void onError(@NonNull String errorMessage) {
                callback.onError(errorMessage);
            }
        });
    }

    public void startShift(int shiftId, @NonNull ShiftActionCallback callback) {
        if (shiftId <= 0) {
            callback.onError("Invalid shift identifier.");
            return;
        }

        HttpUrl endpointUrl = connectionManager.buildUrl(BuildConfig.SHIFT_ACTION_PATH);
        if (endpointUrl == null) {
            callback.onError("Shift action endpoint URL could not be resolved.");
            return;
        }

        RequestBody requestBody = new FormBody.Builder()
                .add("action", BuildConfig.SHIFT_START_ACTION)
                .add("shift_id", String.valueOf(shiftId))
                .build();

        Request request = new Request.Builder()
                .url(endpointUrl)
                .post(requestBody)
                .build();

        executeRequest(request, new JsonResponseHandler() {
            @Override
            public void onSuccess(@NonNull JSONObject body) {
                boolean success = isSuccess(body);
                String message = extractMessage(body);
                if (!success) {
                    callback.onError(message != null ? message : "Shift could not be started.");
                    return;
                }

                ShiftInfo shift = extractShift(body);
                callback.onSuccess(shift, message);
            }

            @Override
            public void onError(@NonNull String errorMessage) {
                callback.onError(errorMessage);
            }
        });
    }

    private void executeRequest(@NonNull Request request, @NonNull JsonResponseHandler handler) {
        okHttpClient.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(@NonNull Call call, @NonNull IOException e) {
                postToMain(() -> handler.onError(e.getMessage() != null ? e.getMessage() : "Network request failed."));
            }

            @Override
            public void onResponse(@NonNull Call call, @NonNull Response response) {
                try (Response res = response) {
                    String bodyString = res.body() != null ? res.body().string() : "";
                    if (!res.isSuccessful()) {
                        final String errorMessage = buildHttpErrorMessage(res.code(), bodyString);
                        postToMain(() -> handler.onError(errorMessage));
                        return;
                    }

                    JSONObject body = parseJson(bodyString);
                    if (body == null) {
                        final String finalMessage = looksLikeHtml(bodyString)
                                ? htmlFallbackMessage()
                                : "Server returned an unexpected response.";
                        postToMain(() -> handler.onError(finalMessage));
                        return;
                    }

                    JSONObject finalBody = body;
                    postToMain(() -> handler.onSuccess(finalBody));
                } catch (IOException e) {
                    postToMain(() -> handler.onError(e.getMessage() != null ? e.getMessage() : "Unable to read response."));
                }
            }
        });
    }

    private void postToMain(@NonNull Runnable runnable) {
        mainHandler.post(runnable);
    }

    @Nullable
    private JSONObject parseJson(@NonNull String bodyString) {
        if (bodyString.trim().isEmpty()) {
            return new JSONObject();
        }
        try {
            return new JSONObject(bodyString);
        } catch (JSONException e) {
            return null;
        }
    }

    private boolean isSuccess(@NonNull JSONObject body) {
        if (body.length() == 0) {
            return true;
        }

        if (body.has("success")) {
            Object successObj = body.opt("success");
            if (successObj instanceof Boolean) {
                return (Boolean) successObj;
            }
            if (successObj instanceof Number) {
                return ((Number) successObj).intValue() != 0;
            }
            if (successObj instanceof String) {
                String value = ((String) successObj).trim().toLowerCase(Locale.US);
                return value.equals("true") || value.equals("success") || value.equals("1");
            }
        }

        String status = body.optString("status", "").trim().toLowerCase(Locale.US);
        return status.equals("success") || status.equals("ok");
    }

    @Nullable
    private String extractMessage(@NonNull JSONObject body) {
        String[] keys = new String[]{"message", "info", "detail", "error", "reason"};
        for (String key : keys) {
            if (body.has(key) && !body.isNull(key)) {
                String value = body.optString(key, "").trim();
                if (!value.isEmpty()) {
                    return value;
                }
            }
        }
        return null;
    }

    @Nullable
    private ShiftInfo extractShift(@NonNull JSONObject body) {
        JSONObject candidate = body.optJSONObject("shift");
        if (candidate != null) {
            ShiftInfo parsed = parseShift(candidate);
            if (parsed != null) {
                return parsed;
            }
        }

        JSONArray shiftsArray = body.optJSONArray("shifts");
        if (shiftsArray != null && shiftsArray.length() > 0) {
            JSONObject first = shiftsArray.optJSONObject(0);
            ShiftInfo parsed = parseShift(first);
            if (parsed != null) {
                return parsed;
            }
        }

        Object dataObject = body.opt("data");
        if (dataObject instanceof JSONObject) {
            ShiftInfo parsed = parseShift((JSONObject) dataObject);
            if (parsed != null) {
                return parsed;
            }
        } else if (dataObject instanceof JSONArray) {
            JSONArray dataArray = (JSONArray) dataObject;
            if (dataArray.length() > 0) {
                JSONObject first = dataArray.optJSONObject(0);
                ShiftInfo parsed = parseShift(first);
                if (parsed != null) {
                    return parsed;
                }
            }
        }

        ShiftInfo direct = parseShift(body);
        if (direct != null && direct.getId() > 0) {
            return direct;
        }

        return null;
    }

    @Nullable
    private ShiftInfo parseShift(@Nullable JSONObject jsonObject) {
        if (jsonObject == null) {
            return null;
        }

        int shiftId = optInt(jsonObject, -1, "Shift_ID", "shift_id", "id");
        int userId = optInt(jsonObject, 0, "User_ID", "user_id", "staff_id", "Store_Staff_ID");
        String shiftDate = optString(jsonObject, "Shift_Date", "shift_date", "date");
        String scheduledStart = optString(jsonObject, "Scheduled_Start", "scheduled_start", "start_time", "start");
        String scheduledEnd = optString(jsonObject, "Scheduled_End", "scheduled_end", "end_time", "end");
        String actualStart = optString(jsonObject, "Actual_Start", "actual_start", "clock_in", "start_actual");
        String actualEnd = optString(jsonObject, "Actual_End", "actual_end", "clock_out", "end_actual");
        String status = optString(jsonObject, "Status", "status", "Shift_Status", "shift_status");
        String notes = optString(jsonObject, "Notes", "notes", "comment", "remarks");
        String location = firstNonEmpty(
                optString(jsonObject, "Location", "location", "Branch", "branch", "Store", "store"),
                notes
        );
        String staffName = optString(jsonObject, "Name", "name", "Staff_Name", "staff_name", "employee_name");

        return new ShiftInfo(
                shiftId,
                userId,
                staffName,
                shiftDate,
                scheduledStart,
                scheduledEnd,
                actualStart,
                actualEnd,
                status,
                notes,
                location
        );
    }

    private int optInt(@NonNull JSONObject jsonObject, int fallback, @NonNull String... keys) {
        for (String key : keys) {
            if (jsonObject.has(key) && !jsonObject.isNull(key)) {
                Object value = jsonObject.opt(key);
                if (value instanceof Number) {
                    return ((Number) value).intValue();
                }
                if (value instanceof String) {
                    try {
                        return Integer.parseInt(((String) value).trim());
                    } catch (NumberFormatException ignored) {
                        // Ignore and continue to next key
                    }
                }
            }
        }
        return fallback;
    }

    @Nullable
    private String optString(@NonNull JSONObject jsonObject, @NonNull String... keys) {
        for (String key : keys) {
            if (jsonObject.has(key) && !jsonObject.isNull(key)) {
                String value = jsonObject.optString(key, null);
                if (value != null) {
                    value = value.trim();
                    if (!value.isEmpty()) {
                        return value;
                    }
                }
            }
        }
        return null;
    }

    @Nullable
    private String firstNonEmpty(@Nullable String primary, @Nullable String fallback) {
        if (primary != null && !primary.trim().isEmpty()) {
            return primary.trim();
        }
        if (fallback != null && !fallback.trim().isEmpty()) {
            return fallback.trim();
        }
        return null;
    }

    private String buildHttpErrorMessage(int code, @NonNull String rawBody) {
        if (looksLikeHtml(rawBody)) {
            if (code == 404) {
                return "Shift endpoint was not found (HTTP 404). Confirm the SHIFT_SCHEDULE_PATH build property points to the correct PHP script.";
            }
            return htmlFallbackMessage();
        }

        JSONObject json = parseJson(rawBody);
        if (json != null) {
            String message = extractMessage(json);
            if (message != null && !message.isEmpty()) {
                return message;
            }
        }

        if (!rawBody.trim().isEmpty()) {
            return "HTTP " + code + ": " + rawBody.trim();
        }
        return "HTTP " + code;
    }

    private boolean looksLikeHtml(@NonNull String value) {
        String trimmed = value.trim();
        if (trimmed.isEmpty()) {
            return false;
        }
        String lower = trimmed.toLowerCase(Locale.US);
        return lower.startsWith("<!doctype") || lower.startsWith("<html") || lower.contains("<body");
    }

    private String htmlFallbackMessage() {
        return "Shift service returned HTML instead of JSON. Verify the configured PHP endpoint returns JSON as described in the Cindy's Bakeshop shift_functions.php utilities.";
    }

    private interface JsonResponseHandler {
        void onSuccess(@NonNull JSONObject body);

        void onError(@NonNull String errorMessage);
    }
}
