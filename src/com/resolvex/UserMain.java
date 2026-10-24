package com.resolvex;

import com.resolvex.web.UserServer;

public class UserMain {

    public static void main(String[] args) throws Exception {
        new UserServer().start(8071);
    }
}
