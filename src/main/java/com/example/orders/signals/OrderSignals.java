package com.example.orders.signals;

import com.example.orders.domain.Order;
import com.example.orders.domain.OrderRepository;
import com.example.orders.domain.OrderState;
import com.vaadin.signals.ListSignal;
import com.vaadin.signals.NumberSignal;
import com.vaadin.signals.Signal;
import com.vaadin.signals.ValueSignal;

import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Central reactive state management for orders using Vaadin Signals.
 * This utility class provides static signals that are shared across all users,
 * enabling real-time updates when orders change.
 */
public class OrderSignals {

    // Shared signals across all users (static fields)
    private static final ListSignal<Order> orders = new ListSignal<>(Order.class);

    private static final NumberSignal todayOrderCount = new NumberSignal();

    private static final NumberSignal newOrderCount = new NumberSignal();

    private static final NumberSignal readyOrderCount = new NumberSignal();

    private static final NumberSignal deliveredOrderCount = new NumberSignal();

    private static final NumberSignal cancelledOrderCount = new NumberSignal();

    // Flag to ensure signals are only initialized once
    private static volatile boolean initialized = false;

    /**
     * Reload all orders from database and update signals.
     * Call this on application startup or when full refresh is needed.
     * This method uses a flag to ensure it only initializes once (unless forceRefresh is true).
     */
    public static void refreshAll(OrderRepository repository) {
        refreshAll(repository, false);
    }

    /**
     * Reload all orders from database and update signals.
     * @param repository The order repository
     * @param forceRefresh If true, refresh even if already initialized
     */
    public static synchronized void refreshAll(OrderRepository repository, boolean forceRefresh) {
        // Only initialize once unless forced
        if (initialized && !forceRefresh) {
            return;
        }

        List<Order> allOrders = repository.findAll();

        // Rebuild the entire list
        rebuildOrderList(allOrders);
        updateDashboardStats();

        initialized = true;
    }

    /**
     * Update a single order in the signal list.
     * Call this after updating an order (e.g., state transition).
     */
    public static void refreshOrder(OrderRepository repository, Long orderId) {
        if (orderId == null) {
            return;
        }

        Order updated = repository.findById(orderId).orElse(null);
        if (updated != null) {
            // Find and replace the order in the list
            List<ValueSignal<Order>> signalList = orders.value();
            for (ValueSignal<Order> orderSignal : signalList) {
                Long existingId = orderSignal.value().getId();
                if (existingId != null && existingId.equals(orderId)) {
                    orderSignal.value(updated);
                    updateDashboardStats();
                    return;
                }
            }
            // Order not found in list, add it
            addOrder(updated);
        } else {
            // Order was deleted, remove from list
            removeOrder(orderId);
        }
    }

    /**
     * Add a new order to the signal list.
     * Call this after creating a new order.
     */
    public static void addOrder(Order order) {
        orders.insertLast(order);
        updateDashboardStats();
    }

    /**
     * Remove an order from the signal list.
     * Call this after deleting an order.
     */
    public static void removeOrder(Long orderId) {
        if (orderId == null) {
            return;
        }

        // Get current orders, filter out the deleted one, and rebuild
        List<ValueSignal<Order>> signalList = orders.value();
        List<Order> remaining = signalList.stream()
                .map(ValueSignal::value)
                .filter(order -> order.getId() != null && !order.getId().equals(orderId))
                .collect(Collectors.toList());

        rebuildOrderList(remaining);
        updateDashboardStats();
    }

    /**
     * Helper method to rebuild the entire order list from scratch.
     * Clears existing orders and adds new ones.
     */
    private static void rebuildOrderList(List<Order> newOrders) {
        // First, clear all existing orders by removing them one by one
        List<ValueSignal<Order>> currentSignals = orders.value();
        for (ValueSignal<Order> signal : currentSignals) {
            orders.remove(signal);
        }

        // Now add all new orders
        for (Order order : newOrders) {
            orders.insertLast(order);
        }
    }

    /**
     * Recalculate dashboard statistics from the current order list.
     * Called automatically by refresh methods.
     */
    private static void updateDashboardStats() {
        List<ValueSignal<Order>> signalList = orders.value();
        LocalDate today = LocalDate.now();

        // Convert to regular list for counting
        List<Order> currentOrders = signalList.stream()
                .map(ValueSignal::value)
                .collect(Collectors.toList());

        // Count orders by state
        long newCount = currentOrders.stream()
                .filter(o -> o.getState() == OrderState.NEW)
                .count();

        long readyCount = currentOrders.stream()
                .filter(o -> o.getState() == OrderState.READY)
                .count();

        long deliveredCount = currentOrders.stream()
                .filter(o -> o.getState() == OrderState.DELIVERED)
                .count();

        long cancelledCount = currentOrders.stream()
                .filter(o -> o.getState() == OrderState.CANCELLED)
                .count();

        // Count today's orders (by due date)
        long todayCount = currentOrders.stream()
                .filter(o -> o.getDueDate().equals(today))
                .count();

        // Update signals
        newOrderCount.value((double) newCount);
        readyOrderCount.value((double) readyCount);
        deliveredOrderCount.value((double) deliveredCount);
        cancelledOrderCount.value((double) cancelledCount);
        todayOrderCount.value((double) todayCount);
    }

    // Getter methods returning read-only signals for UI binding

    public static ListSignal<Order> getOrdersSignal() {
        return orders.asReadonly();
    }

    public static NumberSignal getTodayOrderCountSignal() {
        return todayOrderCount.asReadonly();
    }

    public static NumberSignal getNewOrderCountSignal() {
        return newOrderCount.asReadonly();
    }

    public static NumberSignal getReadyOrderCountSignal() {
        return readyOrderCount.asReadonly();
    }

    public static NumberSignal getDeliveredOrderCountSignal() {
        return deliveredOrderCount.asReadonly();
    }

    public static NumberSignal getCancelledOrderCountSignal() {
        return cancelledOrderCount.asReadonly();
    }

    /**
     * Reset the initialization flag and clear all data.
     * Used primarily for testing to reset state between tests.
     */
    public static synchronized void reset() {
        initialized = false;
        rebuildOrderList(List.of()); // Clear all orders
    }
}
