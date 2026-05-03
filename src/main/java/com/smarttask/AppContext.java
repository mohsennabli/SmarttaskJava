package com.smarttask;

import com.smarttask.dao.*;
import com.smarttask.util.*;

public final class AppContext {
    private static final SessionService SESSION_SERVICE = new SessionService();
    private static final ProjetRepository PROJET_REPOSITORY = new ProjetRepository();
    private static final TacheRepository TACHE_REPOSITORY = new TacheRepository();
    private static final UserRepository USER_REPOSITORY = new UserRepository();
    private static final GroqTaskRecommendationService GROQ_TASK_RECOMMENDATION_SERVICE = new GroqTaskRecommendationService();
    private static final ProjectNotificationService PROJECT_NOTIFICATION_SERVICE = new ProjectNotificationService();

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

    public static UserRepository userRepository() {
        return USER_REPOSITORY;
    }

    public static GroqTaskRecommendationService groqTaskRecommendationService() {
        return GROQ_TASK_RECOMMENDATION_SERVICE;
    }

    public static ProjectNotificationService projectNotificationService() {
        return PROJECT_NOTIFICATION_SERVICE;
    }
}
