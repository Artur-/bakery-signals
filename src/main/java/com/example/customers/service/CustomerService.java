package com.example.customers.service;

import com.example.customers.domain.Customer;
import com.example.customers.domain.CustomerRepository;
import jakarta.annotation.security.RolesAllowed;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
@Transactional
public class CustomerService {

    private final CustomerRepository customerRepository;

    public CustomerService(CustomerRepository customerRepository) {
        this.customerRepository = customerRepository;
    }

    @RolesAllowed("ADMIN")
    public Customer createCustomer(Customer customer) {
        if (customer.getId() != null) {
            throw new IllegalArgumentException("New customer should not have an ID");
        }
        return customerRepository.save(customer);
    }

    @RolesAllowed({"ADMIN", "EMPLOYEE"})
    public List<Customer> findAll() {
        return customerRepository.findAll();
    }

    @RolesAllowed({"ADMIN", "EMPLOYEE"})
    public List<Customer> searchByName(String name) {
        return customerRepository.findByNameContainingIgnoreCase(name);
    }

    @RolesAllowed({"ADMIN", "EMPLOYEE"})
    public List<Customer> searchByPhone(String phone) {
        return customerRepository.findByPhoneContaining(phone);
    }

    @RolesAllowed({"ADMIN", "EMPLOYEE"})
    public List<Customer> searchByEmail(String email) {
        return customerRepository.findByEmailContainingIgnoreCase(email);
    }

    @RolesAllowed({"ADMIN", "EMPLOYEE"})
    public Optional<Customer> findById(Long id) {
        return customerRepository.findById(id);
    }

    @RolesAllowed("ADMIN")
    public Customer updateCustomer(Customer customer) {
        if (customer.getId() == null) {
            throw new IllegalArgumentException("Customer ID is required for update");
        }
        if (!customerRepository.existsById(customer.getId())) {
            throw new IllegalArgumentException("Customer not found with ID: " + customer.getId());
        }
        return customerRepository.save(customer);
    }

    @RolesAllowed("ADMIN")
    public void deleteCustomer(Long id) {
        if (!customerRepository.existsById(id)) {
            throw new IllegalArgumentException("Customer not found with ID: " + id);
        }
        customerRepository.deleteById(id);
    }

    @RolesAllowed({"ADMIN", "EMPLOYEE"})
    public long count() {
        return customerRepository.count();
    }
}
