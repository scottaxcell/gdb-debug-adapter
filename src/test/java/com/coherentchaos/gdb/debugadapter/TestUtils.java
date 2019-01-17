package com.coherentchaos.gdb.debugadapter;

import java.io.File;
import java.io.IOException;

public class TestUtils {
    private static final String[] MAKE_CMD_LINE = {"make", "-f", "makefile"};

    public static void compileExecutionTarget(String dir) {
        try {
            ProcessBuilder processBuilder = new ProcessBuilder(MAKE_CMD_LINE);
            processBuilder.directory(new File(dir));
            processBuilder.start();
        }
        catch (IOException e) {
            e.printStackTrace();
        }
    }
}
