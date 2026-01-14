package com.example.orders.ui;

import com.example.orders.domain.Order;
import com.example.orders.domain.OrderRepository;
import com.example.orders.domain.OrderState;
import com.example.orders.signals.OrderSignals;
import com.example.shared.ui.MainLayout;
import com.vaadin.flow.component.ComponentEffect;
import com.vaadin.flow.component.UI;
import com.vaadin.signals.ValueSignal;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.html.H3;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import jakarta.annotation.security.RolesAllowed;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Route(value = "", layout = MainLayout.class)
@PageTitle("Dashboard")
@RolesAllowed({"ADMIN", "EMPLOYEE"})
public class DashboardView extends VerticalLayout {

    private final OrderRepository orderRepository;

    // Stats cards
    private final Span todayCountValue = new Span("0");
    private final Span newCountValue = new Span("0");
    private final Span readyCountValue = new Span("0");
    private final Span deliveredCountValue = new Span("0");

    // Grid for today's orders
    private final Grid<Order> todayOrdersGrid = new Grid<>(Order.class, false);

    public DashboardView(OrderRepository orderRepository) {
        this.orderRepository = orderRepository;

        // Initialize signals on first load
        OrderSignals.refreshAll(orderRepository);

        setSizeFull();
        setPadding(true);

        add(new H2("Order Dashboard"));
        add(createStatsSection());
        add(createTodayOrdersSection());

        setupReactiveUpdates();
    }

    private HorizontalLayout createStatsSection() {
        HorizontalLayout statsLayout = new HorizontalLayout();
        statsLayout.setWidthFull();
        statsLayout.setSpacing(true);

        statsLayout.add(
                createStatCard("Today's Orders", todayCountValue, "blue"),
                createStatCard("NEW", newCountValue, "orange"),
                createStatCard("READY", readyCountValue, "green"),
                createStatCard("DELIVERED", deliveredCountValue, "gray")
        );

        return statsLayout;
    }

    private Div createStatCard(String title, Span value, String color) {
        Div card = new Div();
        card.addClassNames("stat-card", "stat-card-" + color);
        card.getStyle()
                .set("padding", "20px")
                .set("border-radius", "8px")
                .set("background-color", "#f5f5f5")
                .set("flex", "1")
                .set("text-align", "center");

        H3 titleLabel = new H3(title);
        titleLabel.getStyle().set("margin", "0 0 10px 0");

        value.getStyle()
                .set("font-size", "32px")
                .set("font-weight", "bold")
                .set("color", getColorForStat(color));

        card.add(titleLabel, value);
        return card;
    }

    private String getColorForStat(String color) {
        return switch (color) {
            case "blue" -> "#1976d2";
            case "orange" -> "#f57c00";
            case "green" -> "#388e3c";
            case "gray" -> "#757575";
            default -> "#000000";
        };
    }

    private VerticalLayout createTodayOrdersSection() {
        VerticalLayout section = new VerticalLayout();
        section.setSizeFull();
        section.setPadding(false);

        H3 title = new H3("Today's Orders (Due: " +
                LocalDate.now().format(DateTimeFormatter.ofPattern("MMM dd, yyyy")) + ")");

        configureTodayOrdersGrid();

        section.add(title, todayOrdersGrid);
        return section;
    }

    private void configureTodayOrdersGrid() {
        todayOrdersGrid.setSizeFull();

        todayOrdersGrid.addColumn(Order::getId).setHeader("ID").setWidth("80px");
        todayOrdersGrid.addColumn(order -> order.getCustomer() != null ?
                order.getCustomer().getName() : "N/A")
                .setHeader("Customer")
                .setAutoWidth(true);
        todayOrdersGrid.addColumn(Order::getState).setHeader("Status").setAutoWidth(true);
        todayOrdersGrid.addColumn(order -> order.getTotalPrice() != null ?
                "$" + order.getTotalPrice() : "$0.00")
                .setHeader("Total")
                .setAutoWidth(true);
        todayOrdersGrid.addColumn(Order::getPickupLocation)
                .setHeader("Pickup Location")
                .setAutoWidth(true);
        todayOrdersGrid.addColumn(order -> order.isPaid() ? "Yes" : "No")
                .setHeader("Paid")
                .setAutoWidth(true);

        // Navigate to order details on row click
        todayOrdersGrid.addItemClickListener(event -> {
            if (event.getItem() != null) {
                UI.getCurrent().navigate(OrderDetailsView.class, event.getItem().getId());
            }
        });
    }

    private void setupReactiveUpdates() {
        // Reactive effect for today's order count
        ComponentEffect.effect(this, () -> {
            Number count = OrderSignals.getTodayOrderCountSignal().value();
            todayCountValue.setText(count.toString());
        });

        // Reactive effect for NEW order count
        ComponentEffect.effect(this, () -> {
            Number count = OrderSignals.getNewOrderCountSignal().value();
            newCountValue.setText(count.toString());
        });

        // Reactive effect for READY order count
        ComponentEffect.effect(this, () -> {
            Number count = OrderSignals.getReadyOrderCountSignal().value();
            readyCountValue.setText(count.toString());
        });

        // Reactive effect for DELIVERED order count
        ComponentEffect.effect(this, () -> {
            Number count = OrderSignals.getDeliveredOrderCountSignal().value();
            deliveredCountValue.setText(count.toString());
        });

        // Reactive effect for today's orders grid
        ComponentEffect.effect(this, () -> {
            List<ValueSignal<Order>> orderSignals = OrderSignals.getOrdersSignal().value();
            LocalDate today = LocalDate.now();

            // Convert to orders and filter to show only today's orders
            List<Order> todayOrders = orderSignals.stream()
                    .map(ValueSignal::value)
                    .filter(order -> order.getDueDate().equals(today))
                    .toList();

            todayOrdersGrid.setItems(todayOrders);
        });
    }
}
