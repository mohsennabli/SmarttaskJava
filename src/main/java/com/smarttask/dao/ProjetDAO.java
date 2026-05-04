package com.smarttask.dao;

import com.smarttask.model.Projet;
import com.smarttask.util.DatabaseConnection;

import java.sql.*;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class ProjetDAO {

    public List<Projet> getAllProjets() {
        String sql = "SELECT id, nom, description, date_debut, date_echeance, statut FROM projet ORDER BY id ASC";
        Connection connection = null;
        List<Projet> projets = new ArrayList<>();

        try {
            connection = DatabaseConnection.getConnection();
            try (PreparedStatement statement = connection.prepareStatement(sql);
                 ResultSet resultSet = statement.executeQuery()) {

                while (resultSet.next()) {
                    Projet projet = mapProjet(resultSet);
                    projets.add(projet);
                }
            }
        } catch (SQLException e) {
            System.err.println("Failed to get all projets: " + e.getMessage());
        } finally {
            DatabaseConnection.closeConnection(connection);
        }

        return projets;
    }

    public Optional<Projet> getProjetById(int id) {
        String sql = "SELECT id, nom, description, date_debut, date_echeance, statut FROM projet WHERE id = ?";
        Connection connection = null;

        try {
            connection = DatabaseConnection.getConnection();
            try (PreparedStatement statement = connection.prepareStatement(sql)) {
                statement.setInt(1, id);

                try (ResultSet resultSet = statement.executeQuery()) {
                    if (resultSet.next()) {
                        return Optional.of(mapProjet(resultSet));
                    }
                }
            }
        } catch (SQLException e) {
            System.err.println("Failed to get projet by id: " + e.getMessage());
        } finally {
            DatabaseConnection.closeConnection(connection);
        }

        return Optional.empty();
    }

    public int insertProjet(Projet projet) {
        String sql = "INSERT INTO projet (nom, description, date_debut, date_echeance, statut) VALUES (?, ?, ?, ?, ?)";
        Connection connection = null;

        try {
            connection = DatabaseConnection.getConnection();
            try (PreparedStatement statement = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
                statement.setString(1, projet.getNom());
                statement.setString(2, projet.getDescription());
                statement.setDate(3, Date.valueOf(projet.getDateDebut()));
                statement.setDate(4, Date.valueOf(projet.getDateEcheance()));
                statement.setString(5, projet.getStatut());

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
            System.err.println("Failed to insert projet: " + e.getMessage());
        } finally {
            DatabaseConnection.closeConnection(connection);
        }

        return -1;
    }

    public boolean updateProjet(Projet projet) {
        String sql = "UPDATE projet SET nom = ?, description = ?, date_debut = ?, date_echeance = ?, statut = ? WHERE id = ?";
        Connection connection = null;

        try {
            connection = DatabaseConnection.getConnection();
            try (PreparedStatement statement = connection.prepareStatement(sql)) {
                statement.setString(1, projet.getNom());
                statement.setString(2, projet.getDescription());
                statement.setDate(3, Date.valueOf(projet.getDateDebut()));
                statement.setDate(4, Date.valueOf(projet.getDateEcheance()));
                statement.setString(5, projet.getStatut());
                statement.setInt(6, projet.getId());

                return statement.executeUpdate() > 0;
            }
        } catch (SQLException e) {
            System.err.println("Failed to update projet: " + e.getMessage());
        } finally {
            DatabaseConnection.closeConnection(connection);
        }

        return false;
    }

    public boolean deleteProjet(int id) {
        String sql = "DELETE FROM projet WHERE id = ?";
        Connection connection = null;

        try {
            connection = DatabaseConnection.getConnection();
            try (PreparedStatement statement = connection.prepareStatement(sql)) {
                statement.setInt(1, id);
                return statement.executeUpdate() > 0;
            }
        } catch (SQLException e) {
            System.err.println("Failed to delete projet: " + e.getMessage());
        } finally {
            DatabaseConnection.closeConnection(connection);
        }

        return false;
    }

    private Projet mapProjet(ResultSet rs) throws SQLException {
        int id = rs.getInt("id");
        String nom = rs.getString("nom");
        String description = rs.getString("description");
        LocalDate dateDebut = rs.getDate("date_debut").toLocalDate();
        LocalDate dateEcheance = rs.getDate("date_echeance").toLocalDate();
        String statut = rs.getString("statut");

        return new Projet(id, nom, description, dateDebut, dateEcheance, statut);
    }
}

