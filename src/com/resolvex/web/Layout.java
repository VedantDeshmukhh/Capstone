package com.resolvex.web;

import com.resolvex.model.Complaint;
import com.resolvex.service.PriorityCalculator;

/** Builds the common parts of every page: header, side menu, badges, score bar. */
public class Layout {

    // full page = header + side menu + content
    // menu is a list of {label, link}
    public static String page(String menuTitle, String subtitle, String[][] menu, String active, String content, String msg) {
        StringBuilder sb = new StringBuilder();
        sb.append("<!DOCTYPE html>\n<html>\n<head>\n");
        sb.append("<meta charset=\"UTF-8\">\n<title>ResolveX</title>\n");
        sb.append("<link rel=\"stylesheet\" href=\"/style.css\">\n</head>\n<body>\n");

        // top header
        sb.append("<div class=\"header\">\n");
        sb.append("  <div><span class=\"logo\">ResolveX</span><span class=\"subtitle\">")
          .append(Web.esc(subtitle)).append("</span></div>\n");
        sb.append("  <a class=\"logout\" href=\"/logout\">Logout</a>\n</div>\n");

        // side menu
        sb.append("<div class=\"layout\">\n<div class=\"sidebar\">\n");
        sb.append("  <div class=\"menu-title\">").append(menuTitle).append("</div>\n");
        for (String[] item : menu) {
            if (item[1].isEmpty()) {   // an empty link means a small heading
                sb.append("  <div class=\"menu-title\">").append(item[0]).append("</div>\n");
                continue;
            }
            String css = item[1].equals(active) ? "active" : "";
            sb.append("  <a class=\"").append(css).append("\" href=\"").append(item[1]).append("\">")
              .append(item[0]).append("</a>\n");
        }
        sb.append("</div>\n");

        // main content
        sb.append("<div class=\"content\">\n");
        if (msg != null && !msg.isEmpty()) {
            sb.append("<div class=\"notice\">").append(Web.esc(msg)).append("</div>\n");
        }
        sb.append(content);
        sb.append("\n</div>\n</div>\n</body>\n</html>");
        return sb.toString();
    }

    // login page (same for admin and user)
    public static String loginPage(String heading, String error) {
        StringBuilder sb = new StringBuilder();
        sb.append("<!DOCTYPE html>\n<html>\n<head>\n<meta charset=\"UTF-8\">\n<title>ResolveX Login</title>\n");
        sb.append("<link rel=\"stylesheet\" href=\"/style.css\">\n</head>\n<body class=\"login-body\">\n");
        sb.append("<div class=\"login-box\">\n");
        sb.append("  <h1>ResolveX</h1>\n  <p class=\"muted\">").append(heading).append("</p>\n");
        if (error != null && !error.isEmpty()) {
            sb.append("  <div class=\"error\">").append(Web.esc(error)).append("</div>\n");
        }
        sb.append("  <form method=\"post\" action=\"/login\">\n");
        sb.append("    <label>Username</label>\n    <input type=\"text\" name=\"username\" required>\n");
        sb.append("    <label>Password</label>\n    <input type=\"password\" name=\"password\" required>\n");
        sb.append("    <button type=\"submit\">Login</button>\n  </form>\n</div>\n</body>\n</html>");
        return sb.toString();
    }

    // a small summary box (number + label)
    public static String card(int number, String label) {
        return "<div class=\"card\"><div class=\"number\">" + number + "</div><div class=\"label\">"
                + label + "</div></div>\n";
    }

    // coloured label for the status
    public static String badge(Complaint c) {
        String css;
        String text = c.getStatus();
        if (c.isSafetyCritical() && c.isOpen()) {
            css = "critical";
            text = "Safety Critical";
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

    // score number with a small bar
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
