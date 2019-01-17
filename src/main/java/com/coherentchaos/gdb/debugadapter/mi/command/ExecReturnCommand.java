package com.coherentchaos.gdb.debugadapter.mi.command;

import com.coherentchaos.gdb.debugadapter.ExecutionContext;

public class ExecReturnCommand extends Command {
    private ExecReturnCommand(ExecutionContext executionContext) {
        super("-exec-run", java.util.Optional.ofNullable(executionContext));
        setIgnoreResponse(true);
    }

    public static ExecReturnCommand of(ExecutionContext executionContext) {
        return new ExecReturnCommand(executionContext);
    }
}
