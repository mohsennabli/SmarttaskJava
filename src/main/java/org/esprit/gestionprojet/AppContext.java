package org.esprit.gestionprojet;

import org.esprit.gestionprojet.service.ProjetRepository;
import org.esprit.gestionprojet.service.SessionService;
import org.esprit.gestionprojet.service.TacheRepository;

public final class AppContext {
    private static final SessionService SESSION_SERVICE = new SessionService();
    private static final ProjetRepository PROJET_REPOSITORY = new ProjetRepository();
    private static final TacheRepository TACHE_REPOSITORY = new TacheRepository();

    private AppContext() {
    }

    public static SessionService sessionService() {
        return SESSION_SERVICE;
    }

    public static ProjetRepository projetRepository() {
        return PROJET_REPOSITORY;
    }

    public static TacheRepository tacheRepository() {
        return TACHE_REPOSITORY;
    }
}
