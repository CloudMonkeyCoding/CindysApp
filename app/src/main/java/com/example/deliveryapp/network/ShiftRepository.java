package com.example.deliveryapp.network;

import android.util.Log;

import androidx.annotation.Nullable;

import com.example.deliveryapp.BuildConfig;
import com.example.deliveryapp.network.model.ShiftActionResponse;
import com.example.deliveryapp.network.model.ShiftSchedule;

import java.io.IOException;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.List;

import okhttp3.OkHttpClient;
import okhttp3.logging.HttpLoggingInterceptor;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

public class ShiftRepository {

    private static final String TAG = "ShiftRepository";
    private static final String ACTION_LIST = "list";
    private static final String ACTION_START_SHIFT = "start_shift";
    private static final String ACTION_END_SHIFT = "end_shift";

    private final ShiftApiService apiService;

    @Nullable
    private Call<List<ShiftSchedule>> loadCall;

    @Nullable
    private Call<ShiftActionResponse> actionCall;

    public ShiftRepository() {
        HttpLoggingInterceptor loggingInterceptor = new HttpLoggingInterceptor();
        loggingInterceptor.setLevel(BuildConfig.DEBUG
                ? HttpLoggingInterceptor.Level.BODY
                : HttpLoggingInterceptor.Level.NONE);

        OkHttpClient client = new OkHttpClient.Builder()
                .addInterceptor(loggingInterceptor)
                .build();

        Retrofit retrofit = new Retrofit.Builder()
                .baseUrl(BuildConfig.SHIFT_API_BASE_URL)
                .client(client)
                .addConverterFactory(GsonConverterFactory.create())
                .build();

        apiService = retrofit.create(ShiftApiService.class);
    }

    public void fetchUpcomingShift(@Nullable Integer userId, final ShiftLoadCallback callback) {
        cancelLoadCall();
        loadCall = apiService.getShiftSchedules(ACTION_LIST, userId, null, null);
        loadCall.enqueue(new Callback<List<ShiftSchedule>>() {
            @Override
            public void onResponse(Call<List<ShiftSchedule>> call, Response<List<ShiftSchedule>> response) {
                if (!response.isSuccessful()) {
                    callback.onError(extractErrorMessage(response));
                    return;
                }
                List<ShiftSchedule> body = response.body();
                if (body == null || body.isEmpty()) {
                    callback.onSuccess(null);
                    return;
                }
                callback.onSuccess(selectUpcomingShift(body));
            }

            @Override
            public void onFailure(Call<List<ShiftSchedule>> call, Throwable t) {
                if (call.isCanceled()) {
                    return;
                }
                callback.onError(t.getMessage() != null ? t.getMessage() : "Request failed");
            }
        });
    }

    public void startShift(int shiftId, final ShiftActionCallback callback) {
        performShiftAction(apiService.startShift(ACTION_START_SHIFT, shiftId), callback);
    }

    public void endShift(int shiftId, final ShiftActionCallback callback) {
        performShiftAction(apiService.endShift(ACTION_END_SHIFT, shiftId), callback);
    }

    public void cancelAll() {
        cancelLoadCall();
        if (actionCall != null) {
            actionCall.cancel();
            actionCall = null;
        }
    }

    private void performShiftAction(Call<ShiftActionResponse> call, final ShiftActionCallback callback) {
        cancelActionCall();
        actionCall = call;
        actionCall.enqueue(new Callback<ShiftActionResponse>() {
            @Override
            public void onResponse(Call<ShiftActionResponse> call, Response<ShiftActionResponse> response) {
                if (!response.isSuccessful()) {
                    callback.onCompleted(false, extractErrorMessage(response));
                    return;
                }
                ShiftActionResponse body = response.body();
                if (body == null) {
                    callback.onCompleted(false, "Empty server response");
                    return;
                }
                callback.onCompleted(body.isSuccess(), body.getMessage());
            }

            @Override
            public void onFailure(Call<ShiftActionResponse> call, Throwable t) {
                if (call.isCanceled()) {
                    return;
                }
                callback.onCompleted(false, t.getMessage() != null ? t.getMessage() : "Request failed");
            }
        });
    }

    private void cancelLoadCall() {
        if (loadCall != null) {
            loadCall.cancel();
            loadCall = null;
        }
    }

    private void cancelActionCall() {
        if (actionCall != null) {
            actionCall.cancel();
            actionCall = null;
        }
    }

    @Nullable
    private ShiftSchedule selectUpcomingShift(List<ShiftSchedule> schedules) {
        if (schedules.isEmpty()) {
            return null;
        }
        Collections.sort(schedules, new Comparator<ShiftSchedule>() {
            @Override
            public int compare(ShiftSchedule first, ShiftSchedule second) {
                Date firstDate = first.getScheduledStartDateTime();
                Date secondDate = second.getScheduledStartDateTime();
                if (firstDate == null && secondDate == null) {
                    return 0;
                }
                if (firstDate == null) {
                    return 1;
                }
                if (secondDate == null) {
                    return -1;
                }
                return firstDate.compareTo(secondDate);
            }
        });
        return schedules.get(0);
    }

    private String extractErrorMessage(Response<?> response) {
        try {
            if (response.errorBody() != null) {
                return response.errorBody().string();
            }
        } catch (IOException e) {
            Log.w(TAG, "Failed to read error body", e);
        }
        return "Unexpected server response";
    }

    public interface ShiftLoadCallback {
        void onSuccess(@Nullable ShiftSchedule upcomingShift);

        void onError(String message);
    }

    public interface ShiftActionCallback {
        void onCompleted(boolean success, @Nullable String message);
    }
}
