package com.coherentchaos.gdb.debugadapter.mi.command;

import java.util.Arrays;

public class ThreadSelectCommand extends Command {
    public ThreadSelectCommand(String threadNum) {
        super("-thread-select", Arrays.asList(new String[]{threadNum}));
    }
}
