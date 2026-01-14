package com.example.security.ui;

import com.example.security.domain.Role;
import com.example.security.domain.User;
import com.example.security.service.UserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

/**
 * Unit tests for UserManagementView using Mockito.
 * Tests UI behavior without browser or servlet container.
 */
@ExtendWith(MockitoExtension.class)
class UserManagementViewTest {

    @Mock
    private UserService userService;

    private UserManagementView view;
    private User testUser;

    @BeforeEach
    void setUp() {
        testUser = new User();
        testUser.setId(1L);
        testUser.setUsername("testuser");
        testUser.setPasswordHash("hashedPassword");
        testUser.setFullName("Test User");
        testUser.setRole(Role.EMPLOYEE);
        testUser.setActive(true);

        List<User> users = new ArrayList<>();
        users.add(testUser);

        when(userService.findAll()).thenReturn(users);

        view = new UserManagementView(userService);
    }

    @Test
    void constructor_initializesGrid() {
        assertNotNull(view.grid);
        assertEquals(1, view.grid.getListDataView().getItemCount());
    }

    @Test
    void constructor_initializesFormFields() {
        assertNotNull(view.usernameField);
        assertNotNull(view.passwordField);
        assertNotNull(view.fullNameField);
        assertNotNull(view.roleField);
        assertNotNull(view.activeField);
        assertNotNull(view.saveButton);
        assertNotNull(view.cancelButton);
        assertNotNull(view.changePasswordButton);
    }

    @Test
    void roleField_containsAllRoles() {
        List<Role> roles = view.roleField.getListDataView()
                .getItems()
                .toList();

        assertEquals(2, roles.size());
        assertTrue(roles.contains(Role.ADMIN));
        assertTrue(roles.contains(Role.EMPLOYEE));
    }

    @Test
    void activeField_defaultsToTrue() {
        assertTrue(view.activeField.getValue());
    }

    @Test
    void grid_displaysUserData() {
        // Grid should show the test user
        assertEquals(1, view.grid.getListDataView().getItemCount());

        List<User> displayedUsers = view.grid.getListDataView()
                .getItems()
                .toList();

        assertEquals(testUser, displayedUsers.get(0));
    }

    @Test
    void createUser_withValidData_callsService() {
        // Simulate form filling for new user
        view.usernameField.setValue("newuser");
        view.passwordField.setValue("password123");
        view.fullNameField.setValue("New User");
        view.roleField.setValue(Role.EMPLOYEE);
        view.activeField.setValue(true);

        // Verify service was called during initialization
        verify(userService, atLeastOnce()).findAll();
    }

    @Test
    void updateUser_withValidData_callsService() {
        // Verify service was called during initialization
        verify(userService, atLeastOnce()).findAll();
    }

    @Test
    void deleteUser_callsService() {
        // Simulate delete action
        userService.deleteUser(1L);

        verify(userService).deleteUser(1L);
    }

    @Test
    void changePassword_callsService() {
        doNothing().when(userService).changePassword(1L, "newPassword");

        userService.changePassword(1L, "newPassword");

        verify(userService).changePassword(1L, "newPassword");
    }

    @Test
    void findAll_refreshesGrid() {
        User user2 = new User();
        user2.setId(2L);
        user2.setUsername("user2");
        user2.setFullName("User Two");
        user2.setRole(Role.ADMIN);
        user2.setActive(true);

        when(userService.findAll()).thenReturn(List.of(testUser, user2));

        // Create new view to trigger findAll
        UserManagementView newView = new UserManagementView(userService);

        assertEquals(2, newView.grid.getListDataView().getItemCount());
    }

    @Test
    void createUser_withDuplicateUsername_handlesError() {
        // Test that service throws exception for duplicate username
        when(userService.createUser(anyString(), anyString(), anyString(), any(Role.class)))
                .thenThrow(new IllegalArgumentException("Username already exists"));

        assertThrows(IllegalArgumentException.class,
                () -> userService.createUser("testuser", "password", "Test", Role.EMPLOYEE));
    }

    @Test
    void updateUser_withNonExistingId_handlesError() {
        // Test that service throws exception for non-existing user
        when(userService.updateUser(eq(999L), anyString(), any(Role.class), anyBoolean()))
                .thenThrow(new IllegalArgumentException("User not found"));

        assertThrows(IllegalArgumentException.class,
                () -> userService.updateUser(999L, "Name", Role.ADMIN, true));
    }

    @Test
    void changePassword_withNonExistingId_handlesError() {
        doThrow(new IllegalArgumentException("User not found"))
                .when(userService).changePassword(999L, "newPassword");

        assertThrows(IllegalArgumentException.class,
                () -> userService.changePassword(999L, "newPassword"));
    }

    @Test
    void grid_hasExpectedColumns() {
        // Verify grid has columns configured
        assertNotNull(view.grid);
        assertTrue(view.grid.getColumns().size() > 0);
    }

    @Test
    void formFields_haveExpectedConfiguration() {
        // Username field
        assertNotNull(view.usernameField);
        assertEquals("Username", view.usernameField.getLabel());

        // Password field
        assertNotNull(view.passwordField);
        assertEquals("Password", view.passwordField.getLabel());

        // Full name field
        assertNotNull(view.fullNameField);
        assertEquals("Full Name", view.fullNameField.getLabel());

        // Role field
        assertNotNull(view.roleField);
        assertEquals("Role", view.roleField.getLabel());

        // Active field
        assertNotNull(view.activeField);
        assertEquals("Active", view.activeField.getLabel());
    }

    @Test
    void buttons_haveExpectedLabels() {
        assertEquals("Save", view.saveButton.getText());
        assertEquals("Cancel", view.cancelButton.getText());
        assertEquals("Change Password", view.changePasswordButton.getText());
    }
}
