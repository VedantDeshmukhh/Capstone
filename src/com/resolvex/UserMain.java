package com.resolvex;

import com.resolvex.web.UserServer;

public class UserMain {

    public static void main(String[] args) throws Exception {
        int port = 8081;
        if (args != null && args.length > 0) {
            try {
                port = Integer.parseInt(args[0]);
            } catch (Exception ignored) {}
        }
        new UserServer().start(port);
        // Keep the JVM alive when started via javaw in background
        Thread.currentThread().join();
    }
}
