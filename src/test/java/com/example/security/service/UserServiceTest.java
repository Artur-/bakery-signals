package com.example.security.service;

import com.example.security.domain.Role;
import com.example.security.domain.User;
import com.example.security.domain.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

/**
 * Unit tests for UserService using Mockito.
 * Tests user management operations without Spring context.
 */
@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @InjectMocks
    private UserService userService;

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
    }

    @Test
    void createUser_withValidData_succeeds() {
        when(userRepository.existsByUsername("newuser")).thenReturn(false);
        when(passwordEncoder.encode("password123")).thenReturn("encodedPassword");
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> {
            User user = invocation.getArgument(0);
            user.setId(2L);
            return user;
        });

        User created = userService.createUser("newuser", "password123", "New User", Role.EMPLOYEE);

        assertNotNull(created);
        assertEquals("newuser", created.getUsername());
        assertEquals("New User", created.getFullName());
        assertEquals(Role.EMPLOYEE, created.getRole());
        assertTrue(created.isActive());
        verify(passwordEncoder).encode("password123");
        verify(userRepository).save(any(User.class));
    }

    @Test
    void createUser_withExistingUsername_throwsException() {
        when(userRepository.existsByUsername("testuser")).thenReturn(true);

        assertThrows(IllegalArgumentException.class,
                () -> userService.createUser("testuser", "password", "Test", Role.EMPLOYEE));

        verify(userRepository, never()).save(any());
    }

    @Test
    void updateUser_withValidData_succeeds() {
        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
        when(userRepository.save(any(User.class))).thenReturn(testUser);

        User updated = userService.updateUser(1L, "Updated Name", Role.ADMIN, false);

        assertNotNull(updated);
        assertEquals("Updated Name", updated.getFullName());
        assertEquals(Role.ADMIN, updated.getRole());
        assertFalse(updated.isActive());
        verify(userRepository).findById(1L);
        verify(userRepository).save(testUser);
    }

    @Test
    void updateUser_withNonExistingId_throwsException() {
        when(userRepository.findById(999L)).thenReturn(Optional.empty());

        assertThrows(IllegalArgumentException.class,
                () -> userService.updateUser(999L, "Name", Role.ADMIN, true));

        verify(userRepository, never()).save(any());
    }

    @Test
    void changePassword_withValidId_succeeds() {
        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
        when(passwordEncoder.encode("newPassword")).thenReturn("newHashedPassword");
        when(userRepository.save(any(User.class))).thenReturn(testUser);

        userService.changePassword(1L, "newPassword");

        verify(userRepository).findById(1L);
        verify(passwordEncoder).encode("newPassword");
        verify(userRepository).save(testUser);
        assertEquals("newHashedPassword", testUser.getPasswordHash());
    }

    @Test
    void changePassword_withNonExistingId_throwsException() {
        when(userRepository.findById(999L)).thenReturn(Optional.empty());

        assertThrows(IllegalArgumentException.class,
                () -> userService.changePassword(999L, "newPassword"));

        verify(passwordEncoder, never()).encode(anyString());
        verify(userRepository, never()).save(any());
    }

    @Test
    void deleteUser_withExistingId_succeeds() {
        doNothing().when(userRepository).deleteById(1L);

        userService.deleteUser(1L);

        verify(userRepository).deleteById(1L);
    }

    @Test
    void findById_withExistingId_returnsUser() {
        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));

        Optional<User> result = userService.findById(1L);

        assertTrue(result.isPresent());
        assertEquals(testUser, result.get());
        verify(userRepository).findById(1L);
    }

    @Test
    void findById_withNonExistingId_returnsEmpty() {
        when(userRepository.findById(999L)).thenReturn(Optional.empty());

        Optional<User> result = userService.findById(999L);

        assertFalse(result.isPresent());
        verify(userRepository).findById(999L);
    }

    @Test
    void findByUsername_withExistingUsername_returnsUser() {
        when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(testUser));

        Optional<User> result = userService.findByUsername("testuser");

        assertTrue(result.isPresent());
        assertEquals(testUser, result.get());
        verify(userRepository).findByUsername("testuser");
    }

    @Test
    void findByUsername_withNonExistingUsername_returnsEmpty() {
        when(userRepository.findByUsername("nonexistent")).thenReturn(Optional.empty());

        Optional<User> result = userService.findByUsername("nonexistent");

        assertFalse(result.isPresent());
        verify(userRepository).findByUsername("nonexistent");
    }

    @Test
    void findAll_returnsAllUsers() {
        User user2 = new User();
        user2.setId(2L);
        user2.setUsername("user2");
        user2.setFullName("User Two");
        user2.setRole(Role.ADMIN);

        List<User> users = List.of(testUser, user2);
        when(userRepository.findAll()).thenReturn(users);

        List<User> result = userService.findAll();

        assertEquals(2, result.size());
        assertEquals(testUser, result.get(0));
        assertEquals(user2, result.get(1));
        verify(userRepository).findAll();
    }

    @Test
    void createUser_encodesPassword() {
        String rawPassword = "myPassword123";
        String encodedPassword = "encoded_myPassword123";

        when(userRepository.existsByUsername(anyString())).thenReturn(false);
        when(passwordEncoder.encode(rawPassword)).thenReturn(encodedPassword);
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> {
            User user = invocation.getArgument(0);
            assertEquals(encodedPassword, user.getPasswordHash());
            return user;
        });

        userService.createUser("newuser", rawPassword, "New User", Role.EMPLOYEE);

        verify(passwordEncoder).encode(rawPassword);
    }

    @Test
    void updateUser_doesNotChangePasswordHash() {
        String originalPasswordHash = testUser.getPasswordHash();
        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
        when(userRepository.save(any(User.class))).thenReturn(testUser);

        userService.updateUser(1L, "New Name", Role.ADMIN, true);

        assertEquals(originalPasswordHash, testUser.getPasswordHash());
        verify(passwordEncoder, never()).encode(anyString());
    }

    @Test
    void createUser_setsActiveToTrue() {
        when(userRepository.existsByUsername(anyString())).thenReturn(false);
        when(passwordEncoder.encode(anyString())).thenReturn("encodedPassword");
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));

        User created = userService.createUser("newuser", "password", "New User", Role.EMPLOYEE);

        assertTrue(created.isActive());
    }
}
