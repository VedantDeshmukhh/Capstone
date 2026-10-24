package com.resolvex;

import com.resolvex.web.AdminServer;

public class AdminMain {

    public static void main(String[] args) throws Exception {
        int port = 8080;
        if (args != null && args.length > 0) {
            try {
                port = Integer.parseInt(args[0]);
            } catch (Exception ignored) {}
        }
        new AdminServer().start(port);
    }
}
