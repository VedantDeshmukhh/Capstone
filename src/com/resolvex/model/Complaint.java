package com.resolvex.model;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/** One problem reported by a user. */
public class Complaint {

    private int id;
    private String title;
    private String description;
    private String category;
    private boolean wholeCollege;      // false means only one group is affected
    private String groupName;          // the affected group
    private String reportedBy;         // username of the reporter
    private String assignedTo = "";    // username of the resolver
    private String status = "Submitted";
    private String note = "";          // last note written when status changed
    private int urgency;               // 1 to 5, chosen by the user
    private int severity = 3;          // 1 to 5, chosen by the admin
    private boolean safetyCritical;    // admin marks dangerous issues
    private LocalDate createdOn = LocalDate.now();
    private List<String> supporters = new ArrayList<>();   // usernames who voted

    public Complaint() {
    }

    // ----- getters and setters -----
    public int getId() { return id; }
    public void setId(int id) { this.id = id; }
    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public String getCategory() { return category; }
    public void setCategory(String category) { this.category = category; }
    public boolean isWholeCollege() { return wholeCollege; }
    public void setWholeCollege(boolean wholeCollege) { this.wholeCollege = wholeCollege; }
    public String getGroupName() { return groupName; }
    public void setGroupName(String groupName) { this.groupName = groupName; }
    public String getReportedBy() { return reportedBy; }
    public void setReportedBy(String reportedBy) { this.reportedBy = reportedBy; }
    public String getAssignedTo() { return assignedTo; }
    public void setAssignedTo(String assignedTo) { this.assignedTo = assignedTo; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public String getNote() { return note; }
    public void setNote(String note) { this.note = note; }
    public int getUrgency() { return urgency; }
    public void setUrgency(int urgency) { this.urgency = urgency; }
    public int getSeverity() { return severity; }
    public void setSeverity(int severity) { this.severity = severity; }
    public boolean isSafetyCritical() { return safetyCritical; }
    public void setSafetyCritical(boolean safetyCritical) { this.safetyCritical = safetyCritical; }
    public LocalDate getCreatedOn() { return createdOn; }
    public void setCreatedOn(LocalDate createdOn) { this.createdOn = createdOn; }
    public List<String> getSupporters() { return supporters; }

    // an issue is open until it is resolved, closed or rejected
    public boolean isOpen() {
        return !(status.equals("Resolved") || status.equals("Closed") || status.equals("Rejected"));
    }

    // true if this complaint belongs to the group (whole college complaints belong to everyone)
    public boolean isForGroup(String group) {
        return wholeCollege || groupName.equalsIgnoreCase(group);
    }

    // one line of the complaints.txt file
    public String toLine() {
        return id + "|" + title + "|" + description + "|" + category + "|" + wholeCollege + "|" + groupName
                + "|" + reportedBy + "|" + assignedTo + "|" + status + "|" + note + "|" + urgency
                + "|" + severity + "|" + safetyCritical + "|" + createdOn + "|" + String.join(",", supporters);
    }

    public static Complaint fromLine(String line) {
        String[] p = line.split("\\|", -1);
        Complaint c = new Complaint();
        c.id = Integer.parseInt(p[0]);
        c.title = p[1];
        c.description = p[2];
        c.category = p[3];
        c.wholeCollege = Boolean.parseBoolean(p[4]);
        c.groupName = p[5];
        c.reportedBy = p[6];
        c.assignedTo = p[7];
        c.status = p[8];
        c.note = p[9];
        c.urgency = Integer.parseInt(p[10]);
        c.severity = Integer.parseInt(p[11]);
        c.safetyCritical = Boolean.parseBoolean(p[12]);
        c.createdOn = LocalDate.parse(p[13]);
        if (!p[14].isEmpty()) {
            c.supporters = new ArrayList<>(Arrays.asList(p[14].split(",")));
        }
        return c;
    }
}
