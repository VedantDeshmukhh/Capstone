package com.resolvex.web;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import com.resolvex.model.Account;
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

    // side menu: label and link (a link that is empty means a small heading)
    // the sections come from the groups, so a new group appears here by itself
    private String[][] menu() {
        List<String[]> items = new ArrayList<>();
        items.add(new String[] {"Sections Overview", "/admin"});
        items.add(new String[] {"SECTIONS", ""});
        for (StudentGroup g : manager.getStore().getGroups()) {
            items.add(new String[] {Web.esc(g.getName()), sectionLink(g.getName())});
        }
        items.add(new String[] {"MANAGE", ""});
        items.add(new String[] {"Priority Ranking", "/admin/ranking"});
        items.add(new String[] {"Statistics", "/admin/stats"});
        items.add(new String[] {"Groups &amp; Users", "/admin/groups"});
        items.add(new String[] {"Categories", "/admin/categories"});
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
        server.setExecutor(null);   // one request at a time, keeps the code simple
        server.start();
        System.out.println("ResolveX ADMIN is running at http://localhost:" + port);
        Web.openBrowser("http://localhost:" + port);
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
        manager.reload();   // read the files again (the user program may have changed them)

        // login and logout
        if (path.equals("/login")) {
            if (method.equals("POST")) {
                login(ex);
            } else {
                Web.html(ex, Layout.loginPage("Admin Login", ""));
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

    // wrap the content in the common layout and send it
    private void show(HttpExchange ex, String active, String content, String msg) throws IOException {
        Web.html(ex, Layout.page("ADMIN MENU", "Admin Dashboard", menu(), active, content, msg));
    }

    // check username and password
    private void login(HttpExchange ex) throws IOException {
        Map<String, String> f = Web.form(ex);
        String username = f.getOrDefault("username", "").trim();
        String password = f.getOrDefault("password", "");
        Account a = manager.getStore().findAccount(username);

        if (a == null || !a.getPasswordHash().equals(PasswordUtil.hash(a.getUsername(), password))) {
            Web.html(ex, Layout.loginPage("Admin Login", "Wrong username or password."));
        } else if (!a.getRole().equals("ADMIN")) {
            Web.html(ex, Layout.loginPage("Admin Login", "This is not an admin account. Use the User dashboard."));
        } else {
            session.login(ex, a);
            Web.redirect(ex, "/admin");
        }
    }

    // ranking and statistics always show ONE section (the first one if none is chosen)
    private String sectionOrFirst(String name) {
        StudentGroup g = name == null ? null : manager.getStore().findGroup(name);
        if (g == null && !manager.getStore().getGroups().isEmpty()) {
            g = manager.getStore().getGroups().get(0);
        }
        return g == null ? null : g.getName();
    }

    // "Rahul Sharma (cse1)"
    private String reporter(String username) {
        Account a = manager.getStore().findAccount(username);
        if (a == null) {
            return Web.esc(username);
        }
        return Web.esc(a.getFullName()) + " <span class=\"muted\">(" + Web.esc(a.getUsername()) + ")</span>";
    }

    // ---------------------------------------------------------------
    // HOME : one line for every section (each section has its own dashboard)
    // ---------------------------------------------------------------
    private String overviewPage() {
        PriorityCalculator calc = manager.getCalculator();
        StringBuilder sb = new StringBuilder();
        sb.append("<h2>Sections Overview</h2>\n");
        sb.append("<p class=\"muted\">Every section has its own dashboard. All percentages are calculated out of the students of that section.</p>\n");
        sb.append("<div class=\"panel\">\n<table>\n");
        sb.append("<tr><th>Section</th><th>Students</th><th>Complaints</th><th>Open</th><th>In Progress</th>")
          .append("<th>Resolved / Closed</th><th>Safety Critical</th><th>Average support %</th><th></th></tr>\n");
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
            double avg = total == 0 ? 0 : Math.round(supportSum / total * 100.0) / 100.0;
            sb.append("<tr><td class=\"bold\">").append(Web.esc(g.getName())).append("</td><td>").append(g.getTotalPeople())
              .append("</td><td>").append(total).append("</td><td>").append(open).append("</td><td>").append(progress)
              .append("</td><td>").append(done).append("</td><td>").append(critical).append("</td><td>").append(avg)
              .append("%</td><td><a href=\"").append(sectionLink(g.getName())).append("\">Open dashboard</a></td></tr>\n");
        }
        sb.append("</table>\n</div>");
        return sb.toString();
    }

    // ---------------------------------------------------------------
    // every student of a section and the problem he / she reported
    // ---------------------------------------------------------------
    private String studentsPage(String section) {
        StringBuilder sb = new StringBuilder();
        sb.append("<h2>").append(Web.esc(section)).append(" - Student-wise Report</h2>\n");
        sb.append("<p class=\"muted\"><a href=\"").append(sectionLink(section)).append("\">Back to ").append(Web.esc(section))
          .append(" dashboard</a></p>\n");
        sb.append("<div class=\"panel\">\n<table>\n");
        sb.append("<tr><th>No.</th><th>Username</th><th>Student name</th><th>Reported problem</th><th>Category</th><th>Status</th><th>Reported on</th></tr>\n");
        int no = 1;
        for (Account a : manager.getStore().getAccounts()) {
            if (!a.getRole().equals("USER") || !a.getGroupName().equalsIgnoreCase(section)) {
                continue;
            }
            boolean any = false;
            for (Complaint c : manager.getAllComplaints()) {
                if (!c.getReportedBy().equalsIgnoreCase(a.getUsername())) {
                    continue;
                }
                any = true;
                sb.append("<tr><td>").append(no).append("</td><td>").append(Web.esc(a.getUsername())).append("</td><td>")
                  .append(Web.esc(a.getFullName())).append("</td><td><a href=\"/admin/complaint?id=").append(c.getId()).append("\">")
                  .append(Web.esc(c.getTitle())).append("</a></td><td>").append(Web.esc(c.getCategory())).append("</td><td>")
                  .append(Layout.badge(c)).append("</td><td>").append(c.getCreatedOn()).append("</td></tr>\n");
            }
            if (!any) {
                sb.append("<tr><td>").append(no).append("</td><td>").append(Web.esc(a.getUsername())).append("</td><td>")
                  .append(Web.esc(a.getFullName())).append("</td><td colspan=\"4\">No complaint reported</td></tr>\n");
            }
            no++;
        }
        sb.append("</table>\n</div>");
        return sb.toString();
    }

    // ---------------------------------------------------------------
    // PAGE 1 : all complaints
    // ---------------------------------------------------------------
    // section == null means the overall page (all sections together)
    private String complaintsPage(String section) {
        PriorityCalculator calc = manager.getCalculator();
        List<Complaint> all = new ArrayList<>();
        for (Complaint c : manager.getAllComplaints()) {
            if (section == null || c.isForGroup(section)) {
                all.add(c);
            }
        }
        all.sort((a, b) -> Double.compare(calc.rankValue(b), calc.rankValue(a)));   // highest score first

        // count for the summary boxes
        int open = 0, progress = 0, resolved = 0;
        for (Complaint c : all) {
            if (c.getStatus().equals("In Progress")) {
                progress++;
            } else if (c.isOpen()) {
                open++;   // open but not started yet
            }
            if (c.getStatus().equals("Resolved") || c.getStatus().equals("Closed")) {
                resolved++;
            }
        }

        StringBuilder sb = new StringBuilder();
        if (section == null) {
            sb.append("<h2>All Complaints</h2>\n");
            sb.append("<p class=\"muted\">Logged in as: admin | Total: ").append(all.size()).append(" complaints from all sections</p>\n");
        } else {
            sb.append("<h2>").append(Web.esc(section)).append(" Section</h2>\n");
            sb.append("<p class=\"muted\">Complaints reported by students of the ").append(Web.esc(section))
              .append(" section | Students: ").append(manager.getStore().findGroup(section).getTotalPeople())
              .append(" | Complaints: ").append(all.size())
              .append(" | <a href=\"/admin/students?section=").append(Web.encode(section)).append("\">Student-wise report</a></p>\n");
        }
        sb.append("<div class=\"cards\">\n");
        sb.append(Layout.card(all.size(), "Total"));
        sb.append(Layout.card(open, "Open"));
        sb.append(Layout.card(progress, "In Progress"));
        sb.append(Layout.card(resolved, "Resolved"));
        sb.append("</div>\n");

        // the table
        String tableTitle = section == null ? "Complaint Register" : section + " Complaint Register";
        sb.append("<div class=\"panel\">\n<div class=\"panel-title\">").append(Web.esc(tableTitle)).append("</div>\n<table>\n");
        sb.append("<tr><th>ID</th><th>Title</th><th>Reported By</th><th>Category</th><th>Votes</th><th>Support %</th>")
          .append("<th>Urgency</th><th>Severity</th><th>Score</th><th>Status</th><th>Assigned To</th></tr>\n");
        for (Complaint c : all) {
            String assigned = c.getAssignedTo().isEmpty() ? "-" : Web.esc(c.getAssignedTo());
            String titleClass = c.isSafetyCritical() ? " class=\"bold\"" : "";
            sb.append("<tr>");
            sb.append("<td>#").append(c.getId()).append("</td>");
            sb.append("<td").append(titleClass).append("><a href=\"/admin/complaint?id=").append(c.getId()).append("\">")
              .append(Web.esc(c.getTitle())).append("</a></td>");
            sb.append("<td>").append(reporter(c.getReportedBy())).append("</td>");
            sb.append("<td>").append(Web.esc(c.getCategory())).append("</td>");
            sb.append("<td>").append(Layout.votes(c, calc)).append("</td>");
            sb.append("<td>").append(calc.supportPercent(c)).append("%</td>");
            sb.append("<td>").append(c.getUrgency()).append("</td>");
            sb.append("<td>").append(c.getSeverity()).append("</td>");
            sb.append("<td>").append(Layout.scoreBar(calc.score(c), c.isSafetyCritical())).append("</td>");
            sb.append("<td>").append(Layout.badge(c)).append("</td>");
            sb.append("<td>").append(assigned).append("</td>");
            sb.append("</tr>\n");
        }
        sb.append("</table>\n</div>");
        return sb.toString();
    }

    // ---------------------------------------------------------------
    // PAGE 2 : details of one complaint + admin actions
    // ---------------------------------------------------------------
    private String detailsPage(int id) {
        Complaint c = manager.findComplaint(id);
        if (c == null) {
            return "<h2>Complaint not found</h2><p><a href=\"/admin\">Back</a></p>";
        }
        PriorityCalculator calc = manager.getCalculator();
        DataStore store = manager.getStore();

        StringBuilder sb = new StringBuilder();
        sb.append("<h2>Complaint #").append(c.getId()).append("</h2>\n");
        sb.append("<p class=\"muted\"><a href=\"").append(sectionLink(c.getGroupName())).append("\">Back to ")
          .append(Web.esc(c.getGroupName())).append(" complaints</a></p>\n");

        // information table
        sb.append("<div class=\"panel\">\n<div class=\"panel-title\">").append(Web.esc(c.getTitle())).append("</div>\n");
        sb.append("<table class=\"details\">\n");
        sb.append(row("Description", Web.esc(c.getDescription())));
        sb.append(row("Category", Web.esc(c.getCategory())));
        sb.append(row("Affects", Layout.affects(c, calc)));
        sb.append(row("Reported by", reporter(c.getReportedBy()) + " on " + c.getCreatedOn()));
        sb.append(row("Votes", Layout.votes(c, calc) + " (" + calc.supportPercent(c) + "%)"));
        sb.append(row("Urgency / Severity", c.getUrgency() + " / " + c.getSeverity()));
        sb.append(row("Priority score", Layout.scoreBar(calc.score(c), c.isSafetyCritical())));
        sb.append(row("Why this rank", Web.esc(calc.explain(c))));
        sb.append(row("Status", Layout.badge(c) + " (" + Web.esc(c.getStatus()) + ")"));
        sb.append(row("Assigned to", c.getAssignedTo().isEmpty() ? "-" : Web.esc(c.getAssignedTo())));
        sb.append(row("Last note", c.getNote().isEmpty() ? "-" : Web.esc(c.getNote())));
        sb.append("</table>\n</div>\n");

        // action forms
        sb.append("<div class=\"grid\">\n");

        // 1. severity
        sb.append("<div class=\"panel\"><div class=\"panel-title\">Set severity</div><div class=\"panel-body\">\n");
        sb.append("<form method=\"post\" action=\"/admin/severity\">\n");
        sb.append("<input type=\"hidden\" name=\"id\" value=\"").append(c.getId()).append("\">\n");
        sb.append("<label>Severity (1 to 5)</label>\n<select name=\"severity\">")
          .append(numberOptions(c.getSeverity())).append("</select>\n");
        sb.append("<label><input class=\"check\" type=\"checkbox\" name=\"critical\" value=\"yes\"")
          .append(c.isSafetyCritical() ? " checked" : "").append("> Mark as safety critical</label>\n");
        sb.append("<button type=\"submit\">Save</button>\n</form>\n</div></div>\n");

        // 2. assign
        sb.append("<div class=\"panel\"><div class=\"panel-title\">Assign to team or person</div><div class=\"panel-body\">\n");
        sb.append("<form method=\"post\" action=\"/admin/assign\">\n");
        sb.append("<input type=\"hidden\" name=\"id\" value=\"").append(c.getId()).append("\">\n");
        sb.append("<label>Name</label>\n<input type=\"text\" name=\"assignedTo\" value=\"")
          .append(Web.esc(c.getAssignedTo())).append("\" placeholder=\"Example: Maintenance Team\" required>\n");
        sb.append("<button type=\"submit\">Assign</button>\n</form>\n</div></div>\n");

        // 3. status
        sb.append("<div class=\"panel\"><div class=\"panel-title\">Update status</div><div class=\"panel-body\">\n");
        sb.append("<form method=\"post\" action=\"/admin/status\">\n");
        sb.append("<input type=\"hidden\" name=\"id\" value=\"").append(c.getId()).append("\">\n");
        sb.append("<label>New status</label>\n<select name=\"status\">")
          .append(options(ComplaintManager.STATUSES, c.getStatus())).append("</select>\n");
        sb.append("<label>Reason</label>\n<input type=\"text\" name=\"note\" placeholder=\"Why is the status changing?\" required>\n");
        sb.append("<button type=\"submit\">Update</button>\n</form>\n</div></div>\n");

        // 4. category
        List<String> names = new ArrayList<>();
        for (Category cat : store.getCategories()) {
            names.add(cat.getName());
        }
        sb.append("<div class=\"panel\"><div class=\"panel-title\">Change category</div><div class=\"panel-body\">\n");
        sb.append("<form method=\"post\" action=\"/admin/category\">\n");
        sb.append("<input type=\"hidden\" name=\"id\" value=\"").append(c.getId()).append("\">\n");
        sb.append("<label>Category</label>\n<select name=\"category\">")
          .append(options(names.toArray(new String[0]), c.getCategory())).append("</select>\n");
        sb.append("<button type=\"submit\">Change</button>\n</form>\n</div></div>\n");

        sb.append("</div>");
        return sb.toString();
    }

    // one row of the details table
    private String row(String label, String value) {
        return "<tr><td>" + label + "</td><td>" + value + "</td></tr>\n";
    }

    // ---------------------------------------------------------------
    // PAGE 3 : priority ranking
    // ---------------------------------------------------------------
    private String rankingPage(String section) {
        PriorityCalculator calc = manager.getCalculator();
        StringBuilder sb = new StringBuilder();
        sb.append("<h2>Priority Ranking</h2>\n");
        sb.append("<p class=\"muted\">Open complaints, highest priority first. The last column explains the rank.</p>\n");
        sb.append(filterBar("/admin/ranking", section));
        sb.append("<div class=\"panel\">\n<table>\n");
        sb.append("<tr><th>Rank</th><th>ID</th><th>Title</th><th>Score</th><th>Reason</th></tr>\n");
        int rank = 1;
        for (Complaint c : manager.rankedList()) {
            if (section != null && !c.isForGroup(section)) {
                continue;   // not for this section
            }
            sb.append("<tr>");
            sb.append("<td>").append(rank++).append("</td>");
            sb.append("<td>#").append(c.getId()).append("</td>");
            sb.append("<td><a href=\"/admin/complaint?id=").append(c.getId()).append("\">")
              .append(Web.esc(c.getTitle())).append("</a></td>");
            sb.append("<td>").append(Layout.scoreBar(calc.score(c), c.isSafetyCritical())).append("</td>");
            sb.append("<td>").append(Web.esc(calc.explain(c))).append("</td>");
            sb.append("</tr>\n");
        }
        sb.append("</table>\n</div>\n");

        // formula, so the admin can explain it
        sb.append("<div class=\"panel\">\n<div class=\"panel-title\">How the score is calculated</div>\n<div class=\"panel-body\">\n");
        sb.append("Score = 0.35 x Severity + 0.35 x Support % + 0.20 x Urgency + 0.10 x Waiting time<br>\n");
        sb.append("Every part is converted to a value from 0 to 100. Support % = votes divided by the number of students of the section (CSE 50, AIDS 40, AIML 10), ")
          .append("so the small AIML section is not ignored. Score is shown as a percentage (100% only if every part is at its maximum). Safety critical complaints are always listed first.\n");
        sb.append("</div>\n</div>");
        return sb.toString();
    }

    // ---------------------------------------------------------------
    // PAGE 4 : statistics
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
        sb.append("<h2>Statistics</h2>\n<p class=\"muted\">Total complaints: ").append(total).append("</p>\n");
        sb.append(filterBar("/admin/stats", section));

        sb.append("<div class=\"grid\">\n");

        // by status
        sb.append("<div class=\"panel\"><div class=\"panel-title\">By status</div>\n<table>\n");
        sb.append("<tr><th>Status</th><th>Count</th><th></th></tr>\n");
        for (String status : ComplaintManager.STATUSES) {
            int count = 0;
            for (Complaint c : all) {
                if (c.getStatus().equals(status)) {
                    count++;
                }
            }
            sb.append(statRow(status, count, total));
        }
        sb.append("</table>\n</div>\n");

        // by category
        sb.append("<div class=\"panel\"><div class=\"panel-title\">By category</div>\n<table>\n");
        sb.append("<tr><th>Category</th><th>Count</th><th></th></tr>\n");
        for (Category cat : manager.getStore().getCategories()) {
            int count = 0;
            for (Complaint c : all) {
                if (c.getCategory().equalsIgnoreCase(cat.getName())) {
                    count++;
                }
            }
            sb.append(statRow(cat.getName(), count, total));
        }
        sb.append("</table>\n</div>\n");

        sb.append("</div>");
        return sb.toString();
    }

    // links to choose a section: All sections | CSE | AIML | AIDS
    private String filterBar(String page, String section) {
        StringBuilder sb = new StringBuilder("<div class=\"filter\">Section:");
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
                + percent + "%\"></span></span></td></tr>\n";
    }

    // ---------------------------------------------------------------
    // PAGE 5 : groups and users
    // ---------------------------------------------------------------
    private String groupsPage() {
        DataStore store = manager.getStore();
        List<String> names = new ArrayList<>();
        for (StudentGroup g : store.getGroups()) {
            names.add(g.getName());
        }
        String groupOptions = options(names.toArray(new String[0]), "");

        StringBuilder sb = new StringBuilder();
        sb.append("<h2>Groups &amp; Users</h2>\n");
        sb.append("<p class=\"muted\">Groups are sections or departments. Votes are counted out of the people in the group.</p>\n");

        // groups table
        sb.append("<div class=\"panel\">\n<div class=\"panel-title\">Groups</div>\n<table>\n");
        sb.append("<tr><th>Group</th><th>Total people</th><th>Login accounts</th></tr>\n");
        for (StudentGroup g : store.getGroups()) {
            int accounts = 0;
            for (Account a : store.getAccounts()) {
                if (a.getGroupName().equalsIgnoreCase(g.getName())) {
                    accounts++;
                }
            }
            sb.append("<tr><td>").append(Web.esc(g.getName())).append("</td><td>").append(g.getTotalPeople())
              .append("</td><td>").append(accounts).append("</td></tr>\n");
        }
        sb.append("</table>\n</div>\n");

        sb.append("<div class=\"grid\">\n");

        // add a group
        sb.append("<div class=\"panel\"><div class=\"panel-title\">Add a group</div><div class=\"panel-body\">\n");
        sb.append("<form method=\"post\" action=\"/admin/group-add\">\n");
        sb.append("<label>Group name</label>\n<input type=\"text\" name=\"name\" placeholder=\"Example: BCA\" required>\n");
        sb.append("<label>Total people</label>\n<input type=\"number\" name=\"people\" min=\"1\" required>\n");
        sb.append("<button type=\"submit\">Add group</button>\n</form>\n</div></div>\n");

        // update number of people
        sb.append("<div class=\"panel\"><div class=\"panel-title\">Update number of people</div><div class=\"panel-body\">\n");
        sb.append("<form method=\"post\" action=\"/admin/group-update\">\n");
        sb.append("<label>Group</label>\n<select name=\"name\">").append(groupOptions).append("</select>\n");
        sb.append("<label>Total people</label>\n<input type=\"number\" name=\"people\" min=\"1\" required>\n");
        sb.append("<button type=\"submit\">Update</button>\n</form>\n</div></div>\n");

        // add one user
        sb.append("<div class=\"panel\"><div class=\"panel-title\">Add one user</div><div class=\"panel-body\">\n");
        sb.append("<form method=\"post\" action=\"/admin/user-add\">\n");
        sb.append("<label>Username</label>\n<input type=\"text\" name=\"username\" required>\n");
        sb.append("<label>Full name</label>\n<input type=\"text\" name=\"fullName\" required>\n");
        sb.append("<label>Password</label>\n<input type=\"text\" name=\"password\" required>\n");
        sb.append("<label>Group</label>\n<select name=\"group\">").append(groupOptions).append("</select>\n");
        sb.append("<button type=\"submit\">Add user</button>\n</form>\n</div></div>\n");

        // add many users
        sb.append("<div class=\"panel\"><div class=\"panel-title\">Add many students</div><div class=\"panel-body\">\n");
        sb.append("<form method=\"post\" action=\"/admin/user-many\">\n");
        sb.append("<label>Group</label>\n<select name=\"group\">").append(groupOptions).append("</select>\n");
        sb.append("<label>How many</label>\n<input type=\"number\" name=\"count\" min=\"1\" max=\"200\" required>\n");
        sb.append("<label>Username prefix</label>\n<input type=\"text\" name=\"prefix\" placeholder=\"Example: bca (creates bca1, bca2...)\" required>\n");
        sb.append("<label>Default password</label>\n<input type=\"text\" name=\"password\" required>\n");
        sb.append("<button type=\"submit\">Create accounts</button>\n</form>\n</div></div>\n");

        sb.append("</div>");
        return sb.toString();
    }

    // ---------------------------------------------------------------
    // PAGE 6 : categories
    // ---------------------------------------------------------------
    private String categoriesPage() {
        StringBuilder sb = new StringBuilder();
        sb.append("<h2>Categories</h2>\n");
        sb.append("<p class=\"muted\">Keywords are used to suggest a category when a user reports a problem.</p>\n");
        sb.append("<div class=\"panel\">\n<table>\n<tr><th>Category</th><th>Keywords</th></tr>\n");
        for (Category cat : manager.getStore().getCategories()) {
            sb.append("<tr><td>").append(Web.esc(cat.getName())).append("</td><td>")
              .append(Web.esc(cat.getKeywords())).append("</td></tr>\n");
        }
        sb.append("</table>\n</div>\n");

        sb.append("<div class=\"panel\"><div class=\"panel-title\">Add a category</div><div class=\"panel-body\">\n");
        sb.append("<form method=\"post\" action=\"/admin/category-add\">\n");
        sb.append("<label>Name</label>\n<input type=\"text\" name=\"name\" placeholder=\"Example: Lift\" required>\n");
        sb.append("<label>Keywords (separated by comma)</label>\n<input type=\"text\" name=\"keywords\" placeholder=\"Example: lift,elevator\">\n");
        sb.append("<button type=\"submit\">Add category</button>\n</form>\n</div></div>");
        return sb.toString();
    }

    // ---------------------------------------------------------------
    // forms (POST requests)
    // ---------------------------------------------------------------
    private void handlePost(HttpExchange ex, String path, Map<String, String> f) throws IOException {
        DataStore store = manager.getStore();
        int id = Web.number(f.get("id"), 0);
        String back = "/admin/complaint?id=" + id;   // most forms return to the complaint page

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
                    Web.redirect(ex, back, "Please type a name.");
                } else {
                    Web.redirect(ex, back, manager.assignComplaint(id, name));
                }
                break;
            }
            case "/admin/status": {
                String note = Web.clean(f.get("note"));
                if (note.isEmpty()) {
                    Web.redirect(ex, back, "A reason is required to change the status.");
                } else {
                    Web.redirect(ex, back, manager.changeStatus(id, f.get("status"), note));
                }
                break;
            }
            case "/admin/category":
                Web.redirect(ex, back, manager.changeCategory(id, Web.clean(f.get("category"))));
                break;

            case "/admin/group-add": {
                String name = Web.clean(f.get("name"));
                int people = Web.number(f.get("people"), 0);
                if (name.isEmpty() || people < 1) {
                    Web.redirect(ex, "/admin/groups", "Enter a name and the number of people.");
                } else if (store.findGroup(name) != null) {
                    Web.redirect(ex, "/admin/groups", "This group already exists.");
                } else {
                    store.getGroups().add(new StudentGroup(name, people));
                    store.save();
                    Web.redirect(ex, "/admin/groups", "Group " + name + " added.");
                }
                break;
            }
            case "/admin/group-update": {
                StudentGroup g = store.findGroup(Web.clean(f.get("name")));
                int people = Web.number(f.get("people"), 0);
                if (g == null || people < 1) {
                    Web.redirect(ex, "/admin/groups", "Choose a group and enter the number of people.");
                } else {
                    g.setTotalPeople(people);
                    store.save();
                    Web.redirect(ex, "/admin/groups", "Group " + g.getName() + " now has " + people + " people.");
                }
                break;
            }
            case "/admin/user-add": {
                String username = Web.clean(f.get("username")).replace(" ", "");
                String fullName = Web.clean(f.get("fullName"));
                String password = Web.clean(f.get("password"));
                StudentGroup g = store.findGroup(Web.clean(f.get("group")));
                if (username.isEmpty() || fullName.isEmpty() || password.isEmpty() || g == null) {
                    Web.redirect(ex, "/admin/groups", "Fill all the fields (and create a group first).");
                } else if (store.findAccount(username) != null) {
                    Web.redirect(ex, "/admin/groups", "This username is already taken.");
                } else {
                    store.getAccounts().add(new Account(username, PasswordUtil.hash(username, password), fullName, "USER", g.getName()));
                    store.save();
                    Web.redirect(ex, "/admin/groups", "User " + username + " added.");
                }
                break;
            }
            case "/admin/user-many": {
                StudentGroup g = store.findGroup(Web.clean(f.get("group")));
                int count = Web.number(f.get("count"), 0);
                String prefix = Web.clean(f.get("prefix")).replace(" ", "");
                String password = Web.clean(f.get("password"));
                if (g == null || count < 1 || count > 200 || prefix.isEmpty() || password.isEmpty()) {
                    Web.redirect(ex, "/admin/groups", "Fill all the fields (how many: 1 to 200).");
                } else {
                    int created = 0;
                    int n = 1;
                    while (created < count) {
                        String username = prefix + n;
                        if (store.findAccount(username) == null) {   // skip names that already exist
                            store.getAccounts().add(new Account(username, PasswordUtil.hash(username, password),
                                    g.getName() + " Student " + n, "USER", g.getName()));
                            created++;
                        }
                        n++;
                    }
                    store.save();
                    Web.redirect(ex, "/admin/groups", created + " accounts created for " + g.getName() + ".");
                }
                break;
            }
            case "/admin/category-add": {
                String name = Web.clean(f.get("name"));
                if (name.isEmpty()) {
                    Web.redirect(ex, "/admin/categories", "Please type a category name.");
                } else if (store.findCategory(name) != null) {
                    Web.redirect(ex, "/admin/categories", "This category already exists.");
                } else {
                    store.getCategories().add(new Category(name, Web.clean(f.get("keywords"))));
                    store.save();
                    Web.redirect(ex, "/admin/categories", "Category " + name + " added.");
                }
                break;
            }
            default:
                Web.redirect(ex, "/admin");
        }
    }

    // ---------------------------------------------------------------
    // small helpers for drop-down lists
    // ---------------------------------------------------------------
    private String options(String[] values, String selected) {
        StringBuilder sb = new StringBuilder();
        for (String v : values) {
            sb.append("<option").append(v.equalsIgnoreCase(selected) ? " selected" : "").append(">")
              .append(Web.esc(v)).append("</option>");
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
