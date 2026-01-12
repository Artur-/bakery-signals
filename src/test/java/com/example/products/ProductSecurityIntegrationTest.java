package com.example.products;

import com.example.config.SecurityConfig;
import com.example.products.domain.Product;
import com.example.products.domain.ProductRepository;
import com.example.products.service.ProductService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.security.test.context.support.WithUserDetails;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Integration test for Product security configuration.
 * Tests that Spring Security properly authorizes access to ProductService methods.
 */
@SpringBootTest
@Import(SecurityConfig.class)
@Transactional
class ProductSecurityIntegrationTest {

    @Autowired
    private ProductService productService;

    @Autowired
    private ProductRepository productRepository;

    private Product testProduct;

    @BeforeEach
    void setUp() {
        productRepository.deleteAll();

        testProduct = new Product();
        testProduct.setName("Test Product");
        testProduct.setDescription("Test description");
        testProduct.setPrice(new BigDecimal("5.00"));
        testProduct.setAvailable(true);
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void admin_canCreateProduct() {
        Product created = productService.createProduct(testProduct);

        assertNotNull(created.getId());
        assertEquals("Test Product", created.getName());
    }

    @Test
    @WithMockUser(roles = "EMPLOYEE")
    void employee_cannotCreateProduct() {
        assertThrows(org.springframework.security.access.AccessDeniedException.class,
                () -> productService.createProduct(testProduct));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void admin_canViewProducts() {
        productRepository.save(testProduct);

        List<Product> products = productService.findAll();

        assertEquals(1, products.size());
    }

    @Test
    @WithMockUser(roles = "EMPLOYEE")
    void employee_canViewProducts() {
        productRepository.save(testProduct);

        List<Product> products = productService.findAll();

        assertEquals(1, products.size());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void admin_canUpdateProduct() {
        Product saved = productRepository.save(testProduct);
        saved.setName("Updated Name");

        Product updated = productService.updateProduct(saved);

        assertEquals("Updated Name", updated.getName());
    }

    @Test
    @WithMockUser(roles = "EMPLOYEE")
    void employee_cannotUpdateProduct() {
        Product saved = productRepository.save(testProduct);
        saved.setName("Updated Name");

        assertThrows(org.springframework.security.access.AccessDeniedException.class,
                () -> productService.updateProduct(saved));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void admin_canDeleteProduct() {
        Product saved = productRepository.save(testProduct);

        productService.deleteProduct(saved.getId());

        assertFalse(productRepository.existsById(saved.getId()));
    }

    @Test
    @WithMockUser(roles = "EMPLOYEE")
    void employee_cannotDeleteProduct() {
        Product saved = productRepository.save(testProduct);

        assertThrows(org.springframework.security.access.AccessDeniedException.class,
                () -> productService.deleteProduct(saved.getId()));
    }

    @Test
    void noAuthentication_cannotAccessProducts() {
        assertThrows(org.springframework.security.authentication.AuthenticationCredentialsNotFoundException.class,
                () -> productService.findAll());
    }

    @Test
    @WithUserDetails("admin")
    void realAdminUser_canAccessProducts() {
        productRepository.save(testProduct);

        // This test uses the actual admin user from the database
        List<Product> products = productService.findAll();

        assertEquals(1, products.size());
    }

    @Test
    @WithMockUser(username = "testuser", roles = {"ADMIN"})
    void debugSecurityContext() {
        org.springframework.security.core.Authentication auth =
                org.springframework.security.core.context.SecurityContextHolder.getContext().getAuthentication();

        System.out.println("=== DEBUG SECURITY CONTEXT ===");
        System.out.println("Authentication: " + auth);
        System.out.println("Principal: " + auth.getPrincipal());
        System.out.println("Authorities: " + auth.getAuthorities());
        System.out.println("Is Authenticated: " + auth.isAuthenticated());

        auth.getAuthorities().forEach(authority ->
                System.out.println("  - Authority: " + authority.getAuthority())
        );

        // Now try to call the service
        try {
            productRepository.save(testProduct);
            List<Product> products = productService.findAll();
            System.out.println("SUCCESS: Got " + products.size() + " products");
        } catch (Exception e) {
            System.out.println("FAILED: " + e.getClass().getSimpleName() + ": " + e.getMessage());
            throw e;
        }
    }
}
