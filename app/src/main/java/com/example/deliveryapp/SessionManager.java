package com.example.deliveryapp;

import android.content.Context;
import android.content.SharedPreferences;
import android.text.TextUtils;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

/**
 * Persists the authenticated driver's session details so other screens can
 * resolve the staff identifier without relying on Firebase SDKs.
 */
public final class SessionManager {

    private static final String PREFS_NAME = "delivery_session";
    private static final String KEY_EMAIL = "email";
    private static final String KEY_USER_ID = "user_id";

    private SessionManager() {
        // Utility class
    }

    public static void storeSession(@NonNull Context context, @NonNull String email, int userId) {
        SharedPreferences prefs = getPrefs(context);
        prefs.edit()
                .putString(KEY_EMAIL, email.trim())
                .putInt(KEY_USER_ID, Math.max(userId, 0))
                .apply();
    }

    public static void updateUserId(@NonNull Context context, int userId) {
        SharedPreferences prefs = getPrefs(context);
        if (userId > 0) {
            prefs.edit().putInt(KEY_USER_ID, userId).apply();
        }
    }

    public static void clearSession(@NonNull Context context) {
        getPrefs(context).edit().clear().apply();
    }

    public static boolean hasActiveSession(@NonNull Context context) {
        return getUserId(context) != null && !TextUtils.isEmpty(getEmail(context));
    }

    @Nullable
    public static Integer getUserId(@NonNull Context context) {
        SharedPreferences prefs = getPrefs(context);
        int stored = prefs.getInt(KEY_USER_ID, -1);
        return stored > 0 ? stored : null;
    }

    @Nullable
    public static String getEmail(@NonNull Context context) {
        SharedPreferences prefs = getPrefs(context);
        String value = prefs.getString(KEY_EMAIL, null);
        if (TextUtils.isEmpty(value)) {
            return null;
        }
        return value;
    }

    private static SharedPreferences getPrefs(@NonNull Context context) {
        return context.getApplicationContext().getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
    }
}
