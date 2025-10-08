package com.example.deliveryapp.network;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.util.Locale;

/**
 * Represents a single shift entry returned by the Cindy's Bakeshop backend.
 */
public final class ShiftInfo {

    private final int id;
    private final int userId;
    @Nullable
    private final String staffName;
    @Nullable
    private final String shiftDate;
    @Nullable
    private final String scheduledStart;
    @Nullable
    private final String scheduledEnd;
    @Nullable
    private final String actualStart;
    @Nullable
    private final String actualEnd;
    @Nullable
    private final String status;
    @Nullable
    private final String notes;
    @Nullable
    private final String location;

    public ShiftInfo(
            int id,
            int userId,
            @Nullable String staffName,
            @Nullable String shiftDate,
            @Nullable String scheduledStart,
            @Nullable String scheduledEnd,
            @Nullable String actualStart,
            @Nullable String actualEnd,
            @Nullable String status,
            @Nullable String notes,
            @Nullable String location
    ) {
        this.id = id;
        this.userId = userId;
        this.staffName = staffName;
        this.shiftDate = shiftDate;
        this.scheduledStart = scheduledStart;
        this.scheduledEnd = scheduledEnd;
        this.actualStart = actualStart;
        this.actualEnd = actualEnd;
        this.status = status;
        this.notes = notes;
        this.location = location;
    }

    public int getId() {
        return id;
    }

    public int getUserId() {
        return userId;
    }

    @Nullable
    public String getStaffName() {
        return staffName;
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
    public String getLocation() {
        return location;
    }

    public boolean hasStarted() {
        return actualStart != null && !actualStart.trim().isEmpty();
    }

    public boolean hasEnded() {
        return actualEnd != null && !actualEnd.trim().isEmpty();
    }

    public boolean isCompleted() {
        if (hasEnded()) {
            return true;
        }
        if (status == null) {
            return false;
        }
        String normalized = status.trim().toLowerCase(Locale.US);
        return normalized.equals("completed") || normalized.equals("finished");
    }

    public boolean canStart() {
        if (id <= 0) {
            return false;
        }
        if (hasStarted()) {
            return false;
        }
        if (status == null) {
            return true;
        }
        String normalized = status.trim().toLowerCase(Locale.US);
        return !normalized.equals("in_progress")
                && !normalized.equals("completed")
                && !normalized.equals("finished")
                && !normalized.equals("cancelled")
                && !normalized.equals("missed");
    }

    @NonNull
    public ShiftInfo withStatus(
            @Nullable String newStatus,
            @Nullable String newActualStart,
            @Nullable String newActualEnd
    ) {
        return new ShiftInfo(
                id,
                userId,
                staffName,
                shiftDate,
                scheduledStart,
                scheduledEnd,
                newActualStart != null ? newActualStart : actualStart,
                newActualEnd != null ? newActualEnd : actualEnd,
                newStatus != null ? newStatus : status,
                notes,
                location
        );
    }
}
