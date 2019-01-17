package com.coherentchaos.gdb.debugadapter.mi.command;

import com.coherentchaos.gdb.debugadapter.ExecutionContext;

public class ExecContinueCommand extends Command {
    private ExecContinueCommand(ExecutionContext executionContext, boolean allThreads) {
        super("-exec-continue", java.util.Optional.ofNullable(executionContext));
        setRequiresResponse(true);

        if (allThreads)
            setParameters(new String[]{"--all"});
    }

    public static ExecContinueCommand of(ExecutionContext executionContext) {
        return new ExecContinueCommand(executionContext, false);
    }

    public static ExecContinueCommand of(ExecutionContext executionContext, boolean allThreads) {
        return new ExecContinueCommand(executionContext, allThreads);
    }
}
