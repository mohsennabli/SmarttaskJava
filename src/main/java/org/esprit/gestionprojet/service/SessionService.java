package org.esprit.gestionprojet.service;

import org.esprit.gestionprojet.model.User;

public class SessionService {
    private User currentUser;
    private Integer currentUserId;
    private String office;

    public User getCurrentUser() {
        return currentUser;
    }

    public void setCurrentUser(User currentUser) {
        this.currentUser = currentUser;
    }

    public Integer getCurrentUserId() {
        return currentUserId;
    }

    public void setCurrentUserId(Integer currentUserId) {
        this.currentUserId = currentUserId;
    }

    public String getOffice() {
        return office;
    }

    public void setOffice(String office) {
        this.office = office;
    }

    public void logout() {
        currentUser = null;
        currentUserId = null;
        office = null;
    }
}
