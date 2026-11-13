package org.delcom.ifs25045springbootcashflow.domain.entity;

import java.time.LocalDateTime;

public class User {
    private Integer id;
    private final String username;
    private String name;
    private String passwordHash;
    private final Role role;
    private String photoPath;
    private final LocalDateTime createdAt;

    public User(Integer id, String username, String name, String passwordHash,
                Role role, String photoPath, LocalDateTime createdAt) {
        this.id = id;
        this.username = username;
        this.name = name;
        this.passwordHash = passwordHash;
        this.role = role;
        this.photoPath = photoPath;
        this.createdAt = createdAt;
    }

    public Integer getId() { return id; }
    public void setId(Integer id) { this.id = id; }
    public String getUsername() { return username; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getPasswordHash() { return passwordHash; }
    public void setPasswordHash(String passwordHash) { this.passwordHash = passwordHash; }
    public Role getRole() { return role; }
    public String getPhotoPath() { return photoPath; }
    public void setPhotoPath(String photoPath) { this.photoPath = photoPath; }
    public LocalDateTime getCreatedAt() { return createdAt; }

    public boolean isAdmin() {
        return role == Role.ADMIN;
    }
}
