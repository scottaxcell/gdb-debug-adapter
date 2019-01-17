package com.coherentchaos.gdb.debugadapter.mi.command;

import com.coherentchaos.gdb.debugadapter.ExecutionContext;

import java.util.Optional;

public class ExecStepCommand extends Command {
    private ExecStepCommand(ExecutionContext executionContext) {
        super("-exec-step", Optional.ofNullable(executionContext));
        setIgnoreResponse(true);
    }

    public static ExecStepCommand of(ExecutionContext executionContext) {
        return new ExecStepCommand(executionContext);
    }
}
