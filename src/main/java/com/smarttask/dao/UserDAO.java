package com.smarttask.dao;

import com.smarttask.model.User;
import com.smarttask.util.DatabaseConnection;
import org.mindrot.jbcrypt.BCrypt;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.sql.Statement;
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
                statement.setInt(6, user.isEnabled() ? 1 : 0);

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
                    user.setGithubId(resultSet.getString("github_id"));
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

    public boolean updateProfile(User user) {
        String sql = "UPDATE user SET name=?, email=?, type=?, password=?, avatar_name=? WHERE iduser=?";
        Connection connection = null;

        try {
            connection = DatabaseConnection.getConnection();
            try (PreparedStatement statement = connection.prepareStatement(sql)) {
                statement.setString(1, user.getName());
                statement.setString(2, user.getEmail());
                statement.setString(3, user.getType());
                statement.setString(4, user.getPassword());
                statement.setString(5, user.getAvatarName());
                statement.setInt(6, user.getIduser());
                return statement.executeUpdate() > 0;
            }
        } catch (SQLException e) {
            System.err.println("Failed to update profile: " + e.getMessage());
            return false;
        } finally {
            DatabaseConnection.closeConnection(connection);
        }
    }

    public boolean storeResetToken(int userId, String resetToken, java.time.LocalDateTime expiresAt) {
        String sql = "UPDATE user SET reset_token=?, reset_token_expires_at=? WHERE iduser=?";
        Connection connection = null;

        try {
            connection = DatabaseConnection.getConnection();
            try (PreparedStatement statement = connection.prepareStatement(sql)) {
                statement.setString(1, resetToken);
                statement.setTimestamp(2, expiresAt == null ? null : Timestamp.valueOf(expiresAt));
                statement.setInt(3, userId);
                return statement.executeUpdate() > 0;
            }
        } catch (SQLException e) {
            System.err.println("Failed to store reset token: " + e.getMessage());
            return false;
        } finally {
            DatabaseConnection.closeConnection(connection);
        }
    }

    public boolean updatePasswordAndClearResetToken(int userId, String hashedPassword) {
        String sql = "UPDATE user SET password=?, reset_token=NULL, reset_token_expires_at=NULL WHERE iduser=?";
        Connection connection = null;

        try {
            connection = DatabaseConnection.getConnection();
            try (PreparedStatement statement = connection.prepareStatement(sql)) {
                statement.setString(1, hashedPassword);
                statement.setInt(2, userId);
                return statement.executeUpdate() > 0;
            }
        } catch (SQLException e) {
            System.err.println("Failed to update password after reset: " + e.getMessage());
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
                        user.setGithubId(resultSet.getString("github_id"));
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

                        User user = mapUser(resultSet);
                        user.setPassword(hashedPassword);
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

    public User findByGoogleId(String googleId) {
        String sql = "SELECT * FROM user WHERE google_id = ?";
        Connection connection = null;

        try {
            connection = DatabaseConnection.getConnection();
            try (PreparedStatement statement = connection.prepareStatement(sql)) {
                statement.setString(1, googleId);
                try (ResultSet resultSet = statement.executeQuery()) {
                    if (resultSet.next()) {
                        return mapUser(resultSet);
                    }
                }
            }
        } catch (SQLException e) {
            System.err.println("Failed to find user by Google ID: " + e.getMessage());
        } finally {
            DatabaseConnection.closeConnection(connection);
        }

        return null;
    }

    public User findByGitHubId(String githubId) {
        String sql = "SELECT * FROM user WHERE github_id = ?";
        Connection connection = null;

        try {
            connection = DatabaseConnection.getConnection();
            try (PreparedStatement statement = connection.prepareStatement(sql)) {
                statement.setString(1, githubId);
                try (ResultSet resultSet = statement.executeQuery()) {
                    if (resultSet.next()) {
                        return mapUser(resultSet);
                    }
                }
            }
        } catch (SQLException e) {
            System.err.println("Failed to find user by GitHub ID: " + e.getMessage());
        } finally {
            DatabaseConnection.closeConnection(connection);
        }

        return null;
    }

    public User findByEmail(String email) {
        String sql = "SELECT * FROM user WHERE email = ?";
        Connection connection = null;

        try {
            connection = DatabaseConnection.getConnection();
            try (PreparedStatement statement = connection.prepareStatement(sql)) {
                statement.setString(1, email);
                try (ResultSet resultSet = statement.executeQuery()) {
                    if (resultSet.next()) {
                        return mapUser(resultSet);
                    }
                }
            }
        } catch (SQLException e) {
            System.err.println("Failed to find user by email: " + e.getMessage());
        } finally {
            DatabaseConnection.closeConnection(connection);
        }

        return null;
    }

    public User findById(int iduser) {
        String sql = "SELECT * FROM user WHERE iduser = ?";
        Connection connection = null;

        try {
            connection = DatabaseConnection.getConnection();
            try (PreparedStatement statement = connection.prepareStatement(sql)) {
                statement.setInt(1, iduser);
                try (ResultSet resultSet = statement.executeQuery()) {
                    if (resultSet.next()) {
                        return mapUser(resultSet);
                    }
                }
            }
        } catch (SQLException e) {
            System.err.println("Failed to find user by ID: " + e.getMessage());
        } finally {
            DatabaseConnection.closeConnection(connection);
        }

        return null;
    }

    public List<User> findUsersWithFaceEmbeddings() {
        String sql = "SELECT * FROM user WHERE is_enabled = 1 AND face_embedding IS NOT NULL AND JSON_LENGTH(face_embedding) > 0 ORDER BY iduser ASC";
        Connection connection = null;
        List<User> users = new ArrayList<>();

        try {
            connection = DatabaseConnection.getConnection();
            try (PreparedStatement statement = connection.prepareStatement(sql);
                 ResultSet resultSet = statement.executeQuery()) {

                while (resultSet.next()) {
                    users.add(mapUser(resultSet));
                }
            }
        } catch (SQLException e) {
            System.err.println("Failed to fetch users with face embeddings: " + e.getMessage());
            return new ArrayList<>();
        } finally {
            DatabaseConnection.closeConnection(connection);
        }

        return users;
    }

    public User upsertGoogleUser(String googleId, String email, String name) {
        User byGoogleId = findByGoogleId(googleId);
        if (byGoogleId != null) {
            return byGoogleId;
        }

        User byEmail = findByEmail(email);
        if (byEmail != null) {
            if (byEmail.getGoogleId() == null || byEmail.getGoogleId().isBlank()) {
                if (!linkGoogleAccount(byEmail.getIduser(), googleId)) {
                    return null;
                }
                if (name != null && !name.isBlank()) {
                    updateUserName(byEmail.getIduser(), name);
                }
                return findByEmail(email);
            }

            if (googleId.equals(byEmail.getGoogleId())) {
                return byEmail;
            }

            return null;
        }

        int newUserId = insertGoogleUser(name, email, googleId);
        if (newUserId <= 0) {
            return null;
        }

        return findByGoogleId(googleId);
    }

    public User upsertGitHubUser(String githubId, String email, String name) {
        User byGitHubId = findByGitHubId(githubId);
        if (byGitHubId != null) {
            return byGitHubId;
        }

        User byEmail = findByEmail(email);
        if (byEmail != null) {
            if (byEmail.getGithubId() == null || byEmail.getGithubId().isBlank()) {
                if (!linkGitHubAccount(byEmail.getIduser(), githubId)) {
                    return null;
                }
                if (name != null && !name.isBlank()) {
                    updateUserName(byEmail.getIduser(), name);
                }
                return findByEmail(email);
            }

            if (githubId.equals(byEmail.getGithubId())) {
                return byEmail;
            }

            return null;
        }

        int newUserId = insertGitHubUser(name, email, githubId);
        if (newUserId <= 0) {
            return null;
        }

        return findByGitHubId(githubId);
    }

    private int insertGoogleUser(String name, String email, String googleId) {
        String sql = "INSERT INTO user (name, email, password, type, google_id, roles, is_enabled) VALUES (?, ?, ?, ?, ?, ?, ?)";
        Connection connection = null;

        try {
            connection = DatabaseConnection.getConnection();
            try (PreparedStatement statement = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
                String safeName = (name == null || name.isBlank()) ? email : name;
                statement.setString(1, safeName);
                statement.setString(2, email);
                statement.setString(3, null);
                statement.setString(4, "collaborator");
                statement.setString(5, googleId);
                statement.setString(6, "[]");
                statement.setBoolean(7, true);

                if (statement.executeUpdate() <= 0) {
                    return -1;
                }

                try (ResultSet keys = statement.getGeneratedKeys()) {
                    if (keys.next()) {
                        return keys.getInt(1);
                    }
                }
            }
        } catch (SQLException e) {
            System.err.println("Failed to create Google user: " + e.getMessage());
        } finally {
            DatabaseConnection.closeConnection(connection);
        }

        return -1;
    }

    private int insertGitHubUser(String name, String email, String githubId) {
        String sql = "INSERT INTO user (name, email, password, type, github_id, roles, is_enabled) VALUES (?, ?, ?, ?, ?, ?, ?)";
        Connection connection = null;

        try {
            connection = DatabaseConnection.getConnection();
            try (PreparedStatement statement = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
                String safeName = (name == null || name.isBlank()) ? email : name;
                statement.setString(1, safeName);
                statement.setString(2, email);
                statement.setString(3, null);
                statement.setString(4, "collaborator");
                statement.setString(5, githubId);
                statement.setString(6, "[]");
                statement.setBoolean(7, true);

                if (statement.executeUpdate() <= 0) {
                    return -1;
                }

                try (ResultSet keys = statement.getGeneratedKeys()) {
                    if (keys.next()) {
                        return keys.getInt(1);
                    }
                }
            }
        } catch (SQLException e) {
            System.err.println("Failed to create GitHub user: " + e.getMessage());
        } finally {
            DatabaseConnection.closeConnection(connection);
        }

        return -1;
    }

    private boolean linkGoogleAccount(int userId, String googleId) {
        String sql = "UPDATE user SET google_id = ? WHERE iduser = ?";
        Connection connection = null;

        try {
            connection = DatabaseConnection.getConnection();
            try (PreparedStatement statement = connection.prepareStatement(sql)) {
                statement.setString(1, googleId);
                statement.setInt(2, userId);
                return statement.executeUpdate() > 0;
            }
        } catch (SQLException e) {
            System.err.println("Failed to link Google account: " + e.getMessage());
        } finally {
            DatabaseConnection.closeConnection(connection);
        }

        return false;
    }

    private boolean linkGitHubAccount(int userId, String githubId) {
        String sql = "UPDATE user SET github_id = ? WHERE iduser = ?";
        Connection connection = null;

        try {
            connection = DatabaseConnection.getConnection();
            try (PreparedStatement statement = connection.prepareStatement(sql)) {
                statement.setString(1, githubId);
                statement.setInt(2, userId);
                return statement.executeUpdate() > 0;
            }
        } catch (SQLException e) {
            System.err.println("Failed to link GitHub account: " + e.getMessage());
        } finally {
            DatabaseConnection.closeConnection(connection);
        }

        return false;
    }

    private void updateUserName(int userId, String name) {
        String sql = "UPDATE user SET name = ? WHERE iduser = ?";
        Connection connection = null;

        try {
            connection = DatabaseConnection.getConnection();
            try (PreparedStatement statement = connection.prepareStatement(sql)) {
                statement.setString(1, name);
                statement.setInt(2, userId);
                statement.executeUpdate();
            }
        } catch (SQLException e) {
            System.err.println("Failed to sync user name from Google profile: " + e.getMessage());
        } finally {
            DatabaseConnection.closeConnection(connection);
        }
    }

    private User mapUser(ResultSet resultSet) throws SQLException {
        User user = new User();
        user.setIduser(resultSet.getInt("iduser"));
        user.setName(resultSet.getString("name"));
        user.setEmail(resultSet.getString("email"));
        user.setPassword(resultSet.getString("password"));
        user.setType(resultSet.getString("type"));
        user.setGoogleId(resultSet.getString("google_id"));
        user.setGithubId(resultSet.getString("github_id"));
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

