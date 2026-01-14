package com.example.security.ui;

import com.example.security.domain.Role;
import com.example.security.domain.User;
import com.example.security.service.UserService;
import com.example.shared.ui.MainLayout;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.checkbox.Checkbox;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.confirmdialog.ConfirmDialog;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.formlayout.FormLayout;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.notification.NotificationVariant;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.PasswordField;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.data.binder.BeanValidationBinder;
import com.vaadin.flow.data.binder.Binder;
import com.vaadin.flow.data.binder.ValidationException;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import jakarta.annotation.security.RolesAllowed;

@Route(value = "users", layout = MainLayout.class)
@PageTitle("User Management")
@RolesAllowed("ADMIN")
public class UserManagementView extends VerticalLayout {

    private final UserService userService;

    // Grid and form components (package-protected for testing)
    final Grid<User> grid = new Grid<>(User.class, false);
    final TextField usernameField = new TextField("Username");
    final PasswordField passwordField = new PasswordField("Password");
    final TextField fullNameField = new TextField("Full Name");
    final ComboBox<Role> roleField = new ComboBox<>("Role");
    final Checkbox activeField = new Checkbox("Active");
    final Button saveButton = new Button("Save");
    final Button cancelButton = new Button("Cancel");
    final Button changePasswordButton = new Button("Change Password");

    private final Binder<User> binder = new BeanValidationBinder<>(User.class);
    private User currentUser;
    private boolean isNewUser = false;

    private Dialog formDialog;

    public UserManagementView(UserService userService) {
        this.userService = userService;

        setSizeFull();
        setPadding(true);

        add(new H2("User Management"));
        add(createToolbar());
        add(grid);

        configureGrid();
        configureForm();
        refreshGrid();
    }

    private HorizontalLayout createToolbar() {
        Button newUserButton = new Button("New User");
        newUserButton.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
        newUserButton.addClickListener(e -> openFormForNewUser());

        HorizontalLayout toolbar = new HorizontalLayout(newUserButton);
        toolbar.setWidthFull();
        toolbar.setJustifyContentMode(JustifyContentMode.END);
        return toolbar;
    }

    private void configureGrid() {
        grid.setSizeFull();

        grid.addColumn(User::getId).setHeader("ID").setWidth("80px").setSortable(true);
        grid.addColumn(User::getUsername).setHeader("Username").setSortable(true);
        grid.addColumn(User::getFullName).setHeader("Full Name").setSortable(true);
        grid.addColumn(user -> user.getRole().getDisplayName())
                .setHeader("Role")
                .setSortable(true);
        grid.addColumn(user -> user.isActive() ? "Yes" : "No")
                .setHeader("Active")
                .setSortable(true);

        grid.addComponentColumn(user -> {
            Button editButton = new Button("Edit");
            editButton.addClickListener(e -> openFormForEdit(user));

            Button deleteButton = new Button("Delete");
            deleteButton.addThemeVariants(ButtonVariant.LUMO_ERROR);
            deleteButton.addClickListener(e -> confirmDelete(user));

            return new HorizontalLayout(editButton, deleteButton);
        }).setHeader("Actions").setAutoWidth(true);

        grid.asSingleSelect().addValueChangeListener(event -> {
            if (event.getValue() != null) {
                openFormForEdit(event.getValue());
            }
        });
    }

    private void configureForm() {
        // Configure role field
        roleField.setItems(Role.values());
        roleField.setItemLabelGenerator(Role::getDisplayName);

        // Configure active field
        activeField.setValue(true);

        // Configure buttons
        saveButton.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
        saveButton.addClickListener(e -> saveUser());

        cancelButton.addClickListener(e -> closeForm());

        changePasswordButton.addClickListener(e -> openPasswordChangeDialog());

        // Bind fields
        binder.forField(usernameField)
                .asRequired("Username is required")
                .bind(User::getUsername, User::setUsername);

        binder.forField(fullNameField)
                .asRequired("Full name is required")
                .bind(User::getFullName, User::setFullName);

        binder.forField(roleField)
                .asRequired("Role is required")
                .bind(User::getRole, User::setRole);

        binder.forField(activeField)
                .bind(User::isActive, User::setActive);
    }

    private void openFormForNewUser() {
        isNewUser = true;
        currentUser = new User();
        currentUser.setActive(true);

        binder.readBean(currentUser);

        // Show all fields for new user
        usernameField.setVisible(true);
        usernameField.setReadOnly(false);
        passwordField.setVisible(true);
        passwordField.setRequired(true);
        passwordField.clear();
        changePasswordButton.setVisible(false);

        showFormDialog();
    }

    private void openFormForEdit(User user) {
        isNewUser = false;
        currentUser = user;

        binder.readBean(user);

        // Hide username for editing (cannot change username)
        usernameField.setVisible(true);
        usernameField.setReadOnly(true);
        passwordField.setVisible(false);
        changePasswordButton.setVisible(true);

        showFormDialog();
    }

    private void showFormDialog() {
        formDialog = new Dialog();
        formDialog.setHeaderTitle(isNewUser ? "New User" : "Edit User");
        formDialog.setWidth("500px");

        FormLayout formLayout = new FormLayout();
        formLayout.add(
                usernameField,
                passwordField,
                fullNameField,
                roleField,
                activeField
        );

        HorizontalLayout buttonLayout = new HorizontalLayout();
        buttonLayout.add(saveButton, cancelButton);
        if (!isNewUser) {
            buttonLayout.add(changePasswordButton);
        }
        buttonLayout.setSpacing(true);

        VerticalLayout dialogContent = new VerticalLayout(formLayout, buttonLayout);
        dialogContent.setPadding(false);

        formDialog.add(dialogContent);
        formDialog.open();
    }

    private void saveUser() {
        try {
            binder.writeBean(currentUser);

            if (isNewUser) {
                // Validate password
                String password = passwordField.getValue();
                if (password == null || password.trim().isEmpty()) {
                    Notification.show("Password is required", 3000, Notification.Position.MIDDLE)
                            .addThemeVariants(NotificationVariant.LUMO_ERROR);
                    return;
                }

                userService.createUser(
                        currentUser.getUsername(),
                        password,
                        currentUser.getFullName(),
                        currentUser.getRole()
                );
                Notification.show("User created successfully", 3000, Notification.Position.MIDDLE)
                        .addThemeVariants(NotificationVariant.LUMO_SUCCESS);
            } else {
                userService.updateUser(
                        currentUser.getId(),
                        currentUser.getFullName(),
                        currentUser.getRole(),
                        currentUser.isActive()
                );
                Notification.show("User updated successfully", 3000, Notification.Position.MIDDLE)
                        .addThemeVariants(NotificationVariant.LUMO_SUCCESS);
            }

            refreshGrid();
            closeForm();

        } catch (ValidationException e) {
            Notification.show("Please fix validation errors", 3000, Notification.Position.MIDDLE)
                    .addThemeVariants(NotificationVariant.LUMO_ERROR);
        } catch (IllegalArgumentException e) {
            Notification.show("Error: " + e.getMessage(), 3000, Notification.Position.MIDDLE)
                    .addThemeVariants(NotificationVariant.LUMO_ERROR);
        }
    }

    private void openPasswordChangeDialog() {
        Dialog passwordDialog = new Dialog();
        passwordDialog.setHeaderTitle("Change Password");
        passwordDialog.setWidth("400px");

        PasswordField newPasswordField = new PasswordField("New Password");
        newPasswordField.setRequired(true);
        newPasswordField.setWidthFull();

        PasswordField confirmPasswordField = new PasswordField("Confirm Password");
        confirmPasswordField.setRequired(true);
        confirmPasswordField.setWidthFull();

        Button savePasswordButton = new Button("Save Password");
        savePasswordButton.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
        savePasswordButton.addClickListener(e -> {
            String newPassword = newPasswordField.getValue();
            String confirmPassword = confirmPasswordField.getValue();

            if (newPassword == null || newPassword.trim().isEmpty()) {
                Notification.show("Password cannot be empty", 3000, Notification.Position.MIDDLE)
                        .addThemeVariants(NotificationVariant.LUMO_ERROR);
                return;
            }

            if (!newPassword.equals(confirmPassword)) {
                Notification.show("Passwords do not match", 3000, Notification.Position.MIDDLE)
                        .addThemeVariants(NotificationVariant.LUMO_ERROR);
                return;
            }

            try {
                userService.changePassword(currentUser.getId(), newPassword);
                Notification.show("Password changed successfully", 3000, Notification.Position.MIDDLE)
                        .addThemeVariants(NotificationVariant.LUMO_SUCCESS);
                passwordDialog.close();
            } catch (IllegalArgumentException ex) {
                Notification.show("Error: " + ex.getMessage(), 3000, Notification.Position.MIDDLE)
                        .addThemeVariants(NotificationVariant.LUMO_ERROR);
            }
        });

        Button cancelPasswordButton = new Button("Cancel");
        cancelPasswordButton.addClickListener(e -> passwordDialog.close());

        HorizontalLayout buttonLayout = new HorizontalLayout(savePasswordButton, cancelPasswordButton);
        buttonLayout.setSpacing(true);

        VerticalLayout dialogContent = new VerticalLayout(newPasswordField, confirmPasswordField, buttonLayout);
        dialogContent.setPadding(false);

        passwordDialog.add(dialogContent);
        passwordDialog.open();
    }

    private void confirmDelete(User user) {
        ConfirmDialog dialog = new ConfirmDialog();
        dialog.setHeader("Delete User?");
        dialog.setText("Are you sure you want to delete user '" + user.getUsername() + "'? This action cannot be undone.");
        dialog.setCancelable(true);
        dialog.setConfirmText("Delete");
        dialog.setConfirmButtonTheme("error primary");

        dialog.addConfirmListener(e -> deleteUser(user));
        dialog.open();
    }

    private void deleteUser(User user) {
        try {
            userService.deleteUser(user.getId());
            Notification.show("User deleted successfully", 3000, Notification.Position.MIDDLE)
                    .addThemeVariants(NotificationVariant.LUMO_SUCCESS);
            refreshGrid();
        } catch (Exception e) {
            Notification.show("Error deleting user: " + e.getMessage(), 3000, Notification.Position.MIDDLE)
                    .addThemeVariants(NotificationVariant.LUMO_ERROR);
        }
    }

    private void closeForm() {
        if (formDialog != null) {
            formDialog.close();
        }
        binder.readBean(null);
        currentUser = null;
    }

    private void refreshGrid() {
        grid.setItems(userService.findAll());
    }
}
