package com.example.orders.domain;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

@Repository
public interface OrderRepository extends JpaRepository<Order, Long> {

    List<Order> findByState(OrderState state);

    List<Order> findByDueDate(LocalDate dueDate);

    List<Order> findByDueDateBetween(LocalDate startDate, LocalDate endDate);

    @Query("SELECT o FROM Order o WHERE o.customer.id = :customerId")
    List<Order> findByCustomerId(@Param("customerId") Long customerId);

    @Query("SELECT o FROM Order o WHERE o.customer.name LIKE %:customerName%")
    List<Order> findByCustomerNameContaining(@Param("customerName") String customerName);

    @Query("SELECT o FROM Order o WHERE o.dueDate = :date ORDER BY o.createdAt DESC")
    List<Order> findTodaysOrders(@Param("date") LocalDate date);

    long countByState(OrderState state);

    long countByDueDate(LocalDate dueDate);
}
