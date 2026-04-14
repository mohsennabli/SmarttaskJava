package com.smarttask.dao;

import com.smarttask.model.User;
import com.smarttask.util.DatabaseConnection;
import org.mindrot.jbcrypt.BCrypt;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;

public class UserDAO {
    public boolean register(User user) {
        String sql = "INSERT INTO user (name, email, password, type, roles, is_enabled) VALUES (?, ?, ?, ?, ?, ?)";
        Connection connection = null;

        try {
            connection = DatabaseConnection.getConnection();
            try (PreparedStatement statement = connection.prepareStatement(sql)) {
                String roles = user.getRoles() == null ? "[]" : user.getRoles();
                String rawPassword = user.getPassword();
                if (rawPassword == null || rawPassword.isBlank()) {
                    return false;
                }
                String hashedPassword = BCrypt.hashpw(rawPassword, BCrypt.gensalt());

                statement.setString(1, user.getName());
                statement.setString(2, user.getEmail());
                statement.setString(3, hashedPassword);
                statement.setString(4, user.getType());
                statement.setString(5, roles);
                statement.setInt(6, 1);

                return statement.executeUpdate() > 0;
            }
        } catch (SQLException e) {
            System.err.println("Failed to register user: " + e.getMessage());
            return false;
        } finally {
            DatabaseConnection.closeConnection(connection);
        }
    }

    public boolean emailExists(String email) {
        String sql = "SELECT COUNT(*) FROM user WHERE email = ?";
        Connection connection = null;

        try {
            connection = DatabaseConnection.getConnection();
            try (PreparedStatement statement = connection.prepareStatement(sql)) {
                statement.setString(1, email);

                try (ResultSet resultSet = statement.executeQuery()) {
                    if (resultSet.next()) {
                        return resultSet.getInt(1) > 0;
                    }
                }
            }
        } catch (SQLException e) {
            System.err.println("Failed to check if email exists: " + e.getMessage());
        } finally {
            DatabaseConnection.closeConnection(connection);
        }

        return false;
    }

    public List<User> getAllUsers() {
        String sql = "SELECT * FROM user ORDER BY iduser ASC";
        Connection connection = null;
        List<User> users = new ArrayList<>();

        try {
            connection = DatabaseConnection.getConnection();
            try (PreparedStatement statement = connection.prepareStatement(sql);
                 ResultSet resultSet = statement.executeQuery()) {

                while (resultSet.next()) {
                    User user = new User();
                    user.setIduser(resultSet.getInt("iduser"));
                    user.setName(resultSet.getString("name"));
                    user.setEmail(resultSet.getString("email"));
                    user.setPassword(resultSet.getString("password"));
                    user.setType(resultSet.getString("type"));
                    user.setGoogleId(resultSet.getString("google_id"));
                    user.setRoles(resultSet.getString("roles"));
                    user.setEnabled(resultSet.getBoolean("is_enabled"));
                    user.setLinkedinId(resultSet.getString("linkedin_id"));
                    user.setResetToken(resultSet.getString("reset_token"));

                    Timestamp resetTokenExpiresAtTs = resultSet.getTimestamp("reset_token_expires_at");
                    if (resetTokenExpiresAtTs != null) {
                        user.setResetTokenExpiresAt(resetTokenExpiresAtTs.toLocalDateTime());
                    }

                    user.setAvatarName(resultSet.getString("avatar_name"));

                    Timestamp updatedAtTs = resultSet.getTimestamp("updated_at");
                    if (updatedAtTs != null) {
                        user.setUpdatedAt(updatedAtTs.toLocalDateTime());
                    }

                    user.setFaceEmbedding(resultSet.getString("face_embedding"));
                    users.add(user);
                }
            }
        } catch (SQLException e) {
            System.err.println("Failed to fetch users: " + e.getMessage());
            return new ArrayList<>();
        } finally {
            DatabaseConnection.closeConnection(connection);
        }

        return users;
    }

    public boolean updateUser(User user) {
        String sql = "UPDATE user SET name=?, email=?, type=?, is_enabled=? WHERE iduser=?";
        Connection connection = null;

        try {
            connection = DatabaseConnection.getConnection();
            try (PreparedStatement statement = connection.prepareStatement(sql)) {
                statement.setString(1, user.getName());
                statement.setString(2, user.getEmail());
                statement.setString(3, user.getType());
                statement.setBoolean(4, user.isEnabled());
                statement.setInt(5, user.getIduser());
                return statement.executeUpdate() > 0;
            }
        } catch (SQLException e) {
            System.err.println("Failed to update user: " + e.getMessage());
            return false;
        } finally {
            DatabaseConnection.closeConnection(connection);
        }
    }

    public boolean deleteUser(int iduser) {
        String sql = "DELETE FROM user WHERE iduser=?";
        Connection connection = null;

        try {
            connection = DatabaseConnection.getConnection();
            try (PreparedStatement statement = connection.prepareStatement(sql)) {
                statement.setInt(1, iduser);
                return statement.executeUpdate() > 0;
            }
        } catch (SQLException e) {
            System.err.println("Failed to delete user: " + e.getMessage());
            return false;
        } finally {
            DatabaseConnection.closeConnection(connection);
        }
    }

    public boolean toggleUserStatus(int iduser, boolean newStatus) {
        String sql = "UPDATE user SET is_enabled=? WHERE iduser=?";
        Connection connection = null;

        try {
            connection = DatabaseConnection.getConnection();
            try (PreparedStatement statement = connection.prepareStatement(sql)) {
                statement.setBoolean(1, newStatus);
                statement.setInt(2, iduser);
                return statement.executeUpdate() > 0;
            }
        } catch (SQLException e) {
            System.err.println("Failed to toggle user status: " + e.getMessage());
            return false;
        } finally {
            DatabaseConnection.closeConnection(connection);
        }
    }

    public List<User> searchUsers(String keyword) {
        String sql = "SELECT * FROM user WHERE name LIKE ? OR email LIKE ? OR type LIKE ?";
        Connection connection = null;
        List<User> users = new ArrayList<>();
        String pattern = "%" + keyword + "%";

        try {
            connection = DatabaseConnection.getConnection();
            try (PreparedStatement statement = connection.prepareStatement(sql)) {
                statement.setString(1, pattern);
                statement.setString(2, pattern);
                statement.setString(3, pattern);

                try (ResultSet resultSet = statement.executeQuery()) {
                    while (resultSet.next()) {
                        User user = new User();
                        user.setIduser(resultSet.getInt("iduser"));
                        user.setName(resultSet.getString("name"));
                        user.setEmail(resultSet.getString("email"));
                        user.setPassword(resultSet.getString("password"));
                        user.setType(resultSet.getString("type"));
                        user.setGoogleId(resultSet.getString("google_id"));
                        user.setRoles(resultSet.getString("roles"));
                        user.setEnabled(resultSet.getBoolean("is_enabled"));
                        user.setLinkedinId(resultSet.getString("linkedin_id"));
                        user.setResetToken(resultSet.getString("reset_token"));

                        Timestamp resetTokenExpiresAtTs = resultSet.getTimestamp("reset_token_expires_at");
                        if (resetTokenExpiresAtTs != null) {
                            user.setResetTokenExpiresAt(resetTokenExpiresAtTs.toLocalDateTime());
                        }

                        user.setAvatarName(resultSet.getString("avatar_name"));

                        Timestamp updatedAtTs = resultSet.getTimestamp("updated_at");
                        if (updatedAtTs != null) {
                            user.setUpdatedAt(updatedAtTs.toLocalDateTime());
                        }

                        user.setFaceEmbedding(resultSet.getString("face_embedding"));
                        users.add(user);
                    }
                }
            }
        } catch (SQLException e) {
            System.err.println("Failed to search users: " + e.getMessage());
            return new ArrayList<>();
        } finally {
            DatabaseConnection.closeConnection(connection);
        }

        return users;
    }

    public User login(String email, String password) {
        String sql = "SELECT * FROM user WHERE email = ? AND is_enabled = 1";
        Connection connection = null;

        try {
            connection = DatabaseConnection.getConnection();
            try (PreparedStatement statement = connection.prepareStatement(sql)) {
                statement.setString(1, email);

                try (ResultSet resultSet = statement.executeQuery()) {
                    if (resultSet.next()) {
                        String hashedPassword = resultSet.getString("password");
                        try {
                            if (hashedPassword == null || !BCrypt.checkpw(password, hashedPassword)) {
                                return null;
                            }
                        } catch (IllegalArgumentException e) {
                            return null;
                        }

                        User user = new User();
                        user.setIduser(resultSet.getInt("iduser"));
                        user.setName(resultSet.getString("name"));
                        user.setEmail(resultSet.getString("email"));
                        user.setPassword(hashedPassword);
                        user.setType(resultSet.getString("type"));
                        user.setGoogleId(resultSet.getString("google_id"));
                        user.setRoles(resultSet.getString("roles"));
                        user.setEnabled(resultSet.getBoolean("is_enabled"));
                        user.setLinkedinId(resultSet.getString("linkedin_id"));
                        user.setResetToken(resultSet.getString("reset_token"));

                        Timestamp resetTokenExpiresAtTs = resultSet.getTimestamp("reset_token_expires_at");
                        if (resetTokenExpiresAtTs != null) {
                            user.setResetTokenExpiresAt(resetTokenExpiresAtTs.toLocalDateTime());
                        }

                        user.setAvatarName(resultSet.getString("avatar_name"));

                        Timestamp updatedAtTs = resultSet.getTimestamp("updated_at");
                        if (updatedAtTs != null) {
                            user.setUpdatedAt(updatedAtTs.toLocalDateTime());
                        }

                        user.setFaceEmbedding(resultSet.getString("face_embedding"));
                        return user;
                    }
                }
            }
        } catch (SQLException e) {
            System.err.println("Failed to login user: " + e.getMessage());
        } finally {
            DatabaseConnection.closeConnection(connection);
        }

        return null;
    }
}

