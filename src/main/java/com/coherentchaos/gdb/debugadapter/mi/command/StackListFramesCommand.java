package com.coherentchaos.gdb.debugadapter.mi.command;

import com.coherentchaos.gdb.debugadapter.ExecutionContext;

public class StackListFramesCommand extends Command {
    private StackListFramesCommand(ExecutionContext executionContext) {
        super("-stack-list-frames", java.util.Optional.ofNullable(executionContext));
        setRequiresResponse(true);
    }

    public static StackListFramesCommand of(ExecutionContext executionContext) {
        return new StackListFramesCommand(executionContext);
    }
}
