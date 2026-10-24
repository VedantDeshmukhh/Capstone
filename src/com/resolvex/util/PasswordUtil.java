package com.resolvex.util;

import java.security.MessageDigest;

/** Passwords are stored as a SHA-256 hash, never as plain text. */
public class PasswordUtil {

    public static String hash(String username, String password) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] bytes = digest.digest((username + ":" + password).getBytes("UTF-8"));
            StringBuilder sb = new StringBuilder();
            for (byte b : bytes) {
                sb.append(String.format("%02x", b));
            }
            return sb.toString();
        } catch (Exception e) {
            throw new RuntimeException("Could not hash password");
        }
    }
}
