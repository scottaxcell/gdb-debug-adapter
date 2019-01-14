package com.coherentchaos.gdb.debugadapter.mi.command;

import java.awt.event.ComponentListener;

public class ExecReturnCommand extends Command {
    public ExecReturnCommand() {
        super("-exec-run");
    }
}
