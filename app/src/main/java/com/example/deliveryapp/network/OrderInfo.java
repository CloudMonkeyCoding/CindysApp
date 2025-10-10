package com.example.deliveryapp.network;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

/**
 * Represents a delivery order entry returned by the Cindy's Bakeshop APIs.
 */
public class OrderInfo {

    private final int orderId;
    private final int userId;
    @Nullable
    private final String status;
    @Nullable
    private final String orderDate;
    @Nullable
    private final String fulfillmentType;
    @Nullable
    private final String source;
    private final int itemCount;
    private final double totalAmount;
    @Nullable
    private final String itemSummary;
    @Nullable
    private final String imageUrl;

    public OrderInfo(
            int orderId,
            int userId,
            @Nullable String status,
            @Nullable String orderDate,
            @Nullable String fulfillmentType,
            @Nullable String source,
            int itemCount,
            double totalAmount,
            @Nullable String itemSummary,
            @Nullable String imageUrl
    ) {
        this.orderId = orderId;
        this.userId = userId;
        this.status = status;
        this.orderDate = orderDate;
        this.fulfillmentType = fulfillmentType;
        this.source = source;
        this.itemCount = itemCount;
        this.totalAmount = totalAmount;
        this.itemSummary = itemSummary;
        this.imageUrl = imageUrl;
    }

    public int getOrderId() {
        return orderId;
    }

    public int getUserId() {
        return userId;
    }

    @Nullable
    public String getStatus() {
        return status;
    }

    @Nullable
    public String getOrderDate() {
        return orderDate;
    }

    @Nullable
    public String getFulfillmentType() {
        return fulfillmentType;
    }

    @Nullable
    public String getSource() {
        return source;
    }

    public int getItemCount() {
        return itemCount;
    }

    public double getTotalAmount() {
        return totalAmount;
    }

    @Nullable
    public String getItemSummary() {
        return itemSummary;
    }

    @Nullable
    public String getImageUrl() {
        return imageUrl;
    }

    @NonNull
    @Override
    public String toString() {
        return "OrderInfo{" +
                "orderId=" + orderId +
                ", userId=" + userId +
                ", status='" + status + '\'' +
                ", orderDate='" + orderDate + '\'' +
                ", fulfillmentType='" + fulfillmentType + '\'' +
                ", source='" + source + '\'' +
                ", itemCount=" + itemCount +
                ", totalAmount=" + totalAmount +
                ", itemSummary='" + itemSummary + '\'' +
                ", imageUrl='" + imageUrl + '\'' +
                '}';
    }
}
