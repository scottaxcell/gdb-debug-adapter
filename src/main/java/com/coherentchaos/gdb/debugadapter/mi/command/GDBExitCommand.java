package com.coherentchaos.gdb.debugadapter.mi.command;

import java.util.Optional;

public class GDBExitCommand extends Command {
    private GDBExitCommand() {
        super("-gdb-exit", Optional.empty());
    }

    public static GDBExitCommand of() {
        return new GDBExitCommand();
    }
}
