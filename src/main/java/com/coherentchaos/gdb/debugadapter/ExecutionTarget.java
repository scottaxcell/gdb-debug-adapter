package com.coherentchaos.gdb.debugadapter;

import java.nio.file.Path;
import java.nio.file.Paths;

public class ExecutionTarget {
    private Path path;

    public ExecutionTarget(String target) {
        this.path = Paths.get(target);
    }

    public ExecutionTarget(Path path) {
        this.path = path;
    }

    public Path getPath() {
        return path;
    }

    public void setPath(Path path) {
        this.path = path;
    }
}
