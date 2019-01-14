package com.coherentchaos.gdb.debugadapter.mi.command;

public class CommandFactory {

    public BreakInsertCommand createBreakInsert(String location) {
        BreakInsertCommand breakInsertCommand = new BreakInsertCommand(location);
        breakInsertCommand.setRequiresResponse(true);
        return breakInsertCommand;
    }

    public BreakDeleteCommand createBreakDelete() {
        BreakDeleteCommand breakDeleteCommand = new BreakDeleteCommand();
        breakDeleteCommand.setIgnoreResponse(true);
        return breakDeleteCommand;
    }

    public GDBExitCommand createGDBExit() {
        return new GDBExitCommand();
    }

    public ExecRunCommand createExecRun() {
        ExecRunCommand execRunCommand = new ExecRunCommand();
        execRunCommand.setIgnoreResponse(true);
        return execRunCommand;
    }

    public ExecContinueCommand createExecContinue() {
        ExecContinueCommand execContinueCommand = new ExecContinueCommand();
        execContinueCommand.setRequiresResponse(true);
        return execContinueCommand;
    }

    public ExecNextCommand createExecNext() {
        ExecNextCommand execNextCommand = new ExecNextCommand();
        execNextCommand.setIgnoreResponse(true);
        return execNextCommand;
    }

    public ExecStepCommand createExecStep() {
        ExecStepCommand execStepCommand = new ExecStepCommand();
        execStepCommand.setIgnoreResponse(true);
        return execStepCommand;
    }

    public ExecReturnCommand createExecReturn() {
        ExecReturnCommand execReturnCommand = new ExecReturnCommand();
        execReturnCommand.setIgnoreResponse(true);
        return execReturnCommand;
    }

    public ThreadsInfoCommand createThreadsInfo() {
        ThreadsInfoCommand threadsInfoCommand = new ThreadsInfoCommand();
        threadsInfoCommand.setRequiresResponse(true);
        return threadsInfoCommand;
    }

    public StackListFramesCommand createStackListFrames() {
        StackListFramesCommand stackListFramesCommand = new StackListFramesCommand();
        stackListFramesCommand.setRequiresResponse(true);
        return stackListFramesCommand;
    }

    public ThreadSelectCommand createThreadSelect(Long threadNum) {
        ThreadSelectCommand threadSelectCommand = new ThreadSelectCommand(String.valueOf(threadNum));
        threadSelectCommand.setIgnoreResponse(true);
        return threadSelectCommand;
    }

    public StackSelectFrameCommand createStackSelectFrame(Long frameId) {
        StackSelectFrameCommand stackSelectFrameCommand = new StackSelectFrameCommand(String.valueOf(frameId));
        stackSelectFrameCommand.setIgnoreResponse(true);
        return stackSelectFrameCommand;
    }

    public StackListLocalsCommand createStackListLocals() {
        StackListLocalsCommand stackListLocalsCommand = new StackListLocalsCommand();
        stackListLocalsCommand.setRequiresResponse(true);
        return stackListLocalsCommand;
    }
}
