package com.coherentchaos.gdb.debugadapter.mi.event;

import com.coherentchaos.gdb.debugadapter.mi.output.MIConst;
import com.coherentchaos.gdb.debugadapter.mi.output.Result;
import com.coherentchaos.gdb.debugadapter.mi.output.Value;
import org.eclipse.lsp4j.debug.StoppedEventArguments;
import org.eclipse.lsp4j.debug.StoppedEventArgumentsReason;

public class StoppedEvent extends Event {
    private StoppedEventArguments args;

    public StoppedEvent() {
        args = new StoppedEventArguments();
        args.setReason(StoppedEventArgumentsReason.STEP);
    }

    public static StoppedEvent parse(Result[] results) {
        StoppedEvent event = new StoppedEvent();

        for (Result result : results) {
            String variable = result.getVariable();
            Value value = result.getValue();
            String valueStr = "";
            if (value != null && value instanceof MIConst) {
                valueStr = ((MIConst) value).getcString();
            }

            if ("thread-id".equals(variable)) {
                event.getArgs().setThreadId(Long.valueOf(valueStr));
            }
            else if ("stopped-threads".equals(variable)) {
                if ("all".equals(valueStr)) {
                    event.getArgs().setAllThreadsStopped(true);
                }
            }
        }
        return event;
    }

    public StoppedEventArguments getArgs() {
        return args;
    }
}
