package com.resolvex;

import com.resolvex.web.AdminServer;

public class AdminMain {

    public static void main(String[] args) throws Exception {
        new AdminServer().start(8088);
    }
}
