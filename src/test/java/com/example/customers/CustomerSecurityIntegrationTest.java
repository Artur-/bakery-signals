package com.example.customers;

import com.example.config.SecurityConfig;
import com.example.customers.domain.Customer;
import com.example.customers.domain.CustomerRepository;
import com.example.customers.service.CustomerService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.security.test.context.support.WithUserDetails;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Integration test for Customer security configuration.
 * Tests that Spring Security properly authorizes access to CustomerService methods.
 */
@SpringBootTest
@Import(SecurityConfig.class)
@Transactional
class CustomerSecurityIntegrationTest {

    @Autowired
    private CustomerService customerService;

    @Autowired
    private CustomerRepository customerRepository;

    private Customer testCustomer;

    @BeforeEach
    void setUp() {
        customerRepository.deleteAll();

        testCustomer = new Customer();
        testCustomer.setName("Test Customer");
        testCustomer.setPhone("555-1234");
        testCustomer.setEmail("test@example.com");
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void admin_canCreateCustomer() {
        Customer created = customerService.createCustomer(testCustomer);

        assertNotNull(created.getId());
        assertEquals("Test Customer", created.getName());
    }

    @Test
    @WithMockUser(roles = "EMPLOYEE")
    void employee_cannotCreateCustomer() {
        assertThrows(org.springframework.security.access.AccessDeniedException.class,
                () -> customerService.createCustomer(testCustomer));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void admin_canViewCustomers() {
        customerRepository.save(testCustomer);

        List<Customer> customers = customerService.findAll();

        assertEquals(1, customers.size());
    }

    @Test
    @WithMockUser(roles = "EMPLOYEE")
    void employee_canViewCustomers() {
        customerRepository.save(testCustomer);

        List<Customer> customers = customerService.findAll();

        assertEquals(1, customers.size());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void admin_canUpdateCustomer() {
        Customer saved = customerRepository.save(testCustomer);
        saved.setName("Updated Name");

        Customer updated = customerService.updateCustomer(saved);

        assertEquals("Updated Name", updated.getName());
    }

    @Test
    @WithMockUser(roles = "EMPLOYEE")
    void employee_cannotUpdateCustomer() {
        Customer saved = customerRepository.save(testCustomer);
        saved.setName("Updated Name");

        assertThrows(org.springframework.security.access.AccessDeniedException.class,
                () -> customerService.updateCustomer(saved));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void admin_canDeleteCustomer() {
        Customer saved = customerRepository.save(testCustomer);

        customerService.deleteCustomer(saved.getId());

        assertFalse(customerRepository.existsById(saved.getId()));
    }

    @Test
    @WithMockUser(roles = "EMPLOYEE")
    void employee_cannotDeleteCustomer() {
        Customer saved = customerRepository.save(testCustomer);

        assertThrows(org.springframework.security.access.AccessDeniedException.class,
                () -> customerService.deleteCustomer(saved.getId()));
    }

    @Test
    void noAuthentication_cannotAccessCustomers() {
        assertThrows(org.springframework.security.authentication.AuthenticationCredentialsNotFoundException.class,
                () -> customerService.findAll());
    }

    @Test
    @WithUserDetails("admin")
    void realAdminUser_canAccessCustomers() {
        customerRepository.save(testCustomer);

        // This test uses the actual admin user from the database
        List<Customer> customers = customerService.findAll();

        assertEquals(1, customers.size());
    }

    @Test
    @WithMockUser(roles = "EMPLOYEE")
    void employee_canSearchByName() {
        customerRepository.save(testCustomer);

        List<Customer> results = customerService.searchByName("Test");

        assertEquals(1, results.size());
    }

    @Test
    @WithMockUser(roles = "EMPLOYEE")
    void employee_canSearchByPhone() {
        customerRepository.save(testCustomer);

        List<Customer> results = customerService.searchByPhone("555");

        assertEquals(1, results.size());
    }

    @Test
    @WithMockUser(roles = "EMPLOYEE")
    void employee_canSearchByEmail() {
        customerRepository.save(testCustomer);

        List<Customer> results = customerService.searchByEmail("test");

        assertEquals(1, results.size());
    }
}
