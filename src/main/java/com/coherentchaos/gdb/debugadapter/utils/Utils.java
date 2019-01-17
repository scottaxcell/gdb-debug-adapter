package com.coherentchaos.gdb.debugadapter.utils;

public class Utils {
    public static boolean isDebugEnabled = false;

    public static boolean isIsDebugEnabled() {
        return isDebugEnabled;
    }

    public static final String GDB_PATH = "/usr/bin/gdb";

    public static void out(Object o) {
        System.out.println("INFO: " + o);
    }

    public static void debug(Object o) {
        if (isIsDebugEnabled())
            System.out.println("DEBUG: " + o);
    }

    public static void error(Object o) {
        System.out.println("ERROR: " + o);
    }

    public static Long createUniqueStackFrameId(Long threadId, Long stackFrameId) {
        return Cantor.pair(threadId, stackFrameId);
    }

    public static long[] computeThreadAndStackFrameIds(Long uniqueStackFrameId) {
        return Cantor.depair(uniqueStackFrameId);
    }
}
