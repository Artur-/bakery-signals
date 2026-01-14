package com.example.shared.ui;

import com.example.customers.ui.CustomerManagementView;
import com.example.orders.ui.DashboardView;
import com.example.orders.ui.OrderListView;
import com.example.products.ui.ProductManagementView;
import com.example.security.ui.UserManagementView;
import com.vaadin.flow.component.applayout.AppLayout;
import com.vaadin.flow.component.applayout.DrawerToggle;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.html.H1;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.sidenav.SideNav;
import com.vaadin.flow.component.sidenav.SideNavItem;
import com.vaadin.flow.spring.security.AuthenticationContext;
import com.vaadin.flow.theme.lumo.LumoUtility;
import jakarta.annotation.security.PermitAll;

@PermitAll
public class MainLayout extends AppLayout {

    private final AuthenticationContext authenticationContext;

    public MainLayout(AuthenticationContext authenticationContext) {
        this.authenticationContext = authenticationContext;
        createHeader();
        createDrawer();
    }

    private void createHeader() {
        H1 logo = new H1("Bakery System");
        logo.addClassNames(LumoUtility.FontSize.LARGE, LumoUtility.Margin.MEDIUM);

        Button logout = new Button("Logout", e -> authenticationContext.logout());

        HorizontalLayout header = new HorizontalLayout(new DrawerToggle(), logo, logout);
        header.setDefaultVerticalComponentAlignment(FlexComponent.Alignment.CENTER);
        header.expand(logo);
        header.setWidthFull();
        header.addClassNames(
                LumoUtility.Padding.Vertical.NONE,
                LumoUtility.Padding.Horizontal.MEDIUM
        );

        addToNavbar(header);
    }

    private void createDrawer() {
        SideNav navigation = new SideNav();

        // Add dashboard (home) navigation
        navigation.addItem(new SideNavItem("Dashboard", DashboardView.class, VaadinIcon.DASHBOARD.create()));

        // Orders navigation (available to both ADMIN and EMPLOYEE)
        navigation.addItem(new SideNavItem("Orders", OrderListView.class, VaadinIcon.CLIPBOARD_TEXT.create()));

        // Admin-only menu items
        if (authenticationContext.hasRole("ADMIN")) {
            navigation.addItem(new SideNavItem("Products", ProductManagementView.class, VaadinIcon.PACKAGE.create()));
            navigation.addItem(new SideNavItem("Customers", CustomerManagementView.class, VaadinIcon.USERS.create()));
            navigation.addItem(new SideNavItem("User Management", UserManagementView.class, VaadinIcon.USER.create()));
        }

        addToDrawer(navigation);
    }
}
