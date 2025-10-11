package com.example.deliveryapp;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;

import com.example.deliveryapp.network.UserService;

public class LoginActivity extends AppCompatActivity {

    private TextInputEditText etEmail;
    private TextInputEditText etPassword;
    private MaterialButton btnLogin;
    private ProgressBar progressBar;
    private final UserService userService = new UserService();

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (SessionManager.hasActiveSession(this)) {
            navigateToStatus();
            finish();
            return;
        }
        setContentView(R.layout.activity_login);

        etEmail = findViewById(R.id.etEmail);
        etPassword = findViewById(R.id.etPassword);
        btnLogin = findViewById(R.id.btnLogin);
        progressBar = findViewById(R.id.loginProgress);

        String savedEmail = SessionManager.getEmail(this);
        if (!TextUtils.isEmpty(savedEmail) && etEmail != null) {
            etEmail.setText(savedEmail);
        }

        setupListeners();
    }

    private void setupListeners() {
        btnLogin.setOnClickListener(v -> attemptLogin());
    }

    private void attemptLogin() {
        String email = etEmail.getText() != null ? etEmail.getText().toString().trim() : "";

        if (TextUtils.isEmpty(email)) {
            etEmail.setError("Email is required");
            etEmail.requestFocus();
            return;
        }

        toggleLoading(true);

        userService.fetchUserIdByEmail(email, new UserService.UserIdCallback() {
            @Override
            public void onSuccess(int userId) {
                toggleLoading(false);
                SessionManager.storeSession(getApplicationContext(), email, userId);
                Toast.makeText(LoginActivity.this, "Login successful", Toast.LENGTH_SHORT).show();
                navigateToStatus();
                finish();
            }

            @Override
            public void onError(@NonNull String errorMessage) {
                toggleLoading(false);
                String message = !TextUtils.isEmpty(errorMessage) ? errorMessage : "Login failed";
                Toast.makeText(LoginActivity.this, message, Toast.LENGTH_LONG).show();
            }
        });
    }

    private void toggleLoading(boolean isLoading) {
        progressBar.setVisibility(isLoading ? View.VISIBLE : View.GONE);
        btnLogin.setEnabled(!isLoading);
        if (etEmail != null) {
            etEmail.setEnabled(!isLoading);
        }
        if (etPassword != null) {
            etPassword.setEnabled(!isLoading);
        }
    }

    private void navigateToStatus() {
        Intent intent = new Intent(this, StatusActivity.class);
        startActivity(intent);
    }
}
