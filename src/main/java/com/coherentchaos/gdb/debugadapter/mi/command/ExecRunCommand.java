package com.coherentchaos.gdb.debugadapter.mi.command;

import java.util.Optional;

public class ExecRunCommand extends Command {
    private ExecRunCommand() {
        super("-exec-run", Optional.empty());
        setIgnoreResponse(true);
    }

    public static ExecRunCommand of() {
        return new ExecRunCommand();
    }
}
