package com.smarttask.dao;

import com.smarttask.model.Inscription;
import com.smarttask.model.InscriptionRow;
import com.smarttask.util.DatabaseConnection;

import java.sql.Connection;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class InscriptionDAO {

    public boolean existsByUserAndFormation(int userId, int formationId) {
        String sql = "SELECT COUNT(*) FROM inscription WHERE user_id = ? AND formation_id = ?";
        Connection connection = null;
        try {
            connection = DatabaseConnection.getConnection();
            try (PreparedStatement statement = connection.prepareStatement(sql)) {
                statement.setInt(1, userId);
                statement.setInt(2, formationId);
                try (ResultSet rs = statement.executeQuery()) {
                    if (rs.next()) {
                        return rs.getInt(1) > 0;
                    }
                }
            }
        } catch (SQLException e) {
            System.err.println("InscriptionDAO.existsByUserAndFormation failed: " + e.getMessage());
        } finally {
            DatabaseConnection.closeConnection(connection);
        }
        return false;
    }

    public List<InscriptionRow> findRowsByUserId(int userId) {
        String sql = "SELECT i.id AS inscription_id, i.formation_id, i.date_inscription, i.statut, i.progression, i.certificat, "
                + "f.titre AS formation_titre, f.date_debut AS formation_date_debut, f.date_fin AS formation_date_fin "
                + "FROM inscription i JOIN formation f ON f.id = i.formation_id WHERE i.user_id = ? "
                + "ORDER BY i.date_inscription DESC";
        Connection connection = null;
        List<InscriptionRow> list = new ArrayList<>();
        try {
            connection = DatabaseConnection.getConnection();
            try (PreparedStatement statement = connection.prepareStatement(sql)) {
                statement.setInt(1, userId);
                try (ResultSet rs = statement.executeQuery()) {
                    while (rs.next()) {
                        list.add(mapRow(rs));
                    }
                }
            }
        } catch (SQLException e) {
            System.err.println("InscriptionDAO.findRowsByUserId failed: " + e.getMessage());
        } finally {
            DatabaseConnection.closeConnection(connection);
        }
        return list;
    }

    public Optional<Inscription> findById(int id) {
        String sql = "SELECT * FROM inscription WHERE id = ?";
        Connection connection = null;
        try {
            connection = DatabaseConnection.getConnection();
            try (PreparedStatement statement = connection.prepareStatement(sql)) {
                statement.setInt(1, id);
                try (ResultSet rs = statement.executeQuery()) {
                    if (rs.next()) {
                        return Optional.of(mapInscription(rs));
                    }
                }
            }
        } catch (SQLException e) {
            System.err.println("InscriptionDAO.findById failed: " + e.getMessage());
        } finally {
            DatabaseConnection.closeConnection(connection);
        }
        return Optional.empty();
    }

    /**
     * @return generated id, or -1 on failure
     */
    public int insert(int userId, int formationId, String statut, int progression, boolean certificat) {
        String sql = "INSERT INTO inscription (date_inscription, statut, progression, certificat, user_id, formation_id) "
                + "VALUES (NOW(), ?, ?, ?, ?, ?)";
        Connection connection = null;
        try {
            connection = DatabaseConnection.getConnection();
            try (PreparedStatement statement = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
                statement.setString(1, statut);
                statement.setInt(2, progression);
                statement.setInt(3, certificat ? 1 : 0);
                statement.setInt(4, userId);
                statement.setInt(5, formationId);
                int updated = statement.executeUpdate();
                if (updated <= 0) {
                    return -1;
                }
                try (ResultSet keys = statement.getGeneratedKeys()) {
                    if (keys.next()) {
                        return keys.getInt(1);
                    }
                }
            }
        } catch (SQLException e) {
            System.err.println("InscriptionDAO.insert failed: " + e.getMessage());
        } finally {
            DatabaseConnection.closeConnection(connection);
        }
        return -1;
    }

    public boolean updateProgression(int inscriptionId, int userId, int progression) {
        String sql = "UPDATE inscription SET progression = ? WHERE id = ? AND user_id = ?";
        Connection connection = null;
        try {
            connection = DatabaseConnection.getConnection();
            try (PreparedStatement statement = connection.prepareStatement(sql)) {
                statement.setInt(1, progression);
                statement.setInt(2, inscriptionId);
                statement.setInt(3, userId);
                return statement.executeUpdate() > 0;
            }
        } catch (SQLException e) {
            System.err.println("InscriptionDAO.updateProgression failed: " + e.getMessage());
        } finally {
            DatabaseConnection.closeConnection(connection);
        }
        return false;
    }

    public boolean markCertificatIssued(int inscriptionId, int userId) {
        String sql = "UPDATE inscription SET certificat = 1 WHERE id = ? AND user_id = ?";
        Connection connection = null;
        try {
            connection = DatabaseConnection.getConnection();
            try (PreparedStatement statement = connection.prepareStatement(sql)) {
                statement.setInt(1, inscriptionId);
                statement.setInt(2, userId);
                return statement.executeUpdate() > 0;
            }
        } catch (SQLException e) {
            System.err.println("InscriptionDAO.markCertificatIssued failed: " + e.getMessage());
        } finally {
            DatabaseConnection.closeConnection(connection);
        }
        return false;
    }

    private static InscriptionRow mapRow(ResultSet rs) throws SQLException {
        InscriptionRow row = new InscriptionRow();
        row.setInscriptionId(rs.getInt("inscription_id"));
        row.setFormationId(rs.getInt("formation_id"));
        row.setFormationTitre(rs.getString("formation_titre"));
        Date d1 = rs.getDate("formation_date_debut");
        if (d1 != null) {
            row.setFormationDateDebut(d1.toLocalDate());
        }
        Date d2 = rs.getDate("formation_date_fin");
        if (d2 != null) {
            row.setFormationDateFin(d2.toLocalDate());
        }
        Timestamp ts = rs.getTimestamp("date_inscription");
        if (ts != null) {
            row.setDateInscription(ts.toLocalDateTime());
        }
        row.setStatut(rs.getString("statut"));
        row.setProgression(rs.getInt("progression"));
        row.setCertificat(rs.getInt("certificat") != 0);
        return row;
    }

    private static Inscription mapInscription(ResultSet rs) throws SQLException {
        Inscription i = new Inscription();
        i.setId(rs.getInt("id"));
        Timestamp ts = rs.getTimestamp("date_inscription");
        if (ts != null) {
            i.setDateInscription(ts.toLocalDateTime());
        }
        i.setStatut(rs.getString("statut"));
        i.setProgression(rs.getInt("progression"));
        i.setCertificat(rs.getInt("certificat") != 0);
        i.setUserId(rs.getInt("user_id"));
        i.setFormationId(rs.getInt("formation_id"));
        Timestamp absent = rs.getTimestamp("absent_follow_up_sent_at");
        if (absent != null) {
            i.setAbsentFollowUpSentAt(absent.toLocalDateTime());
        }
        return i;
    }
}
