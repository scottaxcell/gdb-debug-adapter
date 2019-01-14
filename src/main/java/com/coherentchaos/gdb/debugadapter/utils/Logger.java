package com.coherentchaos.gdb.debugadapter.utils;

import java.io.IOException;
import java.util.logging.FileHandler;
import java.util.logging.SimpleFormatter;

public class Logger {
    private static java.util.logging.Logger INSTANCE;
    private static String LOGGER_NAME = "Boris Logger";
    private static String LOG_FILE = "boris.log";

    private Logger() {
        throw new AssertionError("Do not call private constructor");
    }

    public static java.util.logging.Logger getInstance() {
        if (INSTANCE == null) {
            try {
                FileHandler fh = new FileHandler(LOG_FILE);
                fh.setFormatter(new SimpleFormatter());
                INSTANCE = java.util.logging.Logger.getLogger(LOGGER_NAME);
                INSTANCE.addHandler(new FileHandler(LOG_FILE));
                INSTANCE.info(LOGGER_NAME + " started");
            }
            catch (IOException e) {
                e.printStackTrace();
            }
        }
        return INSTANCE;
    }
}
