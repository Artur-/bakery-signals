package com.example.products.ui;

import com.example.products.domain.Product;
import com.example.products.service.ProductService;
import com.example.shared.ui.MainLayout;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.checkbox.Checkbox;
import com.vaadin.flow.component.confirmdialog.ConfirmDialog;
import com.vaadin.flow.component.formlayout.FormLayout;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.notification.NotificationVariant;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.BigDecimalField;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.component.textfield.TextArea;
import com.vaadin.flow.data.binder.BeanValidationBinder;
import com.vaadin.flow.data.binder.Binder;
import com.vaadin.flow.data.value.ValueChangeMode;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import jakarta.annotation.security.RolesAllowed;

import java.math.BigDecimal;

@Route(value = "products", layout = MainLayout.class)
@PageTitle("Product Management")
@RolesAllowed("ADMIN")
public class ProductManagementView extends VerticalLayout {

    private final ProductService productService;

    final Grid<Product> grid = new Grid<>(Product.class, false);
    final TextField searchField = new TextField();
    final FormLayout form = new FormLayout();
    final Binder<Product> binder = new BeanValidationBinder<>(Product.class);

    // Form fields
    final TextField nameField = new TextField("Name");
    final TextArea descriptionField = new TextArea("Description");
    final BigDecimalField priceField = new BigDecimalField("Price");
    final Checkbox availableCheckbox = new Checkbox("Available");

    // Buttons
    final Button addButton = new Button("Add Product");
    final Button saveButton = new Button("Save");
    final Button cancelButton = new Button("Cancel");
    final Button deleteButton = new Button("Delete");

    private Product currentProduct;

    public ProductManagementView(ProductService productService) {
        this.productService = productService;

        setSizeFull();
        configureGrid();
        configureForm();

        add(
            createToolbar(),
            createMainContent()
        );

        refreshGrid();
    }

    private void configureGrid() {
        grid.addColumn(Product::getName).setHeader("Name").setSortable(true);
        grid.addColumn(Product::getDescription).setHeader("Description");
        grid.addColumn(product -> String.format("$%.2f", product.getPrice()))
            .setHeader("Price").setSortable(true);
        grid.addColumn(product -> product.isAvailable() ? "Yes" : "No")
            .setHeader("Available");

        grid.asSingleSelect().addValueChangeListener(event -> {
            if (event.getValue() != null) {
                editProduct(event.getValue());
            } else {
                clearForm();
            }
        });

        grid.setHeight("400px");
    }

    private void configureForm() {
        // Configure fields
        nameField.setRequired(true);
        nameField.setPlaceholder("Enter product name");

        descriptionField.setPlaceholder("Enter product description");
        descriptionField.setHeight("100px");

        priceField.setRequired(true);
        priceField.setPrefixComponent(new com.vaadin.flow.component.html.Span("$"));

        availableCheckbox.setValue(true);

        // Bind fields
        binder.forField(nameField)
            .asRequired("Name is required")
            .bind(Product::getName, Product::setName);

        binder.forField(descriptionField)
            .bind(Product::getDescription, Product::setDescription);

        binder.forField(priceField)
            .asRequired("Price is required")
            .withValidator(price -> price != null && price.compareTo(BigDecimal.ZERO) > 0,
                "Price must be greater than 0")
            .bind(Product::getPrice, Product::setPrice);

        binder.forField(availableCheckbox)
            .bind(Product::isAvailable, Product::setAvailable);

        // Configure form layout
        form.add(nameField, priceField, availableCheckbox, descriptionField);
        form.setColspan(descriptionField, 2);
        form.setResponsiveSteps(
            new FormLayout.ResponsiveStep("0", 1),
            new FormLayout.ResponsiveStep("500px", 2)
        );

        // Configure buttons
        saveButton.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
        saveButton.addClickListener(e -> saveProduct());

        cancelButton.addClickListener(e -> clearForm());

        deleteButton.addThemeVariants(ButtonVariant.LUMO_ERROR);
        deleteButton.addClickListener(e -> confirmDelete());

        form.setVisible(false);
    }

    private HorizontalLayout createToolbar() {
        searchField.setPlaceholder("Search products...");
        searchField.setClearButtonVisible(true);
        searchField.setValueChangeMode(ValueChangeMode.LAZY);
        searchField.addValueChangeListener(e -> applyFilter());

        addButton.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
        addButton.addClickListener(e -> addProduct());

        HorizontalLayout toolbar = new HorizontalLayout(searchField, addButton);
        toolbar.setWidthFull();
        toolbar.expand(searchField);

        return toolbar;
    }

    private VerticalLayout createMainContent() {
        VerticalLayout content = new VerticalLayout();
        content.setSizeFull();
        content.add(grid, createFormLayout());
        return content;
    }

    private VerticalLayout createFormLayout() {
        VerticalLayout formLayout = new VerticalLayout();
        formLayout.add(form);

        HorizontalLayout buttonLayout = new HorizontalLayout(saveButton, deleteButton, cancelButton);
        formLayout.add(buttonLayout);

        return formLayout;
    }

    private void addProduct() {
        grid.asSingleSelect().clear();
        currentProduct = new Product();
        currentProduct.setAvailable(true);
        binder.setBean(currentProduct);
        form.setVisible(true);
        deleteButton.setVisible(false);
        nameField.focus();
    }

    private void editProduct(Product product) {
        if (product == null) {
            clearForm();
            return;
        }

        currentProduct = product;
        binder.setBean(product);
        form.setVisible(true);
        deleteButton.setVisible(true);
    }

    private void saveProduct() {
        if (!binder.validate().isOk()) {
            Notification.show("Please fix the validation errors")
                .addThemeVariants(NotificationVariant.LUMO_ERROR);
            return;
        }

        try {
            if (currentProduct.getId() == null) {
                productService.createProduct(currentProduct);
                Notification.show("Product created successfully")
                    .addThemeVariants(NotificationVariant.LUMO_SUCCESS);
            } else {
                productService.updateProduct(currentProduct);
                Notification.show("Product updated successfully")
                    .addThemeVariants(NotificationVariant.LUMO_SUCCESS);
            }
            refreshGrid();
            clearForm();
        } catch (Exception e) {
            Notification.show("Error saving product: " + e.getMessage())
                .addThemeVariants(NotificationVariant.LUMO_ERROR);
        }
    }

    private void confirmDelete() {
        ConfirmDialog dialog = new ConfirmDialog();
        dialog.setHeader("Confirm Delete");
        dialog.setText("Are you sure you want to delete this product?");

        dialog.setCancelable(true);
        dialog.setConfirmText("Delete");
        dialog.setConfirmButtonTheme("error primary");

        dialog.addConfirmListener(e -> deleteProduct());

        dialog.open();
    }

    private void deleteProduct() {
        try {
            productService.deleteProduct(currentProduct.getId());
            Notification.show("Product deleted successfully")
                .addThemeVariants(NotificationVariant.LUMO_SUCCESS);
            refreshGrid();
            clearForm();
        } catch (Exception e) {
            Notification.show("Error deleting product: " + e.getMessage())
                .addThemeVariants(NotificationVariant.LUMO_ERROR);
        }
    }

    private void clearForm() {
        grid.asSingleSelect().clear();
        currentProduct = null;
        binder.setBean(null);
        form.setVisible(false);
    }

    private void applyFilter() {
        String filterText = searchField.getValue();
        if (filterText == null || filterText.trim().isEmpty()) {
            grid.setItems(productService.findAll());
        } else {
            grid.setItems(productService.searchByName(filterText.trim()));
        }
    }

    private void refreshGrid() {
        grid.setItems(productService.findAll());
    }
}
