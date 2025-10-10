package com.example.deliveryapp.network;

import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.example.deliveryapp.AppConfig;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

public class ServerConnectionManager {

    private static final String TAG = "ServerConnection";
    private static final int CONNECT_TIMEOUT_MS = (int) TimeUnit.SECONDS.toMillis(10);
    private static final int READ_TIMEOUT_MS = (int) TimeUnit.SECONDS.toMillis(10);

    private static volatile ServerConnectionManager instance;

    private final Handler mainThreadHandler;
    private final ExecutorService networkExecutor;
    @Nullable
    private final URL baseUrl;

    private ServerConnectionManager() {
        mainThreadHandler = new Handler(Looper.getMainLooper());
        networkExecutor = Executors.newCachedThreadPool();
        baseUrl = parseUrl(AppConfig.API_BASE_URL);
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
        URL targetUrl = buildUrl(healthPath);
        if (targetUrl == null) {
            postResult(callback, false, "Invalid base URL");
            return;
        }

        networkExecutor.execute(() -> {
            HttpURLConnection connection = null;
            try {
                connection = (HttpURLConnection) targetUrl.openConnection();
                connection.setConnectTimeout(CONNECT_TIMEOUT_MS);
                connection.setReadTimeout(READ_TIMEOUT_MS);
                connection.setRequestMethod("GET");
                connection.connect();

                int statusCode = connection.getResponseCode();
                if (statusCode >= 200 && statusCode < 300) {
                    postResult(callback, true, null);
                } else {
                    postResult(callback, false, "HTTP " + statusCode);
                }
            } catch (IOException e) {
                postResult(callback, false, e.getMessage());
            } finally {
                if (connection != null) {
                    connection.disconnect();
                }
            }
        });
    }

    @Nullable
    public URL getBaseUrl() {
        return baseUrl;
    }

    @Nullable
    public URL buildUrl(@Nullable String relativePath) {
        if (baseUrl == null) {
            return null;
        }
        if (relativePath == null || relativePath.trim().isEmpty()) {
            return baseUrl;
        }
        try {
            return new URL(baseUrl, relativePath.trim());
        } catch (MalformedURLException e) {
            Log.w(TAG, "Unable to resolve URL: " + relativePath, e);
            return null;
        }
    }

    @NonNull
    public ExecutorService getNetworkExecutor() {
        return networkExecutor;
    }

    @NonNull
    public Handler getMainThreadHandler() {
        return mainThreadHandler;
    }

    private URL parseUrl(@Nullable String urlValue) {
        if (urlValue == null || urlValue.trim().isEmpty()) {
            return null;
        }
        try {
            return new URL(urlValue.trim());
        } catch (MalformedURLException e) {
            Log.w(TAG, "Invalid base URL: " + urlValue, e);
            return null;
        }
    }

    private void postResult(@NonNull ConnectionCallback callback, boolean isConnected, @Nullable String errorMessage) {
        mainThreadHandler.post(() -> callback.onResult(isConnected, errorMessage));
    }

    public interface ConnectionCallback {
        void onResult(boolean isConnected, @Nullable String errorMessage);
    }
}
