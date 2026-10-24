package com.resolvex.service;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;

import com.resolvex.model.Complaint;
import com.resolvex.model.StudentGroup;

/**
 * The fair priority formula.
 * score (in %) = 0.35*severity + 0.35*support% + 0.20*urgency + 0.10*waiting
 * (every part is converted to a value from 0 to 100)
 * support% = votes / students of the SECTION * 100, so a small section (AIML = 10)
 * is treated as fairly as a big one (CSE = 50).
 */
public class PriorityCalculator {

    private static final double W_SEVERITY = 0.35;
    private static final double W_SUPPORT = 0.35;
    private static final double W_URGENCY = 0.20;
    private static final double W_WAITING = 0.10;
    private static final int MAX_WAIT_DAYS = 30;   // after 30 days the waiting score is full

    private DataStore store;

    public PriorityCalculator(DataStore store) {
        this.store = store;
    }

    // all people of all groups
    public int totalPeople() {
        int total = 0;
        for (StudentGroup g : store.getGroups()) {
            total = total + g.getTotalPeople();
        }
        return total;
    }

    // how many people can be affected by this complaint (the bottom part of the percentage)
    public int eligiblePeople(Complaint c) {
        if (!c.isWholeCollege()) {
            StudentGroup g = store.findGroup(c.getGroupName());
            if (g != null) {
                return g.getTotalPeople();
            }
        }
        return totalPeople();
    }

    private double round(double value) {
        return Math.round(value * 100.0) / 100.0;
    }

    // support % = votes / eligible people * 100
    public double supportPercent(Complaint c) {
        int eligible = eligiblePeople(c);
        if (eligible == 0) {
            return 0;
        }
        return Math.min(round(c.getSupporters().size() * 100.0 / eligible), 100);
    }

    public long daysWaiting(Complaint c) {
        return ChronoUnit.DAYS.between(c.getCreatedOn(), LocalDate.now());
    }

    public double score(Complaint c) {
        double waiting = Math.min(daysWaiting(c) * 100.0 / MAX_WAIT_DAYS, 100);
        double score = W_SEVERITY * c.getSeverity() * 20
                     + W_SUPPORT * supportPercent(c)
                     + W_URGENCY * c.getUrgency() * 20
                     + W_WAITING * waiting;
        return round(score);
    }

    // used only for sorting: safety critical complaints are listed first, then by score
    public double rankValue(Complaint c) {
        return (c.isSafetyCritical() ? 1000 : 0) + score(c);
    }

    // a sentence that tells the admin WHY the complaint has this rank
    public String explain(Complaint c) {
        String text = "";
        if (c.isSafetyCritical()) {
            text = "Safety critical, so it is listed first. ";
        }
        return text + c.getSupporters().size() + " of " + eligiblePeople(c) + " students of "
                + c.getGroupName() + " support it (" + supportPercent(c) + "%), severity " + c.getSeverity()
                + "/5, urgency " + c.getUrgency() + "/5, waiting " + daysWaiting(c) + " day(s).";
    }
}
