package com.example.products.service;

import com.example.products.domain.Product;
import com.example.products.domain.ProductRepository;
import jakarta.annotation.security.RolesAllowed;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
@Transactional
public class ProductService {

    private final ProductRepository productRepository;

    public ProductService(ProductRepository productRepository) {
        this.productRepository = productRepository;
    }

    @RolesAllowed("ROLE_ADMIN")
    public Product createProduct(Product product) {
        if (product.getId() != null) {
            throw new IllegalArgumentException("New product should not have an ID");
        }
        return productRepository.save(product);
    }

    @RolesAllowed({"ROLE_ADMIN", "ROLE_EMPLOYEE"})
    public List<Product> findAll() {
        return productRepository.findAll();
    }

    @RolesAllowed({"ROLE_ADMIN", "ROLE_EMPLOYEE"})
    public List<Product> findAvailableProducts() {
        return productRepository.findByAvailable(true);
    }

    @RolesAllowed({"ROLE_ADMIN", "ROLE_EMPLOYEE"})
    public List<Product> searchByName(String name) {
        return productRepository.findByNameContainingIgnoreCase(name);
    }

    @RolesAllowed({"ROLE_ADMIN", "ROLE_EMPLOYEE"})
    public Optional<Product> findById(Long id) {
        return productRepository.findById(id);
    }

    @RolesAllowed("ROLE_ADMIN")
    public Product updateProduct(Product product) {
        if (product.getId() == null) {
            throw new IllegalArgumentException("Product ID is required for update");
        }
        if (!productRepository.existsById(product.getId())) {
            throw new IllegalArgumentException("Product not found with ID: " + product.getId());
        }
        return productRepository.save(product);
    }

    @RolesAllowed("ROLE_ADMIN")
    public void deleteProduct(Long id) {
        if (!productRepository.existsById(id)) {
            throw new IllegalArgumentException("Product not found with ID: " + id);
        }
        productRepository.deleteById(id);
    }

    @RolesAllowed("ROLE_ADMIN")
    public long count() {
        return productRepository.count();
    }
}
