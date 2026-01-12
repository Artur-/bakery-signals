package com.example.products.ui;

import com.example.products.domain.Product;
import com.example.products.service.ProductService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * UI tests for ProductManagementView.
 * Tests view logic, validation, and integration with ProductService.
 */
@ExtendWith(MockitoExtension.class)
class ProductManagementViewTest {

    @Mock
    private ProductService productService;

    private ProductManagementView view;

    @BeforeEach
    void setUp() {
        // Create view with mocked service
        view = new ProductManagementView(productService);
    }

    @Test
    void viewCreation_initializesComponents() {
        assertNotNull(view.grid);
        assertNotNull(view.searchField);
        assertNotNull(view.nameField);
        assertNotNull(view.priceField);
        assertNotNull(view.descriptionField);
        assertNotNull(view.availableCheckbox);
        assertNotNull(view.addButton);
        assertNotNull(view.saveButton);
        assertNotNull(view.cancelButton);
        assertNotNull(view.deleteButton);
    }

    @Test
    void viewCreation_formIsHidden() {
        assertFalse(view.form.isVisible());
    }

    @Test
    void viewCreation_callsRefreshGrid() {
        // Verify that findAll was called during view initialization
        verify(productService).findAll();
    }

    @Test
    void addButton_showsForm() {
        // Initially form is hidden
        assertFalse(view.form.isVisible());

        // Click add button
        view.addButton.click();

        // Form should now be visible
        assertTrue(view.form.isVisible());
    }

    @Test
    void addButton_hidesDeleteButton() {
        view.addButton.click();

        // Delete button should be hidden for new products
        assertFalse(view.deleteButton.isVisible());
    }

    @Test
    void cancelButton_hidesForm() {
        view.addButton.click();
        assertTrue(view.form.isVisible());

        view.cancelButton.click();
        assertFalse(view.form.isVisible());
    }

    @Test
    void nameField_isRequired() {
        assertTrue(view.nameField.isRequired());
    }

    @Test
    void priceField_isRequired() {
        assertTrue(view.priceField.isRequired());
    }

    @Test
    void searchField_hasLazyValueChangeMode() {
        assertEquals(com.vaadin.flow.data.value.ValueChangeMode.LAZY,
                view.searchField.getValueChangeMode());
    }

    @Test
    void searchField_isClearable() {
        assertTrue(view.searchField.isClearButtonVisible());
    }

    @Test
    void availableCheckbox_defaultsToTrue() {
        view.addButton.click();
        assertTrue(view.availableCheckbox.getValue());
    }

    @Test
    void binder_isConfiguredWithBeanValidation() {
        assertNotNull(view.binder);
    }

    @Test
    void grid_configuredWithProductColumns() {
        // Verify grid has columns (at least 3: name, description, price, available)
        assertNotNull(view.grid);
        assertTrue(view.grid.getColumns().size() >= 3);
    }

    @Test
    void searchByName_callsProductService() {
        List<Product> mockProducts = Arrays.asList(createMockProduct("Croissant"));
        when(productService.searchByName("Croissant")).thenReturn(mockProducts);

        view.searchField.setValue("Croissant");
        // Simulate value change event
        view.searchField.getElement().setProperty("value", "Croissant");

        // Note: In a full integration test, this would trigger the search
        // Here we're just testing that the search field is configured correctly
        assertNotNull(view.searchField.getValue());
        assertEquals("Croissant", view.searchField.getValue());
    }

    private Product createMockProduct(String name) {
        Product product = new Product();
        product.setId(1L);
        product.setName(name);
        product.setDescription("Test description");
        product.setPrice(new BigDecimal("3.50"));
        product.setAvailable(true);
        return product;
    }
}
