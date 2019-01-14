package com.coherentchaos.gdb.debugadapter.mi.record;

import com.coherentchaos.gdb.debugadapter.mi.output.Result;

/**
 * exec-async-output | status-async-output
 */
public class AsyncRecord extends OutOfBandRecord {
    private int token;
    private Result[] results;
    private String asyncClass; // TODO turn into enum?

    public AsyncRecord() {
        token = -1;
    }

    public int getToken() {
        return token;
    }

    public void setToken(int token) {
        this.token = token;
    }

    public Result[] getResults() {
        return results;
    }

    public void setResults(Result[] results) {
        this.results = results;
    }

    public String getAsyncClass() {
        return asyncClass;
    }

    public void setAsyncClass(String asyncClass) {
        this.asyncClass = asyncClass;
    }
}
