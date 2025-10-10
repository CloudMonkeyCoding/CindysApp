package com.example.deliveryapp.network;

import android.os.Handler;

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
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;

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

    private final Handler mainHandler;
    private final ServerConnectionManager connectionManager;

    public ShiftService() {
        connectionManager = ServerConnectionManager.getInstance();
        mainHandler = connectionManager.getMainThreadHandler();
    }

    public void fetchUpcomingShift(int userId, @NonNull ShiftFetchCallback callback) {
        if (userId <= 0) {
            callback.onError("Missing or invalid staff user ID.");
            return;
        }

        URL endpoint = connectionManager.buildUrl(AppConfig.SHIFT_SCHEDULE_PATH);
        if (endpoint == null) {
            callback.onError("Shift endpoint URL could not be resolved.");
            return;
        }

        String fetchAction = AppConfig.SHIFT_FETCH_ACTION != null && !AppConfig.SHIFT_FETCH_ACTION.trim().isEmpty()
                ? AppConfig.SHIFT_FETCH_ACTION
                : "get_shift_schedules";

        Map<String, String> formFields = new LinkedHashMap<>();
        formFields.put("action", fetchAction);
        formFields.put("user_id", String.valueOf(userId));

        executeRequest(endpoint, formFields, new JsonResponseHandler() {
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

        URL endpointUrl = connectionManager.buildUrl(AppConfig.SHIFT_ACTION_PATH);
        if (endpointUrl == null) {
            callback.onError("Shift action endpoint URL could not be resolved.");
            return;
        }

        Map<String, String> formFields = new LinkedHashMap<>();
        formFields.put("action", AppConfig.SHIFT_START_ACTION);
        formFields.put("shift_id", String.valueOf(shiftId));

        executeRequest(endpointUrl, formFields, new JsonResponseHandler() {
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

    private void executeRequest(@NonNull URL url, @NonNull Map<String, String> formFields, @NonNull JsonResponseHandler handler) {
        connectionManager.getNetworkExecutor().execute(() -> {
            HttpURLConnection connection = null;
            try {
                connection = (HttpURLConnection) url.openConnection();
                connection.setConnectTimeout(10_000);
                connection.setReadTimeout(10_000);
                connection.setRequestMethod("POST");
                connection.setDoInput(true);
                connection.setDoOutput(true);
                connection.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");

                String payload = buildFormPayload(formFields);
                byte[] payloadBytes = payload.getBytes(StandardCharsets.UTF_8);
                connection.setFixedLengthStreamingMode(payloadBytes.length);

                try (OutputStream outputStream = connection.getOutputStream()) {
                    outputStream.write(payloadBytes);
                    outputStream.flush();
                }

                int statusCode = connection.getResponseCode();
                String bodyString = readResponseBody(connection, statusCode);

                if (statusCode < 200 || statusCode >= 300) {
                    final String errorMessage = buildHttpErrorMessage(statusCode, bodyString);
                    postToMain(() -> handler.onError(errorMessage));
                    return;
                }

                JSONObject body = normalizeJsonPayload(bodyString);
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
                postToMain(() -> handler.onError(e.getMessage() != null ? e.getMessage() : "Network request failed."));
            } finally {
                if (connection != null) {
                    connection.disconnect();
                }
            }
        });
    }

    private void postToMain(@NonNull Runnable runnable) {
        mainHandler.post(runnable);
    }

    @Nullable
    private JSONObject normalizeJsonPayload(@NonNull String bodyString) {
        Object parsed = parseJsonValue(bodyString);
        if (parsed instanceof JSONObject) {
            return (JSONObject) parsed;
        }
        if (parsed instanceof JSONArray) {
            JSONArray array = (JSONArray) parsed;
            JSONObject wrapper = new JSONObject();
            try {
                wrapper.put("shifts", array);
            } catch (JSONException ignored) {
                return null;
            }
            return wrapper;
        }
        if (bodyString.trim().isEmpty()) {
            return new JSONObject();
        }
        return null;
    }

    @Nullable
    private Object parseJsonValue(@NonNull String bodyString) {
        String trimmed = bodyString.trim();
        if (trimmed.isEmpty()) {
            return new JSONObject();
        }
        try {
            return new JSONObject(trimmed);
        } catch (JSONException ignored) {
            // Try parsing as an array next.
        }
        try {
            return new JSONArray(trimmed);
        } catch (JSONException ignored) {
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

        if (body.has("shifts") || body.has("shift") || body.has("data")) {
            return true;
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

        Object parsed = parseJsonValue(rawBody);
        if (parsed instanceof JSONObject) {
            JSONObject json = (JSONObject) parsed;
            String message = extractMessage(json);
            if (message != null && !message.isEmpty()) {
                return message;
            }
        } else if (parsed instanceof JSONArray) {
            JSONArray array = (JSONArray) parsed;
            if (array.length() > 0) {
                JSONObject first = array.optJSONObject(0);
                if (first != null) {
                    String message = extractMessage(first);
                    if (message != null && !message.isEmpty()) {
                        return message;
                    }
                }
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

    @NonNull
    private String buildFormPayload(@NonNull Map<String, String> formFields) {
        StringBuilder builder = new StringBuilder();
        for (Map.Entry<String, String> entry : formFields.entrySet()) {
            if (builder.length() > 0) {
                builder.append('&');
            }
            builder.append(encode(entry.getKey()));
            builder.append('=');
            builder.append(encode(entry.getValue()));
        }
        return builder.toString();
    }

    @NonNull
    private String encode(@NonNull String value) {
        return java.net.URLEncoder.encode(value, StandardCharsets.UTF_8);
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

    private interface JsonResponseHandler {
        void onSuccess(@NonNull JSONObject body);

        void onError(@NonNull String errorMessage);
    }
}
