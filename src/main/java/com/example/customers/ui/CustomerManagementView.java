package com.example.customers.ui;

import com.example.customers.domain.Customer;
import com.example.customers.service.CustomerService;
import com.example.shared.ui.MainLayout;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.confirmdialog.ConfirmDialog;
import com.vaadin.flow.component.formlayout.FormLayout;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.notification.NotificationVariant;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.EmailField;
import com.vaadin.flow.component.textfield.TextArea;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.data.binder.BeanValidationBinder;
import com.vaadin.flow.data.binder.Binder;
import com.vaadin.flow.data.value.ValueChangeMode;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import jakarta.annotation.security.RolesAllowed;

@Route(value = "customers", layout = MainLayout.class)
@PageTitle("Customer Management")
@RolesAllowed("ADMIN")
public class CustomerManagementView extends VerticalLayout {

    private final CustomerService customerService;

    final Grid<Customer> grid = new Grid<>(Customer.class, false);
    final TextField searchField = new TextField();
    final FormLayout form = new FormLayout();
    final Binder<Customer> binder = new BeanValidationBinder<>(Customer.class);

    // Form fields
    final TextField nameField = new TextField("Name");
    final TextField phoneField = new TextField("Phone");
    final EmailField emailField = new EmailField("Email");
    final TextField billingInfoField = new TextField("Billing Info");
    final TextArea notesField = new TextArea("Notes");

    // Buttons
    final Button addButton = new Button("Add Customer");
    final Button saveButton = new Button("Save");
    final Button cancelButton = new Button("Cancel");
    final Button deleteButton = new Button("Delete");

    private Customer currentCustomer;

    public CustomerManagementView(CustomerService customerService) {
        this.customerService = customerService;

        setSizeFull();
        configureGrid();
        configureForm();

        add(
            createToolbar(),
            createMainContent()
        );

        refreshGrid();
    }

    private HorizontalLayout createToolbar() {
        searchField.setPlaceholder("Search customers...");
        searchField.setClearButtonVisible(true);
        searchField.setValueChangeMode(ValueChangeMode.LAZY);
        searchField.addValueChangeListener(e -> filterCustomers(e.getValue()));

        addButton.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
        addButton.addClickListener(e -> addCustomer());

        HorizontalLayout toolbar = new HorizontalLayout(searchField, addButton);
        toolbar.setWidthFull();
        toolbar.expand(searchField);
        return toolbar;
    }

    private VerticalLayout createMainContent() {
        VerticalLayout content = new VerticalLayout(grid, form);
        content.setSizeFull();
        content.setFlexGrow(2, grid);
        content.setFlexGrow(1, form);
        return content;
    }

    private void configureGrid() {
        grid.setSizeFull();
        grid.addColumn(Customer::getName).setHeader("Name").setSortable(true);
        grid.addColumn(Customer::getPhone).setHeader("Phone").setSortable(true);
        grid.addColumn(Customer::getEmail).setHeader("Email").setSortable(true);
        grid.addColumn(Customer::getBillingInfo).setHeader("Billing Info");

        grid.asSingleSelect().addValueChangeListener(e -> editCustomer(e.getValue()));
    }

    private void configureForm() {
        form.setVisible(false);
        form.setResponsiveSteps(
            new FormLayout.ResponsiveStep("0", 1),
            new FormLayout.ResponsiveStep("500px", 2)
        );

        nameField.setRequired(true);
        nameField.setPlaceholder("Customer name");

        phoneField.setRequired(true);
        phoneField.setPlaceholder("Phone number");

        emailField.setPlaceholder("customer@example.com");

        billingInfoField.setPlaceholder("Billing address or info");

        notesField.setPlaceholder("Additional notes");
        notesField.setHeight("100px");

        // Bind fields
        binder.forField(nameField)
            .asRequired("Name is required")
            .bind(Customer::getName, Customer::setName);

        binder.forField(phoneField)
            .asRequired("Phone is required")
            .bind(Customer::getPhone, Customer::setPhone);

        binder.forField(emailField)
            .bind(Customer::getEmail, Customer::setEmail);

        binder.forField(billingInfoField)
            .bind(Customer::getBillingInfo, Customer::setBillingInfo);

        binder.forField(notesField)
            .bind(Customer::getNotes, Customer::setNotes);

        // Buttons
        saveButton.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
        saveButton.addClickListener(e -> saveCustomer());

        cancelButton.addClickListener(e -> clearForm());

        deleteButton.addThemeVariants(ButtonVariant.LUMO_ERROR);
        deleteButton.addClickListener(e -> confirmDelete());

        HorizontalLayout buttons = new HorizontalLayout(saveButton, cancelButton, deleteButton);

        form.add(nameField, phoneField);
        form.add(emailField, billingInfoField);
        form.setColspan(notesField, 2);
        form.add(notesField);
        form.setColspan(buttons, 2);
        form.add(buttons);
    }

    private void filterCustomers(String searchTerm) {
        if (searchTerm == null || searchTerm.trim().isEmpty()) {
            refreshGrid();
        } else {
            // Search by name, phone, or email
            List<Customer> results = new java.util.ArrayList<>();
            results.addAll(customerService.searchByName(searchTerm));
            results.addAll(customerService.searchByPhone(searchTerm));
            results.addAll(customerService.searchByEmail(searchTerm));

            // Remove duplicates
            grid.setItems(results.stream().distinct().toList());
        }
    }

    private void addCustomer() {
        clearForm();
        currentCustomer = new Customer();
        binder.setBean(currentCustomer);
        form.setVisible(true);
        deleteButton.setVisible(false);
        nameField.focus();
    }

    private void editCustomer(Customer customer) {
        if (customer == null) {
            clearForm();
        } else {
            currentCustomer = customer;
            binder.setBean(currentCustomer);
            form.setVisible(true);
            deleteButton.setVisible(true);
        }
    }

    private void saveCustomer() {
        if (!binder.validate().isOk()) {
            Notification.show("Please fix the validation errors")
                .addThemeVariants(NotificationVariant.LUMO_ERROR);
            return;
        }

        try {
            if (currentCustomer.getId() == null) {
                customerService.createCustomer(currentCustomer);
                Notification.show("Customer created successfully")
                    .addThemeVariants(NotificationVariant.LUMO_SUCCESS);
            } else {
                customerService.updateCustomer(currentCustomer);
                Notification.show("Customer updated successfully")
                    .addThemeVariants(NotificationVariant.LUMO_SUCCESS);
            }
            refreshGrid();
            clearForm();
        } catch (Exception e) {
            Notification.show("Error saving customer: " + e.getMessage())
                .addThemeVariants(NotificationVariant.LUMO_ERROR);
        }
    }

    private void confirmDelete() {
        ConfirmDialog dialog = new ConfirmDialog();
        dialog.setHeader("Delete Customer");
        dialog.setText("Are you sure you want to delete " + currentCustomer.getName() + "?");
        dialog.setCancelable(true);
        dialog.setConfirmText("Delete");
        dialog.setConfirmButtonTheme("error primary");

        dialog.addConfirmListener(e -> deleteCustomer());

        dialog.open();
    }

    private void deleteCustomer() {
        try {
            customerService.deleteCustomer(currentCustomer.getId());
            Notification.show("Customer deleted successfully")
                .addThemeVariants(NotificationVariant.LUMO_SUCCESS);
            refreshGrid();
            clearForm();
        } catch (Exception e) {
            Notification.show("Error deleting customer: " + e.getMessage())
                .addThemeVariants(NotificationVariant.LUMO_ERROR);
        }
    }

    private void clearForm() {
        form.setVisible(false);
        binder.setBean(null);
        currentCustomer = null;
        grid.asSingleSelect().clear();
    }

    private void refreshGrid() {
        grid.setItems(customerService.findAll());
    }
}
