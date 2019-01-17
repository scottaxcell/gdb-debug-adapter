package com.coherentchaos.gdb.debugadapter.mi.command;

import java.util.Optional;

public class ThreadsInfoCommand extends Command {
    private ThreadsInfoCommand() {
        super("-thread-info", Optional.empty());
        setRequiresResponse(true);
    }

    public static ThreadsInfoCommand of() {
        return new ThreadsInfoCommand();
    }
}
