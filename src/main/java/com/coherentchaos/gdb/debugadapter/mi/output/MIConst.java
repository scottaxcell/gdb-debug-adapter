package com.coherentchaos.gdb.debugadapter.mi.output;

/**
 * const -> c-string
 */
public class MIConst extends Value {
    // TODO write translator to create a human readable string from a cstring
    private String cString;

    public String getcString() {
        return cString;
    }

    public void setcString(String cString) {
        this.cString = cString;
    }
}
