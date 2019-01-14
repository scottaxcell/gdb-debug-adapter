package com.coherentchaos.gdb.debugadapter.mi.record;

/**
 * stream-record -> console-stream-output | target-stream-output | log-stream-output
 */
public class StreamRecord extends OutOfBandRecord {
    public static char CONSOLE_OUTPUT_PREFIX = '~';
    public static char TARGET_OUTPUT_PREFIX = '@';
    public static char LOG_OUTPUT_PREFIX = '&';

    private String cString;

    public String getcString() {
        return cString;
    }

    public void setcString(String cString) {
        this.cString = cString;
    }
}
