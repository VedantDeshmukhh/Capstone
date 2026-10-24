package com.resolvex.service;

import java.util.List;

import com.resolvex.model.Account;
import com.resolvex.model.Category;
import com.resolvex.model.Complaint;

/** Everything that can be done with complaints. */
public interface ComplaintOperations {

    // groupName tells which students are affected (ignored when wholeCollege is true)
    int reportComplaint(String title, String description, String category, boolean wholeCollege,
                        String groupName, int urgency, Account reporter);

    String supportComplaint(int id, Account user);

    String removeSupport(int id, Account user);

    String assignComplaint(int id, String assignedTo);

    String changeStatus(int id, String newStatus, String note);

    String setSeverity(int id, int severity, boolean safetyCritical);

    String changeCategory(int id, String category);

    Category suggestCategory(String text);

    Complaint findComplaint(int id);

    List<Complaint> getAllComplaints();

    List<Complaint> findSimilar(String title);

    List<Complaint> rankedList();
}
