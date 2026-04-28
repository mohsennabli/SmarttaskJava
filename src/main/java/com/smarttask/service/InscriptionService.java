package com.smarttask.service;

import com.smarttask.dao.FormationDAO;
import com.smarttask.dao.EnrollmentTaskDAO;
import com.smarttask.dao.InscriptionDAO;
import com.smarttask.model.EnrollmentTaskRow;
import com.smarttask.model.Formation;
import com.smarttask.model.Inscription;
import com.smarttask.model.InscriptionRow;
import com.smarttask.model.User;
import com.smarttask.util.AppSession;
import com.smarttask.util.CertificatePdfWriter;

import java.nio.file.Path;
import java.util.List;
import java.util.Optional;

public class InscriptionService {

    private static final String DEFAULT_STATUT = "en_cours";

    private final InscriptionDAO inscriptionDAO = new InscriptionDAO();
    private final FormationDAO formationDAO = new FormationDAO();
    private final EnrollmentTaskDAO enrollmentTaskDAO = new EnrollmentTaskDAO();
    private final FormationInscriptionNotifier notifier = new FormationInscriptionNotifier();

    public EnrollmentResult enrollCurrentUser(int formationId) {
        User user = AppSession.getCurrentUser();
        if (user == null) {
            return EnrollmentResult.NOT_LOGGED_IN;
        }
        Optional<Formation> formationOpt = formationDAO.findById(formationId);
        if (formationOpt.isEmpty()) {
            return EnrollmentResult.FORMATION_NOT_FOUND;
        }
        Formation formation = formationOpt.get();
        if (inscriptionDAO.existsByUserAndFormation(user.getIduser(), formationId)) {
            return EnrollmentResult.ALREADY_ENROLLED;
        }
        Integer capacity = formation.getCapacity();
        if (capacity != null && capacity > 0) {
            int count = formationDAO.countInscriptionsForFormation(formationId);
            if (count >= capacity) {
                return EnrollmentResult.FORMATION_FULL;
            }
        }
        int newId = inscriptionDAO.insert(user.getIduser(), formationId, DEFAULT_STATUT, 0, false);
        if (newId < 0) {
            return EnrollmentResult.DB_ERROR;
        }
        notifier.sendInscriptionConfirmation(user, formation, DEFAULT_STATUT);
        notifier.openFormationInGoogleCalendar(formation);
        return EnrollmentResult.SUCCESS;
    }

    public List<InscriptionRow> listMyEnrollments() {
        User user = AppSession.getCurrentUser();
        if (user == null) {
            return List.of();
        }
        return inscriptionDAO.findRowsByUserId(user.getIduser());
    }

    public ProgressUpdateResult updateProgress(int inscriptionId, int newProgress) {
        User user = AppSession.getCurrentUser();
        if (user == null) {
            return ProgressUpdateResult.NOT_LOGGED_IN;
        }
        Optional<Inscription> opt = inscriptionDAO.findById(inscriptionId);
        if (opt.isEmpty()) {
            return ProgressUpdateResult.NOT_FOUND;
        }
        Inscription ins = opt.get();
        if (ins.getUserId() != user.getIduser()) {
            return ProgressUpdateResult.FORBIDDEN;
        }
        int clamped = Math.max(0, Math.min(100, newProgress));
        if (!inscriptionDAO.updateProgression(inscriptionId, user.getIduser(), clamped)) {
            return ProgressUpdateResult.DB_ERROR;
        }
        return ProgressUpdateResult.SUCCESS;
    }

    public boolean canGenerateCertificate(Inscription inscription) {
        return inscription != null && inscription.getProgression() >= 100;
    }

    public boolean canGenerateCertificate(InscriptionRow row) {
        return row != null && row.getProgression() >= 100;
    }

    /**
     * Writes PDF and marks {@code certificat} in DB on success.
     */
    public CertificateGenerationResult generateCertificateForInscription(int inscriptionId, Path outputPath) {
        User user = AppSession.getCurrentUser();
        if (user == null) {
            return CertificateGenerationResult.NOT_LOGGED_IN;
        }
        Optional<Inscription> opt = inscriptionDAO.findById(inscriptionId);
        if (opt.isEmpty()) {
            return CertificateGenerationResult.NOT_FOUND;
        }
        Inscription ins = opt.get();
        if (ins.getUserId() != user.getIduser()) {
            return CertificateGenerationResult.FORBIDDEN;
        }
        if (!canGenerateCertificate(ins)) {
            return CertificateGenerationResult.PROGRESS_INCOMPLETE;
        }
        Optional<Formation> formationOpt = formationDAO.findById(ins.getFormationId());
        if (formationOpt.isEmpty()) {
            return CertificateGenerationResult.FORMATION_NOT_FOUND;
        }
        try {
            CertificatePdfWriter.write(
                    outputPath,
                    user.getName(),
                    formationOpt.get().getTitre(),
                    java.time.LocalDate.now()
            );
        } catch (Exception e) {
            System.err.println("Certificate generation failed: " + e.getMessage());
            return CertificateGenerationResult.IO_ERROR;
        }
        if (!inscriptionDAO.markCertificatIssued(inscriptionId, user.getIduser())) {
            return CertificateGenerationResult.DB_ERROR;
        }
        notifier.sendCertificateIssued(user, formationOpt.get());
        return CertificateGenerationResult.SUCCESS;
    }

    public boolean isEnrolledIn(int userId, int formationId) {
        return inscriptionDAO.existsByUserAndFormation(userId, formationId);
    }

    public List<EnrollmentTaskRow> listTasksForEnrollment(int inscriptionId) {
        User user = AppSession.getCurrentUser();
        if (user == null) {
            return List.of();
        }
        Optional<Inscription> opt = inscriptionDAO.findById(inscriptionId);
        if (opt.isEmpty()) {
            return List.of();
        }
        Inscription ins = opt.get();
        if (ins.getUserId() != user.getIduser()) {
            return List.of();
        }
        return enrollmentTaskDAO.findTasksForInscription(inscriptionId, ins.getFormationId());
    }

    public TaskCompletionResult setTaskCompletion(int inscriptionId, int taskId, boolean completed) {
        User user = AppSession.getCurrentUser();
        if (user == null) {
            return TaskCompletionResult.NOT_LOGGED_IN;
        }
        Optional<Inscription> opt = inscriptionDAO.findById(inscriptionId);
        if (opt.isEmpty()) {
            return TaskCompletionResult.NOT_FOUND;
        }
        Inscription ins = opt.get();
        if (ins.getUserId() != user.getIduser()) {
            return TaskCompletionResult.FORBIDDEN;
        }
        if (!enrollmentTaskDAO.upsertCompletion(inscriptionId, taskId, completed)) {
            return TaskCompletionResult.DB_ERROR;
        }
        int progress = enrollmentTaskDAO.computeProgressPercent(inscriptionId, ins.getFormationId());
        if (!inscriptionDAO.updateProgression(inscriptionId, user.getIduser(), progress)) {
            return TaskCompletionResult.DB_ERROR;
        }
        return TaskCompletionResult.SUCCESS;
    }

    public InscriptionDAO getInscriptionDAO() {
        return inscriptionDAO;
    }
}
