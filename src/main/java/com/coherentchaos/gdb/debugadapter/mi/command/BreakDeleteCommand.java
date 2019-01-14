package com.coherentchaos.gdb.debugadapter.mi.command;

public class BreakDeleteCommand extends com.coherentchaos.gdb.debugadapter.mi.command.Command {
    // TODO add support for deleting single breakpoints
    public BreakDeleteCommand() {
        super("-break-delete");
    }
}
