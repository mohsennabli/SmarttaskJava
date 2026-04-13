package org.esprit.gestionprojet.service;

import org.esprit.gestionprojet.model.Projet;
import org.esprit.gestionprojet.util.DBconnexion;

import java.sql.Connection;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class ProjetRepository {
    public List<Projet> findAll() {
        String sql = "SELECT id, nom, description, date_debut, date_echeance, statut FROM projet";
        List<Projet> projets = new ArrayList<>();

        try {
            Connection connection = DBconnexion.getInstance().getCnx();
            try (PreparedStatement statement = connection.prepareStatement(sql);
                 ResultSet rs = statement.executeQuery()) {
                while (rs.next()) {
                    projets.add(mapProjet(rs));
                }
            }
            return projets;
        } catch (Exception e) {
            throw new IllegalStateException("Failed to read projects", e);
        }
    }

    public Optional<Projet> findById(int id) {
        String sql = "SELECT id, nom, description, date_debut, date_echeance, statut FROM projet WHERE id = ?";

        try {
            Connection connection = DBconnexion.getInstance().getCnx();
            try (PreparedStatement statement = connection.prepareStatement(sql)) {
                statement.setInt(1, id);
                try (ResultSet rs = statement.executeQuery()) {
                    if (!rs.next()) {
                        return Optional.empty();
                    }
                    return Optional.of(mapProjet(rs));
                }
            }
        } catch (Exception e) {
            throw new IllegalStateException("Failed to read project", e);
        }
    }

    public int insert(Projet projet) {
        String sql = "INSERT INTO projet (nom, description, date_debut, date_echeance, statut) VALUES (?, ?, ?, ?, ?)";

        try {
            Connection connection = DBconnexion.getInstance().getCnx();
            try (PreparedStatement statement = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
                statement.setString(1, projet.getNom());
                statement.setString(2, projet.getDescription());
                statement.setDate(3, Date.valueOf(projet.getDateDebut()));
                statement.setDate(4, Date.valueOf(projet.getDateEcheance()));
                statement.setString(5, projet.getStatut());
                statement.executeUpdate();

                try (ResultSet keys = statement.getGeneratedKeys()) {
                    if (keys.next()) {
                        return keys.getInt(1);
                    }
                }
                return 0;
            }
        } catch (Exception e) {
            throw new IllegalStateException("Failed to create project", e);
        }
    }

    public void update(Projet projet) {
        String sql = "UPDATE projet SET nom = ?, description = ?, date_debut = ?, date_echeance = ?, statut = ? WHERE id = ?";

        try {
            Connection connection = DBconnexion.getInstance().getCnx();
            try (PreparedStatement statement = connection.prepareStatement(sql)) {
                statement.setString(1, projet.getNom());
                statement.setString(2, projet.getDescription());
                statement.setDate(3, Date.valueOf(projet.getDateDebut()));
                statement.setDate(4, Date.valueOf(projet.getDateEcheance()));
                statement.setString(5, projet.getStatut());
                statement.setInt(6, projet.getId());
                statement.executeUpdate();
            }
        } catch (Exception e) {
            throw new IllegalStateException("Failed to update project", e);
        }
    }

    public void deleteById(int id) {
        String sql = "DELETE FROM projet WHERE id = ?";

        try {
            Connection connection = DBconnexion.getInstance().getCnx();
            try (PreparedStatement statement = connection.prepareStatement(sql)) {
                statement.setInt(1, id);
                statement.executeUpdate();
            }
        } catch (Exception e) {
            throw new IllegalStateException("Failed to delete project", e);
        }
    }

    private Projet mapProjet(ResultSet rs) throws Exception {
        return new Projet(
            rs.getInt("id"),
            rs.getString("nom"),
            rs.getString("description"),
            rs.getDate("date_debut").toLocalDate(),
            rs.getDate("date_echeance").toLocalDate(),
            rs.getString("statut")
        );
    }
}
