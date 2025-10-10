package com.example.deliveryapp;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

/**
 * Provides centralized access to BuildConfig values with sensible fallbacks so that
 * the application can still compile and run even if Gradle configuration has not
 * generated the BuildConfig class yet (for example during IDE sync failures).
 */
public final class AppConfig {

    private static final String BUILD_CONFIG_CLASS = "com.example.deliveryapp.BuildConfig";

    public static final boolean DEBUG = resolveBoolean("DEBUG", false);

    public static final String API_BASE_URL = resolveString(
            "API_BASE_URL",
            "https://evotech.slarenasitsolutions.com/"
    );

    public static final String SHIFT_SCHEDULE_PATH = resolveString(
            "SHIFT_SCHEDULE_PATH",
            "PHP/shift_functions.php"
    );

    public static final String SHIFT_ACTION_PATH = resolveString(
            "SHIFT_ACTION_PATH",
            SHIFT_SCHEDULE_PATH
    );

    public static final String SHIFT_FETCH_ACTION = resolveString(
            "SHIFT_FETCH_ACTION",
            "get_shift_schedules"
    );

    public static final String SHIFT_START_ACTION = resolveString(
            "SHIFT_START_ACTION",
            "start_shift"
    );

    public static final String USER_PROFILE_PATH = resolveString(
            "USER_PROFILE_PATH",
            "PHP/user_api.php"
    );

    public static final String USER_PROFILE_ACTION = resolveString(
            "USER_PROFILE_ACTION",
            "get_profile"
    );

    public static final int DEFAULT_STAFF_USER_ID = resolveInt(
            "DEFAULT_STAFF_USER_ID",
            0
    );

    private AppConfig() {
        // Utility class
    }

    @NonNull
    private static String resolveString(@NonNull String fieldName, @NonNull String fallback) {
        Object value = resolveField(fieldName);
        if (value instanceof String) {
            String stringValue = ((String) value).trim();
            if (!stringValue.isEmpty()) {
                return stringValue;
            }
        }
        return fallback;
    }

    private static int resolveInt(@NonNull String fieldName, int fallback) {
        Object value = resolveField(fieldName);
        if (value instanceof Number) {
            return ((Number) value).intValue();
        }
        if (value instanceof String) {
            try {
                return Integer.parseInt(((String) value).trim());
            } catch (NumberFormatException ignored) {
                // Fall through to fallback
            }
        }
        return fallback;
    }

    private static boolean resolveBoolean(@NonNull String fieldName, boolean fallback) {
        Object value = resolveField(fieldName);
        if (value instanceof Boolean) {
            return (Boolean) value;
        }
        if (value instanceof String) {
            String trimmed = ((String) value).trim().toLowerCase();
            if ("true".equals(trimmed) || "1".equals(trimmed) || "yes".equals(trimmed)) {
                return true;
            }
            if ("false".equals(trimmed) || "0".equals(trimmed) || "no".equals(trimmed)) {
                return false;
            }
        }
        return fallback;
    }

    @Nullable
    private static Object resolveField(@NonNull String fieldName) {
        try {
            Class<?> clazz = Class.forName(BUILD_CONFIG_CLASS);
            return clazz.getField(fieldName).get(null);
        } catch (ClassNotFoundException | NoSuchFieldException | IllegalAccessException ignored) {
            return null;
        }
    }
}
