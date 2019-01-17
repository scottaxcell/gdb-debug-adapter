package com.coherentchaos.gdb.debugadapter.mi.command;

import com.coherentchaos.gdb.debugadapter.ExecutionContext;

import java.util.Optional;

public class ExecNextCommand extends Command {
    // TODO add --reverse support
    private ExecNextCommand(ExecutionContext executionContext) {
        super("-exec-next", Optional.ofNullable(executionContext));
        setIgnoreResponse(true);
    }

    public static ExecNextCommand of(ExecutionContext executionContext) {
        return new ExecNextCommand(executionContext);
    }
}
