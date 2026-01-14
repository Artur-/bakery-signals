package com.example.orders.ui;

import com.example.orders.domain.Order;
import com.example.orders.domain.OrderRepository;
import com.example.orders.domain.OrderState;
import com.example.orders.signals.OrderSignals;
import com.example.shared.ui.MainLayout;
import com.vaadin.flow.component.ComponentEffect;
import com.vaadin.flow.component.UI;
import com.vaadin.signals.ValueSignal;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.datepicker.DatePicker;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.data.value.ValueChangeMode;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import jakarta.annotation.security.RolesAllowed;

import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;

@Route(value = "orders", layout = MainLayout.class)
@PageTitle("Order List")
@RolesAllowed({"ADMIN", "EMPLOYEE"})
public class OrderListView extends VerticalLayout {

    private final OrderRepository orderRepository;

    // Filters (package-protected for UI unit testing)
    final ComboBox<OrderState> stateFilter = new ComboBox<>("Filter by State");
    final DatePicker fromDateFilter = new DatePicker("From Date");
    final DatePicker toDateFilter = new DatePicker("To Date");
    final TextField customerSearchField = new TextField("Search Customer");
    final Button clearFiltersButton = new Button("Clear Filters");
    final Button newOrderButton = new Button("New Order");

    // Grid (package-protected for UI unit testing)
    final Grid<Order> grid = new Grid<>(Order.class, false);

    // Cached filter values
    private OrderState currentStateFilter = null;
    private LocalDate currentFromDate = null;
    private LocalDate currentToDate = null;
    private String currentCustomerSearch = "";

    public OrderListView(OrderRepository orderRepository) {
        this.orderRepository = orderRepository;

        // Initialize signals
        OrderSignals.refreshAll(orderRepository);

        setSizeFull();
        setPadding(true);

        add(new H2("Orders"));
        add(createToolbar());
        add(createFilterBar());
        add(grid);

        configureGrid();
        setupReactiveUpdates();
    }

    private HorizontalLayout createToolbar() {
        newOrderButton.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
        newOrderButton.addClickListener(e ->
                UI.getCurrent().navigate(NewOrderView.class));

        HorizontalLayout toolbar = new HorizontalLayout(newOrderButton);
        toolbar.setWidthFull();
        toolbar.setJustifyContentMode(JustifyContentMode.END);
        return toolbar;
    }

    private HorizontalLayout createFilterBar() {
        // State filter
        stateFilter.setItems(OrderState.values());
        stateFilter.setItemLabelGenerator(OrderState::name);
        stateFilter.setClearButtonVisible(true);
        stateFilter.addValueChangeListener(e -> {
            currentStateFilter = e.getValue();
            applyFilters();
        });

        // Date filters
        fromDateFilter.setClearButtonVisible(true);
        fromDateFilter.addValueChangeListener(e -> {
            currentFromDate = e.getValue();
            applyFilters();
        });

        toDateFilter.setClearButtonVisible(true);
        toDateFilter.addValueChangeListener(e -> {
            currentToDate = e.getValue();
            applyFilters();
        });

        // Customer search
        customerSearchField.setPlaceholder("Search by customer name...");
        customerSearchField.setClearButtonVisible(true);
        customerSearchField.setValueChangeMode(ValueChangeMode.LAZY);
        customerSearchField.addValueChangeListener(e -> {
            currentCustomerSearch = e.getValue() != null ? e.getValue().trim() : "";
            applyFilters();
        });

        // Clear filters button
        clearFiltersButton.addClickListener(e -> clearFilters());

        HorizontalLayout filterBar = new HorizontalLayout(
                stateFilter,
                fromDateFilter,
                toDateFilter,
                customerSearchField,
                clearFiltersButton
        );
        filterBar.setWidthFull();
        filterBar.setDefaultVerticalComponentAlignment(Alignment.END);
        return filterBar;
    }

    private void configureGrid() {
        grid.setSizeFull();

        grid.addColumn(Order::getId).setHeader("ID").setWidth("80px").setSortable(true);
        grid.addColumn(order -> order.getCustomer() != null ?
                order.getCustomer().getName() : "N/A")
                .setHeader("Customer")
                .setSortable(true);
        grid.addColumn(Order::getDueDate).setHeader("Due Date").setSortable(true);
        grid.addColumn(Order::getState).setHeader("Status").setSortable(true);
        grid.addColumn(order -> order.getTotalPrice() != null ?
                "$" + order.getTotalPrice() : "$0.00")
                .setHeader("Total")
                .setSortable(true);
        grid.addColumn(Order::getPickupLocation)
                .setHeader("Pickup Location")
                .setSortable(true);
        grid.addColumn(order -> order.isPaid() ? "Yes" : "No")
                .setHeader("Paid")
                .setSortable(true);

        // Navigate to order details on row click
        grid.addItemClickListener(event -> {
            if (event.getItem() != null) {
                UI.getCurrent().navigate(OrderDetailsView.class, event.getItem().getId());
            }
        });
    }

    private void setupReactiveUpdates() {
        // Reactive effect to update grid when orders change
        ComponentEffect.effect(this, () -> {
            List<ValueSignal<Order>> orderSignals = OrderSignals.getOrdersSignal().value();
            List<Order> allOrders = orderSignals.stream()
                    .map(ValueSignal::value)
                    .toList();
            applyFiltersToOrders(allOrders);
        });
    }

    private void applyFilters() {
        List<ValueSignal<Order>> orderSignals = OrderSignals.getOrdersSignal().value();
        List<Order> allOrders = orderSignals.stream()
                .map(ValueSignal::value)
                .toList();
        applyFiltersToOrders(allOrders);
    }

    private void applyFiltersToOrders(List<Order> orders) {
        List<Order> filtered = orders.stream()
                .filter(order -> currentStateFilter == null || order.getState() == currentStateFilter)
                .filter(order -> currentFromDate == null || !order.getDueDate().isBefore(currentFromDate))
                .filter(order -> currentToDate == null || !order.getDueDate().isAfter(currentToDate))
                .filter(order -> currentCustomerSearch.isEmpty() ||
                        (order.getCustomer() != null &&
                                order.getCustomer().getName().toLowerCase()
                                        .contains(currentCustomerSearch.toLowerCase())))
                .collect(Collectors.toList());

        grid.setItems(filtered);
    }

    private void clearFilters() {
        stateFilter.clear();
        fromDateFilter.clear();
        toDateFilter.clear();
        customerSearchField.clear();

        currentStateFilter = null;
        currentFromDate = null;
        currentToDate = null;
        currentCustomerSearch = "";

        applyFilters();
    }
}
