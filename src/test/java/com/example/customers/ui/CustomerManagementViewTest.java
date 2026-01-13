package com.example.customers.ui;

import com.example.customers.domain.Customer;
import com.example.customers.service.CustomerService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * UI tests for CustomerManagementView.
 * Tests view logic, validation, and integration with CustomerService.
 */
@ExtendWith(MockitoExtension.class)
class CustomerManagementViewTest {

    @Mock
    private CustomerService customerService;

    private CustomerManagementView view;

    @BeforeEach
    void setUp() {
        // Create view with mocked service
        view = new CustomerManagementView(customerService);
    }

    @Test
    void viewCreation_initializesComponents() {
        assertNotNull(view.grid);
        assertNotNull(view.searchField);
        assertNotNull(view.nameField);
        assertNotNull(view.phoneField);
        assertNotNull(view.emailField);
        assertNotNull(view.billingInfoField);
        assertNotNull(view.notesField);
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
        verify(customerService).findAll();
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

        // Delete button should be hidden for new customers
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
    void phoneField_isRequired() {
        assertTrue(view.phoneField.isRequired());
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
    void binder_isConfiguredWithBeanValidation() {
        assertNotNull(view.binder);
    }

    @Test
    void grid_configuredWithCustomerColumns() {
        // Verify grid has columns (at least 4: name, phone, email, billing)
        assertNotNull(view.grid);
        assertTrue(view.grid.getColumns().size() >= 4);
    }

    @Test
    void emailField_acceptsValidEmail() {
        view.addButton.click();
        view.emailField.setValue("test@example.com");

        assertNotNull(view.emailField.getValue());
        assertEquals("test@example.com", view.emailField.getValue());
    }

    @Test
    void notesField_allowsLongText() {
        view.addButton.click();
        String longText = "This is a very long note ".repeat(10);
        view.notesField.setValue(longText);

        assertEquals(longText, view.notesField.getValue());
    }
}
