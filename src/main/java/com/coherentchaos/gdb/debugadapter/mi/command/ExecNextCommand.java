package com.coherentchaos.gdb.debugadapter.mi.command;

public class ExecNextCommand extends Command {
    // TODO add --reverse support
    public ExecNextCommand() {
        super("-exec-next");
    }
}
