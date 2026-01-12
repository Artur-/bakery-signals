package com.example.products.service;

import com.example.products.domain.Product;
import com.example.products.domain.ProductRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Service layer tests for ProductService.
 * Tests business logic and data operations using mocked repository.
 * Security authorization is tested separately in integration tests.
 */
@ExtendWith(MockitoExtension.class)
class ProductServiceTest {

    @Mock
    private ProductRepository productRepository;

    @InjectMocks
    private ProductService productService;

    private Product testProduct;

    @BeforeEach
    void setUp() {
        testProduct = new Product();
        testProduct.setName("Test Croissant");
        testProduct.setDescription("Buttery and flaky");
        testProduct.setPrice(new BigDecimal("3.50"));
        testProduct.setAvailable(true);
    }

    @Test
    void createProduct_withValidProduct_succeeds() {
        Product savedProduct = new Product();
        savedProduct.setId(1L);
        savedProduct.setName(testProduct.getName());
        savedProduct.setDescription(testProduct.getDescription());
        savedProduct.setPrice(testProduct.getPrice());
        savedProduct.setAvailable(testProduct.isAvailable());

        when(productRepository.save(any(Product.class))).thenReturn(savedProduct);

        Product created = productService.createProduct(testProduct);

        assertNotNull(created.getId());
        assertEquals("Test Croissant", created.getName());
        assertEquals(new BigDecimal("3.50"), created.getPrice());
        assertTrue(created.isAvailable());
        verify(productRepository).save(testProduct);
    }

    @Test
    void createProduct_withExistingId_throwsException() {
        testProduct.setId(999L);

        Exception exception = assertThrows(IllegalArgumentException.class,
                () -> productService.createProduct(testProduct));

        assertEquals("New product should not have an ID", exception.getMessage());
        verify(productRepository, never()).save(any());
    }

    @Test
    void updateProduct_withValidProduct_succeeds() {
        Product existingProduct = new Product();
        existingProduct.setId(1L);
        existingProduct.setName("Original Name");
        existingProduct.setPrice(new BigDecimal("3.50"));

        testProduct.setId(1L);
        testProduct.setName("Updated Name");

        when(productRepository.existsById(1L)).thenReturn(true);
        when(productRepository.save(any(Product.class))).thenReturn(testProduct);

        Product updated = productService.updateProduct(testProduct);

        assertEquals("Updated Name", updated.getName());
        verify(productRepository).existsById(1L);
        verify(productRepository).save(testProduct);
    }

    @Test
    void updateProduct_withoutId_throwsException() {
        Exception exception = assertThrows(IllegalArgumentException.class,
                () -> productService.updateProduct(testProduct));

        assertEquals("Product ID is required for update", exception.getMessage());
        verify(productRepository, never()).save(any());
    }

    @Test
    void updateProduct_nonExistent_throwsException() {
        testProduct.setId(999L);
        when(productRepository.existsById(999L)).thenReturn(false);

        Exception exception = assertThrows(IllegalArgumentException.class,
                () -> productService.updateProduct(testProduct));

        assertTrue(exception.getMessage().contains("Product not found"));
        verify(productRepository).existsById(999L);
        verify(productRepository, never()).save(any());
    }

    @Test
    void deleteProduct_existingProduct_succeeds() {
        when(productRepository.existsById(1L)).thenReturn(true);

        productService.deleteProduct(1L);

        verify(productRepository).existsById(1L);
        verify(productRepository).deleteById(1L);
    }

    @Test
    void deleteProduct_nonExistent_throwsException() {
        when(productRepository.existsById(999L)).thenReturn(false);

        Exception exception = assertThrows(IllegalArgumentException.class,
                () -> productService.deleteProduct(999L));

        assertTrue(exception.getMessage().contains("Product not found"));
        verify(productRepository).existsById(999L);
        verify(productRepository, never()).deleteById(any());
    }

    @Test
    void findAll_returnsAllProducts() {
        Product product1 = new Product();
        product1.setId(1L);
        product1.setName("Croissant");
        product1.setPrice(new BigDecimal("3.50"));

        Product product2 = new Product();
        product2.setId(2L);
        product2.setName("Baguette");
        product2.setPrice(new BigDecimal("2.00"));

        when(productRepository.findAll()).thenReturn(Arrays.asList(product1, product2));

        List<Product> products = productService.findAll();

        assertEquals(2, products.size());
        verify(productRepository).findAll();
    }

    @Test
    void findAvailableProducts_returnsOnlyAvailable() {
        Product availableProduct = new Product();
        availableProduct.setId(1L);
        availableProduct.setName("Croissant");
        availableProduct.setAvailable(true);

        when(productRepository.findByAvailable(true)).thenReturn(Arrays.asList(availableProduct));

        List<Product> available = productService.findAvailableProducts();

        assertEquals(1, available.size());
        assertTrue(available.get(0).isAvailable());
        verify(productRepository).findByAvailable(true);
    }

    @Test
    void searchByName_findsMatchingProducts() {
        Product product1 = new Product();
        product1.setName("Croissant");

        Product product2 = new Product();
        product2.setName("Chocolate Croissant");

        when(productRepository.findByNameContainingIgnoreCase("Croissant"))
                .thenReturn(Arrays.asList(product1, product2));

        List<Product> results = productService.searchByName("Croissant");

        assertEquals(2, results.size());
        verify(productRepository).findByNameContainingIgnoreCase("Croissant");
    }

    @Test
    void findById_existingProduct_returnsProduct() {
        Product product = new Product();
        product.setId(1L);
        product.setName("Croissant");

        when(productRepository.findById(1L)).thenReturn(Optional.of(product));

        Optional<Product> found = productService.findById(1L);

        assertTrue(found.isPresent());
        assertEquals("Croissant", found.get().getName());
        verify(productRepository).findById(1L);
    }

    @Test
    void findById_nonExistent_returnsEmpty() {
        when(productRepository.findById(999L)).thenReturn(Optional.empty());

        Optional<Product> found = productService.findById(999L);

        assertFalse(found.isPresent());
        verify(productRepository).findById(999L);
    }

    @Test
    void count_returnsCorrectNumber() {
        when(productRepository.count()).thenReturn(5L);

        long count = productService.count();

        assertEquals(5, count);
        verify(productRepository).count();
    }
}
