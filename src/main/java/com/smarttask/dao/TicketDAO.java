package com.smarttask.dao;

import com.smarttask.model.Ticket;
import com.smarttask.util.DatabaseConnection;

import java.sql.*;
import java.util.*;

public class TicketDAO {

    // ➕ Ajouter un ticket
    public void add(Ticket t) throws RuntimeException {
        String sql = "INSERT INTO ticket (titre, description, statut, priorite, date_creation) VALUES (?, ?, ?, ?, ?)";

        try (Connection cnx = DatabaseConnection.getConnection();
             PreparedStatement ps = cnx.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {

            ps.setString(1, t.getTitre());
            ps.setString(2, t.getDescription());
            ps.setString(3, t.getStatut());
            ps.setString(4, t.getPriorite());
            ps.setTimestamp(5, new Timestamp(t.getDateCreation().getTime()));

            ps.executeUpdate();

            // Récupérer l'ID généré
            try (ResultSet generatedKeys = ps.getGeneratedKeys()) {
                if (generatedKeys.next()) {
                    t.setId(generatedKeys.getInt(1));
                }
            }

        } catch (SQLException e) {
            throw new RuntimeException("Erreur lors de l'ajout du ticket: " + e.getMessage(), e);
        }
    }

    // 📋 Récupérer tous les tickets
    public List<Ticket> getAll() throws RuntimeException {
        List<Ticket> list = new ArrayList<>();
        String sql = "SELECT * FROM ticket ORDER BY date_creation DESC";

        try (Connection cnx = DatabaseConnection.getConnection();
             Statement stmt = cnx.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {

            while (rs.next()) {
                Ticket t = mapResultSetToTicket(rs);
                list.add(t);
            }

        } catch (SQLException e) {
            throw new RuntimeException("Erreur lors de la récupération des tickets: " + e.getMessage(), e);
        }

        return list;
    }

    // 🔍 Rechercher un ticket par ID
    public Ticket getById(int id) throws RuntimeException {
        String sql = "SELECT * FROM ticket WHERE id = ?";

        try (Connection cnx = DatabaseConnection.getConnection();
             PreparedStatement ps = cnx.prepareStatement(sql)) {

            ps.setInt(1, id);

            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return mapResultSetToTicket(rs);
                }
            }

        } catch (SQLException e) {
            throw new RuntimeException("Erreur lors de la récupération du ticket: " + e.getMessage(), e);
        }

        return null;
    }

    // 🔍 Recherche par mot-clé (titre ou description)
    public List<Ticket> search(String mot) throws RuntimeException {
        List<Ticket> list = new ArrayList<>();
        String sql = "SELECT * FROM ticket WHERE titre LIKE ? OR description LIKE ? ORDER BY date_creation DESC";

        try (Connection cnx = DatabaseConnection.getConnection();
             PreparedStatement ps = cnx.prepareStatement(sql)) {

            String searchPattern = "%" + mot + "%";
            ps.setString(1, searchPattern);
            ps.setString(2, searchPattern);

            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    Ticket t = mapResultSetToTicket(rs);
                    list.add(t);
                }
            }

        } catch (SQLException e) {
            throw new RuntimeException("Erreur lors de la recherche des tickets: " + e.getMessage(), e);
        }

        return list;
    }

    // 🎛️ Filtrage par statut
    public List<Ticket> filterByStatut(String statut) throws RuntimeException {
        List<Ticket> list = new ArrayList<>();
        String sql = "SELECT * FROM ticket WHERE statut = ? ORDER BY date_creation DESC";

        try (Connection cnx = DatabaseConnection.getConnection();
             PreparedStatement ps = cnx.prepareStatement(sql)) {

            ps.setString(1, statut);

            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    Ticket t = mapResultSetToTicket(rs);
                    list.add(t);
                }
            }

        } catch (SQLException e) {
            throw new RuntimeException("Erreur lors du filtrage des tickets: " + e.getMessage(), e);
        }

        return list;
    }

    // 🎛️ Filtrage par priorité
    public List<Ticket> filterByPriorite(String priorite) throws RuntimeException {
        List<Ticket> list = new ArrayList<>();
        String sql = "SELECT * FROM ticket WHERE priorite = ? ORDER BY date_creation DESC";

        try (Connection cnx = DatabaseConnection.getConnection();
             PreparedStatement ps = cnx.prepareStatement(sql)) {

            ps.setString(1, priorite);

            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    Ticket t = mapResultSetToTicket(rs);
                    list.add(t);
                }
            }

        } catch (SQLException e) {
            throw new RuntimeException("Erreur lors du filtrage des tickets: " + e.getMessage(), e);
        }

        return list;
    }

    // 🎛️ Filtrage multiple (statut et priorité)
    public List<Ticket> filter(String statut, String priorite) throws RuntimeException {
        List<Ticket> list = new ArrayList<>();
        StringBuilder sql = new StringBuilder("SELECT * FROM ticket WHERE 1=1");

        if (statut != null && !statut.isEmpty()) {
            sql.append(" AND statut = ?");
        }
        if (priorite != null && !priorite.isEmpty()) {
            sql.append(" AND priorite = ?");
        }
        sql.append(" ORDER BY date_creation DESC");

        try (Connection cnx = DatabaseConnection.getConnection();
             PreparedStatement ps = cnx.prepareStatement(sql.toString())) {

            int index = 1;
            if (statut != null && !statut.isEmpty()) {
                ps.setString(index++, statut);
            }
            if (priorite != null && !priorite.isEmpty()) {
                ps.setString(index, priorite);
            }

            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    Ticket t = mapResultSetToTicket(rs);
                    list.add(t);
                }
            }

        } catch (SQLException e) {
            throw new RuntimeException("Erreur lors du filtrage des tickets: " + e.getMessage(), e);
        }

        return list;
    }

    // ✏️ Mettre à jour un ticket
    public boolean update(Ticket t) throws RuntimeException {
        String sql = "UPDATE ticket SET titre = ?, description = ?, statut = ?, priorite = ? WHERE id = ?";

        try (Connection cnx = DatabaseConnection.getConnection();
             PreparedStatement ps = cnx.prepareStatement(sql)) {

            ps.setString(1, t.getTitre());
            ps.setString(2, t.getDescription());
            ps.setString(3, t.getStatut());
            ps.setString(4, t.getPriorite());
            ps.setInt(5, t.getId());

            return ps.executeUpdate() > 0;

        } catch (SQLException e) {
            throw new RuntimeException("Erreur lors de la mise à jour du ticket: " + e.getMessage(), e);
        }
    }

    // 🗑️ Supprimer un ticket
    public boolean delete(int id) throws RuntimeException {
        String sql = "DELETE FROM ticket WHERE id = ?";

        try (Connection cnx = DatabaseConnection.getConnection();
             PreparedStatement ps = cnx.prepareStatement(sql)) {

            ps.setInt(1, id);
            return ps.executeUpdate() > 0;

        } catch (SQLException e) {
            throw new RuntimeException("Erreur lors de la suppression du ticket: " + e.getMessage(), e);
        }
    }

    // 📊 Compter les tickets par statut
    public Map<String, Integer> countByStatut() throws RuntimeException {
        Map<String, Integer> stats = new HashMap<>();
        String sql = "SELECT statut, COUNT(*) as count FROM ticket GROUP BY statut";

        try (Connection cnx = DatabaseConnection.getConnection();
             Statement stmt = cnx.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {

            while (rs.next()) {
                stats.put(rs.getString("statut"), rs.getInt("count"));
            }

        } catch (SQLException e) {
            throw new RuntimeException("Erreur lors du comptage des tickets: " + e.getMessage(), e);
        }

        return stats;
    }

    // 🔧 Méthode utilitaire pour mapper ResultSet → Ticket
    private Ticket mapResultSetToTicket(ResultSet rs) throws SQLException {
        Ticket t = new Ticket();
        t.setId(rs.getInt("id"));
        t.setTitre(rs.getString("titre"));
        t.setDescription(rs.getString("description"));
        t.setStatut(rs.getString("statut"));
        t.setPriorite(rs.getString("priorite"));
        t.setDateCreation(rs.getTimestamp("date_creation"));
        return t;
    }
}