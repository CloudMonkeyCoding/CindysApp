package com.example.deliveryapp.network;

import android.os.Handler;
import android.os.Looper;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.example.deliveryapp.BuildConfig;

import java.io.IOException;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.HttpUrl;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.logging.HttpLoggingInterceptor;

public class ServerConnectionManager {

    private static volatile ServerConnectionManager instance;

    private final OkHttpClient okHttpClient;
    private final Handler mainThreadHandler;

    private ServerConnectionManager() {
        HttpLoggingInterceptor loggingInterceptor = new HttpLoggingInterceptor();
        loggingInterceptor.setLevel(BuildConfig.DEBUG
                ? HttpLoggingInterceptor.Level.BODY
                : HttpLoggingInterceptor.Level.BASIC);

        okHttpClient = new OkHttpClient.Builder()
                .addInterceptor(loggingInterceptor)
                .build();
        mainThreadHandler = new Handler(Looper.getMainLooper());
    }

    public static ServerConnectionManager getInstance() {
        if (instance == null) {
            synchronized (ServerConnectionManager.class) {
                if (instance == null) {
                    instance = new ServerConnectionManager();
                }
            }
        }
        return instance;
    }

    public void checkConnection(@Nullable String healthPath, @NonNull ConnectionCallback callback) {
        HttpUrl baseUrl = HttpUrl.parse(BuildConfig.API_BASE_URL);
        if (baseUrl == null) {
            postResult(callback, false, "Invalid base URL");
            return;
        }

        HttpUrl requestUrl = buildHealthUrl(baseUrl, healthPath);
        if (requestUrl == null) {
            postResult(callback, false, "Unable to build request URL");
            return;
        }

        Request request = new Request.Builder()
                .url(requestUrl)
                .get()
                .build();

        okHttpClient.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(@NonNull Call call, @NonNull IOException e) {
                postResult(callback, false, e.getMessage());
            }

            @Override
            public void onResponse(@NonNull Call call, @NonNull Response response) {
                try (response) {
                    if (response.isSuccessful()) {
                        postResult(callback, true, null);
                    } else {
                        postResult(callback, false, "HTTP " + response.code());
                    }
                }
            }
        });
    }

    @Nullable
    private HttpUrl buildHealthUrl(@NonNull HttpUrl baseUrl, @Nullable String healthPath) {
        if (healthPath == null || healthPath.trim().isEmpty() || "/".equals(healthPath.trim())) {
            return baseUrl;
        }

        String sanitizedPath = healthPath.trim();
        while (sanitizedPath.startsWith("/")) {
            sanitizedPath = sanitizedPath.substring(1);
        }

        if (sanitizedPath.isEmpty()) {
            return baseUrl;
        }

        HttpUrl.Builder builder = baseUrl.newBuilder();
        for (String segment : sanitizedPath.split("/")) {
            if (segment.isEmpty()) {
                continue;
            }
            builder.addEncodedPathSegment(segment);
        }
        return builder.build();
    }

    private void postResult(@NonNull ConnectionCallback callback, boolean isConnected, @Nullable String errorMessage) {
        mainThreadHandler.post(() -> callback.onResult(isConnected, errorMessage));
    }

    public interface ConnectionCallback {
        void onResult(boolean isConnected, @Nullable String errorMessage);
    }
}
