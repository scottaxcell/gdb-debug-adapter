package com.coherentchaos.gdb.debugadapter;

import java.util.Optional;

public class ExecutionContext {
    private Optional<Long> threadId = Optional.empty();
    private Optional<Long> frameId = Optional.empty();

    public ExecutionContext(Optional<Long> threadId) {
        this.threadId = threadId;
    }

    public ExecutionContext(Optional<Long> threadId, Optional<Long> frameId) {
        this.threadId = threadId;
        this.frameId = frameId;
    }

    public Optional<Long> getThreadId() {
        return threadId;
    }

    public Optional<Long> getFrameId() {
        return frameId;
    }
}
