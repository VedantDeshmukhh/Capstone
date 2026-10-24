package com.resolvex.model;

/** A login account. Role is ADMIN, USER or RESOLVER. */
public class Account {

    private String username;
    private String passwordHash;
    private String fullName;
    private String role;
    private String groupName;   // only for USER accounts

    public Account(String username, String passwordHash, String fullName, String role, String groupName) {
        this.username = username;
        this.passwordHash = passwordHash;
        this.fullName = fullName;
        this.role = role;
        this.groupName = groupName;
    }

    public String getUsername() { return username; }
    public String getPasswordHash() { return passwordHash; }
    public String getFullName() { return fullName; }
    public String getRole() { return role; }
    public String getGroupName() { return groupName; }

    public String toLine() {
        return username + "|" + passwordHash + "|" + fullName + "|" + role + "|" + groupName;
    }

    public static Account fromLine(String line) {
        String[] p = line.split("\\|", -1);
        return new Account(p[0], p[1], p[2], p[3], p[4]);
    }
}
