package com.resolvex.service;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import com.resolvex.model.Account;
import com.resolvex.model.Category;
import com.resolvex.model.Complaint;
import com.resolvex.model.StudentGroup;
import com.resolvex.util.PasswordUtil;

/**
 * Keeps all the data in text files inside the "data" folder, so the Admin and
 * User programs can share the same data even though they run separately.
 */
public class DataStore {

    private static final String FOLDER = "data";
    private static final Path COMPLAINT_FILE = Paths.get(FOLDER, "complaints.txt");
    private static final Path ACCOUNT_FILE = Paths.get(FOLDER, "accounts.txt");
    private static final Path GROUP_FILE = Paths.get(FOLDER, "groups.txt");
    private static final Path CATEGORY_FILE = Paths.get(FOLDER, "categories.txt");
    private static final Path STUDENT_CSV = Paths.get(FOLDER, "import", "students.csv");
    private static final Path COMPLAINT_CSV = Paths.get(FOLDER, "import", "complaints.csv");

    private List<Complaint> complaints = new ArrayList<>();
    private List<Account> accounts = new ArrayList<>();
    private List<StudentGroup> groups = new ArrayList<>();
    private List<Category> categories = new ArrayList<>();

    public DataStore() {
        new File(FOLDER).mkdirs();
        load();
        if (accounts.isEmpty()) {
            importFromCsv();   // first run only: read data/import/*.csv
        }
        if (categories.isEmpty()) {
            createDefaultCategories();
        }
        save();
    }

    public List<Complaint> getComplaints() { return complaints; }
    public List<Account> getAccounts() { return accounts; }
    public List<StudentGroup> getGroups() { return groups; }
    public List<Category> getCategories() { return categories; }

    // ----- reading and writing the files -----

    private List<String> readLines(Path file) {
        try {
            if (Files.exists(file)) {
                return Files.readAllLines(file, StandardCharsets.UTF_8);
            }
        } catch (IOException e) {
            System.out.println("Could not read " + file);
        }
        return new ArrayList<>();
    }

    // loads again from the files (so changes made by the other program are visible)
    public void load() {
        complaints = new ArrayList<>();
        for (String line : readLines(COMPLAINT_FILE)) {
            if (!line.trim().isEmpty()) {
                complaints.add(Complaint.fromLine(line));
            }
        }
        accounts = new ArrayList<>();
        for (String line : readLines(ACCOUNT_FILE)) {
            if (!line.trim().isEmpty()) {
                accounts.add(Account.fromLine(line));
            }
        }
        groups = new ArrayList<>();
        for (String line : readLines(GROUP_FILE)) {
            if (!line.trim().isEmpty()) {
                groups.add(StudentGroup.fromLine(line));
            }
        }
        categories = new ArrayList<>();
        for (String line : readLines(CATEGORY_FILE)) {
            if (!line.trim().isEmpty()) {
                categories.add(Category.fromLine(line));
            }
        }
    }

    public void save() {
        try {
            List<String> lines = new ArrayList<>();
            for (Complaint c : complaints) {
                lines.add(c.toLine());
            }
            Files.write(COMPLAINT_FILE, lines, StandardCharsets.UTF_8);

            lines = new ArrayList<>();
            for (Account a : accounts) {
                lines.add(a.toLine());
            }
            Files.write(ACCOUNT_FILE, lines, StandardCharsets.UTF_8);

            lines = new ArrayList<>();
            for (StudentGroup g : groups) {
                lines.add(g.toLine());
            }
            Files.write(GROUP_FILE, lines, StandardCharsets.UTF_8);

            lines = new ArrayList<>();
            for (Category c : categories) {
                lines.add(c.toLine());
            }
            Files.write(CATEGORY_FILE, lines, StandardCharsets.UTF_8);
        } catch (IOException e) {
            System.out.println("Could not save data: " + e.getMessage());
        }
    }

    // ----- search helpers -----

    public Account findAccount(String username) {
        for (Account a : accounts) {
            if (a.getUsername().equalsIgnoreCase(username)) {
                return a;
            }
        }
        return null;
    }

    public StudentGroup findGroup(String name) {
        for (StudentGroup g : groups) {
            if (g.getName().equalsIgnoreCase(name)) {
                return g;
            }
        }
        return null;
    }

    public Category findCategory(String name) {
        for (Category c : categories) {
            if (c.getName().equalsIgnoreCase(name)) {
                return c;
            }
        }
        return null;
    }

    public Complaint findComplaint(int id) {
        for (Complaint c : complaints) {
            if (c.getId() == id) {
                return c;
            }
        }
        return null;
    }

    public int nextComplaintId() {
        int max = 1000;   // ticket numbers start from 1001
        for (Complaint c : complaints) {
            max = Math.max(max, c.getId());
        }
        return max + 1;
    }

    // ----- first run: load the real student and complaint lists from data/import -----

    // splits one CSV line (handles "quoted, text" and "" inside quotes)
    private static List<String> csvFields(String line) {
        List<String> out = new ArrayList<>();
        StringBuilder cur = new StringBuilder();
        boolean quoted = false;
        for (int i = 0; i < line.length(); i++) {
            char ch = line.charAt(i);
            if (quoted) {
                if (ch == '"' && i + 1 < line.length() && line.charAt(i + 1) == '"') {
                    cur.append('"');
                    i++;
                } else if (ch == '"') {
                    quoted = false;
                } else {
                    cur.append(ch);
                }
            } else if (ch == '"') {
                quoted = true;
            } else if (ch == ',') {
                out.add(cur.toString().trim());
                cur.setLength(0);
            } else {
                cur.append(ch);
            }
        }
        out.add(cur.toString().trim());
        return out;
    }

    private List<List<String>> readCsv(Path file) {
        List<List<String>> rows = new ArrayList<>();
        List<String> lines = readLines(file);
        for (int i = 1; i < lines.size(); i++) {      // line 0 is the heading
            if (!lines.get(i).trim().isEmpty()) {
                rows.add(csvFields(lines.get(i)));
            }
        }
        return rows;
    }

    // Section sizes are the number of students listed for each section in students.csv,
    // so the percentages always match the real student list.
    private void importFromCsv() {
        accounts.add(new Account("admin", PasswordUtil.hash("admin", "admin123"), "College Admin", "ADMIN", "-"));

        Map<String, Integer> sizes = new LinkedHashMap<>();
        for (List<String> r : readCsv(STUDENT_CSV)) {
            String username = r.get(0).toLowerCase();
            String section = r.get(2).toUpperCase();
            accounts.add(new Account(username, PasswordUtil.hash(username, "pass123"), r.get(1), "USER", section));
            sizes.put(section, sizes.getOrDefault(section, 0) + 1);
        }
        for (Map.Entry<String, Integer> e : sizes.entrySet()) {
            groups.add(new StudentGroup(e.getKey(), e.getValue()));
        }

        for (List<String> r : readCsv(COMPLAINT_CSV)) {
            Complaint c = new Complaint();
            c.setId(Integer.parseInt(r.get(0)));
            c.setTitle(r.get(1));
            c.setDescription(r.get(2));
            c.setCategory(r.get(3));
            c.setWholeCollege(false);
            c.setGroupName(r.get(4).toUpperCase());
            c.setReportedBy(r.get(5).toLowerCase());
            c.setUrgency(Integer.parseInt(r.get(6)));
            c.setSeverity(Integer.parseInt(r.get(7)));
            c.setSafetyCritical(Boolean.parseBoolean(r.get(8)));
            c.setStatus(r.get(9));
            c.setAssignedTo(r.get(10));
            c.setNote(r.get(11));
            c.setCreatedOn(LocalDate.now().minusDays(Integer.parseInt(r.get(12))));
            if (!r.get(13).isEmpty()) {
                for (String who : r.get(13).split(";")) {
                    c.getSupporters().add(who.trim().toLowerCase());
                }
            }
            complaints.add(c);
        }
    }

    private void createDefaultCategories() {
        categories.add(new Category("Electrical", "fan,light,power,switch,socket,bulb,projector,ac"));
        categories.add(new Category("IT / Network", "wifi,wi-fi,internet,network,login,computer,server,software"));
        categories.add(new Category("Facilities", "bench,door,wall,furniture,window,roof,desk,water,leak,tap,washroom,clean,printer,canteen,parking"));
        categories.add(new Category("Safety", "wire,fire,exit,danger,spark,unsafe"));
        categories.add(new Category("Academic", "lab,equipment,board,screen,exam,class,timetable,book,marker"));
        categories.add(new Category("Administrative", "id card,document,form,certificate,fees,scholarship,hall ticket"));
        categories.add(new Category("Other", ""));
    }
}
