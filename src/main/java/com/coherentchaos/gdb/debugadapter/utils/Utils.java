package com.coherentchaos.gdb.debugadapter.utils;

public class Utils {
    public static final String GDB_PATH = "/usr/bin/gdb";

    public static void out(Object o) {
        System.out.println("INFO: " + o);
    }

    public static void debug(Object o) {
        System.out.println("DEBUG: " + o);
    }

    public static void error(Object o) {
        System.out.println("ERROR: " + o);
    }
}
