package com.example.orders;

import com.example.customers.domain.Customer;
import com.example.orders.domain.Order;
import com.example.orders.domain.OrderItem;
import com.example.orders.domain.OrderRepository;
import com.example.orders.domain.OrderState;
import com.example.orders.domain.PickupLocation;
import com.example.orders.service.OrderService;
import com.example.products.domain.Product;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Unit tests for OrderService using Mockito.
 * Tests business logic without Spring context or database.
 */
@ExtendWith(MockitoExtension.class)
class OrderServiceTest {

    @Mock
    private OrderRepository orderRepository;

    @InjectMocks
    private OrderService orderService;

    private Order testOrder;
    private Customer testCustomer;
    private Product testProduct;
    private OrderItem testItem;

    @BeforeEach
    void setUp() {
        testCustomer = new Customer();
        testCustomer.setId(1L);
        testCustomer.setName("Test Customer");

        testProduct = new Product();
        testProduct.setId(1L);
        testProduct.setName("Test Product");
        testProduct.setPrice(new BigDecimal("10.00"));

        testItem = new OrderItem();
        testItem.setProduct(testProduct);
        testItem.setQuantity(2);
        testItem.setPricePerUnit(testProduct.getPrice());

        testOrder = new Order();
        testOrder.setDueDate(LocalDate.now().plusDays(1));
        testOrder.setCustomer(testCustomer);
        testOrder.setPickupLocation(PickupLocation.STOREFRONT);
        testOrder.setItems(new ArrayList<>(List.of(testItem)));
        testOrder.setState(OrderState.NEW);
    }

    @Test
    void createOrder_withValidData_succeeds() {
        when(orderRepository.save(any(Order.class))).thenReturn(testOrder);

        Order created = orderService.createOrder(testOrder);

        assertNotNull(created);
        verify(orderRepository).save(testOrder);
    }

    @Test
    void createOrder_withExistingId_throwsException() {
        testOrder.setId(1L);

        assertThrows(IllegalArgumentException.class,
                () -> orderService.createOrder(testOrder));

        verify(orderRepository, never()).save(any());
    }

    @Test
    void createOrder_calculatesTotalPrice() {
        when(orderRepository.save(any(Order.class))).thenAnswer(invocation -> {
            Order order = invocation.getArgument(0);
            assertNotNull(order.getTotalPrice());
            assertEquals(new BigDecimal("20.00"), order.getTotalPrice());
            return order;
        });

        orderService.createOrder(testOrder);

        verify(orderRepository).save(testOrder);
    }

    @Test
    void findAll_returnsAllOrders() {
        List<Order> orders = List.of(testOrder);
        when(orderRepository.findAll()).thenReturn(orders);

        List<Order> result = orderService.findAll();

        assertEquals(1, result.size());
        verify(orderRepository).findAll();
    }

    @Test
    void findById_withExistingId_returnsOrder() {
        testOrder.setId(1L);
        when(orderRepository.findById(1L)).thenReturn(Optional.of(testOrder));

        Optional<Order> result = orderService.findById(1L);

        assertTrue(result.isPresent());
        assertEquals(testOrder, result.get());
        verify(orderRepository).findById(1L);
    }

    @Test
    void findById_withNonExistingId_returnsEmpty() {
        when(orderRepository.findById(999L)).thenReturn(Optional.empty());

        Optional<Order> result = orderService.findById(999L);

        assertFalse(result.isPresent());
        verify(orderRepository).findById(999L);
    }

    @Test
    void findByState_returnsOrdersWithState() {
        List<Order> orders = List.of(testOrder);
        when(orderRepository.findByState(OrderState.NEW)).thenReturn(orders);

        List<Order> result = orderService.findByState(OrderState.NEW);

        assertEquals(1, result.size());
        assertEquals(OrderState.NEW, result.get(0).getState());
        verify(orderRepository).findByState(OrderState.NEW);
    }

    @Test
    void findByDueDate_returnsOrdersWithDueDate() {
        LocalDate dueDate = LocalDate.now().plusDays(1);
        List<Order> orders = List.of(testOrder);
        when(orderRepository.findByDueDate(dueDate)).thenReturn(orders);

        List<Order> result = orderService.findByDueDate(dueDate);

        assertEquals(1, result.size());
        assertEquals(dueDate, result.get(0).getDueDate());
        verify(orderRepository).findByDueDate(dueDate);
    }

    @Test
    void findByCustomerId_returnsOrdersForCustomer() {
        List<Order> orders = List.of(testOrder);
        when(orderRepository.findByCustomerId(1L)).thenReturn(orders);

        List<Order> result = orderService.findByCustomerId(1L);

        assertEquals(1, result.size());
        assertEquals(1L, result.get(0).getCustomer().getId());
        verify(orderRepository).findByCustomerId(1L);
    }

    @Test
    void updateOrder_withValidData_succeeds() {
        testOrder.setId(1L);
        when(orderRepository.existsById(1L)).thenReturn(true);
        when(orderRepository.save(any(Order.class))).thenReturn(testOrder);

        Order updated = orderService.updateOrder(testOrder);

        assertNotNull(updated);
        verify(orderRepository).existsById(1L);
        verify(orderRepository).save(testOrder);
    }

    @Test
    void updateOrder_withoutId_throwsException() {
        assertThrows(IllegalArgumentException.class,
                () -> orderService.updateOrder(testOrder));

        verify(orderRepository, never()).save(any());
    }

    @Test
    void updateOrder_withNonExistingId_throwsException() {
        testOrder.setId(999L);
        when(orderRepository.existsById(999L)).thenReturn(false);

        assertThrows(IllegalArgumentException.class,
                () -> orderService.updateOrder(testOrder));

        verify(orderRepository, never()).save(any());
    }

    @Test
    void markReady_changesStateFromNewToReady() {
        testOrder.setId(1L);
        testOrder.setState(OrderState.NEW);
        when(orderRepository.findById(1L)).thenReturn(Optional.of(testOrder));
        when(orderRepository.save(any(Order.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Order result = orderService.markReady(1L);

        assertEquals(OrderState.READY, result.getState());
        verify(orderRepository, atLeastOnce()).findById(1L); // Called by service and signals
        verify(orderRepository).save(testOrder);
    }

    @Test
    void markReady_withNonExistingOrder_throwsException() {
        when(orderRepository.findById(999L)).thenReturn(Optional.empty());

        assertThrows(IllegalArgumentException.class,
                () -> orderService.markReady(999L));

        verify(orderRepository, never()).save(any());
    }

    @Test
    void markDelivered_changesStateFromReadyToDelivered() {
        testOrder.setId(1L);
        testOrder.setState(OrderState.READY);
        when(orderRepository.findById(1L)).thenReturn(Optional.of(testOrder));
        when(orderRepository.save(any(Order.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Order result = orderService.markDelivered(1L);

        assertEquals(OrderState.DELIVERED, result.getState());
        verify(orderRepository, atLeastOnce()).findById(1L); // Called by service and signals
        verify(orderRepository).save(testOrder);
    }

    @Test
    void markDelivered_withNonExistingOrder_throwsException() {
        when(orderRepository.findById(999L)).thenReturn(Optional.empty());

        assertThrows(IllegalArgumentException.class,
                () -> orderService.markDelivered(999L));

        verify(orderRepository, never()).save(any());
    }

    @Test
    void cancelOrder_changesStateToCancelled() {
        testOrder.setId(1L);
        testOrder.setState(OrderState.NEW);
        when(orderRepository.findById(1L)).thenReturn(Optional.of(testOrder));
        when(orderRepository.save(any(Order.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Order result = orderService.cancelOrder(1L);

        assertEquals(OrderState.CANCELLED, result.getState());
        verify(orderRepository, atLeastOnce()).findById(1L); // Called by service and signals
        verify(orderRepository).save(testOrder);
    }

    @Test
    void cancelOrder_withNonExistingOrder_throwsException() {
        when(orderRepository.findById(999L)).thenReturn(Optional.empty());

        assertThrows(IllegalArgumentException.class,
                () -> orderService.cancelOrder(999L));

        verify(orderRepository, never()).save(any());
    }

    @Test
    void deleteOrder_withExistingOrder_succeeds() {
        when(orderRepository.existsById(1L)).thenReturn(true);

        orderService.deleteOrder(1L);

        verify(orderRepository).existsById(1L);
        verify(orderRepository).deleteById(1L);
    }

    @Test
    void deleteOrder_withNonExistingOrder_throwsException() {
        when(orderRepository.existsById(999L)).thenReturn(false);

        assertThrows(IllegalArgumentException.class,
                () -> orderService.deleteOrder(999L));

        verify(orderRepository, never()).deleteById(any());
    }

    @Test
    void count_returnsOrderCount() {
        when(orderRepository.count()).thenReturn(5L);

        long result = orderService.count();

        assertEquals(5L, result);
        verify(orderRepository).count();
    }

    @Test
    void countByState_returnsCountForState() {
        when(orderRepository.countByState(OrderState.NEW)).thenReturn(3L);

        long result = orderService.countByState(OrderState.NEW);

        assertEquals(3L, result);
        verify(orderRepository).countByState(OrderState.NEW);
    }
}
