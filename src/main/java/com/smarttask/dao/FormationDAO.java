package com.smarttask.dao;

import com.smarttask.model.Formation;
import com.smarttask.util.DatabaseConnection;

import java.sql.Connection;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Types;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class FormationDAO {

    /**
     * When {@code formation.capacity} is {@code NULL}, capacity checks treat it as unlimited
     * (see {@link com.smarttask.service.InscriptionService}).
     */
    public List<Formation> findAll() {
        String sql = "SELECT * FROM formation ORDER BY titre ASC";
        Connection connection = null;
        List<Formation> list = new ArrayList<>();
        try {
            connection = DatabaseConnection.getConnection();
            try (PreparedStatement statement = connection.prepareStatement(sql);
                 ResultSet rs = statement.executeQuery()) {
                while (rs.next()) {
                    list.add(mapFormation(rs));
                }
            }
        } catch (SQLException e) {
            System.err.println("FormationDAO.findAll failed: " + e.getMessage());
        } finally {
            DatabaseConnection.closeConnection(connection);
        }
        return list;
    }

    public List<Formation> findActive() {
        String sql = "SELECT * FROM formation WHERE LOWER(statut) = 'active' ORDER BY titre ASC";
        Connection connection = null;
        List<Formation> list = new ArrayList<>();
        try {
            connection = DatabaseConnection.getConnection();
            try (PreparedStatement statement = connection.prepareStatement(sql);
                 ResultSet rs = statement.executeQuery()) {
                while (rs.next()) {
                    list.add(mapFormation(rs));
                }
            }
        } catch (SQLException e) {
            System.err.println("FormationDAO.findActive failed: " + e.getMessage());
        } finally {
            DatabaseConnection.closeConnection(connection);
        }
        return list;
    }

    public Optional<Formation> findById(int id) {
        String sql = "SELECT * FROM formation WHERE id = ?";
        Connection connection = null;
        try {
            connection = DatabaseConnection.getConnection();
            try (PreparedStatement statement = connection.prepareStatement(sql)) {
                statement.setInt(1, id);
                try (ResultSet rs = statement.executeQuery()) {
                    if (rs.next()) {
                        return Optional.of(mapFormation(rs));
                    }
                }
            }
        } catch (SQLException e) {
            System.err.println("FormationDAO.findById failed: " + e.getMessage());
        } finally {
            DatabaseConnection.closeConnection(connection);
        }
        return Optional.empty();
    }

    public List<Formation> search(String keyword) {
        if (keyword == null || keyword.isBlank()) {
            return findAll();
        }
        String pattern = "%" + keyword.trim() + "%";
        String sql = "SELECT * FROM formation WHERE titre LIKE ? OR categorie LIKE ? OR niveau LIKE ? "
                + "OR description LIKE ? ORDER BY titre ASC";
        Connection connection = null;
        List<Formation> list = new ArrayList<>();
        try {
            connection = DatabaseConnection.getConnection();
            try (PreparedStatement statement = connection.prepareStatement(sql)) {
                statement.setString(1, pattern);
                statement.setString(2, pattern);
                statement.setString(3, pattern);
                statement.setString(4, pattern);
                try (ResultSet rs = statement.executeQuery()) {
                    while (rs.next()) {
                        list.add(mapFormation(rs));
                    }
                }
            }
        } catch (SQLException e) {
            System.err.println("FormationDAO.search failed: " + e.getMessage());
        } finally {
            DatabaseConnection.closeConnection(connection);
        }
        return list;
    }

    public int countInscriptionsForFormation(int formationId) {
        String sql = "SELECT COUNT(*) FROM inscription WHERE formation_id = ?";
        Connection connection = null;
        try {
            connection = DatabaseConnection.getConnection();
            try (PreparedStatement statement = connection.prepareStatement(sql)) {
                statement.setInt(1, formationId);
                try (ResultSet rs = statement.executeQuery()) {
                    if (rs.next()) {
                        return rs.getInt(1);
                    }
                }
            }
        } catch (SQLException e) {
            System.err.println("FormationDAO.countInscriptionsForFormation failed: " + e.getMessage());
        } finally {
            DatabaseConnection.closeConnection(connection);
        }
        return 0;
    }

    /**
     * @return generated id, or -1 on failure
     */
    public int insert(Formation f) {
        String sql = "INSERT INTO formation (titre, description, date_debut, date_fin, duree, niveau, categorie, "
                + "statut, google_event_id, capacity) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
        Connection connection = null;
        try {
            connection = DatabaseConnection.getConnection();
            try (PreparedStatement statement = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
                bindFormationWrite(statement, f);
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
            System.err.println("FormationDAO.insert failed: " + e.getMessage());
        } finally {
            DatabaseConnection.closeConnection(connection);
        }
        return -1;
    }

    public boolean update(Formation f) {
        String sql = "UPDATE formation SET titre = ?, description = ?, date_debut = ?, date_fin = ?, duree = ?, "
                + "niveau = ?, categorie = ?, statut = ?, google_event_id = ?, capacity = ? WHERE id = ?";
        Connection connection = null;
        try {
            connection = DatabaseConnection.getConnection();
            try (PreparedStatement statement = connection.prepareStatement(sql)) {
                bindFormationWrite(statement, f);
                statement.setInt(11, f.getId());
                return statement.executeUpdate() > 0;
            }
        } catch (SQLException e) {
            System.err.println("FormationDAO.update failed: " + e.getMessage());
        } finally {
            DatabaseConnection.closeConnection(connection);
        }
        return false;
    }

    /**
     * Deletes a formation. Inscriptions are removed by DB ON DELETE CASCADE.
     */
    public boolean deleteById(int id) {
        String sql = "DELETE FROM formation WHERE id = ?";
        Connection connection = null;
        try {
            connection = DatabaseConnection.getConnection();
            try (PreparedStatement statement = connection.prepareStatement(sql)) {
                statement.setInt(1, id);
                return statement.executeUpdate() > 0;
            }
        } catch (SQLException e) {
            System.err.println("FormationDAO.deleteById failed: " + e.getMessage());
        } finally {
            DatabaseConnection.closeConnection(connection);
        }
        return false;
    }

    private static void bindFormationWrite(PreparedStatement statement, Formation f) throws SQLException {
        statement.setString(1, f.getTitre());
        if (f.getDescription() != null) {
            statement.setString(2, f.getDescription());
        } else {
            statement.setNull(2, Types.LONGVARCHAR);
        }
        statement.setDate(3, f.getDateDebut() != null ? Date.valueOf(f.getDateDebut()) : null);
        statement.setDate(4, f.getDateFin() != null ? Date.valueOf(f.getDateFin()) : null);
        if (f.getDuree() != null) {
            statement.setInt(5, f.getDuree());
        } else {
            statement.setNull(5, Types.INTEGER);
        }
        statement.setString(6, f.getNiveau());
        if (f.getCategorie() != null && !f.getCategorie().isBlank()) {
            statement.setString(7, f.getCategorie());
        } else {
            statement.setNull(7, Types.VARCHAR);
        }
        statement.setString(8, f.getStatut());
        if (f.getGoogleEventId() != null && !f.getGoogleEventId().isBlank()) {
            statement.setString(9, f.getGoogleEventId());
        } else {
            statement.setNull(9, Types.VARCHAR);
        }
        if (f.getCapacity() != null) {
            statement.setInt(10, f.getCapacity());
        } else {
            statement.setNull(10, Types.INTEGER);
        }
    }

    private static Formation mapFormation(ResultSet rs) throws SQLException {
        Formation f = new Formation();
        f.setId(rs.getInt("id"));
        f.setTitre(rs.getString("titre"));
        f.setDescription(rs.getString("description"));
        Date dd = rs.getDate("date_debut");
        if (dd != null) {
            f.setDateDebut(dd.toLocalDate());
        }
        Date df = rs.getDate("date_fin");
        if (df != null) {
            f.setDateFin(df.toLocalDate());
        }
        int duree = rs.getInt("duree");
        if (!rs.wasNull()) {
            f.setDuree(duree);
        } else {
            f.setDuree(null);
        }
        f.setNiveau(rs.getString("niveau"));
        f.setCategorie(rs.getString("categorie"));
        f.setStatut(rs.getString("statut"));
        f.setGoogleEventId(rs.getString("google_event_id"));
        int cap = rs.getInt("capacity");
        if (!rs.wasNull()) {
            f.setCapacity(cap);
        } else {
            f.setCapacity(null);
        }
        return f;
    }
}
