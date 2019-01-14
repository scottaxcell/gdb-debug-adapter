package com.coherentchaos.gdb.debugadapter;

import java.nio.file.Path;
import java.nio.file.Paths;

public class Target {
    private Path path;

    public Target(String target) {
        this.path = Paths.get(target);
    }

    public Target(Path path) {
        this.path = path;
    }

    public Path getPath() {
        return path;
    }

    public void setPath(Path path) {
        this.path = path;
    }
}
