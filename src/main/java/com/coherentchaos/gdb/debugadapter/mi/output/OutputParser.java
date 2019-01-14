package com.coherentchaos.gdb.debugadapter.mi.output;

import com.coherentchaos.gdb.debugadapter.mi.record.ResultRecord;
import org.eclipse.lsp4j.debug.Thread;
import org.eclipse.lsp4j.debug.*;

import java.util.ArrayList;
import java.util.List;

public class OutputParser {
    public static Breakpoint parseBreakpointsResponse(Output output) {
        ResultRecord resultRecord = output.getResultRecord();

        if (resultRecord.getResultClass() == ResultRecord.ResultClass.DONE) {
            Result[] results = resultRecord.getResults();
            for (Result result : results) {
                if ("bkpt".equals(result.getVariable())) {
                    Tuple tuple = (Tuple) result.getValue();
                    MIConst file = (MIConst) tuple.getFieldValue("file");
                    MIConst fullname = (MIConst) tuple.getFieldValue("fullname");
                    MIConst line = (MIConst) tuple.getFieldValue("line");
                    Source source = new Source();
                    source.setName(file.getcString());
                    source.setPath(fullname.getcString());
                    Breakpoint breakpoint = new Breakpoint();
                    breakpoint.setSource(source);
                    breakpoint.setLine(Long.parseLong(line.getcString()));
                    return breakpoint;
                }
            }
        }

        return null;
    }

    public static ThreadsResponse parseThreadsResponse(Output output) {
        ThreadsResponse response = new ThreadsResponse();
        List<Thread> threads = new ArrayList<>();

        ResultRecord resultRecord = output.getResultRecord();
        if (resultRecord.getResultClass() == ResultRecord.ResultClass.DONE) {
            Result[] results = resultRecord.getResults();
            for (Result result : results) {
                if ("threads".equals(result.getVariable())) {
                    Value value = result.getValue();
                    if (value instanceof MIList) {
                        MIList list = (MIList) value;
                        Value[] values = list.getValues();
                        for (Value v : values) {
                            if (v instanceof Tuple) {
                                org.eclipse.lsp4j.debug.Thread thread = parseThread((Tuple) v);
                                threads.add(thread);
                            }
                        }
                    }
                }
            }
        }

        response.setThreads(threads.toArray(new org.eclipse.lsp4j.debug.Thread[threads.size()]));

        return response;
    }

    private static Thread parseThread(Tuple tuple) {
        org.eclipse.lsp4j.debug.Thread thread = new org.eclipse.lsp4j.debug.Thread();
        Result[] tupleResults = tuple.getResults();
        for (Result tr : tupleResults) {
            String var = tr.getVariable();
            if ("id".equals(var)) {
                Value val = tr.getValue();
                if (val instanceof MIConst)
                    thread.setId(Long.valueOf(((MIConst) val).getcString().trim()));
            }
            else if ("name".equals(var)) {
                Value val = tr.getValue();
                if (val instanceof MIConst)
                    thread.setName(((MIConst) val).getcString().trim());
            }
        }
        return thread;
    }

    public static StackTraceResponse parseStackListFramesResponse(Output output) {
        StackTraceResponse response = new StackTraceResponse();
        List<StackFrame> stackFrames = new ArrayList<>();

        ResultRecord resultRecord = output.getResultRecord();
        if (resultRecord.getResultClass() == ResultRecord.ResultClass.DONE) {
            Result[] results = resultRecord.getResults();
            for (Result result : results) {
                Value val = result.getValue();
                if (val instanceof MIList)
                    parseStack((MIList) val, stackFrames);
            }
        }

        response.setStackFrames(stackFrames.toArray(new StackFrame[stackFrames.size()]));

        return response;
    }

    private static void parseStack(MIList miList, List<StackFrame> aList) {
        Result[] results = miList.getResults();
        for (Result result : results) {
            String var = result.getVariable();
            if ("frame".equals(var)) {
                Value value = result.getValue();
                if (value instanceof Tuple) {
                    aList.add(parseFrame((Tuple) value));
                }
            }
        }
    }

    private static StackFrame parseFrame(Tuple tuple) {
        StackFrame stackFrame = new StackFrame();
        stackFrame.setSource(new Source());
        Result[] results = tuple.getResults();
        for (Result result : results) {
            String var = result.getVariable();
            Value value = result.getValue();
            String str = "";
            if (value != null && value instanceof MIConst) {
                str = ((MIConst) value).getcString();
            }

            if (var.equals("level")) { //$NON-NLS-1$
                stackFrame.setId(Long.valueOf(str.trim()));
            }
            else if (var.equals("func")) { //$NON-NLS-1$
                if (str != null) {
                    stackFrame.setName(str.trim() + "()");
                }
            }
            else if (var.equals("file")) { //$NON-NLS-1$
                stackFrame.getSource().setName(str.trim());
            }
            else if (var.equals("fullname")) { //$NON-NLS-1$
                stackFrame.getSource().setPath(str.trim());
            }
            else if (var.equals("line")) { //$NON-NLS-1$
                stackFrame.setLine(Long.valueOf(str.trim()));
            }
        }

        return stackFrame;
    }

    public static VariablesResponse parseVariablesResponse(Output output) {
        VariablesResponse response = new VariablesResponse();
        MIArg[] locals = new MIArg[0];

        ResultRecord resultRecord = output.getResultRecord();
        if (resultRecord.getResultClass() == ResultRecord.ResultClass.DONE) {
            Result[] results = resultRecord.getResults();
            for (Result result : results) {
                String variable = result.getVariable();
                if (variable.equals("locals")) {
                    Value value = result.getValue();
                    if (value instanceof MIList) {
                        locals = MIArg.getMIArgs((MIList) value);
                    }
                    else if (value instanceof Tuple) {
                        locals = MIArg.getMIArgs((Tuple) value);
                    }
                }
            }
        }

        List<Variable> variables = new ArrayList<>();
        for (MIArg local : locals) {
            Variable v = new Variable();
            v.setName(local.getName());
            v.setValue(local.getValue());
            variables.add(v);
        }

        response.setVariables(variables.toArray(new Variable[variables.size()]));

        return response;
    }

}
