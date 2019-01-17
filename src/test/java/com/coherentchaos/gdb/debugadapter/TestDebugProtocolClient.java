package com.coherentchaos.gdb.debugadapter;

import org.eclipse.lsp4j.debug.ExitedEventArguments;
import org.eclipse.lsp4j.debug.StoppedEventArguments;
import org.eclipse.lsp4j.debug.services.IDebugProtocolClient;

public class TestDebugProtocolClient implements IDebugProtocolClient {
    private boolean initializedExercised;
    private boolean stopped;
    private long exitReturnCode = Long.MAX_VALUE;
    private StoppedEventArguments stoppedEventArguments;

    boolean isInitializedExercised() {
        return initializedExercised;
    }

    @Override
    public void initialized() {
        initializedExercised = true;
    }

    long exitedCleanly() {
        return exitReturnCode;
    }

    @Override
    public void exited(ExitedEventArguments args) {
        exitReturnCode = args.getExitCode();
    }

    boolean isStopped() {
        return stopped;
    }

    StoppedEventArguments getStoppedEventArguments() {
        return stoppedEventArguments;
    }

    @Override
    public void stopped(StoppedEventArguments args) {
        stopped = true;
        stoppedEventArguments = args;
    }
}

