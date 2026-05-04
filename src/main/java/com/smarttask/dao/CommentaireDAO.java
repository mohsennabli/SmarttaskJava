package com.smarttask.dao;

import com.smarttask.model.Commentaire;
import com.smarttask.util.DatabaseConnection;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class CommentaireDAO {

    // ➕ Ajouter un commentaire
    public void add(Commentaire c) throws RuntimeException {
        String sql = "INSERT INTO commentaire (contenu, date_commentaire, ticket_id) VALUES (?, ?, ?)";

        try (Connection cnx = DatabaseConnection.getConnection();
             PreparedStatement ps = cnx.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {

            ps.setString(1, c.getContenu());
            ps.setTimestamp(2, new Timestamp(c.getDateCommentaire().getTime()));
            ps.setInt(3, c.getTicketId());

            ps.executeUpdate();

            // Récupérer l'ID généré
            try (ResultSet generatedKeys = ps.getGeneratedKeys()) {
                if (generatedKeys.next()) {
                    c.setId(generatedKeys.getInt(1));
                }
            }

        } catch (SQLException e) {
            throw new RuntimeException("Erreur lors de l'ajout du commentaire: " + e.getMessage(), e);
        }
    }

    // 📋 Afficher tous les commentaires d'un ticket
    public List<Commentaire> getByTicket(int ticketId) throws RuntimeException {
        List<Commentaire> list = new ArrayList<>();
        String sql = "SELECT * FROM commentaire WHERE ticket_id = ? ORDER BY date_commentaire DESC";

        try (Connection cnx = DatabaseConnection.getConnection();
             PreparedStatement ps = cnx.prepareStatement(sql)) {

            ps.setInt(1, ticketId);

            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    Commentaire c = new Commentaire();
                    c.setId(rs.getInt("id"));
                    c.setContenu(rs.getString("contenu"));
                    c.setDateCommentaire(rs.getTimestamp("date_commentaire"));
                    c.setTicketId(rs.getInt("ticket_id"));
                    list.add(c);
                }
            }

        } catch (SQLException e) {
            throw new RuntimeException("Erreur lors de la récupération des commentaires: " + e.getMessage(), e);
        }

        return list;
    }

    // 🔍 Récupérer un commentaire par son ID
    public Commentaire getById(int id) throws RuntimeException {
        String sql = "SELECT * FROM commentaire WHERE id = ?";

        try (Connection cnx = DatabaseConnection.getConnection();
             PreparedStatement ps = cnx.prepareStatement(sql)) {

            ps.setInt(1, id);

            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    Commentaire c = new Commentaire();
                    c.setId(rs.getInt("id"));
                    c.setContenu(rs.getString("contenu"));
                    c.setDateCommentaire(rs.getTimestamp("date_commentaire"));
                    c.setTicketId(rs.getInt("ticket_id"));
                    return c;
                }
            }

        } catch (SQLException e) {
            throw new RuntimeException("Erreur lors de la récupération du commentaire: " + e.getMessage(), e);
        }

        return null;
    }

    // ✏️ Mettre à jour un commentaire
    public boolean update(Commentaire c) throws RuntimeException {
        String sql = "UPDATE commentaire SET contenu = ? WHERE id = ?";

        try (Connection cnx = DatabaseConnection.getConnection();
             PreparedStatement ps = cnx.prepareStatement(sql)) {

            ps.setString(1, c.getContenu());
            ps.setInt(2, c.getId());

            return ps.executeUpdate() > 0;

        } catch (SQLException e) {
            throw new RuntimeException("Erreur lors de la mise à jour du commentaire: " + e.getMessage(), e);
        }
    }

    // 🗑️ Supprimer un commentaire
    public boolean delete(int id) throws RuntimeException {
        String sql = "DELETE FROM commentaire WHERE id = ?";

        try (Connection cnx = DatabaseConnection.getConnection();
             PreparedStatement ps = cnx.prepareStatement(sql)) {

            ps.setInt(1, id);
            return ps.executeUpdate() > 0;

        } catch (SQLException e) {
            throw new RuntimeException("Erreur lors de la suppression du commentaire: " + e.getMessage(), e);
        }
    }

    // 📊 Compter les commentaires d'un ticket
    public int countByTicket(int ticketId) throws RuntimeException {
        String sql = "SELECT COUNT(*) FROM commentaire WHERE ticket_id = ?";

        try (Connection cnx = DatabaseConnection.getConnection();
             PreparedStatement ps = cnx.prepareStatement(sql)) {

            ps.setInt(1, ticketId);

            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt(1);
                }
            }

        } catch (SQLException e) {
            throw new RuntimeException("Erreur lors du comptage des commentaires: " + e.getMessage(), e);
        }

        return 0;
    }
}