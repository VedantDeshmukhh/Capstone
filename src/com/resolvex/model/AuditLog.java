package com.resolvex.model;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/** Record of an audit event or user action */
public class AuditLog {
    private static final DateTimeFormatter FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private String timestamp;
    private String username;
    private String actionType; // STATUS_CHANGE, SEVERITY_CHANGE, ASSIGNMENT, VOTE_SUPPORT, VOTE_REMOVE, STUDENT_EDIT, STUDENT_ADD
    private int complaintId;   // 0 if not tied to specific complaint
    private String details;

    public AuditLog(String timestamp, String username, String actionType, int complaintId, String details) {
        this.timestamp = timestamp;
        this.username = username;
        this.actionType = actionType;
        this.complaintId = complaintId;
        this.details = details;
    }

    public static AuditLog create(String username, String actionType, int complaintId, String details) {
        return new AuditLog(LocalDateTime.now().format(FMT), username, actionType, complaintId, details);
    }

    public String getTimestamp() { return timestamp; }
    public String getUsername() { return username; }
    public String getActionType() { return actionType; }
    public int getComplaintId() { return complaintId; }
    public String getDetails() { return details; }

    public String toLine() {
        return timestamp + "|" + username + "|" + actionType + "|" + complaintId + "|" + details;
    }

    public static AuditLog fromLine(String line) {
        String[] p = line.split("\\|", 5);
        if (p.length < 5) return null;
        int cid = 0;
        try { cid = Integer.parseInt(p[3]); } catch (Exception e) {}
        return new AuditLog(p[0], p[1], p[2], cid, p[4]);
    }
}
