package com.coherentchaos.gdb.debugadapter;

import java.util.Optional;

public class ExecutionContext {
    private Optional<Long> threadId;
    private Optional<Long> frameId;

    private ExecutionContext(Optional<Long> threadId, Optional<Long> frameId) {
        this.threadId = threadId;
        this.frameId = frameId;
    }

    public static ExecutionContext of(Long threadId) {
        return new ExecutionContext(Optional.ofNullable(threadId), Optional.empty());
    }

    public static ExecutionContext of(Long threadId, Long frameId) {
        return new ExecutionContext(Optional.ofNullable(threadId), Optional.ofNullable(frameId));
    }

    public Optional<Long> getThreadId() {
        return threadId;
    }

    public Optional<Long> getFrameId() {
        return frameId;
    }
}
