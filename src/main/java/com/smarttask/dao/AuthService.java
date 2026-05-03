package com.smarttask.dao;

import com.smarttask.model.User;
import com.smarttask.model.UserRole;

import java.util.Optional;

public class AuthService {
    private final UserRepository userRepository;

    public AuthService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    public Optional<User> loginBackoffice(String email, String password) {
        return userRepository.findByCredentialsAndRole(email, password, UserRole.ADMIN);
    }

    public Optional<User> loginFrontoffice(String email, String password) {
        return userRepository.findByCredentialsAndRole(email, password, UserRole.USER);
    }
}
