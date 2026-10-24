package com.resolvex.service;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import com.resolvex.model.Account;
import com.resolvex.model.Category;
import com.resolvex.model.Complaint;

/** The real work behind the two dashboards. Saves to the files after every change. */
public class ComplaintManager implements ComplaintOperations {

    public static final String[] STATUSES = {"Submitted", "Under Review", "Assigned", "In Progress",
            "Waiting", "Resolved", "Closed", "Reopened", "Rejected"};

    private DataStore store = new DataStore();
    private PriorityCalculator calculator = new PriorityCalculator(store);

    public DataStore getStore() { return store; }
    public PriorityCalculator getCalculator() { return calculator; }

    // read the files again, so we see changes made by the other program
    public void reload() {
        store.load();
    }

    @Override
    public int reportComplaint(String title, String description, String category, boolean wholeCollege,
                               String groupName, int urgency, Account reporter) {
        Complaint c = new Complaint();
        c.setId(store.nextComplaintId());
        c.setTitle(title);
        c.setDescription(description);
        c.setCategory(category);
        c.setWholeCollege(wholeCollege);
        c.setGroupName(wholeCollege ? "-" : groupName);
        c.setReportedBy(reporter.getUsername());
        c.setUrgency(urgency);
        // the reporter supports the complaint automatically, if the reporter is one of the affected people
        if (wholeCollege || groupName.equalsIgnoreCase(reporter.getGroupName())) {
            c.getSupporters().add(reporter.getUsername());
        }
        store.getComplaints().add(c);
        store.save();
        return c.getId();
    }

    @Override
    public String supportComplaint(int id, Account user) {
        Complaint c = store.findComplaint(id);
        if (c == null) {
            return "No complaint with this ID.";
        }
        if (!c.isOpen()) {
            return "This complaint is already closed.";
        }
        if (!c.isWholeCollege() && !c.getGroupName().equalsIgnoreCase(user.getGroupName())) {
            return "Only members of " + c.getGroupName() + " can support this complaint.";
        }
        if (c.getSupporters().contains(user.getUsername())) {
            return "You already supported this complaint.";   // one vote per person
        }
        c.getSupporters().add(user.getUsername());
        store.addAuditLog(user.getUsername(), "VOTE_ADDED", id, "Supported complaint #" + id);
        store.save();
        return "Thanks, your support was added.";
    }

    @Override
    public String removeSupport(int id, Account user) {
        Complaint c = store.findComplaint(id);
        if (c == null) {
            return "No complaint with this ID.";
        }
        if (!c.getSupporters().remove(user.getUsername())) {
            return "You have not supported this complaint.";
        }
        store.addAuditLog(user.getUsername(), "VOTE_REMOVED", id, "Removed vote from complaint #" + id);
        store.save();
        return "Your support was removed.";
    }

    @Override
    public String assignComplaint(int id, String assignedTo) {
        Complaint c = store.findComplaint(id);
        if (c == null) {
            return "No complaint with this ID.";
        }
        c.setAssignedTo(assignedTo);
        if (c.getStatus().equals("Submitted") || c.getStatus().equals("Under Review")) {
            c.setStatus("Assigned");
        }
        store.addAuditLog("admin", "ASSIGNED", id, "Assigned complaint #" + id + " to " + assignedTo);
        store.save();
        return "Assigned to " + assignedTo + ".";
    }

    @Override
    public String changeStatus(int id, String newStatus, String note) {
        Complaint c = store.findComplaint(id);
        if (c == null) {
            return "No complaint with this ID.";
        }
        String oldStatus = c.getStatus();
        c.setStatus(newStatus);
        c.setNote(note);
        store.addAuditLog("admin", "STATUS_CHANGE", id, "Changed status from '" + oldStatus + "' to '" + newStatus + "'" + (note.isEmpty() ? "" : " (Note: " + note + ")"));
        store.save();
        return "Status changed to " + newStatus + ".";
    }

    @Override
    public String setSeverity(int id, int severity, boolean safetyCritical) {
        Complaint c = store.findComplaint(id);
        if (c == null) {
            return "No complaint with this ID.";
        }
        c.setSeverity(severity);
        c.setSafetyCritical(safetyCritical);
        store.addAuditLog("admin", "SEVERITY_UPDATE", id, "Set severity=" + severity + ", safetyCritical=" + safetyCritical);
        store.save();
        return "Severity saved.";
    }

    @Override
    public String changeCategory(int id, String category) {
        Complaint c = store.findComplaint(id);
        if (c == null) {
            return "No complaint with this ID.";
        }
        c.setCategory(category);
        store.save();
        return "Category changed to " + category + ".";
    }

    // counts keyword matches for every category and returns the best one (or null)
    @Override
    public Category suggestCategory(String text) {
        String lower = text.toLowerCase();
        Category best = null;
        int bestHits = 0;
        for (Category cat : store.getCategories()) {
            int hits = 0;
            for (String keyword : cat.getKeywords().split(",")) {
                if (!keyword.trim().isEmpty() && lower.contains(keyword.trim().toLowerCase())) {
                    hits++;
                }
            }
            if (hits > bestHits) {
                best = cat;
                bestHits = hits;
            }
        }
        return best;
    }

    @Override
    public Complaint findComplaint(int id) {
        return store.findComplaint(id);
    }

    @Override
    public List<Complaint> getAllComplaints() {
        return store.getComplaints();
    }

    // open complaints that share a word with the new title (to avoid duplicates)
    @Override
    public List<Complaint> findSimilar(String title) {
        List<Complaint> found = new ArrayList<>();
        for (Complaint c : store.getComplaints()) {
            if (!c.isOpen()) {
                continue;
            }
            for (String word : title.toLowerCase().split("\\s+")) {
                if (word.length() > 3 && c.getTitle().toLowerCase().contains(word)) {
                    found.add(c);
                    break;
                }
            }
        }
        return found;
    }

    // open complaints, highest priority score first
    @Override
    public List<Complaint> rankedList() {
        List<Complaint> list = new ArrayList<>();
        for (Complaint c : store.getComplaints()) {
            if (c.isOpen()) {
                list.add(c);
            }
        }
        Collections.sort(list, new Comparator<Complaint>() {
            @Override
            public int compare(Complaint a, Complaint b) {
                return Double.compare(calculator.rankValue(b), calculator.rankValue(a));
            }
        });
        return list;
    }

    public String updateAccount(String username, String newFullName, String newGroup, String newPassword) {
        Account acc = store.findAccount(username);
        if (acc == null) {
            return "User not found.";
        }
        acc.setFullName(newFullName);
        acc.setGroupName(newGroup);
        if (newPassword != null && !newPassword.trim().isEmpty()) {
            acc.setPasswordHash(com.resolvex.util.PasswordUtil.hash(acc.getUsername(), newPassword.trim()));
        }
        store.addAuditLog("admin", "ACCOUNT_UPDATE", 0, "Updated account: " + username + " (" + newFullName + ", " + newGroup + ")");
        store.save();
        return "Student updated successfully.";
    }
}
