package com.example.home;

import com.example.shared.ui.MainLayout;
import com.vaadin.flow.component.html.H1;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import jakarta.annotation.security.PermitAll;

@Route(value = "", layout = MainLayout.class)
@PageTitle("Home - Bakery System")
@PermitAll
public class HomeView extends VerticalLayout {

    public HomeView() {
        setSpacing(true);
        setPadding(true);

        add(
            new H1("Welcome to Bakery Order System"),
            new Paragraph("You have successfully logged in!"),
            new Paragraph("Phase 1 is complete. More features coming in Phase 2-6.")
        );
    }
}
