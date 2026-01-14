package com.example.orders.ui;

import com.example.orders.domain.Order;
import com.example.orders.domain.OrderItem;
import com.example.orders.domain.OrderState;
import com.example.orders.service.OrderService;
import com.example.shared.ui.MainLayout;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.confirmdialog.ConfirmDialog;
import com.vaadin.flow.component.formlayout.FormLayout;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.html.H3;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.notification.NotificationVariant;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.router.BeforeEvent;
import com.vaadin.flow.router.HasUrlParameter;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import jakarta.annotation.security.RolesAllowed;

import java.math.BigDecimal;

@Route(value = "orders/details", layout = MainLayout.class)
@PageTitle("Order Details")
@RolesAllowed({"ADMIN", "EMPLOYEE"})
public class OrderDetailsView extends VerticalLayout implements HasUrlParameter<Long> {

    private final OrderService orderService;

    private Order currentOrder;

    // Display fields
    private final TextField orderIdField = new TextField("Order ID");
    private final TextField customerField = new TextField("Customer");
    private final TextField dueDateField = new TextField("Due Date");
    private final TextField stateField = new TextField("Status");
    private final TextField pickupLocationField = new TextField("Pickup Location");
    private final TextField discountField = new TextField("Discount");
    private final TextField paidField = new TextField("Paid");
    private final TextField notesField = new TextField("Notes");
    private final TextField totalField = new TextField("Total");

    // Order items grid
    private final Grid<OrderItem> itemsGrid = new Grid<>(OrderItem.class, false);

    // State transition buttons
    private final Button markReadyButton = new Button("Mark Ready");
    private final Button markDeliveredButton = new Button("Mark Delivered");
    private final Button cancelOrderButton = new Button("Cancel Order");
    private final Button backButton = new Button("Back to Orders");

    public OrderDetailsView(OrderService orderService) {
        this.orderService = orderService;

        setSizeFull();
        setPadding(true);

        add(new H2("Order Details"));
        add(createOrderInfoForm());
        add(createOrderItemsSection());
        add(createActionButtons());
    }

    @Override
    public void setParameter(BeforeEvent event, Long orderId) {
        loadOrder(orderId);
    }

    private void loadOrder(Long orderId) {
        currentOrder = orderService.findById(orderId)
                .orElseThrow(() -> new IllegalArgumentException("Order not found: " + orderId));

        displayOrderInfo();
        displayOrderItems();
        updateActionButtons();
    }

    private FormLayout createOrderInfoForm() {
        FormLayout form = new FormLayout();
        form.setResponsiveSteps(
                new FormLayout.ResponsiveStep("0", 1),
                new FormLayout.ResponsiveStep("500px", 2)
        );

        // Make all fields read-only
        orderIdField.setReadOnly(true);
        customerField.setReadOnly(true);
        dueDateField.setReadOnly(true);
        stateField.setReadOnly(true);
        pickupLocationField.setReadOnly(true);
        discountField.setReadOnly(true);
        paidField.setReadOnly(true);
        notesField.setReadOnly(true);
        totalField.setReadOnly(true);

        form.add(orderIdField, customerField);
        form.add(dueDateField, stateField);
        form.add(pickupLocationField, discountField);
        form.add(paidField, totalField);
        form.setColspan(notesField, 2);
        form.add(notesField);

        return form;
    }

    private VerticalLayout createOrderItemsSection() {
        VerticalLayout section = new VerticalLayout();
        section.setPadding(false);

        section.add(new H3("Order Items"));

        configureItemsGrid();
        section.add(itemsGrid);

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
    }

    private HorizontalLayout createActionButtons() {
        // State transition buttons
        markReadyButton.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
        markReadyButton.addClickListener(e -> markOrderReady());

        markDeliveredButton.addThemeVariants(ButtonVariant.LUMO_SUCCESS);
        markDeliveredButton.addClickListener(e -> markOrderDelivered());

        cancelOrderButton.addThemeVariants(ButtonVariant.LUMO_ERROR);
        cancelOrderButton.addClickListener(e -> confirmCancelOrder());

        // Back button
        backButton.addClickListener(e -> UI.getCurrent().navigate(OrderListView.class));

        HorizontalLayout buttonBar = new HorizontalLayout();
        buttonBar.setWidthFull();
        buttonBar.setJustifyContentMode(JustifyContentMode.BETWEEN);

        HorizontalLayout leftButtons = new HorizontalLayout(backButton);
        HorizontalLayout rightButtons = new HorizontalLayout(
                markReadyButton,
                markDeliveredButton,
                cancelOrderButton
        );

        buttonBar.add(leftButtons, rightButtons);
        return buttonBar;
    }

    private void displayOrderInfo() {
        orderIdField.setValue(currentOrder.getId().toString());
        customerField.setValue(currentOrder.getCustomer() != null ?
                currentOrder.getCustomer().getName() : "N/A");
        dueDateField.setValue(currentOrder.getDueDate().toString());
        stateField.setValue(currentOrder.getState().toString());
        pickupLocationField.setValue(currentOrder.getPickupLocation().toString());
        discountField.setValue(currentOrder.getDiscount() != null ?
                "$" + currentOrder.getDiscount() : "$0.00");
        paidField.setValue(currentOrder.isPaid() ? "Yes" : "No");
        notesField.setValue(currentOrder.getNotes() != null ? currentOrder.getNotes() : "");
        totalField.setValue(currentOrder.getTotalPrice() != null ?
                "$" + currentOrder.getTotalPrice() : "$0.00");
    }

    private void displayOrderItems() {
        itemsGrid.setItems(currentOrder.getItems());
    }

    private void updateActionButtons() {
        OrderState state = currentOrder.getState();

        // Show/hide buttons based on current state
        markReadyButton.setVisible(state == OrderState.NEW);
        markDeliveredButton.setVisible(state == OrderState.READY);
        cancelOrderButton.setVisible(state != OrderState.DELIVERED &&
                state != OrderState.CANCELLED);
    }

    private void markOrderReady() {
        try {
            currentOrder = orderService.markReady(currentOrder.getId());
            displayOrderInfo();
            updateActionButtons();

            Notification.show("Order marked as READY")
                    .addThemeVariants(NotificationVariant.LUMO_SUCCESS);
        } catch (Exception e) {
            Notification.show("Error: " + e.getMessage())
                    .addThemeVariants(NotificationVariant.LUMO_ERROR);
        }
    }

    private void markOrderDelivered() {
        try {
            currentOrder = orderService.markDelivered(currentOrder.getId());
            displayOrderInfo();
            updateActionButtons();

            Notification.show("Order marked as DELIVERED")
                    .addThemeVariants(NotificationVariant.LUMO_SUCCESS);
        } catch (Exception e) {
            Notification.show("Error: " + e.getMessage())
                    .addThemeVariants(NotificationVariant.LUMO_ERROR);
        }
    }

    private void confirmCancelOrder() {
        ConfirmDialog dialog = new ConfirmDialog();
        dialog.setHeader("Cancel Order");
        dialog.setText("Are you sure you want to cancel this order? This action cannot be undone.");
        dialog.setCancelable(true);
        dialog.setConfirmText("Cancel Order");
        dialog.setConfirmButtonTheme("error primary");

        dialog.addConfirmListener(e -> cancelOrder());

        dialog.open();
    }

    private void cancelOrder() {
        try {
            currentOrder = orderService.cancelOrder(currentOrder.getId());
            displayOrderInfo();
            updateActionButtons();

            Notification.show("Order cancelled")
                    .addThemeVariants(NotificationVariant.LUMO_SUCCESS);
        } catch (Exception e) {
            Notification.show("Error: " + e.getMessage())
                    .addThemeVariants(NotificationVariant.LUMO_ERROR);
        }
    }
}
