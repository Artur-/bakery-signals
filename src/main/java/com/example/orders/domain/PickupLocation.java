package com.example.orders.domain;

public enum PickupLocation {
    STOREFRONT("Storefront"),
    PRODUCTION_FACILITY("Production Facility");

    private final String displayName;

    PickupLocation(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }
}
