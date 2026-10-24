package com.resolvex.web;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import com.resolvex.model.Account;
import com.resolvex.model.AuditLog;
import com.resolvex.model.Category;
import com.resolvex.model.Complaint;
import com.resolvex.model.StudentGroup;
import com.resolvex.service.ComplaintManager;
import com.resolvex.service.DataStore;
import com.resolvex.service.PriorityCalculator;
import com.resolvex.util.PasswordUtil;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;

/** The ADMIN dashboard (web pages). Started from AdminMain. */
public class AdminServer {

    private ComplaintManager manager = new ComplaintManager();
    private Session session = new Session("RESOLVEX_ADMIN");

    // side menu: label and link
    private String[][] menu() {
        List<String[]> items = new ArrayList<>();
        items.add(new String[] {"Sections Overview", "/admin"});
        items.add(new String[] {"SECTIONS", ""});
        for (StudentGroup g : manager.getStore().getGroups()) {
            items.add(new String[] {Web.esc(g.getName()), sectionLink(g.getName())});
        }
        items.add(new String[] {"MANAGEMENT", ""});
        items.add(new String[] {"Priority Ranking", "/admin/ranking"});
        items.add(new String[] {"Statistics", "/admin/stats"});
        items.add(new String[] {"All Students", "/admin/students-manage"});
        items.add(new String[] {"Groups &amp; Quotas", "/admin/groups"});
        items.add(new String[] {"Categories", "/admin/categories"});
        items.add(new String[] {"Audit History", "/admin/history"});
        return items.toArray(new String[0][]);
    }

    private String sectionLink(String name) {
        return "/admin/section?name=" + Web.encode(name);
    }

    // ---------------------------------------------------------------
    // start the server
    // ---------------------------------------------------------------
    public void start(int port) throws IOException {
        HttpServer server = HttpServer.create(new InetSocketAddress(port), 0);
        server.createContext("/", ex -> {
            try {
                route(ex);
            } catch (Exception e) {
                e.printStackTrace();
                Web.send(ex, 500, "text/plain; charset=UTF-8", ("Something went wrong: " + e).getBytes());
            }
        });
        server.setExecutor(null);
        server.start();
        System.out.println("ResolveX ADMIN is running at http://localhost:" + port);
    }

    // ---------------------------------------------------------------
    // decide which page to show
    // ---------------------------------------------------------------
    private void route(HttpExchange ex) throws IOException {
        String path = ex.getRequestURI().getPath();
        String method = ex.getRequestMethod();

        if (path.equals("/style.css")) {
            Web.css(ex);
            return;
        }
        manager.reload();

        // login and logout
        if (path.equals("/login")) {
            if (method.equals("POST")) {
                login(ex);
            } else {
                Web.html(ex, Layout.loginPage("Admin Portal", ""));
            }
            return;
        }
        if (path.equals("/logout")) {
            session.logout(ex);
            Web.redirect(ex, "/login");
            return;
        }

        // all other pages need a logged in admin
        if (session.current(ex) == null) {
            Web.redirect(ex, "/login");
            return;
        }

        if (method.equals("POST")) {
            handlePost(ex, path, Web.form(ex));
            return;
        }

        Map<String, String> q = Web.query(ex);
        String msg = q.get("msg");
        switch (path) {
            case "/":
                Web.redirect(ex, "/admin");
                break;
            case "/admin":
                show(ex, "/admin", overviewPage(), msg);
                break;
            case "/admin/students": {
                StudentGroup g = manager.getStore().findGroup(q.getOrDefault("section", ""));
                if (g == null) {
                    Web.redirect(ex, "/admin");
                } else {
                    show(ex, sectionLink(g.getName()), studentsPage(g.getName()), msg);
                }
                break;
            }
            case "/admin/students-manage":
                show(ex, "/admin/students-manage", allStudentsPage(q.get("section")), msg);
                break;
            case "/admin/history":
                show(ex, "/admin/history", historyPage(), msg);
                break;
            case "/admin/section": {
                StudentGroup g = manager.getStore().findGroup(q.getOrDefault("name", ""));
                if (g == null) {
                    Web.redirect(ex, "/admin");
                } else {
                    show(ex, sectionLink(g.getName()), complaintsPage(g.getName()), msg);
                }
                break;
            }
            case "/admin/complaint": {
                Complaint cc = manager.findComplaint(Web.number(q.get("id"), 0));
                String where = cc == null ? "/admin" : sectionLink(cc.getGroupName());
                show(ex, where, detailsPage(Web.number(q.get("id"), 0)), msg);
                break;
            }
            case "/admin/ranking":
                show(ex, "/admin/ranking", rankingPage(sectionOrFirst(q.get("section"))), msg);
                break;
            case "/admin/stats":
                show(ex, "/admin/stats", statsPage(sectionOrFirst(q.get("section"))), msg);
                break;
            case "/admin/groups":
                show(ex, "/admin/groups", groupsPage(), msg);
                break;
            case "/admin/categories":
                show(ex, "/admin/categories", categoriesPage(), msg);
                break;
            default:
                show(ex, "", "<h2>Page not found</h2>", null);
        }
    }

    private void show(HttpExchange ex, String active, String content, String msg) throws IOException {
        Web.html(ex, Layout.page("ADMIN PANEL", "Admin Dashboard", menu(), active, content, msg));
    }

    private void login(HttpExchange ex) throws IOException {
        Map<String, String> f = Web.form(ex);
        String username = f.getOrDefault("username", "").trim();
        String password = f.getOrDefault("password", "");
        Account a = manager.getStore().findAccount(username);

        if (a == null || !a.getPasswordHash().equals(PasswordUtil.hash(a.getUsername(), password))) {
            Web.html(ex, Layout.loginPage("Admin Portal", "Invalid administrator credentials."));
        } else if (!a.getRole().equals("ADMIN")) {
            Web.html(ex, Layout.loginPage("Admin Portal", "Access denied. Student accounts must use User Portal."));
        } else {
            session.login(ex, a);
            Web.redirect(ex, "/admin");
        }
    }

    private String sectionOrFirst(String name) {
        StudentGroup g = name == null ? null : manager.getStore().findGroup(name);
        if (g == null && !manager.getStore().getGroups().isEmpty()) {
            g = manager.getStore().getGroups().get(0);
        }
        return g == null ? null : g.getName();
    }

    private String reporter(String username) {
        Account a = manager.getStore().findAccount(username);
        if (a == null) {
            return Web.esc(username);
        }
        return Web.esc(a.getFullName()) + " <span class=\"muted\">(" + Web.esc(a.getUsername()) + ")</span>";
    }

    // ---------------------------------------------------------------
    // HOME : Sections Overview
    // ---------------------------------------------------------------
    private String overviewPage() {
        PriorityCalculator calc = manager.getCalculator();
        StringBuilder sb = new StringBuilder();
        sb.append("<div class=\"page-head\"><div><h2>Sections Overview</h2>\n");
        sb.append("<p class=\"muted\">Live health metrics and student support percentages across college departments.</p></div></div>\n");
        
        sb.append("<div class=\"panel\">\n<table>\n");
        sb.append("<thead><tr><th>Section</th><th>Enrolled</th><th>Complaints</th><th>Open</th><th>In Progress</th>")
          .append("<th>Resolved</th><th>Critical</th><th>Avg Support</th><th>Action</th></tr></thead><tbody>\n");
        for (StudentGroup g : manager.getStore().getGroups()) {
            int total = 0, open = 0, progress = 0, done = 0, critical = 0;
            double supportSum = 0;
            for (Complaint c : manager.getAllComplaints()) {
                if (!c.getGroupName().equalsIgnoreCase(g.getName())) {
                    continue;
                }
                total++;
                supportSum += calc.supportPercent(c);
                if (c.getStatus().equals("In Progress")) {
                    progress++;
                } else if (c.isOpen()) {
                    open++;
                }
                if (c.getStatus().equals("Resolved") || c.getStatus().equals("Closed")) {
                    done++;
                }
                if (c.isSafetyCritical() && c.isOpen()) {
                    critical++;
                }
            }
            double avg = total == 0 ? 0 : Math.round(supportSum / total * 10.0) / 10.0;
            sb.append("<tr><td class=\"bold\"><a href=\"").append(sectionLink(g.getName())).append("\">")
              .append(Web.esc(g.getName())).append("</a></td><td>").append(g.getTotalPeople())
              .append("</td><td>").append(total).append("</td><td>").append(open).append("</td><td>").append(progress)
              .append("</td><td>").append(done).append("</td><td>")
              .append(critical > 0 ? "<span class=\"badge critical\">" + critical + " Critical</span>" : "0")
              .append("</td><td>").append(avg).append("%</td><td>")
              .append("<a class=\"button small light\" href=\"").append(sectionLink(g.getName())).append("\">Open Board</a>")
              .append("</td></tr>\n");
        }
        sb.append("</tbody></table>\n</div>\n");
        return sb.toString();
    }

    // ---------------------------------------------------------------
    // Complaints per section
    // ---------------------------------------------------------------
    private String complaintsPage(String section) {
        PriorityCalculator calc = manager.getCalculator();
        List<Complaint> all = new ArrayList<>();
        int open = 0, critical = 0, resolved = 0;
        for (Complaint c : manager.getAllComplaints()) {
            if (c.isForGroup(section)) {
                all.add(c);
                if (c.isOpen()) open++;
                if (c.isSafetyCritical() && c.isOpen()) critical++;
                if (c.getStatus().equals("Resolved") || c.getStatus().equals("Closed")) resolved++;
            }
        }

        StringBuilder sb = new StringBuilder();
        sb.append("<div class=\"page-head\"><div><h2>Section: ").append(Web.esc(section)).append("</h2>\n");
        sb.append("<p class=\"muted\">Active tickets and prioritized feedback for ").append(Web.esc(section)).append(".</p></div>\n");
        sb.append("<div><a class=\"button small light\" href=\"/admin/students-manage?section=").append(Web.encode(section)).append("\">Manage Students</a></div></div>\n");

        sb.append("<div class=\"cards\">\n");
        sb.append(Layout.card(all.size(), "Total Tickets"));
        sb.append(Layout.card(open, "Open Pending"));
        sb.append(Layout.card(critical, "Safety Critical"));
        sb.append(Layout.card(resolved, "Resolved Issues"));
        sb.append("</div>\n");

        sb.append("<div class=\"panel\">\n<div class=\"panel-title\"><span>Section Complaints</span></div>\n<table>\n");
        sb.append("<thead><tr><th>ID</th><th>Title</th><th>Reporter</th><th>Category</th><th>Votes</th><th>Support %</th>")
          .append("<th>Priority</th><th>Status</th><th>Assigned</th><th>Action</th></tr></thead><tbody>\n");
        for (Complaint c : all) {
            String assigned = c.getAssignedTo().isEmpty() ? "<span class=\"muted\">Unassigned</span>" : Web.esc(c.getAssignedTo());
            sb.append("<tr>");
            sb.append("<td><span class=\"muted\">#</span>").append(c.getId()).append("</td>");
            sb.append("<td class=\"bold\"><a href=\"/admin/complaint?id=").append(c.getId()).append("\">")
              .append(Web.esc(c.getTitle())).append("</a></td>");
            sb.append("<td>").append(reporter(c.getReportedBy())).append("</td>");
            sb.append("<td><span class=\"badge grey\">").append(Web.esc(c.getCategory())).append("</span></td>");
            sb.append("<td>").append(c.getSupporters().size()).append(" / ").append(calc.eligiblePeople(c)).append("</td>");
            sb.append("<td>").append(calc.supportPercent(c)).append("%</td>");
            sb.append("<td>").append(Layout.scoreBar(calc.score(c), c.isSafetyCritical())).append("</td>");
            sb.append("<td>").append(Layout.badge(c)).append("</td>");
            sb.append("<td>").append(assigned).append("</td>");
            sb.append("<td><a class=\"button small light\" href=\"/admin/complaint?id=").append(c.getId()).append("\">Review &amp; Votes</a></td>");
            sb.append("</tr>\n");
        }
        sb.append("</tbody></table>\n</div>");
        return sb.toString();
    }

    // ---------------------------------------------------------------
    // Details of one complaint + SEE VOTES list + Actions
    // ---------------------------------------------------------------
    private String detailsPage(int id) {
        Complaint c = manager.findComplaint(id);
        if (c == null) {
            return "<h2>Complaint not found</h2><p><a href=\"/admin\">Back to Overview</a></p>";
        }
        PriorityCalculator calc = manager.getCalculator();
        DataStore store = manager.getStore();

        StringBuilder sb = new StringBuilder();
        sb.append("<div class=\"page-head\"><div><h2>Complaint #").append(c.getId()).append("</h2>\n");
        sb.append("<p class=\"muted\"><a href=\"").append(sectionLink(c.getGroupName())).append("\">&larr; Back to ")
          .append(Web.esc(c.getGroupName())).append(" complaints</a></p></div></div>\n");

        // Primary details table
        sb.append("<div class=\"panel\">\n<div class=\"panel-title\">").append(Web.esc(c.getTitle())).append("</div>\n");
        sb.append("<table class=\"details\">\n");
        sb.append(row("Description", Web.esc(c.getDescription())));
        sb.append(row("Category", Web.esc(c.getCategory())));
        sb.append(row("Scope / Section", Layout.affects(c, calc)));
        sb.append(row("Reported by", reporter(c.getReportedBy()) + " on " + c.getCreatedOn()));
        
        // "See Votes" detail section
        StringBuilder votesHtml = new StringBuilder();
        votesHtml.append("<strong>").append(c.getSupporters().size()).append(" votes</strong> (")
                 .append(calc.supportPercent(c)).append("% of section)\n");
        
        if (c.getSupporters().isEmpty()) {
            votesHtml.append("<div class=\"muted\" style=\"margin-top:6px;\">No student votes recorded yet.</div>");
        } else {
            votesHtml.append("<div class=\"voter-badge-list\">\n");
            for (String username : c.getSupporters()) {
                Account voter = store.findAccount(username);
                String name = voter != null ? voter.getFullName() : username;
                String sec = voter != null ? voter.getGroupName() : "";
                votesHtml.append("<span class=\"voter-chip\">")
                         .append("<strong>").append(Web.esc(name)).append("</strong> ")
                         .append("<span class=\"muted\">(").append(Web.esc(username)).append(sec.isEmpty() ? "" : " &bull; " + Web.esc(sec)).append(")</span>")
                         .append("</span>\n");
            }
            votesHtml.append("</div>\n");
        }
        sb.append(row("Support &amp; Student Votes", votesHtml.toString()));

        sb.append(row("Urgency / Severity", "Urgency: " + c.getUrgency() + " / 5 &nbsp;|&nbsp; Severity: " + c.getSeverity() + " / 5"));
        sb.append(row("Priority score", Layout.scoreBar(calc.score(c), c.isSafetyCritical())));
        sb.append(row("Ranking Rationale", Web.esc(calc.explain(c))));
        sb.append(row("Current Status", Layout.badge(c) + " &nbsp; <span class=\"muted\">" + Web.esc(c.getStatus()) + "</span>"));
        sb.append(row("Assigned to", c.getAssignedTo().isEmpty() ? "<span class=\"muted\">Unassigned</span>" : Web.esc(c.getAssignedTo())));
        sb.append(row("Latest note", c.getNote().isEmpty() ? "<span class=\"muted\">No note added</span>" : Web.esc(c.getNote())));
        sb.append("</table>\n</div>\n");

        // Action grid
        sb.append("<div class=\"grid\">\n");

        // 1. severity
        sb.append("<div class=\"panel\"><div class=\"panel-title\">Set Severity &amp; Safety</div><div class=\"panel-body\">\n");
        sb.append("<form method=\"post\" action=\"/admin/severity\">\n");
        sb.append("<input type=\"hidden\" name=\"id\" value=\"").append(c.getId()).append("\">\n");
        sb.append("<label>Severity (3 Levels: Low, Medium, High)</label>\n<select name=\"severity\">")
          .append(severityOptions(c.getSeverity())).append("</select>\n");
        sb.append("<label style=\"margin-top:14px;\"><input class=\"check\" type=\"checkbox\" name=\"critical\" value=\"yes\"")
          .append(c.isSafetyCritical() ? " checked" : "").append("> Mark as Safety Critical</label>\n");
        sb.append("<button type=\"submit\">Save Severity</button>\n</form>\n</div></div>\n");

        // 2. assign
        sb.append("<div class=\"panel\"><div class=\"panel-title\">Assign Responsibility</div><div class=\"panel-body\">\n");
        sb.append("<form method=\"post\" action=\"/admin/assign\">\n");
        sb.append("<input type=\"hidden\" name=\"id\" value=\"").append(c.getId()).append("\">\n");
        sb.append("<label>Assignee (Team or Staff)</label>\n<input type=\"text\" name=\"assignedTo\" value=\"")
          .append(Web.esc(c.getAssignedTo())).append("\" placeholder=\"e.g. IT Department or Facility Lead\" required>\n");
        sb.append("<button type=\"submit\">Assign Ticket</button>\n</form>\n</div></div>\n");

        // 3. status
        sb.append("<div class=\"panel\"><div class=\"panel-title\">Update Status &amp; Reason</div><div class=\"panel-body\">\n");
        sb.append("<form method=\"post\" action=\"/admin/status\">\n");
        sb.append("<input type=\"hidden\" name=\"id\" value=\"").append(c.getId()).append("\">\n");
        sb.append("<label>New Status</label>\n<select name=\"status\">")
          .append(options(ComplaintManager.STATUSES, c.getStatus())).append("</select>\n");
        sb.append("<label>Status Note / Reason</label>\n<input type=\"text\" name=\"note\" placeholder=\"Reason for status change\" required>\n");
        sb.append("<button type=\"submit\">Update Status</button>\n</form>\n</div></div>\n");

        // 4. category
        List<String> names = new ArrayList<>();
        for (Category cat : store.getCategories()) {
            names.add(cat.getName());
        }
        sb.append("<div class=\"panel\"><div class=\"panel-title\">Reclassify Category</div><div class=\"panel-body\">\n");
        sb.append("<form method=\"post\" action=\"/admin/category\">\n");
        sb.append("<input type=\"hidden\" name=\"id\" value=\"").append(c.getId()).append("\">\n");
        sb.append("<label>Category</label>\n<select name=\"category\">")
          .append(options(names.toArray(new String[0]), c.getCategory())).append("</select>\n");
        sb.append("<button type=\"submit\">Change Category</button>\n</form>\n</div></div>\n");

        sb.append("</div>");
        return sb.toString();
    }

    private String row(String label, String value) {
        return "<tr><td>" + label + "</td><td>" + value + "</td></tr>\n";
    }

    // ---------------------------------------------------------------
    // PAGE: ALL STUDENTS DIRECTORY & EDITING
    // ---------------------------------------------------------------
    private String allStudentsPage(String filterSection) {
        DataStore store = manager.getStore();
        List<Account> students = new ArrayList<>();
        for (Account a : store.getAccounts()) {
            if ("USER".equalsIgnoreCase(a.getRole())) {
                if (filterSection == null || filterSection.trim().isEmpty() || a.getGroupName().equalsIgnoreCase(filterSection)) {
                    students.add(a);
                }
            }
        }

        StringBuilder sb = new StringBuilder();
        sb.append("<div class=\"page-head\"><div><h2>Student Directory</h2>\n");
        sb.append("<p class=\"muted\">Manage all enrolled student accounts, update student names, sections, and access passwords.</p></div>\n");
        sb.append("<div><a class=\"button small light\" href=\"/admin/groups\">Add Students</a></div></div>\n");

        // Section filter
        sb.append("<div class=\"filter\">Filter by Section: ");
        sb.append("<a class=\"chip").append(filterSection == null || filterSection.isEmpty() ? " active" : "")
          .append("\" href=\"/admin/students-manage\">All (").append(store.getAccounts().size() - 1).append(")</a>");
        for (StudentGroup g : store.getGroups()) {
            boolean active = g.getName().equalsIgnoreCase(filterSection);
            sb.append("<a class=\"chip").append(active ? " active" : "").append("\" href=\"/admin/students-manage?section=")
              .append(Web.encode(g.getName())).append("\">").append(Web.esc(g.getName())).append("</a>");
        }
        sb.append("</div>\n");

        // Students Table
        sb.append("<div class=\"panel\">\n<div class=\"panel-title\">Registered Students (").append(students.size()).append(")</div>\n");
        sb.append("<table>\n<thead><tr><th>Username</th><th>Full Name</th><th>Section</th><th>Edit Name &amp; Section</th></tr></thead><tbody>\n");

        List<String> groupNames = new ArrayList<>();
        for (StudentGroup g : store.getGroups()) {
            groupNames.add(g.getName());
        }

        for (Account acc : students) {
            sb.append("<tr>");
            sb.append("<td style=\"font-family:'JetBrains Mono',monospace;\"><strong>").append(Web.esc(acc.getUsername())).append("</strong></td>");
            sb.append("<td>").append(Web.esc(acc.getFullName())).append("</td>");
            sb.append("<td><span class=\"badge grey\">").append(Web.esc(acc.getGroupName())).append("</span></td>");
            sb.append("<td>\n");
            // Inline update form
            sb.append("<form method=\"post\" action=\"/admin/student-edit\" style=\"display:flex; gap:8px; align-items:center;\">\n");
            sb.append("<input type=\"hidden\" name=\"username\" value=\"").append(Web.esc(acc.getUsername())).append("\">\n");
            sb.append("<input type=\"hidden\" name=\"redirectSection\" value=\"").append(Web.esc(filterSection == null ? "" : filterSection)).append("\">\n");
            sb.append("<input type=\"text\" name=\"fullName\" value=\"").append(Web.esc(acc.getFullName())).append("\" placeholder=\"Full Name\" style=\"width:160px; padding:5px 8px; font-size:12.5px;\" required>\n");
            sb.append("<select name=\"group\" style=\"width:100px; padding:5px 8px; font-size:12.5px;\">")
              .append(options(groupNames.toArray(new String[0]), acc.getGroupName())).append("</select>\n");
            sb.append("<input type=\"password\" name=\"newPassword\" placeholder=\"New Pass (opt)\" style=\"width:120px; padding:5px 8px; font-size:12.5px;\">\n");
            sb.append("<button type=\"submit\" class=\"small\">Save</button>\n");
            sb.append("</form>\n");
            sb.append("</td>");
            sb.append("</tr>\n");
        }
        sb.append("</tbody></table>\n</div>\n");
        return sb.toString();
    }

    private String studentsPage(String section) {
        return allStudentsPage(section);
    }

    // ---------------------------------------------------------------
    // PAGE: AUDIT HISTORY LOG
    // ---------------------------------------------------------------
    private String historyPage() {
        List<AuditLog> logs = manager.getStore().getAuditLogs();
        StringBuilder sb = new StringBuilder();
        sb.append("<div class=\"page-head\"><div><h2>Audit History Log</h2>\n");
        sb.append("<p class=\"muted\">System audit trail recording status changes, student votes, assignments, and account modifications.</p></div></div>\n");

        sb.append("<div class=\"panel\">\n<div class=\"panel-title\">Recent Activity Events (").append(logs.size()).append(")</div>\n");
        sb.append("<table>\n<thead><tr><th>Timestamp</th><th>Actor</th><th>Action</th><th>Ticket</th><th>Details</th></tr></thead><tbody>\n");
        if (logs.isEmpty()) {
            sb.append("<tr><td colspan=\"5\" style=\"text-align:center; padding:24px;\" class=\"muted\">No activity records logged yet.</td></tr>\n");
        } else {
            for (AuditLog l : logs) {
                String actionClass = "log-action " + l.getActionType();
                String ticketLink = l.getComplaintId() > 0 
                    ? "<a href=\"/admin/complaint?id=" + l.getComplaintId() + "\">#" + l.getComplaintId() + "</a>" 
                    : "<span class=\"muted\">-</span>";
                
                sb.append("<tr>");
                sb.append("<td style=\"font-family:'JetBrains Mono',monospace; font-size:12px; color:#a1a1aa;\">")
                  .append(Web.esc(l.getTimestamp())).append("</td>");
                sb.append("<td class=\"bold\">").append(Web.esc(l.getUsername())).append("</td>");
                sb.append("<td><span class=\"").append(actionClass).append("\">").append(Web.esc(l.getActionType())).append("</span></td>");
                sb.append("<td>").append(ticketLink).append("</td>");
                sb.append("<td>").append(Web.esc(l.getDetails())).append("</td>");
                sb.append("</tr>\n");
            }
        }
        sb.append("</tbody></table>\n</div>\n");
        return sb.toString();
    }

    // ---------------------------------------------------------------
    // Priority Ranking
    // ---------------------------------------------------------------
    private String rankingPage(String section) {
        PriorityCalculator calc = manager.getCalculator();
        StringBuilder sb = new StringBuilder();
        sb.append("<div class=\"page-head\"><div><h2>Priority Ranking Engine</h2>\n");
        sb.append("<p class=\"muted\">Auto-prioritized tickets ordered by student consensus, urgency, severity, and waiting time.</p></div></div>\n");
        sb.append(filterBar("/admin/ranking", section));
        sb.append("<div class=\"panel\">\n<table>\n");
        sb.append("<thead><tr><th>Rank</th><th>ID</th><th>Title</th><th>Score</th><th>Priority Rationale</th></tr></thead><tbody>\n");
        int rank = 1;
        for (Complaint c : manager.rankedList()) {
            if (section != null && !c.isForGroup(section)) {
                continue;
            }
            sb.append("<tr>");
            sb.append("<td class=\"bold\">#").append(rank++).append("</td>");
            sb.append("<td><span class=\"muted\">#</span>").append(c.getId()).append("</td>");
            sb.append("<td><a class=\"bold\" href=\"/admin/complaint?id=").append(c.getId()).append("\">")
              .append(Web.esc(c.getTitle())).append("</a></td>");
            sb.append("<td>").append(Layout.scoreBar(calc.score(c), c.isSafetyCritical())).append("</td>");
            sb.append("<td>").append(Web.esc(calc.explain(c))).append("</td>");
            sb.append("</tr>\n");
        }
        sb.append("</tbody></table>\n</div>\n");

        sb.append("<div class=\"panel\">\n<div class=\"panel-title\">Ranking Mathematical Model</div>\n<div class=\"panel-body\">\n");
        sb.append("<strong>Priority Formula:</strong> <code>Score = 0.35 &times; Severity + 0.35 &times; Support % + 0.20 &times; Urgency + 0.10 &times; Age</code><br><br>\n");
        sb.append("Support percentage is normalized against the specific cohort size (e.g. CSE: 50, AIDS: 40, AIML: 10), preventing small sections from being drowned out by larger departments. Safety-critical flags automatically receive top priority.\n");
        sb.append("</div>\n</div>");
        return sb.toString();
    }

    // ---------------------------------------------------------------
    // Statistics
    // ---------------------------------------------------------------
    private String statsPage(String section) {
        List<Complaint> all = new ArrayList<>();
        for (Complaint c : manager.getAllComplaints()) {
            if (section == null || c.isForGroup(section)) {
                all.add(c);
            }
        }
        int total = all.size();

        StringBuilder sb = new StringBuilder();
        sb.append("<div class=\"page-head\"><div><h2>Analytical Statistics</h2>\n");
        sb.append("<p class=\"muted\">Total complaints analyzed: ").append(total).append("</p></div></div>\n");
        sb.append(filterBar("/admin/stats", section));

        sb.append("<div class=\"grid\">\n");

        // by status
        sb.append("<div class=\"panel\"><div class=\"panel-title\">Breakdown by Status</div>\n<table>\n");
        sb.append("<thead><tr><th>Status</th><th>Count</th><th>Distribution</th></tr></thead><tbody>\n");
        for (String status : ComplaintManager.STATUSES) {
            int count = 0;
            for (Complaint c : all) {
                if (c.getStatus().equals(status)) {
                    count++;
                }
            }
            sb.append(statRow(status, count, total));
        }
        sb.append("</tbody></table>\n</div>\n");

        // by category
        sb.append("<div class=\"panel\"><div class=\"panel-title\">Breakdown by Category</div>\n<table>\n");
        sb.append("<thead><tr><th>Category</th><th>Count</th><th>Distribution</th></tr></thead><tbody>\n");
        for (Category cat : manager.getStore().getCategories()) {
            int count = 0;
            for (Complaint c : all) {
                if (c.getCategory().equalsIgnoreCase(cat.getName())) {
                    count++;
                }
            }
            sb.append(statRow(cat.getName(), count, total));
        }
        sb.append("</tbody></table>\n</div>\n");

        sb.append("</div>");
        return sb.toString();
    }

    private String filterBar(String page, String section) {
        StringBuilder sb = new StringBuilder("<div class=\"filter\">Section: ");
        for (StudentGroup g : manager.getStore().getGroups()) {
            boolean on = g.getName().equalsIgnoreCase(section);
            sb.append("<a class=\"chip").append(on ? " active" : "").append("\" href=\"").append(page)
              .append("?section=").append(Web.encode(g.getName())).append("\">").append(Web.esc(g.getName())).append("</a>");
        }
        return sb.append("</div>\n").toString();
    }

    private String statRow(String name, int count, int total) {
        int percent = total == 0 ? 0 : count * 100 / total;
        return "<tr><td>" + Web.esc(name) + "</td><td>" + count + "</td><td><span class=\"bar\"><span style=\"width:"
                + percent + "%\"></span></span> &nbsp; <span class=\"muted\">" + percent + "%</span></td></tr>\n";
    }

    // ---------------------------------------------------------------
    // Groups & Quotas
    // ---------------------------------------------------------------
    private String groupsPage() {
        DataStore store = manager.getStore();
        List<String> names = new ArrayList<>();
        for (StudentGroup g : store.getGroups()) {
            names.add(g.getName());
        }
        String groupOptions = options(names.toArray(new String[0]), "");

        StringBuilder sb = new StringBuilder();
        sb.append("<div class=\"page-head\"><div><h2>Groups &amp; Quotas</h2>\n");
        sb.append("<p class=\"muted\">Define student cohorts and batch provision student login credentials.</p></div></div>\n");

        // groups table
        sb.append("<div class=\"panel\">\n<div class=\"panel-title\">Configured Groups</div>\n<table>\n");
        sb.append("<thead><tr><th>Group / Section</th><th>Cohort Population</th><th>Active Accounts</th></tr></thead><tbody>\n");
        for (StudentGroup g : store.getGroups()) {
            int accounts = 0;
            for (Account a : store.getAccounts()) {
                if (a.getGroupName().equalsIgnoreCase(g.getName())) {
                    accounts++;
                }
            }
            sb.append("<tr><td class=\"bold\">").append(Web.esc(g.getName())).append("</td><td>").append(g.getTotalPeople())
              .append("</td><td>").append(accounts).append("</td></tr>\n");
        }
        sb.append("</tbody></table>\n</div>\n");

        sb.append("<div class=\"grid\">\n");

        // add a group
        sb.append("<div class=\"panel\"><div class=\"panel-title\">Add New Group</div><div class=\"panel-body\">\n");
        sb.append("<form method=\"post\" action=\"/admin/group-add\">\n");
        sb.append("<label>Group / Section Name</label>\n<input type=\"text\" name=\"name\" placeholder=\"e.g. MECH\" required>\n");
        sb.append("<label>Cohort Size (People)</label>\n<input type=\"number\" name=\"people\" min=\"1\" required>\n");
        sb.append("<button type=\"submit\">Create Group</button>\n</form>\n</div></div>\n");

        // update number of people
        sb.append("<div class=\"panel\"><div class=\"panel-title\">Update Cohort Size</div><div class=\"panel-body\">\n");
        sb.append("<form method=\"post\" action=\"/admin/group-update\">\n");
        sb.append("<label>Group</label>\n<select name=\"name\">").append(groupOptions).append("</select>\n");
        sb.append("<label>New Size</label>\n<input type=\"number\" name=\"people\" min=\"1\" required>\n");
        sb.append("<button type=\"submit\">Save Quota</button>\n</form>\n</div></div>\n");

        // add one user
        sb.append("<div class=\"panel\"><div class=\"panel-title\">Add Single Student</div><div class=\"panel-body\">\n");
        sb.append("<form method=\"post\" action=\"/admin/user-add\">\n");
        sb.append("<label>Username</label>\n<input type=\"text\" name=\"username\" placeholder=\"e.g. cse99\" required>\n");
        sb.append("<label>Full Name</label>\n<input type=\"text\" name=\"fullName\" placeholder=\"e.g. John Doe\" required>\n");
        sb.append("<label>Password</label>\n<input type=\"text\" name=\"password\" placeholder=\"e.g. pass123\" required>\n");
        sb.append("<label>Section</label>\n<select name=\"group\">").append(groupOptions).append("</select>\n");
        sb.append("<button type=\"submit\">Add Student</button>\n</form>\n</div></div>\n");

        // add many users
        sb.append("<div class=\"panel\"><div class=\"panel-title\">Bulk Provision Students</div><div class=\"panel-body\">\n");
        sb.append("<form method=\"post\" action=\"/admin/user-many\">\n");
        sb.append("<label>Section</label>\n<select name=\"group\">").append(groupOptions).append("</select>\n");
        sb.append("<label>Student Count</label>\n<input type=\"number\" name=\"count\" min=\"1\" max=\"200\" value=\"10\" required>\n");
        sb.append("<label>Prefix</label>\n<input type=\"text\" name=\"prefix\" placeholder=\"e.g. cse (generates cse1, cse2...)\" required>\n");
        sb.append("<label>Default Password</label>\n<input type=\"text\" name=\"password\" value=\"pass123\" required>\n");
        sb.append("<button type=\"submit\">Bulk Generate Accounts</button>\n</form>\n</div></div>\n");

        sb.append("</div>");
        return sb.toString();
    }

    // ---------------------------------------------------------------
    // Categories
    // ---------------------------------------------------------------
    private String categoriesPage() {
        StringBuilder sb = new StringBuilder();
        sb.append("<div class=\"page-head\"><div><h2>Categories</h2>\n");
        sb.append("<p class=\"muted\">Smart keywords automatically classify student complaints into relevant departments.</p></div></div>\n");
        sb.append("<div class=\"panel\">\n<table>\n<thead><tr><th>Category</th><th>Keywords</th></tr></thead><tbody>\n");
        for (Category cat : manager.getStore().getCategories()) {
            sb.append("<tr><td class=\"bold\">").append(Web.esc(cat.getName())).append("</td><td>")
              .append(Web.esc(cat.getKeywords())).append("</td></tr>\n");
        }
        sb.append("</tbody></table>\n</div>\n");

        sb.append("<div class=\"panel\"><div class=\"panel-title\">Add Category</div><div class=\"panel-body\">\n");
        sb.append("<form method=\"post\" action=\"/admin/category-add\">\n");
        sb.append("<label>Category Name</label>\n<input type=\"text\" name=\"name\" placeholder=\"e.g. Cafeteria\" required>\n");
        sb.append("<label>Keywords (comma separated)</label>\n<input type=\"text\" name=\"keywords\" placeholder=\"e.g. food,canteen,hygiene,lunch\">\n");
        sb.append("<button type=\"submit\">Add Category</button>\n</form>\n</div></div>");
        return sb.toString();
    }

    // ---------------------------------------------------------------
    // Form Actions (POST)
    // ---------------------------------------------------------------
    private void handlePost(HttpExchange ex, String path, Map<String, String> f) throws IOException {
        DataStore store = manager.getStore();
        int id = Web.number(f.get("id"), 0);
        String back = "/admin/complaint?id=" + id;

        switch (path) {
            case "/admin/severity": {
                int severity = Math.max(1, Math.min(5, Web.number(f.get("severity"), 3)));
                boolean critical = "yes".equals(f.get("critical"));
                Web.redirect(ex, back, manager.setSeverity(id, severity, critical));
                break;
            }
            case "/admin/assign": {
                String name = Web.clean(f.get("assignedTo"));
                if (name.isEmpty()) {
                    Web.redirect(ex, back, "Please specify an assignee.");
                } else {
                    Web.redirect(ex, back, manager.assignComplaint(id, name));
                }
                break;
            }
            case "/admin/status": {
                String note = Web.clean(f.get("note"));
                if (note.isEmpty()) {
                    Web.redirect(ex, back, "A reason note is required to change status.");
                } else {
                    Web.redirect(ex, back, manager.changeStatus(id, f.get("status"), note));
                }
                break;
            }
            case "/admin/category":
                Web.redirect(ex, back, manager.changeCategory(id, Web.clean(f.get("category"))));
                break;

            case "/admin/student-edit": {
                String username = Web.clean(f.get("username"));
                String fullName = Web.clean(f.get("fullName"));
                String group = Web.clean(f.get("group"));
                String newPassword = Web.clean(f.get("newPassword"));
                String redirectSec = Web.clean(f.get("redirectSection"));

                String redirectUrl = "/admin/students-manage" + (redirectSec.isEmpty() ? "" : "?section=" + Web.encode(redirectSec));
                if (username.isEmpty() || fullName.isEmpty() || group.isEmpty()) {
                    Web.redirect(ex, redirectUrl, "All student fields must be filled.");
                } else {
                    String res = manager.updateAccount(username, fullName, group, newPassword);
                    Web.redirect(ex, redirectUrl, res);
                }
                break;
            }

            case "/admin/group-add": {
                String name = Web.clean(f.get("name"));
                int people = Web.number(f.get("people"), 0);
                if (name.isEmpty() || people < 1) {
                    Web.redirect(ex, "/admin/groups", "Enter a name and total cohort people.");
                } else if (store.findGroup(name) != null) {
                    Web.redirect(ex, "/admin/groups", "This group already exists.");
                } else {
                    store.getGroups().add(new StudentGroup(name, people));
                    store.addAuditLog("admin", "ACCOUNT_UPDATE", 0, "Created student group: " + name + " (" + people + " students)");
                    store.save();
                    Web.redirect(ex, "/admin/groups", "Group " + name + " created.");
                }
                break;
            }
            case "/admin/group-update": {
                StudentGroup g = store.findGroup(Web.clean(f.get("name")));
                int people = Web.number(f.get("people"), 0);
                if (g == null || people < 1) {
                    Web.redirect(ex, "/admin/groups", "Choose a group and enter cohort people count.");
                } else {
                    g.setTotalPeople(people);
                    store.addAuditLog("admin", "ACCOUNT_UPDATE", 0, "Updated group quota: " + g.getName() + " to " + people);
                    store.save();
                    Web.redirect(ex, "/admin/groups", "Group " + g.getName() + " updated.");
                }
                break;
            }
            case "/admin/user-add": {
                String username = Web.clean(f.get("username")).replace(" ", "");
                String fullName = Web.clean(f.get("fullName"));
                String password = Web.clean(f.get("password"));
                StudentGroup g = store.findGroup(Web.clean(f.get("group")));
                if (username.isEmpty() || fullName.isEmpty() || password.isEmpty() || g == null) {
                    Web.redirect(ex, "/admin/students-manage", "Fill all required fields.");
                } else if (store.findAccount(username) != null) {
                    Web.redirect(ex, "/admin/students-manage", "This username is already taken.");
                } else {
                    store.getAccounts().add(new Account(username, PasswordUtil.hash(username, password), fullName, "USER", g.getName()));
                    store.addAuditLog("admin", "ACCOUNT_UPDATE", 0, "Added student: " + username + " (" + fullName + ", " + g.getName() + ")");
                    store.save();
                    Web.redirect(ex, "/admin/students-manage", "Student " + username + " added successfully.");
                }
                break;
            }
            case "/admin/user-many": {
                StudentGroup g = store.findGroup(Web.clean(f.get("group")));
                int count = Web.number(f.get("count"), 0);
                String prefix = Web.clean(f.get("prefix")).replace(" ", "");
                String password = Web.clean(f.get("password"));
                if (g == null || count < 1 || count > 200 || prefix.isEmpty() || password.isEmpty()) {
                    Web.redirect(ex, "/admin/groups", "Fill all fields (limit 1 to 200).");
                } else {
                    int created = 0;
                    int n = 1;
                    while (created < count) {
                        String username = prefix + n;
                        if (store.findAccount(username) == null) {
                            store.getAccounts().add(new Account(username, PasswordUtil.hash(username, password),
                                    g.getName() + " Student " + n, "USER", g.getName()));
                            created++;
                        }
                        n++;
                    }
                    store.addAuditLog("admin", "ACCOUNT_UPDATE", 0, "Bulk created " + created + " students for " + g.getName());
                    store.save();
                    Web.redirect(ex, "/admin/students-manage", created + " accounts created for " + g.getName() + ".");
                }
                break;
            }
            case "/admin/category-add": {
                String name = Web.clean(f.get("name"));
                if (name.isEmpty()) {
                    Web.redirect(ex, "/admin/categories", "Category name required.");
                } else if (store.findCategory(name) != null) {
                    Web.redirect(ex, "/admin/categories", "Category already exists.");
                } else {
                    store.getCategories().add(new Category(name, Web.clean(f.get("keywords"))));
                    store.addAuditLog("admin", "ACCOUNT_UPDATE", 0, "Created category: " + name);
                    store.save();
                    Web.redirect(ex, "/admin/categories", "Category " + name + " created.");
                }
                break;
            }
            default:
                Web.redirect(ex, "/admin");
        }
    }

    private String options(String[] values, String selected) {
        StringBuilder sb = new StringBuilder();
        for (String v : values) {
            sb.append("<option").append(v.equalsIgnoreCase(selected) ? " selected" : "").append(">")
              .append(Web.esc(v)).append("</option>");
        }
        return sb.toString();
    }

    private String severityOptions(int selected) {
        StringBuilder sb = new StringBuilder();
        String[][] levels = {
            {"1", "1 - Low"},
            {"2", "2 - Medium"},
            {"3", "3 - High"}
        };
        for (String[] lvl : levels) {
            int val = Integer.parseInt(lvl[0]);
            sb.append("<option value=\"").append(val).append("\"")
              .append(val == selected || (selected > 3 && val == 3) ? " selected" : "")
              .append(">").append(lvl[1]).append("</option>");
        }
        return sb.toString();
    }

    private String numberOptions(int selected) {
        StringBuilder sb = new StringBuilder();
        for (int n = 1; n <= 5; n++) {
            sb.append("<option").append(n == selected ? " selected" : "").append(">").append(n).append("</option>");
        }
        return sb.toString();
    }
}
