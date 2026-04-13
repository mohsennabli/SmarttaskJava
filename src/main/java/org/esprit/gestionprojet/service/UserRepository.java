package org.esprit.gestionprojet.service;

import org.esprit.gestionprojet.model.User;
import org.esprit.gestionprojet.model.UserRole;
import org.esprit.gestionprojet.util.DBconnexion;
import org.mindrot.jbcrypt.BCrypt;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.Optional;

public class UserRepository {
    public Optional<User> findByCredentialsAndRole(String email, String password, UserRole role) {
        String sql = """
            SELECT email, password, name, roles
            FROM user
            WHERE lower(email) = lower(?)
            LIMIT 1
            """;

        try (Connection connection = DBconnexion.getInstance().getCnx();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, email.trim());

            try (ResultSet rs = statement.executeQuery()) {
                if (!rs.next()) {
                    return Optional.empty();
                }

                String dbPassword = rs.getString("password");
                String dbRoles = rs.getString("roles");

                if (!passwordMatches(password, dbPassword)) {
                    return Optional.empty();
                }

                if (!roleMatches(role, dbRoles)) {
                    return Optional.empty();
                }

                User user = new User(
                    rs.getString("email"),
                    dbPassword,
                    rs.getString("name"),
                    role
                );
                return Optional.of(user);
            }
        } catch (Exception e) {
            throw new IllegalStateException("Failed to read user from database", e);
        }
    }

    private boolean roleMatches(UserRole role, String rolesJson) {
        String roles = rolesJson == null ? "" : rolesJson.toUpperCase();
        if (role == UserRole.ADMIN) {
            return roles.contains("ROLE_ADMIN");
        }
        return !roles.contains("ROLE_ADMIN");
    }

    private boolean passwordMatches(String rawPassword, String storedPassword) {
        if (storedPassword == null) {
            return false;
        }

        String normalizedHash = storedPassword;
        if (storedPassword.startsWith("$2y$")) {
            normalizedHash = "$2a$" + storedPassword.substring(4);
        }

        if (normalizedHash.startsWith("$2a$") || normalizedHash.startsWith("$2b$")) {
            try {
                return BCrypt.checkpw(rawPassword, normalizedHash);
            } catch (IllegalArgumentException ex) {
                return false;
            }
        }

        return storedPassword.equals(rawPassword);
    }
}
