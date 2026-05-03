package com.smarttask.dao;

import com.smarttask.model.Tache;
import com.smarttask.util.DBconnexion;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class TacheRepository {
    public List<Tache> findAll() {
        String sql = "SELECT id, libelle, priorite, date_limite, etat, projet_id FROM tache";
        List<Tache> taches = new ArrayList<>();

        try {
            Connection connection = DBconnexion.getInstance().getCnx();
            try (PreparedStatement statement = connection.prepareStatement(sql);
                 ResultSet rs = statement.executeQuery()) {
                while (rs.next()) {
                    taches.add(mapTache(rs));
                }
            }
            return taches;
        } catch (Exception e) {
            throw new IllegalStateException("Failed to read tasks", e);
        }
    }

    public int insert(Tache tache) {
        String sql = "INSERT INTO tache (libelle, priorite, date_limite, etat, projet_id) VALUES (?, ?, ?, ?, ?)";

        try {
            Connection connection = DBconnexion.getInstance().getCnx();
            try (PreparedStatement statement = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
                statement.setString(1, tache.getLibelle());
                statement.setString(2, tache.getPriorite());
                statement.setDate(3, Date.valueOf(tache.getDateLimite()));
                statement.setString(4, tache.getEtat());
                statement.setInt(5, tache.getProjetId());
                statement.executeUpdate();

                try (ResultSet keys = statement.getGeneratedKeys()) {
                    if (keys.next()) {
                        return keys.getInt(1);
                    }
                }
                return 0;
            }
        } catch (Exception e) {
            throw new IllegalStateException("Failed to create task", e);
        }
    }

    public List<Tache> findByProjetId(int projetId) {
        String sql = "SELECT id, libelle, priorite, date_limite, etat, projet_id FROM tache WHERE projet_id = ?";
        List<Tache> taches = new ArrayList<>();

        try {
            Connection connection = DBconnexion.getInstance().getCnx();
            try (PreparedStatement statement = connection.prepareStatement(sql)) {
                statement.setInt(1, projetId);
                try (ResultSet rs = statement.executeQuery()) {
                    while (rs.next()) {
                        taches.add(mapTache(rs));
                    }
                }
            }
            return taches;
        } catch (Exception e) {
            throw new IllegalStateException("Failed to read tasks for project", e);
        }
    }

    public void update(Tache tache) {
        String sql = "UPDATE tache SET libelle = ?, priorite = ?, date_limite = ?, etat = ?, projet_id = ? WHERE id = ?";

        try {
            Connection connection = DBconnexion.getInstance().getCnx();
            try (PreparedStatement statement = connection.prepareStatement(sql)) {
                statement.setString(1, tache.getLibelle());
                statement.setString(2, tache.getPriorite());
                statement.setDate(3, Date.valueOf(tache.getDateLimite()));
                statement.setString(4, tache.getEtat());
                statement.setInt(5, tache.getProjetId());
                statement.setInt(6, tache.getId());
                statement.executeUpdate();
            }
        } catch (Exception e) {
            throw new IllegalStateException("Failed to update task", e);
        }
    }

    public void deleteById(int id) {
        String sql = "DELETE FROM tache WHERE id = ?";

        try {
            Connection connection = DBconnexion.getInstance().getCnx();
            try (PreparedStatement statement = connection.prepareStatement(sql)) {
                statement.setInt(1, id);
                statement.executeUpdate();
            }
        } catch (Exception e) {
            throw new IllegalStateException("Failed to delete task", e);
        }
    }

    private Tache mapTache(ResultSet rs) throws Exception {
        return new Tache(
            rs.getInt("id"),
            rs.getString("libelle"),
            rs.getString("priorite"),
            rs.getDate("date_limite").toLocalDate(),
            rs.getString("etat"),
            rs.getInt("projet_id")
        );
    }
}
