package com.smarttask.dao;

import com.smarttask.model.Tache;
import com.smarttask.util.DatabaseConnection;

import java.sql.*;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class TacheDAO {

    public List<Tache> getAllTaches() {
        String sql = "SELECT id, libelle, priorite, date_limite, etat, projet_id FROM tache ORDER BY id ASC";
        Connection connection = null;
        List<Tache> taches = new ArrayList<>();

        try {
            connection = DatabaseConnection.getConnection();
            try (PreparedStatement statement = connection.prepareStatement(sql);
                 ResultSet resultSet = statement.executeQuery()) {

                while (resultSet.next()) {
                    Tache tache = mapTache(resultSet);
                    taches.add(tache);
                }
            }
        } catch (SQLException e) {
            System.err.println("Failed to get all taches: " + e.getMessage());
        } finally {
            DatabaseConnection.closeConnection(connection);
        }

        return taches;
    }

    public List<Tache> getTachesByProjetId(int projetId) {
        String sql = "SELECT id, libelle, priorite, date_limite, etat, projet_id FROM tache WHERE projet_id = ? ORDER BY id ASC";
        Connection connection = null;
        List<Tache> taches = new ArrayList<>();

        try {
            connection = DatabaseConnection.getConnection();
            try (PreparedStatement statement = connection.prepareStatement(sql)) {
                statement.setInt(1, projetId);

                try (ResultSet resultSet = statement.executeQuery()) {
                    while (resultSet.next()) {
                        Tache tache = mapTache(resultSet);
                        taches.add(tache);
                    }
                }
            }
        } catch (SQLException e) {
            System.err.println("Failed to get taches by projet id: " + e.getMessage());
        } finally {
            DatabaseConnection.closeConnection(connection);
        }

        return taches;
    }

    public Optional<Tache> getTacheById(int id) {
        String sql = "SELECT id, libelle, priorite, date_limite, etat, projet_id FROM tache WHERE id = ?";
        Connection connection = null;

        try {
            connection = DatabaseConnection.getConnection();
            try (PreparedStatement statement = connection.prepareStatement(sql)) {
                statement.setInt(1, id);

                try (ResultSet resultSet = statement.executeQuery()) {
                    if (resultSet.next()) {
                        return Optional.of(mapTache(resultSet));
                    }
                }
            }
        } catch (SQLException e) {
            System.err.println("Failed to get tache by id: " + e.getMessage());
        } finally {
            DatabaseConnection.closeConnection(connection);
        }

        return Optional.empty();
    }

    public int insertTache(Tache tache) {
        String sql = "INSERT INTO tache (libelle, priorite, date_limite, etat, projet_id) VALUES (?, ?, ?, ?, ?)";
        Connection connection = null;

        try {
            connection = DatabaseConnection.getConnection();
            try (PreparedStatement statement = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
                statement.setString(1, tache.getLibelle());
                statement.setString(2, tache.getPriorite());
                statement.setDate(3, Date.valueOf(tache.getDateLimite()));
                statement.setString(4, tache.getEtat());
                statement.setInt(5, tache.getProjetId());

                int affectedRows = statement.executeUpdate();
                if (affectedRows > 0) {
                    try (ResultSet generatedKeys = statement.getGeneratedKeys()) {
                        if (generatedKeys.next()) {
                            return generatedKeys.getInt(1);
                        }
                    }
                }
            }
        } catch (SQLException e) {
            System.err.println("Failed to insert tache: " + e.getMessage());
        } finally {
            DatabaseConnection.closeConnection(connection);
        }

        return -1;
    }

    public boolean updateTache(Tache tache) {
        String sql = "UPDATE tache SET libelle = ?, priorite = ?, date_limite = ?, etat = ?, projet_id = ? WHERE id = ?";
        Connection connection = null;

        try {
            connection = DatabaseConnection.getConnection();
            try (PreparedStatement statement = connection.prepareStatement(sql)) {
                statement.setString(1, tache.getLibelle());
                statement.setString(2, tache.getPriorite());
                statement.setDate(3, Date.valueOf(tache.getDateLimite()));
                statement.setString(4, tache.getEtat());
                statement.setInt(5, tache.getProjetId());
                statement.setInt(6, tache.getId());

                return statement.executeUpdate() > 0;
            }
        } catch (SQLException e) {
            System.err.println("Failed to update tache: " + e.getMessage());
        } finally {
            DatabaseConnection.closeConnection(connection);
        }

        return false;
    }

    public boolean deleteTache(int id) {
        String sql = "DELETE FROM tache WHERE id = ?";
        Connection connection = null;

        try {
            connection = DatabaseConnection.getConnection();
            try (PreparedStatement statement = connection.prepareStatement(sql)) {
                statement.setInt(1, id);
                return statement.executeUpdate() > 0;
            }
        } catch (SQLException e) {
            System.err.println("Failed to delete tache: " + e.getMessage());
        } finally {
            DatabaseConnection.closeConnection(connection);
        }

        return false;
    }

    private Tache mapTache(ResultSet rs) throws SQLException {
        int id = rs.getInt("id");
        String libelle = rs.getString("libelle");
        String priorite = rs.getString("priorite");
        LocalDate dateLimite = rs.getDate("date_limite").toLocalDate();
        String etat = rs.getString("etat");
        int projetId = rs.getInt("projet_id");

        return new Tache(id, libelle, priorite, dateLimite, etat, projetId);
    }
}

