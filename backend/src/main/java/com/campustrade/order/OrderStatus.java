package com.campustrade.order;

public enum OrderStatus {
    PENDING_PAYMENT,
    PAID,
    SHIPPED,
    COMPLETED,
    CANCELED;

    public boolean canTransitionTo(OrderStatus target) {
        return switch (this) {
            case PENDING_PAYMENT -> target == PAID || target == CANCELED;
            case PAID -> target == SHIPPED || target == CANCELED;
            case SHIPPED -> target == COMPLETED;
            case COMPLETED, CANCELED -> false;
        };
    }
}
