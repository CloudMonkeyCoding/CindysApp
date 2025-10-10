package com.example.deliveryapp;

import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;

import com.example.deliveryapp.network.ShiftRepository;
import com.example.deliveryapp.network.model.ShiftSchedule;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.concurrent.TimeUnit;

public class StatusActivity extends BottomNavActivity {

    private static final SimpleDateFormat SERVER_DATE_FORMAT = new SimpleDateFormat("yyyy-MM-dd", Locale.US);
    private static final SimpleDateFormat SERVER_TIME_FORMAT = new SimpleDateFormat("HH:mm", Locale.US);
    private static final SimpleDateFormat MONTH_FORMAT = new SimpleDateFormat("MMM", Locale.US);
    private static final SimpleDateFormat DAY_FORMAT = new SimpleDateFormat("d", Locale.US);
    private static final SimpleDateFormat WEEKDAY_FORMAT = new SimpleDateFormat("EEE", Locale.US);
    private static final SimpleDateFormat DISPLAY_TIME_FORMAT = new SimpleDateFormat("h:mm a", Locale.US);

    private TextView statusBanner;
    private View shiftCard;
    private TextView shiftEmptyState;
    private TextView shiftMonthLabel;
    private TextView shiftDayLabel;
    private TextView shiftWeekdayLabel;
    private TextView shiftTimeRange;
    private TextView shiftLocationLabel;
    private TextView shiftCountdownLabel;
    private Button shiftActionButton;
    private ProgressBar loadingIndicator;

    private final ShiftRepository shiftRepository = new ShiftRepository();
    @Nullable
    private ShiftSchedule currentShift;
    private boolean actionInProgress;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_status);
        setupBottomNavigation(R.id.menu_status);

        statusBanner = findViewById(R.id.statusBanner);
        shiftCard = findViewById(R.id.shiftCard);
        shiftEmptyState = findViewById(R.id.shiftEmptyState);
        shiftMonthLabel = findViewById(R.id.shiftMonthLabel);
        shiftDayLabel = findViewById(R.id.shiftDayLabel);
        shiftWeekdayLabel = findViewById(R.id.shiftWeekdayLabel);
        shiftTimeRange = findViewById(R.id.shiftTimeRange);
        shiftLocationLabel = findViewById(R.id.shiftLocationLabel);
        shiftCountdownLabel = findViewById(R.id.shiftCountdownLabel);
        shiftActionButton = findViewById(R.id.btnShiftAction);
        loadingIndicator = findViewById(R.id.shiftLoadingIndicator);

        shiftActionButton.setOnClickListener(v -> {
            if (currentShift == null) {
                Toast.makeText(this, R.string.status_no_shift, Toast.LENGTH_SHORT).show();
                return;
            }
            if (actionInProgress) {
                return;
            }
            if (currentShift.isInProgress()) {
                endCurrentShift();
            } else {
                startCurrentShift();
            }
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadUpcomingShift();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        shiftRepository.cancelAll();
    }

    private void loadUpcomingShift() {
        setLoading(true);
        shiftRepository.fetchUpcomingShift(null, new ShiftRepository.ShiftLoadCallback() {
            @Override
            public void onSuccess(@Nullable ShiftSchedule upcomingShift) {
                setLoading(false);
                currentShift = upcomingShift;
                shiftEmptyState.setText(R.string.status_no_shift);
                updateUiForShift(upcomingShift);
            }

            @Override
            public void onError(String message) {
                setLoading(false);
                currentShift = null;
                updateUiForShift(null);
                shiftEmptyState.setText(R.string.status_loading_error);
                shiftEmptyState.setVisibility(View.VISIBLE);
                Toast.makeText(StatusActivity.this, R.string.status_loading_error, Toast.LENGTH_LONG).show();
            }
        });
    }

    private void startCurrentShift() {
        if (currentShift == null) {
            return;
        }
        setActionInProgress(true);
        shiftRepository.startShift(currentShift.getShiftId(), (success, message) -> {
            setActionInProgress(false);
            Toast.makeText(
                    StatusActivity.this,
                    message != null ? message : (success
                            ? getString(R.string.status_shift_in_progress_message)
                            : getString(R.string.status_loading_error)),
                    Toast.LENGTH_SHORT
            ).show();
            if (success) {
                loadUpcomingShift();
            }
        });
    }

    private void endCurrentShift() {
        if (currentShift == null) {
            return;
        }
        setActionInProgress(true);
        shiftRepository.endShift(currentShift.getShiftId(), (success, message) -> {
            setActionInProgress(false);
            Toast.makeText(
                    StatusActivity.this,
                    message != null ? message : (success
                            ? getString(R.string.status_shift_completed_message)
                            : getString(R.string.status_loading_error)),
                    Toast.LENGTH_SHORT
            ).show();
            if (success) {
                loadUpcomingShift();
            }
        });
    }

    private void updateUiForShift(@Nullable ShiftSchedule shift) {
        if (shift == null) {
            statusBanner.setText(R.string.status_banner_not_working);
            shiftCard.setVisibility(View.GONE);
            shiftEmptyState.setVisibility(View.VISIBLE);
            shiftActionButton.setText(R.string.status_start_shift);
            shiftActionButton.setEnabled(false);
            return;
        }

        shiftCard.setVisibility(View.VISIBLE);
        shiftEmptyState.setVisibility(View.GONE);
        shiftActionButton.setEnabled(!shift.isCompleted());

        populateShiftCard(shift);
    }

    private void populateShiftCard(ShiftSchedule shift) {
        Date shiftDate = parseDate(shift.getShiftDate());
        if (shiftDate != null) {
            shiftMonthLabel.setText(MONTH_FORMAT.format(shiftDate));
            shiftDayLabel.setText(DAY_FORMAT.format(shiftDate));
            shiftWeekdayLabel.setText(WEEKDAY_FORMAT.format(shiftDate));
        } else {
            shiftMonthLabel.setText("--");
            shiftDayLabel.setText("--");
            shiftWeekdayLabel.setText("--");
        }

        shiftTimeRange.setText(buildTimeRangeText(shift));

        String location = !TextUtils.isEmpty(shift.getNotes())
                ? shift.getNotes()
                : getString(R.string.status_default_location);
        shiftLocationLabel.setText(location);

        if (shift.isInProgress()) {
            statusBanner.setText(R.string.status_banner_in_progress);
            shiftActionButton.setText(R.string.status_end_shift);
            shiftCountdownLabel.setText(R.string.status_shift_in_progress_message);
        } else if (shift.isCompleted()) {
            statusBanner.setText(R.string.status_banner_completed);
            shiftActionButton.setText(R.string.status_end_shift);
            shiftActionButton.setEnabled(false);
            shiftCountdownLabel.setText(R.string.status_shift_completed_message);
        } else {
            statusBanner.setText(R.string.status_banner_scheduled);
            shiftActionButton.setText(R.string.status_start_shift);
            Date startDateTime = shift.getScheduledStartDateTime();
            if (startDateTime != null) {
                shiftCountdownLabel.setText(buildCountdownText(startDateTime));
            } else {
                shiftCountdownLabel.setText(R.string.status_starting_placeholder);
            }
        }
    }

    private void setLoading(boolean loading) {
        loadingIndicator.setVisibility(loading ? View.VISIBLE : View.GONE);
        shiftActionButton.setEnabled(!loading && currentShift != null && !currentShift.isCompleted());
    }

    private void setActionInProgress(boolean inProgress) {
        actionInProgress = inProgress;
        shiftActionButton.setEnabled(!inProgress && currentShift != null && !currentShift.isCompleted());
        loadingIndicator.setVisibility(inProgress ? View.VISIBLE : View.GONE);
    }

    private String buildTimeRangeText(ShiftSchedule shift) {
        String start = formatTime(shift.getScheduledStart());
        String end = formatTime(shift.getScheduledEnd());
        if (TextUtils.isEmpty(start) && TextUtils.isEmpty(end)) {
            return "--";
        }
        if (TextUtils.isEmpty(end)) {
            return start;
        }
        if (TextUtils.isEmpty(start)) {
            return end;
        }
        return start + " - " + end;
    }

    private String formatTime(@Nullable String value) {
        if (TextUtils.isEmpty(value)) {
            return "";
        }
        try {
            Date parsed = SERVER_TIME_FORMAT.parse(sanitizeTime(value));
            if (parsed != null) {
                return DISPLAY_TIME_FORMAT.format(parsed);
            }
        } catch (ParseException ignored) {
            // Fallback to raw value below
        }
        return value;
    }

    private String buildCountdownText(Date startDateTime) {
        long diff = startDateTime.getTime() - System.currentTimeMillis();
        if (diff <= 0L) {
            return getString(R.string.status_shift_ready);
        }
        long minutes = TimeUnit.MILLISECONDS.toMinutes(diff);
        long hours = minutes / 60;
        long remainingMinutes = minutes % 60;
        String duration;
        if (hours > 0) {
            duration = getString(R.string.status_duration_hours_minutes, hours, remainingMinutes);
        } else {
            duration = getString(R.string.status_duration_minutes, remainingMinutes);
        }
        return getString(R.string.status_starting_soon, duration);
    }

    @Nullable
    private Date parseDate(@Nullable String value) {
        if (TextUtils.isEmpty(value)) {
            return null;
        }
        try {
            return SERVER_DATE_FORMAT.parse(value.trim());
        } catch (ParseException ignored) {
            return null;
        }
    }

    private static String sanitizeTime(String raw) {
        if (raw == null) {
            return "";
        }
        String trimmed = raw.trim();
        if (trimmed.length() >= 5) {
            return trimmed.substring(0, 5);
        }
        return trimmed;
    }
}
