package com.example.orders;

import com.example.config.SecurityConfig;
import com.example.customers.domain.Customer;
import com.example.customers.domain.CustomerRepository;
import com.example.orders.domain.Order;
import com.example.orders.domain.OrderItem;
import com.example.orders.domain.OrderRepository;
import com.example.orders.domain.OrderState;
import com.example.orders.domain.PickupLocation;
import com.example.orders.service.OrderService;
import com.example.products.domain.Product;
import com.example.products.domain.ProductRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.security.test.context.support.WithUserDetails;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Integration test for Order security configuration.
 * Tests that Spring Security properly authorizes access to OrderService methods.
 */
@SpringBootTest
@Import(SecurityConfig.class)
@Transactional
class OrderSecurityIntegrationTest {

    @Autowired
    private OrderService orderService;

    @Autowired
    private OrderRepository orderRepository;

    @Autowired
    private CustomerRepository customerRepository;

    @Autowired
    private ProductRepository productRepository;

    private Order testOrder;
    private Customer testCustomer;
    private Product testProduct;

    @BeforeEach
    void setUp() {
        orderRepository.deleteAll();
        customerRepository.deleteAll();
        productRepository.deleteAll();

        // Create test customer
        testCustomer = new Customer();
        testCustomer.setName("Test Customer");
        testCustomer.setPhone("555-1234");
        testCustomer = customerRepository.save(testCustomer);

        // Create test product
        testProduct = new Product();
        testProduct.setName("Test Product");
        testProduct.setPrice(new BigDecimal("10.00"));
        testProduct.setAvailable(true);
        testProduct = productRepository.save(testProduct);

        // Create test order
        testOrder = new Order();
        testOrder.setDueDate(LocalDate.now().plusDays(1));
        testOrder.setCustomer(testCustomer);
        testOrder.setPickupLocation(PickupLocation.STOREFRONT);
        testOrder.setState(OrderState.NEW);

        OrderItem item = new OrderItem();
        item.setProduct(testProduct);
        item.setQuantity(2);
        item.setPricePerUnit(testProduct.getPrice());

        testOrder.setItems(new ArrayList<>(List.of(item)));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void admin_canCreateOrder() {
        Order created = orderService.createOrder(testOrder);

        assertNotNull(created.getId());
        assertEquals(OrderState.NEW, created.getState());
    }

    @Test
    @WithMockUser(roles = "EMPLOYEE")
    void employee_canCreateOrder() {
        Order created = orderService.createOrder(testOrder);

        assertNotNull(created.getId());
        assertEquals(OrderState.NEW, created.getState());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void admin_canViewOrders() {
        orderRepository.save(testOrder);

        List<Order> orders = orderService.findAll();

        assertEquals(1, orders.size());
    }

    @Test
    @WithMockUser(roles = "EMPLOYEE")
    void employee_canViewOrders() {
        orderRepository.save(testOrder);

        List<Order> orders = orderService.findAll();

        assertEquals(1, orders.size());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void admin_canUpdateOrder() {
        Order saved = orderRepository.save(testOrder);
        saved.setNotes("Updated notes");

        Order updated = orderService.updateOrder(saved);

        assertEquals("Updated notes", updated.getNotes());
    }

    @Test
    @WithMockUser(roles = "EMPLOYEE")
    void employee_canUpdateOrder() {
        Order saved = orderRepository.save(testOrder);
        saved.setNotes("Updated notes");

        Order updated = orderService.updateOrder(saved);

        assertEquals("Updated notes", updated.getNotes());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void admin_canMarkOrderReady() {
        Order saved = orderRepository.save(testOrder);

        Order updated = orderService.markReady(saved.getId());

        assertEquals(OrderState.READY, updated.getState());
    }

    @Test
    @WithMockUser(roles = "EMPLOYEE")
    void employee_canMarkOrderReady() {
        Order saved = orderRepository.save(testOrder);

        Order updated = orderService.markReady(saved.getId());

        assertEquals(OrderState.READY, updated.getState());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void admin_canMarkOrderDelivered() {
        testOrder.setState(OrderState.READY);
        Order saved = orderRepository.save(testOrder);

        Order updated = orderService.markDelivered(saved.getId());

        assertEquals(OrderState.DELIVERED, updated.getState());
    }

    @Test
    @WithMockUser(roles = "EMPLOYEE")
    void employee_canMarkOrderDelivered() {
        testOrder.setState(OrderState.READY);
        Order saved = orderRepository.save(testOrder);

        Order updated = orderService.markDelivered(saved.getId());

        assertEquals(OrderState.DELIVERED, updated.getState());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void admin_canCancelOrder() {
        Order saved = orderRepository.save(testOrder);

        Order updated = orderService.cancelOrder(saved.getId());

        assertEquals(OrderState.CANCELLED, updated.getState());
    }

    @Test
    @WithMockUser(roles = "EMPLOYEE")
    void employee_canCancelOrder() {
        Order saved = orderRepository.save(testOrder);

        Order updated = orderService.cancelOrder(saved.getId());

        assertEquals(OrderState.CANCELLED, updated.getState());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void admin_canDeleteOrder() {
        Order saved = orderRepository.save(testOrder);

        orderService.deleteOrder(saved.getId());

        assertFalse(orderRepository.existsById(saved.getId()));
    }

    @Test
    @WithMockUser(roles = "EMPLOYEE")
    void employee_cannotDeleteOrder() {
        Order saved = orderRepository.save(testOrder);

        assertThrows(org.springframework.security.access.AccessDeniedException.class,
                () -> orderService.deleteOrder(saved.getId()));
    }

    @Test
    void noAuthentication_cannotAccessOrders() {
        assertThrows(org.springframework.security.authentication.AuthenticationCredentialsNotFoundException.class,
                () -> orderService.findAll());
    }

    @Test
    @WithUserDetails("admin")
    void realAdminUser_canAccessOrders() {
        orderRepository.save(testOrder);

        // This test uses the actual admin user from the database
        List<Order> orders = orderService.findAll();

        assertEquals(1, orders.size());
    }

    @Test
    @WithMockUser(roles = "EMPLOYEE")
    void employee_canFindByState() {
        orderRepository.save(testOrder);

        List<Order> results = orderService.findByState(OrderState.NEW);

        assertEquals(1, results.size());
    }

    @Test
    @WithMockUser(roles = "EMPLOYEE")
    void employee_canFindByDueDate() {
        orderRepository.save(testOrder);

        List<Order> results = orderService.findByDueDate(testOrder.getDueDate());

        assertEquals(1, results.size());
    }

    @Test
    @WithMockUser(roles = "EMPLOYEE")
    void employee_canFindByCustomerId() {
        orderRepository.save(testOrder);

        List<Order> results = orderService.findByCustomerId(testCustomer.getId());

        assertEquals(1, results.size());
    }

    @Test
    @WithMockUser(roles = "EMPLOYEE")
    void employee_canCountOrders() {
        orderRepository.save(testOrder);

        long count = orderService.count();

        assertEquals(1L, count);
    }

    @Test
    @WithMockUser(roles = "EMPLOYEE")
    void employee_canCountByState() {
        orderRepository.save(testOrder);

        long count = orderService.countByState(OrderState.NEW);

        assertEquals(1L, count);
    }
}
