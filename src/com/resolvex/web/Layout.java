package com.resolvex.web;

import com.resolvex.model.Complaint;
import com.resolvex.service.PriorityCalculator;

/** Builds the common parts of every page: header, side menu, badges, score bar. */
public class Layout {

    // Clean SVG icons (no childish emojis)
    public static final String ICON_GRID = "<svg width=\"15\" height=\"15\" viewBox=\"0 0 24 24\" fill=\"none\" stroke=\"currentColor\" stroke-width=\"2\" stroke-linecap=\"round\" stroke-linejoin=\"round\"><rect x=\"3\" y=\"3\" width=\"7\" height=\"7\"/><rect x=\"14\" y=\"3\" width=\"7\" height=\"7\"/><rect x=\"14\" y=\"14\" width=\"7\" height=\"7\"/><rect x=\"3\" y=\"14\" width=\"7\" height=\"7\"/></svg>";
    public static final String ICON_LIST = "<svg width=\"15\" height=\"15\" viewBox=\"0 0 24 24\" fill=\"none\" stroke=\"currentColor\" stroke-width=\"2\" stroke-linecap=\"round\" stroke-linejoin=\"round\"><line x1=\"8\" y1=\"6\" x2=\"21\" y2=\"6\"/><line x1=\"8\" y1=\"12\" x2=\"21\" y2=\"12\"/><line x1=\"8\" y1=\"18\" x2=\"21\" y2=\"18\"/><line x1=\"3\" y1=\"6\" x2=\"3.01\" y2=\"6\"/><line x1=\"3\" y1=\"12\" x2=\"3.01\" y2=\"12\"/><line x1=\"3\" y1=\"18\" x2=\"3.01\" y2=\"18\"/></svg>";
    public static final String ICON_PLUS = "<svg width=\"15\" height=\"15\" viewBox=\"0 0 24 24\" fill=\"none\" stroke=\"currentColor\" stroke-width=\"2\" stroke-linecap=\"round\" stroke-linejoin=\"round\"><line x1=\"12\" y1=\"5\" x2=\"12\" y2=\"19\"/><line x1=\"5\" y1=\"12\" x2=\"19\" y2=\"12\"/></svg>";
    public static final String ICON_USERS = "<svg width=\"15\" height=\"15\" viewBox=\"0 0 24 24\" fill=\"none\" stroke=\"currentColor\" stroke-width=\"2\" stroke-linecap=\"round\" stroke-linejoin=\"round\"><path d=\"M17 21v-2a4 4 0 0 0-4-4H5a4 4 0 0 0-4 4v2\"/><circle cx=\"9\" cy=\"7\" r=\"4\"/><path d=\"M23 21v-2a4 4 0 0 0-3-3.87\"/><path d=\"M16 3.13a4 4 0 0 1 0 7.75\"/></svg>";
    public static final String ICON_CHART = "<svg width=\"15\" height=\"15\" viewBox=\"0 0 24 24\" fill=\"none\" stroke=\"currentColor\" stroke-width=\"2\" stroke-linecap=\"round\" stroke-linejoin=\"round\"><line x1=\"18\" y1=\"20\" x2=\"18\" y2=\"10\"/><line x1=\"12\" y1=\"20\" x2=\"12\" y2=\"4\"/><line x1=\"6\" y1=\"20\" x2=\"6\" y2=\"14\"/></svg>";
    public static final String ICON_HISTORY = "<svg width=\"15\" height=\"15\" viewBox=\"0 0 24 24\" fill=\"none\" stroke=\"currentColor\" stroke-width=\"2\" stroke-linecap=\"round\" stroke-linejoin=\"round\"><circle cx=\"12\" cy=\"12\" r=\"10\"/><polyline points=\"12 6 12 12 16 14\"/></svg>";
    public static final String ICON_TAG = "<svg width=\"15\" height=\"15\" viewBox=\"0 0 24 24\" fill=\"none\" stroke=\"currentColor\" stroke-width=\"2\" stroke-linecap=\"round\" stroke-linejoin=\"round\"><path d=\"M20.59 13.41l-7.17 7.17a2 2 0 0 1-2.83 0L2 12V2h10l8.59 8.59a2 2 0 0 1 0 2.82z\"/><line x1=\"7\" y1=\"7\" x2=\"7.01\" y2=\"7\"/></svg>";
    public static final String ICON_CHECK = "<svg width=\"14\" height=\"14\" viewBox=\"0 0 24 24\" fill=\"none\" stroke=\"currentColor\" stroke-width=\"2.5\" stroke-linecap=\"round\" stroke-linejoin=\"round\"><polyline points=\"20 6 9 17 4 12\"/></svg>";

    // full page = header + side menu + content
    public static String page(String menuTitle, String subtitle, String[][] menu, String active, String content, String msg) {
        StringBuilder sb = new StringBuilder();
        sb.append("<!DOCTYPE html>\n<html lang=\"en\">\n<head>\n");
        sb.append("<meta charset=\"UTF-8\">\n<meta name=\"viewport\" content=\"width=device-width, initial-scale=1.0\">\n");
        sb.append("<title>ResolveX - College Resolution Platform</title>\n");
        sb.append("<link rel=\"stylesheet\" href=\"/style.css\">\n</head>\n<body>\n");

        // top header
        sb.append("<header class=\"header\">\n");
        sb.append("  <div class=\"brand-wrap\">\n");
        sb.append("    <span class=\"brand-title\">Resolve<span class=\"brand-gradient\">X</span></span>\n");
        sb.append("    <span class=\"subtitle-tag\">").append(Web.esc(subtitle)).append("</span>\n");
        sb.append("  </div>\n");
        sb.append("  <a class=\"logout-btn\" href=\"/logout\">Sign Out</a>\n");
        sb.append("</header>\n");

        // layout container
        sb.append("<div class=\"layout\">\n<nav class=\"sidebar\">\n");
        sb.append("  <div class=\"menu-title\">").append(Web.esc(menuTitle)).append("</div>\n");
        for (String[] item : menu) {
            if (item[1].isEmpty()) {   // small section heading
                sb.append("  <div class=\"menu-title\">").append(item[0]).append("</div>\n");
                continue;
            }
            String css = item[1].equals(active) ? "active" : "";
            sb.append("  <a class=\"").append(css).append("\" href=\"").append(item[1]).append("\">")
              .append(getMenuIcon(item[0]))
              .append("<span>").append(item[0]).append("</span></a>\n");
        }
        sb.append("</nav>\n");

        // main content area
        sb.append("<main class=\"content\">\n");
        if (msg != null && !msg.isEmpty()) {
            sb.append("<div class=\"notice\">").append(Web.esc(msg)).append("</div>\n");
        }
        sb.append(content);
        sb.append("\n</main>\n</div>\n</body>\n</html>");
        return sb.toString();
    }

    private static String getMenuIcon(String label) {
        String l = label.toLowerCase();
        if (l.contains("overview") || l.contains("dashboard")) return ICON_GRID + " ";
        if (l.contains("history") || l.contains("audit")) return ICON_HISTORY + " ";
        if (l.contains("student") || l.contains("user") || l.contains("group")) return ICON_USERS + " ";
        if (l.contains("rank") || l.contains("stat")) return ICON_CHART + " ";
        if (l.contains("report") || l.contains("add")) return ICON_PLUS + " ";
        if (l.contains("category") || l.contains("tag")) return ICON_TAG + " ";
        return ICON_LIST + " ";
    }

    // login page
    public static String loginPage(String heading, String error) {
        StringBuilder sb = new StringBuilder();
        sb.append("<!DOCTYPE html>\n<html lang=\"en\">\n<head>\n");
        sb.append("<meta charset=\"UTF-8\">\n<meta name=\"viewport\" content=\"width=device-width, initial-scale=1.0\">\n");
        sb.append("<title>ResolveX Login</title>\n");
        sb.append("<link rel=\"stylesheet\" href=\"/style.css\">\n</head>\n<body class=\"login-body\">\n");
        sb.append("<div class=\"login-box\">\n");
        sb.append("  <div class=\"logo-center\">\n");
        sb.append("    <h1 class=\"brand-title\" style=\"font-size:32px;\">Resolve<span class=\"brand-gradient\">X</span></h1>\n");
        sb.append("    <p class=\"muted\" style=\"margin-top:6px;\">").append(Web.esc(heading)).append("</p>\n");
        sb.append("  </div>\n");
        if (error != null && !error.isEmpty()) {
            sb.append("  <div class=\"error\">").append(Web.esc(error)).append("</div>\n");
        }
        sb.append("  <form method=\"post\" action=\"/login\">\n");
        sb.append("    <label>Username</label>\n");
        sb.append("    <input type=\"text\" name=\"username\" placeholder=\"e.g. admin or cse1\" required autofocus>\n");
        sb.append("    <label>Password</label>\n");
        sb.append("    <input type=\"password\" name=\"password\" placeholder=\"••••••••\" required>\n");
        sb.append("    <button type=\"submit\">Sign In</button>\n");
        sb.append("  </form>\n");
        sb.append("</div>\n</body>\n</html>");
        return sb.toString();
    }

    // a summary box (number + label)
    public static String card(int number, String label) {
        return "<div class=\"card\"><div class=\"number\">" + number + "</div><div class=\"label\">"
                + Web.esc(label) + "</div></div>\n";
    }

    // coloured badge for the status
    public static String badge(Complaint c) {
        String css;
        String text = c.getStatus();
        if (c.isSafetyCritical() && c.isOpen()) {
            css = "critical";
            text = "Critical";
        } else if (c.getStatus().equals("Resolved") || c.getStatus().equals("Closed")) {
            css = "done";
        } else if (c.getStatus().equals("Submitted")) {
            css = "new";
        } else if (c.getStatus().equals("Rejected")) {
            css = "grey";
        } else {
            css = "progress";
        }
        return "<span class=\"badge " + css + "\">" + Web.esc(text) + "</span>";
    }

    // distinct coloured badge for section
    public static String sectionBadge(String section) {
        String sec = section == null ? "" : section.toUpperCase().trim();
        String css = "badge-sec " + sec;
        return "<span class=\"" + css + "\">" + Web.esc(sec) + "</span>";
    }

    // distinct coloured badge for category
    public static String categoryBadge(String category) {
        String cat = category == null ? "Other" : category.trim();
        String catClass = cat.replace(" ", "_").replace("/", "_");
        return "<span class=\"badge-cat " + Web.esc(catClass) + "\">" + Web.esc(cat) + "</span>";
    }

    // Clean metric pill tags for priority rationale
    public static String priorityTags(Complaint c, PriorityCalculator calc) {
        StringBuilder sb = new StringBuilder("<div style=\"display:flex; flex-wrap:wrap; gap:6px; align-items:center;\">");
        if (c.isSafetyCritical()) {
            sb.append("<span class=\"badge critical\">Safety Critical (Top Priority)</span>");
        }
        sb.append("<span class=\"badge grey\">Severity: ").append(c.getSeverity()).append("/3</span>");
        sb.append("<span class=\"badge grey\">Urgency: ").append(c.getUrgency()).append("/3</span>");
        sb.append("<span class=\"badge grey\">Waiting: ").append(calc.daysWaiting(c)).append("d</span>");
        sb.append("</div>");
        return sb.toString();
    }

    // score number with a bar
    public static String scoreBar(double score, boolean critical) {
        String css = critical ? "bar red" : "bar";
        return "<span class=\"score\">" + Math.round(score) + "%</span><span class=\"" + css
                + "\"><span style=\"width:" + Math.round(score) + "%\"></span></span>";
    }

    // "AIDS (40)" = section and its number of students
    public static String affects(Complaint c, PriorityCalculator calc) {
        return Web.esc(c.getGroupName()) + " (" + calc.eligiblePeople(c) + ")";
    }

    // "12 / 139"
    public static String votes(Complaint c, PriorityCalculator calc) {
        return c.getSupporters().size() + " / " + calc.eligiblePeople(c);
    }
}
