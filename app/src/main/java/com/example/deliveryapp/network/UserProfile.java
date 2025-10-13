package com.example.deliveryapp.network;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

/**
 * Immutable representation of the driver profile returned by the Cindy's Bakeshop user API.
 */
public class UserProfile {

    private final int userId;
    @Nullable
    private final String email;
    @Nullable
    private final String name;
    @Nullable
    private final String address;

    public UserProfile(int userId, @Nullable String email, @Nullable String name, @Nullable String address) {
        this.userId = userId;
        this.email = email;
        this.name = name;
        this.address = address;
    }

    public int getUserId() {
        return userId;
    }

    @Nullable
    public String getEmail() {
        return email;
    }

    @Nullable
    public String getName() {
        return name;
    }

    @Nullable
    public String getAddress() {
        return address;
    }

    @NonNull
    @Override
    public String toString() {
        return "UserProfile{" +
                "userId=" + userId +
                ", email='" + email + '\'' +
                ", name='" + name + '\'' +
                ", address='" + address + '\'' +
                '}';
    }
}
