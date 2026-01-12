package com.example.orders.domain;

import com.example.customers.domain.Customer;
import com.example.security.domain.User;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

@Entity
@Table(name = "orders")
public class Order {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotNull
    @Column(name = "due_date", nullable = false)
    private LocalDate dueDate;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 50)
    private OrderState state = OrderState.NEW;

    @NotNull
    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "customer_id", nullable = false)
    private Customer customer;

    @OneToMany(cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.EAGER)
    @JoinColumn(name = "order_id")
    private List<OrderItem> items = new ArrayList<>();

    @Column(name = "total_price", nullable = false, precision = 10, scale = 2)
    private BigDecimal totalPrice = BigDecimal.ZERO;

    @Column(precision = 10, scale = 2)
    private BigDecimal discount = BigDecimal.ZERO;

    @Column(nullable = false)
    private boolean paid = false;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(name = "pickup_location", nullable = false, length = 50)
    private PickupLocation pickupLocation;

    @Column(columnDefinition = "TEXT")
    private String notes;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "created_by_user_id")
    private User createdBy;

    @Column(name = "state_changed_at", nullable = false)
    private LocalDateTime stateChangedAt = LocalDateTime.now();

    @Version
    private Long version;

    public Order() {
    }

    public Order(LocalDate dueDate, Customer customer, PickupLocation pickupLocation) {
        this.dueDate = dueDate;
        this.customer = customer;
        this.pickupLocation = pickupLocation;
    }

    // Business methods for state transitions

    public void markReady() {
        if (state != OrderState.NEW) {
            throw new IllegalStateException("Can only mark NEW orders as READY");
        }
        state = OrderState.READY;
        stateChangedAt = LocalDateTime.now();
    }

    public void markDelivered() {
        if (state != OrderState.READY) {
            throw new IllegalStateException("Can only mark READY orders as DELIVERED");
        }
        state = OrderState.DELIVERED;
        stateChangedAt = LocalDateTime.now();
    }

    public void cancel() {
        if (state == OrderState.DELIVERED) {
            throw new IllegalStateException("Cannot cancel DELIVERED orders");
        }
        state = OrderState.CANCELLED;
        stateChangedAt = LocalDateTime.now();
    }

    // Business method to add items
    public void addItem(OrderItem item) {
        items.add(item);
        recalculateTotalPrice();
    }

    public void removeItem(OrderItem item) {
        items.remove(item);
        recalculateTotalPrice();
    }

    public void recalculateTotalPrice() {
        BigDecimal subtotal = items.stream()
                .map(OrderItem::calculateSubtotal)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        totalPrice = subtotal.subtract(discount != null ? discount : BigDecimal.ZERO);
        if (totalPrice.compareTo(BigDecimal.ZERO) < 0) {
            totalPrice = BigDecimal.ZERO;
        }
    }

    // Getters and setters

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public LocalDate getDueDate() {
        return dueDate;
    }

    public void setDueDate(LocalDate dueDate) {
        this.dueDate = dueDate;
    }

    public OrderState getState() {
        return state;
    }

    public void setState(OrderState state) {
        this.state = state;
    }

    public Customer getCustomer() {
        return customer;
    }

    public void setCustomer(Customer customer) {
        this.customer = customer;
    }

    public List<OrderItem> getItems() {
        return items;
    }

    public void setItems(List<OrderItem> items) {
        this.items = items;
        recalculateTotalPrice();
    }

    public BigDecimal getTotalPrice() {
        return totalPrice;
    }

    public void setTotalPrice(BigDecimal totalPrice) {
        this.totalPrice = totalPrice;
    }

    public BigDecimal getDiscount() {
        return discount;
    }

    public void setDiscount(BigDecimal discount) {
        this.discount = discount;
        recalculateTotalPrice();
    }

    public boolean isPaid() {
        return paid;
    }

    public void setPaid(boolean paid) {
        this.paid = paid;
    }

    public PickupLocation getPickupLocation() {
        return pickupLocation;
    }

    public void setPickupLocation(PickupLocation pickupLocation) {
        this.pickupLocation = pickupLocation;
    }

    public String getNotes() {
        return notes;
    }

    public void setNotes(String notes) {
        this.notes = notes;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public User getCreatedBy() {
        return createdBy;
    }

    public void setCreatedBy(User createdBy) {
        this.createdBy = createdBy;
    }

    public LocalDateTime getStateChangedAt() {
        return stateChangedAt;
    }

    public void setStateChangedAt(LocalDateTime stateChangedAt) {
        this.stateChangedAt = stateChangedAt;
    }

    public Long getVersion() {
        return version;
    }

    public void setVersion(Long version) {
        this.version = version;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Order order = (Order) o;
        return Objects.equals(id, order.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }

    @Override
    public String toString() {
        return "Order{" +
                "id=" + id +
                ", dueDate=" + dueDate +
                ", state=" + state +
                ", customer=" + (customer != null ? customer.getName() : "null") +
                ", totalPrice=" + totalPrice +
                '}';
    }
}
