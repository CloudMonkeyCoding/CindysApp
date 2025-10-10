package com.example.deliveryapp.network.model;

import com.google.gson.annotations.SerializedName;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;
import java.util.Objects;

import androidx.annotation.Nullable;

public class ShiftSchedule {

    @SerializedName("Shift_ID")
    private int shiftId;

    @SerializedName("User_ID")
    private Integer userId;

    @SerializedName("Shift_Date")
    private String shiftDate;

    @SerializedName("Scheduled_Start")
    private String scheduledStart;

    @SerializedName("Scheduled_End")
    private String scheduledEnd;

    @SerializedName("Actual_Start")
    private String actualStart;

    @SerializedName("Actual_End")
    private String actualEnd;

    @SerializedName("Status")
    private String status;

    @SerializedName("Notes")
    private String notes;

    @SerializedName("Name")
    private String staffName;

    public int getShiftId() {
        return shiftId;
    }

    @Nullable
    public Integer getUserId() {
        return userId;
    }

    @Nullable
    public String getShiftDate() {
        return shiftDate;
    }

    @Nullable
    public String getScheduledStart() {
        return scheduledStart;
    }

    @Nullable
    public String getScheduledEnd() {
        return scheduledEnd;
    }

    @Nullable
    public String getActualStart() {
        return actualStart;
    }

    @Nullable
    public String getActualEnd() {
        return actualEnd;
    }

    @Nullable
    public String getStatus() {
        return status;
    }

    @Nullable
    public String getNotes() {
        return notes;
    }

    @Nullable
    public String getStaffName() {
        return staffName;
    }

    public boolean isInProgress() {
        return !isNullOrEmpty(actualStart) && isNullOrEmpty(actualEnd);
    }

    public boolean isCompleted() {
        return !isNullOrEmpty(actualStart) && !isNullOrEmpty(actualEnd);
    }

    public boolean isScheduled() {
        return isNullOrEmpty(actualStart);
    }

    @Nullable
    public Date getScheduledStartDateTime() {
        if (isNullOrEmpty(shiftDate) || isNullOrEmpty(scheduledStart)) {
            return null;
        }

        try {
            Date datePart = createDateFormat().parse(shiftDate.trim());
            Date timePart = createTimeFormat().parse(sanitizeTime(scheduledStart));
            if (datePart == null || timePart == null) {
                return null;
            }
            Calendar result = Calendar.getInstance();
            result.setTime(datePart);

            Calendar timeCalendar = Calendar.getInstance();
            timeCalendar.setTime(timePart);

            result.set(Calendar.HOUR_OF_DAY, timeCalendar.get(Calendar.HOUR_OF_DAY));
            result.set(Calendar.MINUTE, timeCalendar.get(Calendar.MINUTE));
            result.set(Calendar.SECOND, 0);
            result.set(Calendar.MILLISECOND, 0);
            return result.getTime();
        } catch (ParseException ignored) {
            return null;
        }
    }

    private static boolean isNullOrEmpty(@Nullable String value) {
        return value == null || value.trim().isEmpty();
    }

    private static String sanitizeTime(String value) {
        if (value == null) {
            return "";
        }
        String trimmed = value.trim();
        if (trimmed.length() >= 5) {
            return trimmed.substring(0, 5);
        }
        return trimmed;
    }

    private static SimpleDateFormat createDateFormat() {
        return new SimpleDateFormat("yyyy-MM-dd", Locale.US);
    }

    private static SimpleDateFormat createTimeFormat() {
        return new SimpleDateFormat("HH:mm", Locale.US);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof ShiftSchedule)) return false;
        ShiftSchedule that = (ShiftSchedule) o;
        return shiftId == that.shiftId;
    }

    @Override
    public int hashCode() {
        return Objects.hash(shiftId);
    }
}
