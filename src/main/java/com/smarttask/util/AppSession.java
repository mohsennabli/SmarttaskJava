package com.smarttask.util;

import com.smarttask.model.User;

public final class AppSession {
    private static User currentUser;

    private AppSession() {
        // Utility class.
    }

    public static User getCurrentUser() {
        return currentUser;
    }

    public static void setCurrentUser(User user) {
        currentUser = user;
    }

    public static void clear() {
        currentUser = null;
    }
}

