package com.coherentchaos.gdb.debugadapter.mi.event;

import org.eclipse.lsp4j.debug.ContinuedEventArguments;

public class RunningEvent extends Event {
    private ContinuedEventArguments args;

    public ContinuedEventArguments getArgs() {
        return args;
    }

}
