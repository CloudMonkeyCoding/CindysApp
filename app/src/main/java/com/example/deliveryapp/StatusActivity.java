package com.example.deliveryapp;

import android.os.Build;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;

import com.example.deliveryapp.AppConfig;
import com.example.deliveryapp.network.ServerConnectionManager;
import com.example.deliveryapp.network.ShiftInfo;
import com.example.deliveryapp.network.ShiftService;
import com.example.deliveryapp.network.UserService;

import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.time.format.TextStyle;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

public class StatusActivity extends BottomNavActivity {

    private TextView statusBanner;
    private ProgressBar connectionProgress;

    private View shiftCard;
    private ProgressBar shiftLoading;
    private TextView shiftEmptyView;
    private TextView shiftMonthView;
    private TextView shiftDayView;
    private TextView shiftWeekdayView;
    private TextView shiftTimeRangeView;
    private TextView shiftStatusView;
    private TextView shiftLocationView;
    private TextView shiftCountdownView;
    private TextView shiftRefreshView;
    private Button startShiftButton;
    private ProgressBar startShiftProgress;
    private View shiftListSection;
    private LinearLayout shiftListContainer;
    private TextView shiftListEmptyView;

    private final ShiftService shiftService = new ShiftService();
    private final UserService userService = new UserService();
    @Nullable
    private ShiftInfo currentShift;
    @Nullable
    private Integer resolvedUserId;
    private final List<ShiftInfo> loadedShifts = new ArrayList<>();
    private boolean isShiftLoading;
    private boolean isStartRequestRunning;
    private boolean isResolvingUserId;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_status);
        setupBottomNavigation(R.id.menu_status);

        initViews();
        checkServerConnection();
        resolveStaffIdentity(false);
    }

    private void initViews() {
        statusBanner = findViewById(R.id.statusBanner);
        connectionProgress = findViewById(R.id.connectionProgress);
        shiftCard = findViewById(R.id.shiftCard);
        shiftLoading = findViewById(R.id.shiftLoading);
        shiftEmptyView = findViewById(R.id.shiftEmptyView);
        shiftMonthView = findViewById(R.id.shiftMonth);
        shiftDayView = findViewById(R.id.shiftDay);
        shiftWeekdayView = findViewById(R.id.shiftWeekday);
        shiftTimeRangeView = findViewById(R.id.shiftTimeRange);
        shiftStatusView = findViewById(R.id.shiftStatusText);
        shiftLocationView = findViewById(R.id.shiftLocation);
        shiftCountdownView = findViewById(R.id.shiftCountdown);
        shiftRefreshView = findViewById(R.id.shiftRefresh);
        startShiftButton = findViewById(R.id.startShiftButton);
        startShiftProgress = findViewById(R.id.startShiftProgress);
        shiftListSection = findViewById(R.id.shiftListSection);
        shiftListContainer = findViewById(R.id.shiftListContainer);
        shiftListEmptyView = findViewById(R.id.shiftListEmpty);

        if (shiftRefreshView != null) {
            shiftRefreshView.setOnClickListener(v -> {
                if (resolvedUserId != null) {
                    loadShifts(true);
                } else {
                    resolveStaffIdentity(true);
                }
            });
        }

        if (startShiftButton != null) {
            startShiftButton.setOnClickListener(v -> onStartShiftClicked());
        }

        updateStartButtonState();
    }

    private void checkServerConnection() {
        if (statusBanner == null || connectionProgress == null) {
            return;
        }

        connectionProgress.setVisibility(View.VISIBLE);
        updateStatusUi(StatusState.CHECKING, getString(R.string.status_checking_message));

        ServerConnectionManager.getInstance().checkConnection(
                getString(R.string.server_health_path),
                (isConnected, errorMessage) -> {
                    connectionProgress.setVisibility(View.GONE);

                    if (isConnected) {
                        updateStatusUi(
                                StatusState.CONNECTED,
                                getString(R.string.status_connected, AppConfig.API_BASE_URL)
                        );
                    } else {
                        String errorText = getString(R.string.status_connection_failed);
                        if (errorMessage != null && !errorMessage.isEmpty()) {
                            errorText = errorText + " (" + errorMessage + ")";
                        }
                        updateStatusUi(StatusState.ERROR, errorText);
                    }
                }
        );
    }

    private void loadShifts(boolean userRequestedRefresh) {
        if (resolvedUserId == null) {
            if (!isResolvingUserId) {
                resolveStaffIdentity(userRequestedRefresh);
            } else if (userRequestedRefresh) {
                showToast(getString(R.string.status_shift_user_id_lookup_in_progress));
            }
            return;
        }

        showShiftLoading(true);
        shiftService.fetchShifts(resolvedUserId, new ShiftService.ShiftFetchCallback() {
            @Override
            public void onSuccess(@NonNull List<ShiftInfo> shifts, @Nullable String serverMessage) {
                showShiftLoading(false);
                if (shifts.isEmpty()) {
                    String message = !TextUtils.isEmpty(serverMessage)
                            ? serverMessage
                            : getString(R.string.status_shift_no_shift_message);
                    showNoShift(message);
                    populateShiftList(Collections.emptyList());
                } else {
                    ShiftInfo first = shifts.get(0);
                    bindShift(first);
                    populateShiftList(shifts);
                    if (!TextUtils.isEmpty(serverMessage) && userRequestedRefresh) {
                        showToast(serverMessage);
                    }
                }
            }

            @Override
            public void onError(@NonNull String errorMessage) {
                showShiftLoading(false);
                showNoShift(!TextUtils.isEmpty(errorMessage)
                        ? errorMessage
                        : getString(R.string.status_shift_load_error));
                if (userRequestedRefresh) {
                    showToast(errorMessage);
                }
                populateShiftList(Collections.emptyList());
            }
        });
    }

    private void resolveStaffIdentity(boolean userRequestedRefresh) {
        if (isResolvingUserId) {
            if (userRequestedRefresh) {
                showToast(getString(R.string.status_shift_user_id_lookup_in_progress));
            }
            return;
        }

        Integer sessionUserId = SessionManager.getUserId(this);
        if (sessionUserId != null) {
            resolvedUserId = sessionUserId;
            loadShifts(userRequestedRefresh);
            return;
        }

        if (AppConfig.DEFAULT_STAFF_USER_ID > 0) {
            resolvedUserId = AppConfig.DEFAULT_STAFF_USER_ID;
            loadShifts(userRequestedRefresh);
            return;
        }

        String email = SessionManager.getEmail(this);
        if (TextUtils.isEmpty(email)) {
            resolvedUserId = null;
            showShiftLoading(false);
            showNoShift(getString(R.string.status_shift_missing_user_id));
            if (userRequestedRefresh) {
                showToast(getString(R.string.status_shift_missing_user_id));
            }
            return;
        }

        isResolvingUserId = true;
        if (currentShift == null) {
            showResolvingUserIdState();
        } else {
            showShiftLoading(true);
        }

        final String lookupEmail = email;
        userService.fetchUserIdByEmail(lookupEmail, new UserService.UserIdCallback() {
            @Override
            public void onSuccess(int userId) {
                isResolvingUserId = false;
                resolvedUserId = userId;
                SessionManager.storeSession(getApplicationContext(), lookupEmail, userId);
                loadShifts(userRequestedRefresh);
            }

            @Override
            public void onError(@NonNull String errorMessage) {
                isResolvingUserId = false;
                resolvedUserId = null;
                showShiftLoading(false);
                String display = !TextUtils.isEmpty(errorMessage)
                        ? errorMessage
                        : getString(R.string.status_shift_user_id_error);
                showNoShift(display);
                if (userRequestedRefresh || currentShift != null) {
                    showToast(display);
                }
            }
        });
    }

    private void showResolvingUserIdState() {
        showShiftLoading(true);
        if (shiftCard != null && currentShift == null) {
            shiftCard.setVisibility(View.GONE);
        }
        if (shiftEmptyView != null) {
            shiftEmptyView.setText(R.string.status_shift_resolving_user_id);
            shiftEmptyView.setVisibility(View.VISIBLE);
        }
        if (shiftListSection != null) {
            shiftListSection.setVisibility(View.GONE);
        }
    }

    private void showShiftLoading(boolean show) {
        isShiftLoading = show;
        if (shiftLoading != null) {
            shiftLoading.setVisibility(show ? View.VISIBLE : View.GONE);
        }
        if (shiftCard != null) {
            shiftCard.setAlpha(show ? 0.6f : 1f);
        }
        if (shiftRefreshView != null) {
            shiftRefreshView.setEnabled(!show);
            shiftRefreshView.setAlpha(show ? 0.5f : 1f);
        }
        if (shiftListSection != null && shiftListSection.getVisibility() == View.VISIBLE) {
            shiftListSection.setAlpha(show ? 0.6f : 1f);
        }
        updateStartButtonState();
    }

    private void showNoShift(@Nullable String message) {
        currentShift = null;
        loadedShifts.clear();
        if (shiftCard != null) {
            shiftCard.setVisibility(View.GONE);
        }
        if (shiftEmptyView != null) {
            String display = !TextUtils.isEmpty(message)
                    ? message
                    : getString(R.string.status_shift_no_shift_message);
            shiftEmptyView.setText(display);
            shiftEmptyView.setVisibility(View.VISIBLE);
        }
        if (shiftListSection != null) {
            shiftListSection.setVisibility(View.GONE);
        }
        if (shiftListContainer != null) {
            shiftListContainer.removeAllViews();
        }
        if (shiftListEmptyView != null) {
            shiftListEmptyView.setVisibility(View.GONE);
        }
        updateStartButtonState();
    }

    private void bindShift(@NonNull ShiftInfo shift) {
        currentShift = shift;
        if (shiftCard != null) {
            shiftCard.setVisibility(View.VISIBLE);
            shiftCard.setAlpha(isShiftLoading ? 0.6f : 1f);
        }
        if (shiftEmptyView != null) {
            shiftEmptyView.setVisibility(View.GONE);
        }

        Locale locale = getCurrentLocale();
        LocalDate shiftDate = parseDate(shift.getShiftDate(), locale);
        LocalTime scheduledStart = parseTime(shift.getScheduledStart(), locale);
        LocalTime scheduledEnd = parseTime(shift.getScheduledEnd(), locale);

        if (shiftMonthView != null && shiftDayView != null && shiftWeekdayView != null) {
            if (shiftDate != null) {
                shiftMonthView.setText(shiftDate.getMonth().getDisplayName(TextStyle.SHORT, locale));
                shiftDayView.setText(String.valueOf(shiftDate.getDayOfMonth()));
                shiftWeekdayView.setText(shiftDate.getDayOfWeek().getDisplayName(TextStyle.SHORT, locale));
            } else {
                shiftMonthView.setText(R.string.status_shift_month_placeholder);
                shiftDayView.setText(R.string.status_shift_day_placeholder);
                shiftWeekdayView.setText(R.string.status_shift_weekday_placeholder);
            }
        }

        if (shiftTimeRangeView != null) {
            shiftTimeRangeView.setText(
                    formatTimeRange(scheduledStart, scheduledEnd, shift.getScheduledStart(), shift.getScheduledEnd(), locale)
            );
        }

        if (shiftStatusView != null) {
            shiftStatusView.setText(formatStatusLabel(shift.getStatus(), locale));
        }

        if (shiftLocationView != null) {
            String location = !TextUtils.isEmpty(shift.getLocation())
                    ? shift.getLocation()
                    : (!TextUtils.isEmpty(shift.getNotes())
                    ? shift.getNotes()
                    : getString(R.string.status_shift_location_placeholder));
            shiftLocationView.setText(location);
        }

        if (shiftCountdownView != null) {
            CountdownDisplay countdownDisplay = buildCountdownDisplay(shift, shiftDate, scheduledStart, locale);
            shiftCountdownView.setText(countdownDisplay.text);
            shiftCountdownView.setTextColor(ContextCompat.getColor(this, countdownDisplay.colorRes));
        }

        updateStartButtonState();
    }

    private void populateShiftList(@NonNull List<ShiftInfo> shifts) {
        loadedShifts.clear();
        loadedShifts.addAll(shifts);

        if (shiftListContainer == null || shiftListSection == null || shiftListEmptyView == null) {
            return;
        }

        shiftListContainer.removeAllViews();

        if (shifts.isEmpty()) {
            shiftListSection.setVisibility(View.GONE);
            shiftListEmptyView.setVisibility(View.GONE);
            return;
        }

        if (shifts.size() <= 1) {
            shiftListSection.setVisibility(View.GONE);
            shiftListEmptyView.setVisibility(View.GONE);
            return;
        }

        LayoutInflater inflater = LayoutInflater.from(this);
        Locale locale = getCurrentLocale();

        for (int i = 0; i < shifts.size(); i++) {
            ShiftInfo shift = shifts.get(i);
            View itemView = inflater.inflate(R.layout.item_shift_summary, shiftListContainer, false);
            bindShiftListItem(itemView, shift, i == 0, locale);
            shiftListContainer.addView(itemView);
        }

        shiftListSection.setVisibility(View.VISIBLE);
        shiftListSection.setAlpha(isShiftLoading ? 0.6f : 1f);
        shiftListEmptyView.setVisibility(View.GONE);
    }

    private void bindShiftListItem(@NonNull View itemView, @NonNull ShiftInfo shift, boolean isPrimary, @NonNull Locale locale) {
        TextView primaryLabel = itemView.findViewById(R.id.itemShiftPrimaryLabel);
        if (primaryLabel != null) {
            primaryLabel.setVisibility(isPrimary ? View.VISIBLE : View.GONE);
        }

        LocalDate shiftDate = parseDate(shift.getShiftDate(), locale);
        LocalTime scheduledStart = parseTime(shift.getScheduledStart(), locale);
        LocalTime scheduledEnd = parseTime(shift.getScheduledEnd(), locale);

        TextView monthView = itemView.findViewById(R.id.itemShiftMonth);
        TextView dayView = itemView.findViewById(R.id.itemShiftDay);
        TextView weekdayView = itemView.findViewById(R.id.itemShiftWeekday);
        if (monthView != null && dayView != null && weekdayView != null) {
            if (shiftDate != null) {
                monthView.setText(shiftDate.getMonth().getDisplayName(TextStyle.SHORT, locale));
                dayView.setText(String.valueOf(shiftDate.getDayOfMonth()));
                weekdayView.setText(shiftDate.getDayOfWeek().getDisplayName(TextStyle.SHORT, locale));
            } else {
                monthView.setText(R.string.status_shift_month_placeholder);
                dayView.setText(R.string.status_shift_day_placeholder);
                weekdayView.setText(R.string.status_shift_weekday_placeholder);
            }
        }

        TextView timeRangeView = itemView.findViewById(R.id.itemShiftTimeRange);
        if (timeRangeView != null) {
            timeRangeView.setText(
                    formatTimeRange(scheduledStart, scheduledEnd, shift.getScheduledStart(), shift.getScheduledEnd(), locale)
            );
        }

        TextView statusView = itemView.findViewById(R.id.itemShiftStatus);
        if (statusView != null) {
            statusView.setText(formatStatusLabel(shift.getStatus(), locale));
        }

        TextView locationView = itemView.findViewById(R.id.itemShiftLocation);
        if (locationView != null) {
            String location = !TextUtils.isEmpty(shift.getLocation())
                    ? shift.getLocation()
                    : (!TextUtils.isEmpty(shift.getNotes())
                    ? shift.getNotes()
                    : getString(R.string.status_shift_location_placeholder));
            locationView.setText(location);
        }
    }

    private void onStartShiftClicked() {
        if (currentShift == null) {
            showToast(getString(R.string.status_shift_no_shift_message));
            loadShifts(true);
            return;
        }

        if (!currentShift.canStart()) {
            showToast(getString(R.string.status_shift_start_unavailable_toast));
            return;
        }

        setStartShiftLoading(true);
        shiftService.startShift(currentShift.getId(), new ShiftService.ShiftActionCallback() {
            @Override
            public void onSuccess(@Nullable ShiftInfo updatedShift, @Nullable String serverMessage) {
                setStartShiftLoading(false);
                loadShifts(false);
                String message = !TextUtils.isEmpty(serverMessage)
                        ? serverMessage
                        : getString(R.string.status_shift_start_success);
                showToast(message);
            }

            @Override
            public void onError(@NonNull String errorMessage) {
                setStartShiftLoading(false);
                showToast(!TextUtils.isEmpty(errorMessage)
                        ? errorMessage
                        : getString(R.string.status_shift_start_failed));
            }
        });
    }

    private void setStartShiftLoading(boolean loading) {
        isStartRequestRunning = loading;
        if (startShiftProgress != null) {
            startShiftProgress.setVisibility(loading ? View.VISIBLE : View.GONE);
        }
        updateStartButtonState();
    }

    private void updateStartButtonState() {
        if (startShiftButton == null) {
            return;
        }

        if (resolvedUserId == null || isResolvingUserId) {
            startShiftButton.setEnabled(false);
            startShiftButton.setAlpha(0.6f);
            startShiftButton.setText(R.string.status_shift_start_button);
            if (startShiftProgress != null) {
                startShiftProgress.setVisibility(View.GONE);
            }
            return;
        }

        if (isStartRequestRunning) {
            startShiftButton.setEnabled(false);
            startShiftButton.setAlpha(0.6f);
            if (startShiftProgress != null) {
                startShiftProgress.setVisibility(View.VISIBLE);
            }
            return;
        }

        if (startShiftProgress != null) {
            startShiftProgress.setVisibility(View.GONE);
        }

        if (currentShift != null && currentShift.canStart() && !isShiftLoading) {
            startShiftButton.setEnabled(true);
            startShiftButton.setAlpha(1f);
            startShiftButton.setText(R.string.status_shift_start_button);
        } else {
            startShiftButton.setEnabled(false);
            startShiftButton.setAlpha(0.6f);
            if (currentShift != null && !currentShift.canStart()) {
                startShiftButton.setText(R.string.status_shift_start_unavailable_button);
            } else {
                startShiftButton.setText(R.string.status_shift_start_button);
            }
        }
    }

    private void updateStatusUi(StatusState state, String message) {
        if (statusBanner == null) {
            return;
        }

        statusBanner.setText(message);

        int backgroundColorRes;
        int textColorRes;

        if (state == StatusState.CONNECTED) {
            backgroundColorRes = R.color.status_connected_bg;
            textColorRes = R.color.status_connected_text;
        } else if (state == StatusState.ERROR) {
            backgroundColorRes = R.color.status_error_bg;
            textColorRes = R.color.status_error_text;
        } else {
            backgroundColorRes = R.color.status_checking_bg;
            textColorRes = R.color.status_checking_text;
        }

        statusBanner.setBackgroundColor(ContextCompat.getColor(this, backgroundColorRes));
        statusBanner.setTextColor(ContextCompat.getColor(this, textColorRes));
    }

    private CountdownDisplay buildCountdownDisplay(
            @NonNull ShiftInfo shift,
            @Nullable LocalDate shiftDate,
            @Nullable LocalTime scheduledStart,
            @NonNull Locale locale
    ) {
        if (shift.hasStarted()) {
            LocalDateTime startDateTime = parseDateTime(shift.getActualStart(), locale);
            if (startDateTime != null) {
                ZonedDateTime zonedDateTime = startDateTime.atZone(ZoneId.systemDefault());
                String formatted = zonedDateTime.format(DateTimeFormatter.ofPattern("MMM d, h:mm a", locale));
                return new CountdownDisplay(getString(R.string.status_shift_started_at, formatted), R.color.status_connected_text);
            }
            return new CountdownDisplay(getString(R.string.status_shift_started_raw, shift.getActualStart()), R.color.status_connected_text);
        }

        if (shiftDate != null && scheduledStart != null) {
            ZonedDateTime startDateTime = shiftDate.atTime(scheduledStart).atZone(ZoneId.systemDefault());
            ZonedDateTime now = ZonedDateTime.now();
            if (startDateTime.isAfter(now)) {
                Duration untilStart = Duration.between(now, startDateTime);
                return new CountdownDisplay(
                        getString(R.string.status_shift_starts_in, formatDuration(untilStart)),
                        R.color.status_connected_text
                );
            }
            Duration overdue = Duration.between(startDateTime, now);
            if (overdue.toMinutes() <= 5) {
                return new CountdownDisplay(getString(R.string.status_shift_should_start_now), R.color.status_checking_text);
            }
            return new CountdownDisplay(
                    getString(R.string.status_shift_overdue_by, formatDuration(overdue)),
                    R.color.status_error_text
            );
        }

        if (scheduledStart != null) {
            return new CountdownDisplay(
                    getString(R.string.status_shift_scheduled_time, formatTime(scheduledStart, locale)),
                    R.color.status_neutral_text
            );
        }

        if (!TextUtils.isEmpty(shift.getScheduledStart())) {
            return new CountdownDisplay(
                    getString(R.string.status_shift_scheduled_time, shift.getScheduledStart()),
                    R.color.status_neutral_text
            );
        }

        return new CountdownDisplay(getString(R.string.status_shift_unknown_schedule), R.color.status_neutral_text);
    }

    private String formatTimeRange(
            @Nullable LocalTime start,
            @Nullable LocalTime end,
            @Nullable String rawStart,
            @Nullable String rawEnd,
            @NonNull Locale locale
    ) {
        if (start != null && end != null) {
            return formatTime(start, locale) + " - " + formatTime(end, locale);
        }
        if (start != null) {
            return getString(R.string.status_shift_time_single, formatTime(start, locale));
        }
        if (!TextUtils.isEmpty(rawStart) && !TextUtils.isEmpty(rawEnd)) {
            return rawStart + " - " + rawEnd;
        }
        if (!TextUtils.isEmpty(rawStart)) {
            return getString(R.string.status_shift_time_single, rawStart);
        }
        if (!TextUtils.isEmpty(rawEnd)) {
            return getString(R.string.status_shift_time_ends, rawEnd);
        }
        return getString(R.string.status_shift_time_placeholder);
    }

    private String formatStatusLabel(@Nullable String status, @NonNull Locale locale) {
        String value;
        if (TextUtils.isEmpty(status)) {
            value = getString(R.string.status_shift_status_unknown_value);
        } else {
            value = capitalizeStatus(status, locale);
        }
        return getString(R.string.status_shift_status, value);
    }

    private String capitalizeStatus(@NonNull String status, @NonNull Locale locale) {
        String normalized = status.trim().replace('_', ' ');
        if (normalized.isEmpty()) {
            return getString(R.string.status_shift_status_unknown_value);
        }
        String[] parts = normalized.split("\\s+");
        StringBuilder builder = new StringBuilder();
        for (String part : parts) {
            if (part.isEmpty()) {
                continue;
            }
            if (builder.length() > 0) {
                builder.append(' ');
            }
            String lower = part.toLowerCase(locale);
            builder.append(Character.toUpperCase(lower.charAt(0)));
            if (lower.length() > 1) {
                builder.append(lower.substring(1));
            }
        }
        return builder.length() > 0 ? builder.toString() : status;
    }

    private String formatTime(@NonNull LocalTime time, @NonNull Locale locale) {
        return time.format(DateTimeFormatter.ofPattern("h:mm a", locale));
    }

    private String formatDuration(@NonNull Duration duration) {
        long totalMinutes = Math.max(duration.toMinutes(), 0);
        long hours = totalMinutes / 60;
        long minutes = totalMinutes % 60;

        if (hours > 0) {
            if (minutes > 0) {
                return getString(
                        R.string.status_shift_duration_hours_minutes,
                        hours,
                        hours == 1 ? "" : "s",
                        minutes,
                        minutes == 1 ? "" : "s"
                );
            }
            return getString(
                    R.string.status_shift_duration_hours_only,
                    hours,
                    hours == 1 ? "" : "s"
            );
        }

        long displayMinutes = Math.max(minutes, 1);
        return getString(
                R.string.status_shift_duration_minutes_only,
                displayMinutes,
                displayMinutes == 1 ? "" : "s"
        );
    }

    private void showToast(@NonNull String message) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
    }

    @Nullable
    private LocalDate parseDate(@Nullable String raw, @NonNull Locale locale) {
        if (TextUtils.isEmpty(raw)) {
            return null;
        }
        String value = raw.trim();
        DateTimeFormatter[] formatters = new DateTimeFormatter[]{
                DateTimeFormatter.ISO_LOCAL_DATE,
                DateTimeFormatter.ofPattern("yyyy/MM/dd", locale),
                DateTimeFormatter.ofPattern("MM/dd/yyyy", locale),
                DateTimeFormatter.ofPattern("M/d/yyyy", locale)
        };
        for (DateTimeFormatter formatter : formatters) {
            try {
                return LocalDate.parse(value, formatter);
            } catch (DateTimeParseException ignored) {
            }
        }
        return null;
    }

    @Nullable
    private LocalTime parseTime(@Nullable String raw, @NonNull Locale locale) {
        if (TextUtils.isEmpty(raw)) {
            return null;
        }
        String value = raw.trim();
        DateTimeFormatter[] formatters = new DateTimeFormatter[]{
                DateTimeFormatter.ISO_LOCAL_TIME,
                DateTimeFormatter.ofPattern("HH:mm", locale),
                DateTimeFormatter.ofPattern("HH:mm:ss", locale),
                DateTimeFormatter.ofPattern("h:mm a", locale)
        };
        for (DateTimeFormatter formatter : formatters) {
            try {
                return LocalTime.parse(value, formatter);
            } catch (DateTimeParseException ignored) {
            }
        }
        return null;
    }

    @Nullable
    private LocalDateTime parseDateTime(@Nullable String raw, @NonNull Locale locale) {
        if (TextUtils.isEmpty(raw)) {
            return null;
        }
        String value = raw.trim();
        DateTimeFormatter[] formatters = new DateTimeFormatter[]{
                DateTimeFormatter.ISO_LOCAL_DATE_TIME,
                DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss", locale),
                DateTimeFormatter.ofPattern("yyyy/MM/dd HH:mm:ss", locale),
                DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss", locale),
                DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSS", locale)
        };
        for (DateTimeFormatter formatter : formatters) {
            try {
                return LocalDateTime.parse(value, formatter);
            } catch (DateTimeParseException ignored) {
            }
        }
        try {
            OffsetDateTime offsetDateTime = OffsetDateTime.parse(value);
            return offsetDateTime.toLocalDateTime();
        } catch (DateTimeParseException ignored) {
            return null;
        }
    }

    private Locale getCurrentLocale() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            return getResources().getConfiguration().getLocales().get(0);
        }
        //noinspection deprecation
        return getResources().getConfiguration().locale;
    }

    private enum StatusState {
        CHECKING,
        CONNECTED,
        ERROR
    }

    private static final class CountdownDisplay {
        final String text;
        final int colorRes;

        CountdownDisplay(String text, int colorRes) {
            this.text = text;
            this.colorRes = colorRes;
        }
    }
}
