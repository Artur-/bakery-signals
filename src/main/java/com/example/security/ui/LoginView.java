package com.example.security.ui;

import com.vaadin.flow.component.html.H1;
import com.vaadin.flow.component.html.Main;
import com.vaadin.flow.component.login.LoginForm;
import com.vaadin.flow.router.BeforeEnterEvent;
import com.vaadin.flow.router.BeforeEnterObserver;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.server.auth.AnonymousAllowed;
import com.vaadin.flow.theme.lumo.LumoUtility;

@Route(value = "login", autoLayout = false)
@PageTitle("Login - Bakery System")
@AnonymousAllowed
public class LoginView extends Main implements BeforeEnterObserver {

    private final LoginForm loginForm = new LoginForm();

    public LoginView() {
        addClassNames(
                LumoUtility.Display.FLEX,
                LumoUtility.FlexDirection.COLUMN,
                LumoUtility.JustifyContent.CENTER,
                LumoUtility.AlignItems.CENTER,
                LumoUtility.Height.FULL,
                LumoUtility.Width.FULL
        );

        loginForm.setAction("login");
        loginForm.setForgotPasswordButtonVisible(false);

        add(
                new H1("Bakery Order System"),
                loginForm
        );
    }

    @Override
    public void beforeEnter(BeforeEnterEvent event) {
        // Show error message if login failed
        if (event.getLocation().getQueryParameters().getParameters().containsKey("error")) {
            loginForm.setError(true);
        }
    }
}
