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

public class UserServer {

    private ComplaintManager manager = new ComplaintManager();
    private Session session = new Session("RESOLVEX_USER");

    // side menu: label and link
    private static final String[][] MENU = {
        {"My Complaints", "/user"},
        {"Section Complaints", "/user/all"},
        {"Report a Problem", "/user/report"}
    };

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
        System.out.println("ResolveX USER is running at http://localhost:" + port);
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
        manager.reload();   // read the files again (the admin program may have changed them)

        // login and logout
        if (path.equals("/login")) {
            if (method.equals("POST")) {
                login(ex);
            } else {
                Web.html(ex, Layout.loginPage("User Login", ""));
            }
            return;
        }
        if (path.equals("/logout")) {
            session.logout(ex);
            Web.redirect(ex, "/login");
            return;
        }

        // all other pages need a logged in user
        Account user = session.current(ex);
        if (user == null) {
            Web.redirect(ex, "/login");
            return;
        }

        if (method.equals("POST")) {
            handlePost(ex, path, Web.form(ex), user);
            return;
        }

        Map<String, String> q = Web.query(ex);
        String msg = q.get("msg");
        switch (path) {
            case "/":
                Web.redirect(ex, "/user");
                break;
            case "/user":
                show(ex, user, "/user", myComplaintsPage(user), msg);
                break;
            case "/user/all":
                show(ex, user, "/user/all", allComplaintsPage(user), msg);
                break;
            case "/user/report":
                show(ex, user, "/user/report", reportPage(user), msg);
                break;
            case "/user/complaint":
                show(ex, user, "/user/all", detailsPage(Web.number(q.get("id"), 0), user), msg);
                break;
            default:
                show(ex, user, "", "<h2>Page not found</h2>", null);
        }
    }

    // wrap the content in the common layout and send it
    private void show(HttpExchange ex, Account user, String active, String content, String msg) throws IOException {
        String subtitle = "User Dashboard \u2014 " + user.getUsername() + " (" + user.getGroupName() + " Section)";
        Web.html(ex, Layout.page("USER MENU", subtitle, MENU, active, content, msg));
    }

    // check username and password
    private void login(HttpExchange ex) throws IOException {
        Map<String, String> f = Web.form(ex);
        String username = f.getOrDefault("username", "").trim();
        String password = f.getOrDefault("password", "");
        Account a = manager.getStore().findAccount(username);

        if (a == null || !a.getPasswordHash().equals(PasswordUtil.hash(a.getUsername(), password))) {
            Web.html(ex, Layout.loginPage("User Login", "Wrong username or password."));
        } else if (!a.getRole().equals("USER")) {
            Web.html(ex, Layout.loginPage("User Login", "This is not a user account. Use the Admin dashboard."));
        } else {
            session.login(ex, a);
            Web.redirect(ex, "/user");
        }
    }

    // ---------------------------------------------------------------
    // PAGE 1 : my complaints
    // ---------------------------------------------------------------
    private String myComplaintsPage(Account user) {
        PriorityCalculator calc = manager.getCalculator();

        // only the complaints reported by this user
        List<Complaint> mine = new ArrayList<>();
        for (Complaint c : manager.getAllComplaints()) {
            if (c.getReportedBy().equalsIgnoreCase(user.getUsername())) {
                mine.add(c);
            }
        }

        int waiting = 0, progress = 0, resolved = 0;
        for (Complaint c : mine) {
            if (c.getStatus().equals("In Progress")) {
                progress++;
            } else if (c.getStatus().equals("Resolved") || c.getStatus().equals("Closed")) {
                resolved++;
            } else if (c.isOpen()) {
                waiting++;
            }
        }

        StringBuilder sb = new StringBuilder();
        sb.append("<h2>My Complaints</h2>\n");
        sb.append("<p class=\"muted\">Logged in as: ").append(Web.esc(user.getUsername())).append(" \u2014 ")
          .append(Web.esc(user.getGroupName())).append(" Section</p>\n");
        sb.append("<div class=\"cards\">\n");
        sb.append(Layout.card(mine.size(), "My Complaints"));
        sb.append(Layout.card(waiting, "Waiting"));
        sb.append(Layout.card(progress, "In Progress"));
        sb.append(Layout.card(resolved, "Resolved"));
        sb.append("</div>\n");

        sb.append("<div class=\"panel\">\n<div class=\"panel-title\">Tickets Reported by ").append(Web.esc(user.getUsername())).append("</div>\n");
        sb.append("<div class=\"panel-body\">\n");
        if (mine.isEmpty()) {
            sb.append("You have not reported any problem yet. Use \"Report a Problem\" in the menu.\n");
        }
        for (Complaint c : mine) {
            String assigned = c.getAssignedTo().isEmpty() ? "Not yet assigned" : "Assigned to " + Web.esc(c.getAssignedTo());
            sb.append("<div class=\"ticket\">\n");
            sb.append("  <div class=\"ticket-title\"><a href=\"/user/complaint?id=").append(c.getId()).append("\">")
              .append(Web.esc(c.getTitle())).append("</a></div>\n");
            sb.append("  <div class=\"ticket-meta\">");
            sb.append("<span>Ticket #").append(c.getId()).append("</span>");
            sb.append(Layout.categoryBadge(c.getCategory()));
            sb.append(Layout.sectionBadge(c.getGroupName()));
            sb.append("<span>Votes: ").append(Layout.votes(c, calc)).append(" (").append(calc.supportPercent(c)).append("%)</span>");
            sb.append(Layout.badge(c));
            sb.append("</div>\n");
            sb.append("  <div class=\"ticket-foot\">Reported ").append(ago(calc.daysWaiting(c))).append(" \u2014 ")
              .append(assigned).append(". Priority Score: ").append(Math.round(calc.score(c))).append("%.</div>\n");
            sb.append("</div>\n");
        }
        sb.append("</div>\n</div>");
        return sb.toString();
    }

    private String ago(long days) {
        if (days == 0) {
            return "today";
        }
        return days + (days == 1 ? " day ago" : " days ago");
    }

    // ---------------------------------------------------------------
    // PAGE 2 : all complaints (with a Support button)
    // ---------------------------------------------------------------
    private String allComplaintsPage(Account user) {
        PriorityCalculator calc = manager.getCalculator();
        // only the complaints of my section and complaints for the whole college
        List<Complaint> all = new ArrayList<>();
        for (Complaint c : manager.getAllComplaints()) {
            if (c.isForGroup(user.getGroupName())) {
                all.add(c);
            }
        }
        all.sort((a, b) -> Double.compare(calc.rankValue(b), calc.rankValue(a)));   // highest score first

        StringBuilder sb = new StringBuilder();
        sb.append("<h2>").append(Web.esc(user.getGroupName())).append(" Section Complaints</h2>\n");
        sb.append("<p class=\"muted\">Complaints of the ").append(Web.esc(user.getGroupName()))
          .append(" section (").append(manager.getStore().findGroup(user.getGroupName()).getTotalPeople())
          .append(" students). Support a complaint if you have the same problem. One vote per person.</p>\n");
        sb.append("<div class=\"panel\">\n<table>\n");
        sb.append("<tr><th>ID</th><th>Title</th><th>Category</th><th>Affects</th><th>Votes</th><th>Score</th><th>Status</th><th>Action</th></tr>\n");
        for (Complaint c : all) {
            sb.append("<tr>");
            sb.append("<td>#").append(c.getId()).append("</td>");
            sb.append("<td><a href=\"/user/complaint?id=").append(c.getId()).append("\">").append(Web.esc(c.getTitle())).append("</a></td>");
            sb.append("<td>").append(Web.esc(c.getCategory())).append("</td>");
            sb.append("<td>").append(Layout.affects(c, calc)).append("</td>");
            sb.append("<td>").append(Layout.votes(c, calc)).append("</td>");
            sb.append("<td>").append(Layout.scoreBar(calc.score(c), c.isSafetyCritical())).append("</td>");
            sb.append("<td>").append(Layout.badge(c)).append("</td>");
            sb.append("<td>").append(supportButton(c, user, "/user/all")).append("</td>");
            sb.append("</tr>\n");
        }
        sb.append("</table>\n</div>");
        return sb.toString();
    }

    // can this user vote for this complaint?
    private boolean canSupport(Complaint c, Account user) {
        if (!c.isOpen()) {
            return false;
        }
        if (!c.isWholeCollege() && !c.getGroupName().equalsIgnoreCase(user.getGroupName())) {
            return false;   // only the affected group can vote
        }
        return !c.getSupporters().contains(user.getUsername());
    }

    // a small form with one button (or a text if the user cannot vote)
    private String supportButton(Complaint c, Account user, String back) {
        if (canSupport(c, user)) {
            return postButton("/user/support", c.getId(), back, "Support", "small");
        }
        if (c.getSupporters().contains(user.getUsername())) {
            return "Supported";
        }
        return "-";
    }

    // a form that has only a button
    private String postButton(String action, int id, String back, String label, String css) {
        return "<form method=\"post\" action=\"" + action + "\" style=\"display:inline\">"
                + "<input type=\"hidden\" name=\"id\" value=\"" + id + "\">"
                + "<input type=\"hidden\" name=\"back\" value=\"" + back + "\">"
                + "<button class=\"" + css + "\" type=\"submit\">" + label + "</button></form> ";
    }

    // ---------------------------------------------------------------
    // PAGE 3 : report a problem
    // ---------------------------------------------------------------
    private String reportPage(Account user) {
        DataStore store = manager.getStore();

        // category list (first choice lets the system decide)
        StringBuilder cats = new StringBuilder("<option value=\"AUTO\">Detect automatically</option>");
        for (Category c : store.getCategories()) {
            cats.append("<option>").append(Web.esc(c.getName())).append("</option>");
        }
        // the complaint always belongs to my own section
        StringBuilder who = new StringBuilder();
        who.append("<option>").append(Web.esc(user.getGroupName())).append("</option>");

        StringBuilder sb = new StringBuilder();
        sb.append("<h2>Report a Problem</h2>\n<p class=\"muted\">Tell us what is wrong. The admin will set the priority.</p>\n");
        sb.append("<div class=\"panel\"><div class=\"panel-title\">New complaint</div><div class=\"panel-body\">\n");
        sb.append("<form method=\"post\" action=\"/user/report\">\n");
        sb.append("<label>Title</label>\n<input type=\"text\" name=\"title\" placeholder=\"Example: Projector not working in Room 302\" required>\n");
        sb.append("<label>Description</label>\n<textarea name=\"description\" required></textarea>\n");
        sb.append("<label>Category</label>\n<select name=\"category\">").append(cats).append("</select>\n");
        sb.append("<label>Section affected</label>\n<select name=\"affects\">").append(who).append("</select>\n");
        sb.append("<label>Urgency (3 Levels)</label>\n<select name=\"urgency\">")
          .append("<option value=\"1\">1 - Low (Minor delay)</option>")
          .append("<option value=\"2\" selected>2 - Medium (Affecting classes)</option>")
          .append("<option value=\"3\">3 - High (Urgent attention needed)</option></select>\n");
        sb.append("<button type=\"submit\">Submit complaint</button>\n</form>\n</div></div>");
        return sb.toString();
    }

    // shown when a similar open complaint already exists (to avoid duplicates)
    private String similarPage(List<Complaint> similar, Map<String, String> f, Account user) {
        PriorityCalculator calc = manager.getCalculator();
        StringBuilder sb = new StringBuilder();
        sb.append("<h2>Similar complaints found</h2>\n");
        sb.append("<p class=\"muted\">Someone may have reported this already. You can support that complaint instead.</p>\n");
        sb.append("<div class=\"panel\">\n<table>\n<tr><th>ID</th><th>Title</th><th>Affects</th><th>Votes</th><th>Action</th></tr>\n");
        for (Complaint c : similar) {
            sb.append("<tr><td>#").append(c.getId()).append("</td><td>").append(Web.esc(c.getTitle())).append("</td>");
            sb.append("<td>").append(Layout.affects(c, calc)).append("</td><td>").append(Layout.votes(c, calc)).append("</td>");
            sb.append("<td>").append(supportButton(c, user, "/user")).append("</td></tr>\n");
        }
        sb.append("</table>\n</div>\n");

        // submit anyway: the same values are sent again with force=yes
        sb.append("<div class=\"panel\"><div class=\"panel-title\">My problem is different</div><div class=\"panel-body\">\n");
        sb.append("<form method=\"post\" action=\"/user/report\">\n");
        for (String key : new String[] {"title", "description", "category", "affects", "urgency"}) {
            sb.append("<input type=\"hidden\" name=\"").append(key).append("\" value=\"")
              .append(Web.esc(f.getOrDefault(key, ""))).append("\">\n");
        }
        sb.append("<input type=\"hidden\" name=\"force\" value=\"yes\">\n");
        sb.append("<button type=\"submit\">Submit as a new complaint</button>\n</form>\n</div></div>");
        return sb.toString();
    }

    // ---------------------------------------------------------------
    // PAGE 4 : details of one complaint
    // ---------------------------------------------------------------
    private String detailsPage(int id, Account user) {
        Complaint c = manager.findComplaint(id);
        if (c == null) {
            return "<h2>Complaint not found</h2><p><a href=\"/user/all\">Back</a></p>";
        }
        if (!c.isForGroup(user.getGroupName()) && !c.getReportedBy().equalsIgnoreCase(user.getUsername())) {
            return "<h2>Not available</h2><p class=\"muted\">This complaint belongs to another section.</p>";
        }
        PriorityCalculator calc = manager.getCalculator();
        String back = "/user/complaint?id=" + id;

        StringBuilder sb = new StringBuilder();
        sb.append("<h2>Ticket #").append(c.getId()).append("</h2>\n");
        sb.append("<p class=\"muted\"><a href=\"/user/all\">Back to all complaints</a></p>\n");
        sb.append("<div class=\"panel\">\n<div class=\"panel-title\">").append(Web.esc(c.getTitle())).append("</div>\n");
        sb.append("<table class=\"details\">\n");
        sb.append(row("Description", Web.esc(c.getDescription())));
        sb.append(row("Category", Web.esc(c.getCategory())));
        sb.append(row("Affects", Layout.affects(c, calc)));
        sb.append(row("Reported by", Web.esc(c.getReportedBy()) + " (" + ago(calc.daysWaiting(c)) + ")"));
        sb.append(row("Votes", Layout.votes(c, calc) + " (" + calc.supportPercent(c) + "%)"));
        sb.append(row("Priority score", Layout.scoreBar(calc.score(c), c.isSafetyCritical())));
        sb.append(row("Status", Layout.badge(c) + " (" + Web.esc(c.getStatus()) + ")"));
        sb.append(row("Assigned to", c.getAssignedTo().isEmpty() ? "Not yet assigned" : Web.esc(c.getAssignedTo())));
        sb.append(row("Admin note", c.getNote().isEmpty() ? "-" : Web.esc(c.getNote())));
        sb.append("</table>\n</div>\n");

        // buttons
        if (canSupport(c, user)) {
            sb.append(postButton("/user/support", id, back, "Support this complaint", ""));
        } else if (c.getSupporters().contains(user.getUsername()) && c.isOpen()) {
            sb.append(postButton("/user/unsupport", id, back, "Remove my support", "light"));
        }
        // the reporter confirms whether the fix is really done
        if (c.getReportedBy().equalsIgnoreCase(user.getUsername()) && c.getStatus().equals("Resolved")) {
            sb.append(postButton("/user/confirm", id, back, "Confirm fix", ""));
            sb.append(postButton("/user/reopen", id, back, "Problem is not fixed", "light"));
        }
        return sb.toString();
    }

    private String row(String label, String value) {
        return "<tr><td>" + label + "</td><td>" + value + "</td></tr>\n";
    }

    // ---------------------------------------------------------------
    // forms (POST requests)
    // ---------------------------------------------------------------
    private void handlePost(HttpExchange ex, String path, Map<String, String> f, Account user) throws IOException {
        int id = Web.number(f.get("id"), 0);
        String back = f.getOrDefault("back", "/user");
        if (!back.startsWith("/user")) {
            back = "/user";   // only go back to our own pages
        }

        switch (path) {
            case "/user/support":
                Web.redirect(ex, back, manager.supportComplaint(id, user));
                break;

            case "/user/unsupport":
                Web.redirect(ex, back, manager.removeSupport(id, user));
                break;

            case "/user/confirm":
            case "/user/reopen": {
                Complaint c = manager.findComplaint(id);
                if (c == null || !c.getReportedBy().equalsIgnoreCase(user.getUsername()) || !c.getStatus().equals("Resolved")) {
                    Web.redirect(ex, back, "You cannot do this for this complaint.");
                } else if (path.equals("/user/confirm")) {
                    Web.redirect(ex, back, manager.changeStatus(id, "Closed", "Fix confirmed by the reporter."));
                } else {
                    Web.redirect(ex, back, manager.changeStatus(id, "Reopened", "Reporter says the problem is not fixed."));
                }
                break;
            }
            case "/user/report":
                report(ex, f, user);
                break;

            default:
                Web.redirect(ex, "/user");
        }
    }

    // save a new complaint
    private void report(HttpExchange ex, Map<String, String> f, Account user) throws IOException {
        DataStore store = manager.getStore();
        String title = Web.clean(f.get("title"));
        String description = Web.clean(f.get("description"));
        String category = Web.clean(f.get("category"));
        String affects = Web.clean(f.get("affects"));
        int urgency = Math.max(1, Math.min(5, Web.number(f.get("urgency"), 3)));

        if (title.isEmpty() || description.isEmpty()) {
            Web.redirect(ex, "/user/report", "Please fill the title and the description.");
            return;
        }

        // look for similar open complaints first (unless the user said "submit anyway")
        if (!"yes".equals(f.get("force"))) {
            List<Complaint> similar = new ArrayList<>();
            for (Complaint c : manager.findSimilar(title)) {
                if (c.isForGroup(user.getGroupName())) {
                    similar.add(c);   // do not show complaints of other sections
                }
            }
            if (!similar.isEmpty()) {
                show(ex, user, "/user/report", similarPage(similar, f, user), null);
                return;
            }
        }

        // let the system choose the category from the words
        if (category.equals("AUTO") || store.findCategory(category) == null) {
            Category suggested = manager.suggestCategory(title + " " + description);
            category = suggested != null ? suggested.getName() : "Other";
        }

        if (!affects.equalsIgnoreCase(user.getGroupName())) {
            Web.redirect(ex, "/user/report", "Please choose your section.");
            return;
        }

        int newId = manager.reportComplaint(title, description, category, false, user.getGroupName(), urgency, user);
        Web.redirect(ex, "/user/complaint?id=" + newId, "Your complaint was submitted. Category: " + category + ".");
    }
}
