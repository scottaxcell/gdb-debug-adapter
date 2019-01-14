package com.coherentchaos.gdb.debugadapter.mi.output;

import java.util.HashMap;
import java.util.Map;

/**
 * tuple -> "{}" | "{" result ( "," result )* "}"
 */
public class Tuple extends Value {
    final private static Value[] NULL_VALUES = new Value[0];
    final private static Result[] NULL_RESULTS = new Result[0];
    private Value[] values = NULL_VALUES;
    private Result[] results = NULL_RESULTS;
    private Map<String, Value> fields;

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

    public Value getFieldValue(String name) {
        if (fields == null) {
            fields = new HashMap<>();
            for (Result result : results)
                fields.put(result.getVariable(), result.getValue());
        }
        return fields.get(name);
    }
}
