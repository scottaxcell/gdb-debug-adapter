package com.coherentchaos.gdb.debugadapter.mi.command;

import com.coherentchaos.gdb.debugadapter.ExecutionContext;

import java.util.Optional;

public class StackListLocalsCommand extends Command {
    private StackListLocalsCommand(ExecutionContext executionContext) {
        super("-stack-list-locals", Optional.ofNullable(executionContext), new String[]{"--all-values"});
        setRequiresResponse(true);
    }

    public static StackListLocalsCommand of(ExecutionContext executionContext) {
        return new StackListLocalsCommand(executionContext);
    }
}
