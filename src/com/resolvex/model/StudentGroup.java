package com.resolvex.model;

/** A section / department / team and how many people it has. */
public class StudentGroup {

    private String name;
    private int totalPeople;

    public StudentGroup(String name, int totalPeople) {
        this.name = name;
        this.totalPeople = totalPeople;
    }

    public String getName() { return name; }
    public int getTotalPeople() { return totalPeople; }
    public void setTotalPeople(int totalPeople) { this.totalPeople = totalPeople; }

    // one line of the groups.txt file
    public String toLine() {
        return name + "|" + totalPeople;
    }

    public static StudentGroup fromLine(String line) {
        String[] p = line.split("\\|", -1);
        return new StudentGroup(p[0], Integer.parseInt(p[1]));
    }
}
