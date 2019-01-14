package com.coherentchaos.gdb.debugadapter.mi.output;

/**
 * result -> variable "=" value
 */
public class Result {
    private String variable;
    private Value value;

    public String getVariable() {
        return variable;
    }

    public void setVariable(String variable) {
        this.variable = variable;
    }

    public Value getValue() {
        return value;
    }

    public void setValue(Value value) {
        this.value = value;
    }

    @Override
    public String toString() {
        StringBuilder buffer = new StringBuilder();
        buffer.append(variable);
        if (value != null) {
            String v = value.toString();
            buffer.append('=');
            if (!v.isEmpty() && (v.charAt(0) == '[' || v.charAt(0) == '{')) {
                buffer.append(v);
            }
            else {
                buffer.append('"').append(v).append('"');
            }
        }
        return buffer.toString();
    }
}
