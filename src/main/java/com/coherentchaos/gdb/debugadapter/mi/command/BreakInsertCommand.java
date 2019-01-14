package com.coherentchaos.gdb.debugadapter.mi.command;

import java.util.ArrayList;
import java.util.List;

public class BreakInsertCommand extends Command {
    private String location;

    public BreakInsertCommand(String location) {
        super("-break-insert");
        this.location = location;
        setParameters();
    }

    public String getLocation() {
        return location;
    }

    public void setLocation(String location) {
        this.location = location;
    }

    public void setParameters() {
        List<String> parameters = new ArrayList<>();
        parameters.add(getLocation());
        setParameters(parameters);
    }
}
