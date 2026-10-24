package com.resolvex.web;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import com.resolvex.model.Account;
import com.sun.net.httpserver.HttpExchange;

/** Remembers who is logged in using a cookie. Admin and User use different cookie names. */
public class Session {

    private String cookieName;
    private Map<String, Account> loggedIn = new HashMap<>();

    public Session(String cookieName) {
        this.cookieName = cookieName;
    }

    // the logged in account of this browser, or null
    public Account current(HttpExchange ex) {
        String token = readToken(ex);
        return token == null ? null : loggedIn.get(token);
    }

    public void login(HttpExchange ex, Account account) {
        String token = UUID.randomUUID().toString();
        loggedIn.put(token, account);
        ex.getResponseHeaders().add("Set-Cookie", cookieName + "=" + token + "; Path=/; HttpOnly");
    }

    public void logout(HttpExchange ex) {
        String token = readToken(ex);
        if (token != null) {
            loggedIn.remove(token);
        }
        ex.getResponseHeaders().add("Set-Cookie", cookieName + "=; Path=/; Max-Age=0");
    }

    // finds our cookie inside the Cookie header
    private String readToken(HttpExchange ex) {
        String header = ex.getRequestHeaders().getFirst("Cookie");
        if (header == null) {
            return null;
        }
        for (String part : header.split(";")) {
            String[] pair = part.trim().split("=", 2);
            if (pair.length == 2 && pair[0].equals(cookieName)) {
                return pair[1];
            }
        }
        return null;
    }
}
