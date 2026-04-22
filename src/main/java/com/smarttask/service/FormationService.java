package com.smarttask.service;

import com.smarttask.dao.FormationDAO;
import com.smarttask.model.Formation;
import com.smarttask.model.User;
import com.smarttask.util.AppSession;

import java.util.List;
import java.util.Optional;

public class FormationService {

    private final FormationDAO formationDAO = new FormationDAO();

    public List<Formation> listAll() {
        return formationDAO.findAll();
    }

    public List<Formation> search(String keyword) {
        return formationDAO.search(keyword);
    }

    public Optional<Formation> findById(int id) {
        return formationDAO.findById(id);
    }

    /**
     * Only users with type {@code manager} (case-insensitive) may add, update, or delete formations.
     */
    public boolean isCurrentUserManager() {
        User u = AppSession.getCurrentUser();
        return u != null && u.getType() != null && "manager".equalsIgnoreCase(u.getType().trim());
    }

    public FormationCrudResult createFormation(Formation f) {
        if (!isCurrentUserManager()) {
            return FormationCrudResult.FORBIDDEN;
        }
        if (!validateForWrite(f)) {
            return FormationCrudResult.VALIDATION_ERROR;
        }
        int id = formationDAO.insert(f);
        return id > 0 ? FormationCrudResult.SUCCESS : FormationCrudResult.DB_ERROR;
    }

    public FormationCrudResult updateFormation(Formation f) {
        if (!isCurrentUserManager()) {
            return FormationCrudResult.FORBIDDEN;
        }
        if (f.getId() <= 0 || !validateForWrite(f)) {
            return FormationCrudResult.VALIDATION_ERROR;
        }
        if (formationDAO.findById(f.getId()).isEmpty()) {
            return FormationCrudResult.NOT_FOUND;
        }
        return formationDAO.update(f) ? FormationCrudResult.SUCCESS : FormationCrudResult.DB_ERROR;
    }

    public FormationCrudResult deleteFormation(int formationId) {
        if (!isCurrentUserManager()) {
            return FormationCrudResult.FORBIDDEN;
        }
        if (formationId <= 0) {
            return FormationCrudResult.VALIDATION_ERROR;
        }
        if (formationDAO.findById(formationId).isEmpty()) {
            return FormationCrudResult.NOT_FOUND;
        }
        return formationDAO.deleteById(formationId) ? FormationCrudResult.SUCCESS : FormationCrudResult.DB_ERROR;
    }

    private static boolean validateForWrite(Formation f) {
        if (f == null) {
            return false;
        }
        if (f.getTitre() == null || f.getTitre().isBlank()) {
            return false;
        }
        if (f.getDateDebut() == null || f.getDateFin() == null) {
            return false;
        }
        if (f.getNiveau() == null || f.getNiveau().isBlank()) {
            return false;
        }
        if (f.getStatut() == null || f.getStatut().isBlank()) {
            return false;
        }
        return true;
    }

    public FormationDAO getFormationDAO() {
        return formationDAO;
    }
}
