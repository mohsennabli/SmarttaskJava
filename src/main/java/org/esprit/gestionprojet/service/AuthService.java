package org.esprit.gestionprojet.service;

import org.esprit.gestionprojet.model.User;
import org.esprit.gestionprojet.model.UserRole;

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
