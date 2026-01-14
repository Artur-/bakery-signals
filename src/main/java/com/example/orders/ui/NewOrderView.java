package com.example.orders.ui;

import com.example.customers.domain.Customer;
import com.example.customers.service.CustomerService;
import com.example.orders.domain.Order;
import com.example.orders.domain.OrderItem;
import com.example.orders.domain.PickupLocation;
import com.example.orders.service.OrderService;
import com.example.products.domain.Product;
import com.example.products.service.ProductService;
import com.example.shared.ui.MainLayout;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.checkbox.Checkbox;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.datepicker.DatePicker;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.formlayout.FormLayout;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.html.H3;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.notification.NotificationVariant;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.IntegerField;
import com.vaadin.flow.component.textfield.NumberField;
import com.vaadin.flow.component.textfield.TextArea;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import jakarta.annotation.security.RolesAllowed;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Route(value = "orders/new", layout = MainLayout.class)
@PageTitle("New Order")
@RolesAllowed({"ADMIN", "EMPLOYEE"})
public class NewOrderView extends VerticalLayout {

    private final OrderService orderService;
    private final ProductService productService;
    private final CustomerService customerService;

    // Form fields
    private final DatePicker dueDatePicker = new DatePicker("Due Date");
    private final ComboBox<Customer> customerComboBox = new ComboBox<>("Customer");
    private final ComboBox<PickupLocation> pickupLocationComboBox = new ComboBox<>("Pickup Location");
    private final NumberField discountField = new NumberField("Discount ($)");
    private final Checkbox paidCheckbox = new Checkbox("Paid");
    private final TextArea notesArea = new TextArea("Notes");

    // Order items
    private final List<OrderItem> orderItems = new ArrayList<>();
    private final Grid<OrderItem> itemsGrid = new Grid<>(OrderItem.class, false);
    private final Button addProductButton = new Button("Add Product");
    private final Span totalLabel = new Span("Total: $0.00");

    // Action buttons
    private final Button saveButton = new Button("Create Order");
    private final Button cancelButton = new Button("Cancel");

    public NewOrderView(OrderService orderService,
                        ProductService productService,
                        CustomerService customerService) {
        this.orderService = orderService;
        this.productService = productService;
        this.customerService = customerService;

        setSizeFull();
        setPadding(true);

        add(new H2("New Order"));
        add(createOrderForm());
        add(createOrderItemsSection());
        add(createTotalSection());
        add(createButtonBar());
    }

    private FormLayout createOrderForm() {
        FormLayout form = new FormLayout();
        form.setResponsiveSteps(
                new FormLayout.ResponsiveStep("0", 1),
                new FormLayout.ResponsiveStep("500px", 2)
        );

        // Due date (required, first field)
        dueDatePicker.setRequired(true);
        dueDatePicker.setMin(LocalDate.now());
        dueDatePicker.setPlaceholder("Select due date");
        dueDatePicker.setErrorMessage("Due date is required and cannot be in the past");

        // Customer selection with autocomplete
        customerComboBox.setItems(customerService.findAll());
        customerComboBox.setItemLabelGenerator(Customer::getName);
        customerComboBox.setPlaceholder("Select or search customer");
        customerComboBox.setRequired(true);

        // Pickup location
        pickupLocationComboBox.setItems(PickupLocation.values());
        pickupLocationComboBox.setItemLabelGenerator(PickupLocation::name);
        pickupLocationComboBox.setValue(PickupLocation.STOREFRONT);
        pickupLocationComboBox.setRequired(true);

        // Discount
        discountField.setValue(0.0);
        discountField.setMin(0);
        discountField.setPrefixComponent(new Span("$"));
        discountField.addValueChangeListener(e -> recalculateTotal());

        // Paid checkbox
        paidCheckbox.setValue(false);

        // Notes
        notesArea.setPlaceholder("Additional notes or special instructions");
        notesArea.setHeight("100px");

        form.add(dueDatePicker, customerComboBox);
        form.add(pickupLocationComboBox, discountField);
        form.add(paidCheckbox);
        form.setColspan(notesArea, 2);
        form.add(notesArea);

        return form;
    }

    private VerticalLayout createOrderItemsSection() {
        VerticalLayout section = new VerticalLayout();
        section.setPadding(false);

        HorizontalLayout header = new HorizontalLayout();
        header.setWidthFull();
        header.setJustifyContentMode(JustifyContentMode.BETWEEN);
        header.setDefaultVerticalComponentAlignment(Alignment.CENTER);

        H3 title = new H3("Order Items");
        addProductButton.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
        addProductButton.addClickListener(e -> openProductSelectionDialog());

        header.add(title, addProductButton);

        configureItemsGrid();

        section.add(header, itemsGrid);
        return section;
    }

    private void configureItemsGrid() {
        itemsGrid.setHeight("300px");

        itemsGrid.addColumn(item -> item.getProduct().getName())
                .setHeader("Product")
                .setAutoWidth(true);
        itemsGrid.addColumn(OrderItem::getQuantity)
                .setHeader("Quantity")
                .setAutoWidth(true);
        itemsGrid.addColumn(item -> item.getPricePerUnit() != null ?
                "$" + item.getPricePerUnit() : "$0.00")
                .setHeader("Price/Unit")
                .setAutoWidth(true);
        itemsGrid.addColumn(item -> {
            if (item.getPricePerUnit() != null) {
                BigDecimal total = item.getPricePerUnit()
                        .multiply(BigDecimal.valueOf(item.getQuantity()));
                return "$" + total;
            }
            return "$0.00";
        })
                .setHeader("Subtotal")
                .setAutoWidth(true);
        itemsGrid.addColumn(OrderItem::getCustomizationSpecs)
                .setHeader("Customization")
                .setAutoWidth(true);

        itemsGrid.addComponentColumn(item -> {
            Button removeButton = new Button("Remove");
            removeButton.addThemeVariants(ButtonVariant.LUMO_ERROR, ButtonVariant.LUMO_SMALL);
            removeButton.addClickListener(e -> {
                orderItems.remove(item);
                refreshItemsGrid();
                recalculateTotal();
            });
            return removeButton;
        }).setHeader("Actions").setAutoWidth(true);
    }

    private void openProductSelectionDialog() {
        Dialog dialog = new Dialog();
        dialog.setHeaderTitle("Add Product");
        dialog.setWidth("500px");

        VerticalLayout dialogLayout = new VerticalLayout();

        ComboBox<Product> productComboBox = new ComboBox<>("Product");
        productComboBox.setItems(productService.findAvailableProducts());
        productComboBox.setItemLabelGenerator(product ->
                product.getName() + " - $" + product.getPrice());
        productComboBox.setWidthFull();

        IntegerField quantityField = new IntegerField("Quantity");
        quantityField.setValue(1);
        quantityField.setMin(1);
        quantityField.setStepButtonsVisible(true);
        quantityField.setWidthFull();

        TextField customizationField = new TextField("Customization");
        customizationField.setPlaceholder("e.g., Happy Birthday John");
        customizationField.setWidthFull();

        Button addButton = new Button("Add to Order");
        addButton.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
        addButton.addClickListener(e -> {
            if (productComboBox.getValue() == null) {
                Notification.show("Please select a product")
                        .addThemeVariants(NotificationVariant.LUMO_ERROR);
                return;
            }

            Product product = productComboBox.getValue();
            OrderItem item = new OrderItem();
            item.setProduct(product);
            item.setQuantity(quantityField.getValue());
            item.setPricePerUnit(product.getPrice());
            item.setCustomizationSpecs(customizationField.getValue());

            orderItems.add(item);
            refreshItemsGrid();
            recalculateTotal();

            dialog.close();
            Notification.show("Product added to order")
                    .addThemeVariants(NotificationVariant.LUMO_SUCCESS);
        });

        Button cancelDialogButton = new Button("Cancel", e -> dialog.close());

        dialogLayout.add(productComboBox, quantityField, customizationField);
        dialog.add(dialogLayout);
        dialog.getFooter().add(cancelDialogButton, addButton);

        dialog.open();
    }

    private HorizontalLayout createTotalSection() {
        HorizontalLayout totalSection = new HorizontalLayout();
        totalSection.setWidthFull();
        totalSection.setJustifyContentMode(JustifyContentMode.END);

        totalLabel.getStyle()
                .set("font-size", "24px")
                .set("font-weight", "bold");

        totalSection.add(totalLabel);
        return totalSection;
    }

    private HorizontalLayout createButtonBar() {
        saveButton.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
        saveButton.addClickListener(e -> saveOrder());

        cancelButton.addClickListener(e ->
                UI.getCurrent().navigate(OrderListView.class));

        HorizontalLayout buttonBar = new HorizontalLayout(saveButton, cancelButton);
        buttonBar.setJustifyContentMode(JustifyContentMode.END);
        return buttonBar;
    }

    private void refreshItemsGrid() {
        itemsGrid.setItems(orderItems);
    }

    private void recalculateTotal() {
        BigDecimal subtotal = orderItems.stream()
                .map(item -> item.getPricePerUnit()
                        .multiply(BigDecimal.valueOf(item.getQuantity())))
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal discount = BigDecimal.valueOf(
                discountField.getValue() != null ? discountField.getValue() : 0);

        BigDecimal total = subtotal.subtract(discount);
        if (total.compareTo(BigDecimal.ZERO) < 0) {
            total = BigDecimal.ZERO;
        }

        totalLabel.setText("Total: $" + total);
    }

    private void saveOrder() {
        // Validation
        if (dueDatePicker.getValue() == null) {
            Notification.show("Please select a due date")
                    .addThemeVariants(NotificationVariant.LUMO_ERROR);
            return;
        }

        if (dueDatePicker.getValue().isBefore(LocalDate.now())) {
            Notification.show("Due date cannot be in the past")
                    .addThemeVariants(NotificationVariant.LUMO_ERROR);
            return;
        }

        if (customerComboBox.getValue() == null) {
            Notification.show("Please select a customer")
                    .addThemeVariants(NotificationVariant.LUMO_ERROR);
            return;
        }

        if (orderItems.isEmpty()) {
            Notification.show("Please add at least one product")
                    .addThemeVariants(NotificationVariant.LUMO_ERROR);
            return;
        }

        try {
            Order order = new Order();
            order.setDueDate(dueDatePicker.getValue());
            order.setCustomer(customerComboBox.getValue());
            order.setPickupLocation(pickupLocationComboBox.getValue());
            order.setDiscount(BigDecimal.valueOf(
                    discountField.getValue() != null ? discountField.getValue() : 0));
            order.setPaid(paidCheckbox.getValue());
            order.setNotes(notesArea.getValue());
            order.setItems(orderItems);

            orderService.createOrder(order);

            Notification.show("Order created successfully!")
                    .addThemeVariants(NotificationVariant.LUMO_SUCCESS);

            UI.getCurrent().navigate(OrderListView.class);
        } catch (Exception e) {
            Notification.show("Error creating order: " + e.getMessage())
                    .addThemeVariants(NotificationVariant.LUMO_ERROR);
        }
    }
}
