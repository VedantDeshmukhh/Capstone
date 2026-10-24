package com.resolvex.web;

import java.io.IOException;
import java.io.OutputStream;
import java.net.URI;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;

import com.sun.net.httpserver.HttpExchange;

/** Small helper methods used by both servers (send a page, read a form, redirect...). */
public class Web {

    // make text safe to show inside HTML
    public static String esc(String s) {
        if (s == null) {
            return "";
        }
        return s.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;").replace("\"", "&quot;");
    }

    // the data files use | as separator, so remove it (and new lines) from typed text
    public static String clean(String s) {
        if (s == null) {
            return "";
        }
        return s.replace("|", "/").replace("\r", " ").replace("\n", " ").trim();
    }

    // text to number, with a default value if it is not a number
    public static int number(String s, int defaultValue) {
        try {
            return Integer.parseInt(s.trim());
        } catch (Exception e) {
            return defaultValue;
        }
    }

    public static String encode(String s) {
        return URLEncoder.encode(s, StandardCharsets.UTF_8);
    }

    // send any bytes back to the browser
    public static void send(HttpExchange ex, int code, String type, byte[] data) throws IOException {
        ex.getResponseHeaders().set("Content-Type", type);
        ex.sendResponseHeaders(code, data.length);
        try (OutputStream out = ex.getResponseBody()) {
            out.write(data);
        }
    }

    public static void html(HttpExchange ex, String page) throws IOException {
        send(ex, 200, "text/html; charset=UTF-8", page.getBytes(StandardCharsets.UTF_8));
    }

    // send the browser to another page
    public static void redirect(HttpExchange ex, String url) throws IOException {
        ex.getResponseHeaders().set("Location", url);
        ex.sendResponseHeaders(302, -1);
        ex.close();
    }

    // redirect and show a message on the next page
    public static void redirect(HttpExchange ex, String url, String message) throws IOException {
        String join = url.contains("?") ? "&" : "?";
        redirect(ex, url + join + "msg=" + encode(message));
    }

    // values after ? in the address (example: id=1001)
    public static Map<String, String> query(HttpExchange ex) {
        URI uri = ex.getRequestURI();
        return parse(uri.getRawQuery());
    }

    // values sent by a form
    public static Map<String, String> form(HttpExchange ex) throws IOException {
        String body = new String(ex.getRequestBody().readAllBytes(), StandardCharsets.UTF_8);
        return parse(body);
    }

    private static Map<String, String> parse(String text) {
        Map<String, String> map = new HashMap<>();
        if (text == null || text.isEmpty()) {
            return map;
        }
        for (String pair : text.split("&")) {
            String[] part = pair.split("=", 2);
            String key = URLDecoder.decode(part[0], StandardCharsets.UTF_8);
            String value = part.length > 1 ? URLDecoder.decode(part[1], StandardCharsets.UTF_8) : "";
            map.put(key, value);
        }
        return map;
    }

    // sends web/style.css
    public static void css(HttpExchange ex) throws IOException {
        byte[] data = Files.readAllBytes(Paths.get("web", "style.css"));
        send(ex, 200, "text/css; charset=UTF-8", data);
    }

    // opens the browser automatically across platforms (Windows cmd start fallback)
    public static void openBrowser(String url) {
        try {
            if (java.awt.Desktop.isDesktopSupported() && java.awt.Desktop.getDesktop().isSupported(java.awt.Desktop.Action.BROWSE)) {
                java.awt.Desktop.getDesktop().browse(new URI(url));
                return;
            }
        } catch (Exception ignored) {}
        try {
            String os = System.getProperty("os.name", "").toLowerCase();
            if (os.contains("win")) {
                new ProcessBuilder("cmd", "/c", "start", url).start();
            } else if (os.contains("mac")) {
                new ProcessBuilder("open", url).start();
            } else {
                new ProcessBuilder("xdg-open", url).start();
            }
        } catch (Exception ignored) {}
    }
}
