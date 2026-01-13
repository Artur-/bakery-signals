package com.example.customers.service;

import com.example.customers.domain.Customer;
import com.example.customers.domain.CustomerRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Service layer tests for CustomerService.
 * Tests business logic and data operations using mocked repository.
 * Security authorization is tested separately in integration tests.
 */
@ExtendWith(MockitoExtension.class)
class CustomerServiceTest {

    @Mock
    private CustomerRepository customerRepository;

    @InjectMocks
    private CustomerService customerService;

    private Customer testCustomer;

    @BeforeEach
    void setUp() {
        testCustomer = new Customer();
        testCustomer.setName("Test Customer");
        testCustomer.setPhone("555-1234");
        testCustomer.setEmail("test@example.com");
        testCustomer.setBillingInfo("123 Main St");
        testCustomer.setNotes("Test notes");
    }

    @Test
    void createCustomer_withValidCustomer_succeeds() {
        Customer savedCustomer = new Customer();
        savedCustomer.setId(1L);
        savedCustomer.setName(testCustomer.getName());
        savedCustomer.setPhone(testCustomer.getPhone());
        savedCustomer.setEmail(testCustomer.getEmail());

        when(customerRepository.save(any(Customer.class))).thenReturn(savedCustomer);

        Customer created = customerService.createCustomer(testCustomer);

        assertNotNull(created.getId());
        assertEquals("Test Customer", created.getName());
        assertEquals("555-1234", created.getPhone());
        verify(customerRepository).save(testCustomer);
    }

    @Test
    void createCustomer_withExistingId_throwsException() {
        testCustomer.setId(999L);

        Exception exception = assertThrows(IllegalArgumentException.class,
                () -> customerService.createCustomer(testCustomer));

        assertEquals("New customer should not have an ID", exception.getMessage());
        verify(customerRepository, never()).save(any());
    }

    @Test
    void updateCustomer_withValidCustomer_succeeds() {
        testCustomer.setId(1L);
        testCustomer.setName("Updated Name");

        when(customerRepository.existsById(1L)).thenReturn(true);
        when(customerRepository.save(any(Customer.class))).thenReturn(testCustomer);

        Customer updated = customerService.updateCustomer(testCustomer);

        assertEquals("Updated Name", updated.getName());
        verify(customerRepository).existsById(1L);
        verify(customerRepository).save(testCustomer);
    }

    @Test
    void updateCustomer_withoutId_throwsException() {
        Exception exception = assertThrows(IllegalArgumentException.class,
                () -> customerService.updateCustomer(testCustomer));

        assertEquals("Customer ID is required for update", exception.getMessage());
        verify(customerRepository, never()).save(any());
    }

    @Test
    void updateCustomer_nonExistent_throwsException() {
        testCustomer.setId(999L);
        when(customerRepository.existsById(999L)).thenReturn(false);

        Exception exception = assertThrows(IllegalArgumentException.class,
                () -> customerService.updateCustomer(testCustomer));

        assertTrue(exception.getMessage().contains("Customer not found"));
        verify(customerRepository).existsById(999L);
        verify(customerRepository, never()).save(any());
    }

    @Test
    void deleteCustomer_existingCustomer_succeeds() {
        when(customerRepository.existsById(1L)).thenReturn(true);

        customerService.deleteCustomer(1L);

        verify(customerRepository).existsById(1L);
        verify(customerRepository).deleteById(1L);
    }

    @Test
    void deleteCustomer_nonExistent_throwsException() {
        when(customerRepository.existsById(999L)).thenReturn(false);

        Exception exception = assertThrows(IllegalArgumentException.class,
                () -> customerService.deleteCustomer(999L));

        assertTrue(exception.getMessage().contains("Customer not found"));
        verify(customerRepository).existsById(999L);
        verify(customerRepository, never()).deleteById(any());
    }

    @Test
    void findAll_returnsAllCustomers() {
        Customer customer1 = new Customer();
        customer1.setId(1L);
        customer1.setName("Customer 1");
        customer1.setPhone("555-1111");

        Customer customer2 = new Customer();
        customer2.setId(2L);
        customer2.setName("Customer 2");
        customer2.setPhone("555-2222");

        when(customerRepository.findAll()).thenReturn(Arrays.asList(customer1, customer2));

        List<Customer> customers = customerService.findAll();

        assertEquals(2, customers.size());
        verify(customerRepository).findAll();
    }

    @Test
    void searchByName_findsMatchingCustomers() {
        Customer customer1 = new Customer();
        customer1.setName("John Smith");

        Customer customer2 = new Customer();
        customer2.setName("John Doe");

        when(customerRepository.findByNameContainingIgnoreCase("John"))
                .thenReturn(Arrays.asList(customer1, customer2));

        List<Customer> results = customerService.searchByName("John");

        assertEquals(2, results.size());
        verify(customerRepository).findByNameContainingIgnoreCase("John");
    }

    @Test
    void searchByPhone_findsMatchingCustomers() {
        Customer customer = new Customer();
        customer.setPhone("555-1234");

        when(customerRepository.findByPhoneContaining("555"))
                .thenReturn(Arrays.asList(customer));

        List<Customer> results = customerService.searchByPhone("555");

        assertEquals(1, results.size());
        verify(customerRepository).findByPhoneContaining("555");
    }

    @Test
    void searchByEmail_findsMatchingCustomers() {
        Customer customer = new Customer();
        customer.setEmail("test@example.com");

        when(customerRepository.findByEmailContainingIgnoreCase("example"))
                .thenReturn(Arrays.asList(customer));

        List<Customer> results = customerService.searchByEmail("example");

        assertEquals(1, results.size());
        verify(customerRepository).findByEmailContainingIgnoreCase("example");
    }

    @Test
    void findById_existingCustomer_returnsCustomer() {
        Customer customer = new Customer();
        customer.setId(1L);
        customer.setName("Test Customer");

        when(customerRepository.findById(1L)).thenReturn(Optional.of(customer));

        Optional<Customer> found = customerService.findById(1L);

        assertTrue(found.isPresent());
        assertEquals("Test Customer", found.get().getName());
        verify(customerRepository).findById(1L);
    }

    @Test
    void findById_nonExistent_returnsEmpty() {
        when(customerRepository.findById(999L)).thenReturn(Optional.empty());

        Optional<Customer> found = customerService.findById(999L);

        assertFalse(found.isPresent());
        verify(customerRepository).findById(999L);
    }

    @Test
    void count_returnsCorrectNumber() {
        when(customerRepository.count()).thenReturn(5L);

        long count = customerService.count();

        assertEquals(5, count);
        verify(customerRepository).count();
    }
}
