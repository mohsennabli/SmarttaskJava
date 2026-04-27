package com.smarttask.model;

import java.time.LocalDateTime;

public class User {
    private int iduser;
    private String name;
    private String email;
    private String password;
    private String type;
    private String googleId;
    private String githubId;
    private String roles;
    private boolean isEnabled;
    private String linkedinId;
    private String resetToken;
    private LocalDateTime resetTokenExpiresAt;
    private String avatarName;
    private LocalDateTime updatedAt;
    private String faceEmbedding;

    public User() {
    }

    public User(int iduser, String name, String email, String password, String type, String googleId, String roles,
                boolean isEnabled, String linkedinId, String resetToken, LocalDateTime resetTokenExpiresAt,
                String avatarName, LocalDateTime updatedAt, String faceEmbedding) {
        this.iduser = iduser;
        this.name = name;
        this.email = email;
        this.password = password;
        this.type = type;
        this.googleId = googleId;
        this.roles = roles;
        this.isEnabled = isEnabled;
        this.linkedinId = linkedinId;
        this.resetToken = resetToken;
        this.resetTokenExpiresAt = resetTokenExpiresAt;
        this.avatarName = avatarName;
        this.updatedAt = updatedAt;
        this.faceEmbedding = faceEmbedding;
    }

    public int getIduser() {
        return iduser;
    }

    public void setIduser(int iduser) {
        this.iduser = iduser;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getGoogleId() {
        return googleId;
    }

    public void setGoogleId(String googleId) {
        this.googleId = googleId;
    }

    public String getGithubId() {
        return githubId;
    }

    public void setGithubId(String githubId) {
        this.githubId = githubId;
    }

    public String getRoles() {
        return roles;
    }

    public void setRoles(String roles) {
        this.roles = roles;
    }

    public boolean isEnabled() {
        return isEnabled;
    }

    public void setEnabled(boolean enabled) {
        isEnabled = enabled;
    }

    public String getLinkedinId() {
        return linkedinId;
    }

    public void setLinkedinId(String linkedinId) {
        this.linkedinId = linkedinId;
    }

    public String getResetToken() {
        return resetToken;
    }

    public void setResetToken(String resetToken) {
        this.resetToken = resetToken;
    }

    public LocalDateTime getResetTokenExpiresAt() {
        return resetTokenExpiresAt;
    }

    public void setResetTokenExpiresAt(LocalDateTime resetTokenExpiresAt) {
        this.resetTokenExpiresAt = resetTokenExpiresAt;
    }

    public String getAvatarName() {
        return avatarName;
    }

    public void setAvatarName(String avatarName) {
        this.avatarName = avatarName;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }

    public String getFaceEmbedding() {
        return faceEmbedding;
    }

    public void setFaceEmbedding(String faceEmbedding) {
        this.faceEmbedding = faceEmbedding;
    }

    @Override
    public String toString() {
        return "User{" +
                "iduser=" + iduser +
                ", name='" + name + '\'' +
                ", email='" + email + '\'' +
                ", type='" + type + '\'' +
                ", isEnabled=" + isEnabled +
                '}';
    }
}

