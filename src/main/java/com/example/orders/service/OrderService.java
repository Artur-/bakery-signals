package com.example.orders.service;

import com.example.orders.domain.Order;
import com.example.orders.domain.OrderRepository;
import com.example.orders.domain.OrderState;
import com.example.orders.signals.OrderSignals;
import jakarta.annotation.security.RolesAllowed;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Service
@Transactional
public class OrderService {

    private final OrderRepository orderRepository;

    public OrderService(OrderRepository orderRepository) {
        this.orderRepository = orderRepository;
    }

    @RolesAllowed({"ADMIN", "EMPLOYEE"})
    public Order createOrder(Order order) {
        if (order.getId() != null) {
            throw new IllegalArgumentException("New order should not have an ID");
        }
        order.recalculateTotalPrice();
        Order saved = orderRepository.save(order);
        OrderSignals.addOrder(saved);
        return saved;
    }

    @RolesAllowed({"ADMIN", "EMPLOYEE"})
    public List<Order> findAll() {
        return orderRepository.findAll();
    }

    @RolesAllowed({"ADMIN", "EMPLOYEE"})
    public List<Order> findByState(OrderState state) {
        return orderRepository.findByState(state);
    }

    @RolesAllowed({"ADMIN", "EMPLOYEE"})
    public List<Order> findByDueDate(LocalDate dueDate) {
        return orderRepository.findByDueDate(dueDate);
    }

    @RolesAllowed({"ADMIN", "EMPLOYEE"})
    public List<Order> findByCustomerId(Long customerId) {
        return orderRepository.findByCustomerId(customerId);
    }

    @RolesAllowed({"ADMIN", "EMPLOYEE"})
    public Optional<Order> findById(Long id) {
        return orderRepository.findById(id);
    }

    @RolesAllowed({"ADMIN", "EMPLOYEE"})
    public Order updateOrder(Order order) {
        if (order.getId() == null) {
            throw new IllegalArgumentException("Order ID is required for update");
        }
        if (!orderRepository.existsById(order.getId())) {
            throw new IllegalArgumentException("Order not found with ID: " + order.getId());
        }
        order.recalculateTotalPrice();
        Order saved = orderRepository.save(order);
        OrderSignals.refreshOrder(orderRepository, saved.getId());
        return saved;
    }

    @RolesAllowed({"ADMIN", "EMPLOYEE"})
    public Order markReady(Long orderId) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new IllegalArgumentException("Order not found with ID: " + orderId));
        order.markReady();
        Order saved = orderRepository.save(order);
        OrderSignals.refreshOrder(orderRepository, saved.getId());
        return saved;
    }

    @RolesAllowed({"ADMIN", "EMPLOYEE"})
    public Order markDelivered(Long orderId) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new IllegalArgumentException("Order not found with ID: " + orderId));
        order.markDelivered();
        Order saved = orderRepository.save(order);
        OrderSignals.refreshOrder(orderRepository, saved.getId());
        return saved;
    }

    @RolesAllowed({"ADMIN", "EMPLOYEE"})
    public Order cancelOrder(Long orderId) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new IllegalArgumentException("Order not found with ID: " + orderId));
        order.cancel();
        Order saved = orderRepository.save(order);
        OrderSignals.refreshOrder(orderRepository, saved.getId());
        return saved;
    }

    @RolesAllowed("ADMIN")
    public void deleteOrder(Long id) {
        if (!orderRepository.existsById(id)) {
            throw new IllegalArgumentException("Order not found with ID: " + id);
        }
        orderRepository.deleteById(id);
        OrderSignals.removeOrder(id);
    }

    @RolesAllowed({"ADMIN", "EMPLOYEE"})
    public long count() {
        return orderRepository.count();
    }

    @RolesAllowed({"ADMIN", "EMPLOYEE"})
    public long countByState(OrderState state) {
        return orderRepository.countByState(state);
    }
}
