package com.example.deliveryapp.network.model;

import com.google.gson.annotations.SerializedName;

import androidx.annotation.Nullable;

public class ShiftActionResponse {

    @SerializedName("success")
    private boolean success;

    @SerializedName("message")
    @Nullable
    private String message;

    @SerializedName("shift")
    @Nullable
    private ShiftSchedule shift;

    public boolean isSuccess() {
        return success;
    }

    @Nullable
    public String getMessage() {
        return message;
    }

    @Nullable
    public ShiftSchedule getShift() {
        return shift;
    }
}
