package com.coherentchaos.gdb.debugadapter.mi.command;

import com.coherentchaos.gdb.debugadapter.mi.output.Output;

public class CommandResponse {
    private Output output;

    public CommandResponse(Output output) {
        this.output = output;
    }

    public Output getOutput() {
        return output;
    }
}
