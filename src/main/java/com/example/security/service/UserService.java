package com.example.security.service;

import com.example.security.domain.Role;
import com.example.security.domain.User;
import com.example.security.domain.UserRepository;
import jakarta.annotation.security.RolesAllowed;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
@Transactional
public class UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public UserService(UserRepository userRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @RolesAllowed("ROLE_ADMIN")
    public User createUser(String username, String password, String fullName, Role role) {
        if (userRepository.existsByUsername(username)) {
            throw new IllegalArgumentException("Username already exists");
        }

        String passwordHash = passwordEncoder.encode(password);
        User user = new User(username, passwordHash, fullName, role);
        return userRepository.save(user);
    }

    @RolesAllowed("ROLE_ADMIN")
    public User updateUser(Long id, String fullName, Role role, boolean active) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));

        user.setFullName(fullName);
        user.setRole(role);
        user.setActive(active);
        return userRepository.save(user);
    }

    @RolesAllowed("ROLE_ADMIN")
    public void changePassword(Long id, String newPassword) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));

        String passwordHash = passwordEncoder.encode(newPassword);
        user.setPasswordHash(passwordHash);
        userRepository.save(user);
    }

    @RolesAllowed("ROLE_ADMIN")
    public void deleteUser(Long id) {
        userRepository.deleteById(id);
    }

    @RolesAllowed({"ROLE_ADMIN", "ROLE_EMPLOYEE"})
    public Optional<User> findById(Long id) {
        return userRepository.findById(id);
    }

    @RolesAllowed({"ROLE_ADMIN", "ROLE_EMPLOYEE"})
    public Optional<User> findByUsername(String username) {
        return userRepository.findByUsername(username);
    }

    @RolesAllowed("ROLE_ADMIN")
    public List<User> findAll() {
        return userRepository.findAll();
    }
}
