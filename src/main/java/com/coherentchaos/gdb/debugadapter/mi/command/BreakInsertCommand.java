package com.coherentchaos.gdb.debugadapter.mi.command;

import java.util.Optional;

public class BreakInsertCommand extends Command {
    private BreakInsertCommand(String location) {
        super("-break-insert", Optional.empty(), new String[]{location});
        setRequiresResponse(true);
    }

    public static BreakInsertCommand of (String location) {
        return new BreakInsertCommand(location);
    }
}
