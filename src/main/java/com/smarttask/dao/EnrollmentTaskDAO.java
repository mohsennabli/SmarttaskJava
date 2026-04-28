package com.smarttask.dao;

import com.smarttask.model.EnrollmentTaskRow;
import com.smarttask.util.DatabaseConnection;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;

public class EnrollmentTaskDAO {

    public EnrollmentTaskDAO() {
        ensureSchema();
    }

    public List<EnrollmentTaskRow> findTasksForInscription(int inscriptionId, int formationId) {
        ensureDefaultTasksForFormation(formationId);
        String sql = "SELECT ft.id AS task_id, ft.title, ft.description, ft.position, "
                + "COALESCE(it.completed, 0) AS completed "
                + "FROM formation_task ft "
                + "LEFT JOIN inscription_task it ON it.task_id = ft.id AND it.inscription_id = ? "
                + "WHERE ft.formation_id = ? "
                + "ORDER BY ft.position ASC, ft.id ASC";

        Connection connection = null;
        List<EnrollmentTaskRow> list = new ArrayList<>();
        try {
            connection = DatabaseConnection.getConnection();
            try (PreparedStatement statement = connection.prepareStatement(sql)) {
                statement.setInt(1, inscriptionId);
                statement.setInt(2, formationId);
                try (ResultSet rs = statement.executeQuery()) {
                    while (rs.next()) {
                        EnrollmentTaskRow row = new EnrollmentTaskRow();
                        row.setTaskId(rs.getInt("task_id"));
                        row.setTitle(rs.getString("title"));
                        row.setDescription(rs.getString("description"));
                        row.setPosition(rs.getInt("position"));
                        row.setCompleted(rs.getInt("completed") != 0);
                        list.add(row);
                    }
                }
            }
        } catch (SQLException e) {
            System.err.println("EnrollmentTaskDAO.findTasksForInscription failed: " + e.getMessage());
        } finally {
            DatabaseConnection.closeConnection(connection);
        }
        return list;
    }

    public boolean upsertCompletion(int inscriptionId, int taskId, boolean completed) {
        String update = "UPDATE inscription_task SET completed = ?, completed_at = ? WHERE inscription_id = ? AND task_id = ?";
        String insert = "INSERT INTO inscription_task (inscription_id, task_id, completed, completed_at) VALUES (?, ?, ?, ?)";
        Connection connection = null;
        try {
            connection = DatabaseConnection.getConnection();
            try (PreparedStatement st = connection.prepareStatement(update)) {
                st.setInt(1, completed ? 1 : 0);
                st.setTimestamp(2, completed ? new Timestamp(System.currentTimeMillis()) : null);
                st.setInt(3, inscriptionId);
                st.setInt(4, taskId);
                int updated = st.executeUpdate();
                if (updated > 0) {
                    return true;
                }
            }
            try (PreparedStatement st = connection.prepareStatement(insert)) {
                st.setInt(1, inscriptionId);
                st.setInt(2, taskId);
                st.setInt(3, completed ? 1 : 0);
                st.setTimestamp(4, completed ? new Timestamp(System.currentTimeMillis()) : null);
                return st.executeUpdate() > 0;
            }
        } catch (SQLException e) {
            System.err.println("EnrollmentTaskDAO.upsertCompletion failed: " + e.getMessage());
            return false;
        } finally {
            DatabaseConnection.closeConnection(connection);
        }
    }

    public int computeProgressPercent(int inscriptionId, int formationId) {
        String sql = "SELECT COUNT(ft.id) AS total, COALESCE(SUM(CASE WHEN it.completed = 1 THEN 1 ELSE 0 END), 0) AS done "
                + "FROM formation_task ft "
                + "LEFT JOIN inscription_task it ON it.task_id = ft.id AND it.inscription_id = ? "
                + "WHERE ft.formation_id = ?";
        Connection connection = null;
        try {
            connection = DatabaseConnection.getConnection();
            try (PreparedStatement st = connection.prepareStatement(sql)) {
                st.setInt(1, inscriptionId);
                st.setInt(2, formationId);
                try (ResultSet rs = st.executeQuery()) {
                    if (!rs.next()) {
                        return 0;
                    }
                    int total = rs.getInt("total");
                    int done = rs.getInt("done");
                    if (total <= 0) {
                        return 0;
                    }
                    return (int) Math.round((done * 100.0) / total);
                }
            }
        } catch (SQLException e) {
            System.err.println("EnrollmentTaskDAO.computeProgressPercent failed: " + e.getMessage());
            return 0;
        } finally {
            DatabaseConnection.closeConnection(connection);
        }
    }

    private void ensureDefaultTasksForFormation(int formationId) {
        String countSql = "SELECT COUNT(*) FROM formation_task WHERE formation_id = ?";
        String insertSql = "INSERT INTO formation_task (formation_id, title, description, position) VALUES (?, ?, ?, ?)";
        Connection connection = null;
        try {
            connection = DatabaseConnection.getConnection();
            int count = 0;
            try (PreparedStatement st = connection.prepareStatement(countSql)) {
                st.setInt(1, formationId);
                try (ResultSet rs = st.executeQuery()) {
                    if (rs.next()) {
                        count = rs.getInt(1);
                    }
                }
            }
            if (count > 0) {
                return;
            }
            String[][] defaults = new String[][]{
                    {"Introduction", "Read course intro and objectives"},
                    {"Main Module", "Complete the main learning content"},
                    {"Practice", "Do the practical exercise"},
                    {"Final Quiz", "Complete and validate final quiz"}
            };
            try (PreparedStatement st = connection.prepareStatement(insertSql)) {
                for (int i = 0; i < defaults.length; i++) {
                    st.setInt(1, formationId);
                    st.setString(2, defaults[i][0]);
                    st.setString(3, defaults[i][1]);
                    st.setInt(4, i + 1);
                    st.addBatch();
                }
                st.executeBatch();
            }
        } catch (SQLException e) {
            System.err.println("EnrollmentTaskDAO.ensureDefaultTasksForFormation failed: " + e.getMessage());
        } finally {
            DatabaseConnection.closeConnection(connection);
        }
    }

    private void ensureSchema() {
        String createFormationTask = "CREATE TABLE IF NOT EXISTS formation_task ("
                + "id INT AUTO_INCREMENT PRIMARY KEY,"
                + "formation_id INT NOT NULL,"
                + "title VARCHAR(255) NOT NULL,"
                + "description TEXT NULL,"
                + "position INT NOT NULL DEFAULT 0,"
                + "CONSTRAINT fk_formation_task_formation FOREIGN KEY (formation_id) REFERENCES formation(id) ON DELETE CASCADE"
                + ")";
        String createInscriptionTask = "CREATE TABLE IF NOT EXISTS inscription_task ("
                + "id INT AUTO_INCREMENT PRIMARY KEY,"
                + "inscription_id INT NOT NULL,"
                + "task_id INT NOT NULL,"
                + "completed TINYINT NOT NULL DEFAULT 0,"
                + "completed_at DATETIME NULL,"
                + "UNIQUE KEY uq_inscription_task (inscription_id, task_id),"
                + "CONSTRAINT fk_inscription_task_inscription FOREIGN KEY (inscription_id) REFERENCES inscription(id) ON DELETE CASCADE,"
                + "CONSTRAINT fk_inscription_task_task FOREIGN KEY (task_id) REFERENCES formation_task(id) ON DELETE CASCADE"
                + ")";
        Connection connection = null;
        try {
            connection = DatabaseConnection.getConnection();
            try (PreparedStatement st = connection.prepareStatement(createFormationTask)) {
                st.execute();
            }
            try (PreparedStatement st = connection.prepareStatement(createInscriptionTask)) {
                st.execute();
            }
        } catch (SQLException e) {
            System.err.println("EnrollmentTaskDAO.ensureSchema failed: " + e.getMessage());
        } finally {
            DatabaseConnection.closeConnection(connection);
        }
    }
}
