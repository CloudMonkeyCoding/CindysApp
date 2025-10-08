package com.example.deliveryapp;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;
import com.google.firebase.auth.FirebaseAuth;

public class LoginActivity extends AppCompatActivity {

    private TextInputEditText etEmail;
    private TextInputEditText etPassword;
    private MaterialButton btnLogin;
    private ProgressBar progressBar;
    private FirebaseAuth firebaseAuth;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        etEmail = findViewById(R.id.etEmail);
        etPassword = findViewById(R.id.etPassword);
        btnLogin = findViewById(R.id.btnLogin);
        progressBar = findViewById(R.id.loginProgress);

        initializeFirebase();
        setupListeners();
    }

    private void initializeFirebase() {
        if (FirebaseApp.getApps(this).isEmpty()) {
            FirebaseOptions options = new FirebaseOptions.Builder()
                    .setProjectId("cindys-bakeshop")
                    .setApplicationId("1:958275560878:web:3731b0ba27925db3f5543f")
                    .setApiKey("AIzaSyBQ9XuHo1kPCppj5M-NaCsiwZ2zlxajCe8")
                    .setStorageBucket("cindys-bakeshop.appspot.com")
                    .setGcmSenderId("958275560878")
                    .build();

            FirebaseApp.initializeApp(this, options);
        }

        firebaseAuth = FirebaseAuth.getInstance();
    }

    private void setupListeners() {
        btnLogin.setOnClickListener(v -> attemptLogin());
    }

    private void attemptLogin() {
        String email = etEmail.getText() != null ? etEmail.getText().toString().trim() : "";
        String password = etPassword.getText() != null ? etPassword.getText().toString().trim() : "";

        if (TextUtils.isEmpty(email)) {
            etEmail.setError("Email is required");
            etEmail.requestFocus();
            return;
        }

        if (TextUtils.isEmpty(password)) {
            etPassword.setError("Password is required");
            etPassword.requestFocus();
            return;
        }

        toggleLoading(true);

        firebaseAuth.signInWithEmailAndPassword(email, password)
                .addOnCompleteListener(this, task -> {
                    toggleLoading(false);

                    if (task.isSuccessful()) {
                        Toast.makeText(this, "Login successful", Toast.LENGTH_SHORT).show();
                        Intent intent = new Intent(this, StatusActivity.class);
                        startActivity(intent);
                        finish();
                    } else {
                        String message = task.getException() != null
                                ? task.getException().getMessage()
                                : "Login failed";
                        Toast.makeText(this, message, Toast.LENGTH_LONG).show();
                    }
                });
    }

    private void toggleLoading(boolean isLoading) {
        progressBar.setVisibility(isLoading ? View.VISIBLE : View.GONE);
        btnLogin.setEnabled(!isLoading);
    }
}
