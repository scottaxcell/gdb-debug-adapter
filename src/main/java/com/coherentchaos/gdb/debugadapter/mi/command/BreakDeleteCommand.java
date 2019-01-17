package com.coherentchaos.gdb.debugadapter.mi.command;

import java.util.Optional;

public class BreakDeleteCommand extends com.coherentchaos.gdb.debugadapter.mi.command.Command {
    private BreakDeleteCommand() {
        super("-break-delete", Optional.empty());
        setIgnoreResponse(true);
    }

    public static BreakDeleteCommand of() {
        return new BreakDeleteCommand();
    }
}
