package com.example.deliveryapp.network;

import com.example.deliveryapp.network.model.ShiftActionResponse;
import com.example.deliveryapp.network.model.ShiftSchedule;

import java.util.List;

import retrofit2.Call;
import retrofit2.http.Field;
import retrofit2.http.FormUrlEncoded;
import retrofit2.http.GET;
import retrofit2.http.POST;
import retrofit2.http.Query;

public interface ShiftApiService {

    @GET("shift_api.php")
    Call<List<ShiftSchedule>> getShiftSchedules(
            @Query("action") String action,
            @Query("user_id") Integer userId,
            @Query("start_date") String startDate,
            @Query("end_date") String endDate
    );

    @FormUrlEncoded
    @POST("shift_api.php")
    Call<ShiftActionResponse> startShift(
            @Field("action") String action,
            @Field("shift_id") int shiftId
    );

    @FormUrlEncoded
    @POST("shift_api.php")
    Call<ShiftActionResponse> endShift(
            @Field("action") String action,
            @Field("shift_id") int shiftId
    );
}
