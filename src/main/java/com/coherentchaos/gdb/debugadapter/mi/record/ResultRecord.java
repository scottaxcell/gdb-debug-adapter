package com.coherentchaos.gdb.debugadapter.mi.record;

import com.coherentchaos.gdb.debugadapter.mi.output.Result;
import com.coherentchaos.gdb.debugadapter.mi.output.Tuple;

/**
 * result-record -> [ token ] "^" result-class ( "," result )* nl
 */
public class ResultRecord {
    public static char RESULT_RECORD_PREFIX = '^';
    private int token;
    private ResultClass resultClass;
    private Tuple value;

    public ResultRecord() {
        token = -1;
        value = new Tuple();
    }

    public int getToken() {
        return token;
    }

    public void setToken(int token) {
        this.token = token;
    }

    public ResultClass getResultClass() {
        return resultClass;
    }

    public void setResultClass(ResultClass resultClass) {
        this.resultClass = resultClass;
    }

    public Result[] getResults() {
        return value.getResults();
    }

    public void setResults(Result[] res) {
        value.setResults(res);
    }

    /**
     * result-class -> "done" | "running" | "connected" | "error" | "exit"
     */
    public enum ResultClass {
        CONNECTED("connected"),
        DONE("done"),
        ERROR("error"),
        EXIT("exit"),
        RUNNING("running");

        private final String resultClass;

        ResultClass(String resultClass) {
            this.resultClass = resultClass;
        }

        public String getValue() {
            return resultClass;
        }
    }
}
