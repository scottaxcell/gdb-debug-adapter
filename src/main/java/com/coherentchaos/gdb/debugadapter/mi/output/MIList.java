package com.coherentchaos.gdb.debugadapter.mi.output;

/**
 * list -> "[]" | "[" value ( "," value )* "]" | "[" result ( "," result )* "]"
 */
public class MIList extends Value {
    Value[] values;
    Result[] results;

    public Value[] getValues() {
        return values;
    }

    public void setValues(Value[] values) {
        this.values = values;
    }

    public Result[] getResults() {
        return results;
    }

    public void setResults(Result[] results) {
        this.results = results;
    }

    @Override
    public String toString() {
        StringBuilder buffer = new StringBuilder();
        buffer.append('[');
        for (int i = 0; i < results.length; i++) {
            if (i != 0) {
                buffer.append(',');
            }
            buffer.append(results[i].toString());
        }
        for (int i = 0; i < values.length; i++) {
            if (i != 0) {
                buffer.append(',');
            }
            buffer.append(values[i].toString());
        }
        buffer.append(']');
        return buffer.toString();
    }
}
