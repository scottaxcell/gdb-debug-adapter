package com.coherentchaos.gdb.debugadapter.mi.parser;

import com.coherentchaos.gdb.debugadapter.mi.output.*;
import com.coherentchaos.gdb.debugadapter.mi.record.AsyncRecord;
import com.coherentchaos.gdb.debugadapter.mi.record.OutOfBandRecord;
import com.coherentchaos.gdb.debugadapter.mi.record.ResultRecord;
import com.coherentchaos.gdb.debugadapter.mi.record.StreamRecord;

import java.util.ArrayList;
import java.util.List;

public class Parser {
    private static final String GDB_PROMPT = "(gdb)";

    public RecordType getRecordType(String line) {
        int i = 0;
        if (Character.isDigit(line.charAt(0))) {
            i = 1;
            while (i < line.length() && Character.isDigit(line.charAt(i)))
                i++;
        }
        if (i < line.length()) {
            if (ResultRecord.RESULT_RECORD_PREFIX == line.charAt(i))
                return RecordType.Result;
            else if (line.startsWith(GDB_PROMPT, i))
                return RecordType.GDBPrompt;
            else
                return RecordType.OutOfBand;
        }
        throw new IllegalStateException("Can't process end of line");
    }

    public OutOfBandRecord parseOutOfBandRecord(String line) {
        OutOfBandRecord outOfBandRecord = null;
        StringBuffer buffer = new StringBuffer(line);
        int token = parseToken(buffer);

        char c = buffer.length() != 0 ? buffer.charAt(0) : 0;
        if (c == '*' || c == '+' || c == '=') {
            buffer.deleteCharAt(0);
            AsyncRecord asyncRecord = null;
            switch (c) {
                case '*':
                    asyncRecord = new ExecAsyncOutput();
                    break;
                case '+':
                    asyncRecord = new StatusAsyncOutput();
                    break;
                case '=':
                    asyncRecord = new NotifyAsyncOutput();
                    break;
                default:
                    throw new IllegalStateException("expected an async character");
            }
            asyncRecord.setToken(token);
            int i = buffer.toString().indexOf(',');
            if (i != -1) {
                String asyncClass = buffer.substring(0, i);
                asyncRecord.setAsyncClass(asyncClass);
                buffer.delete(0, i + 1);
            }
            else {
                asyncRecord.setAsyncClass(buffer.toString().trim());
                buffer.setLength(0);
            }
            Result[] results = parseResults(buffer);
            asyncRecord.setResults(results);
            outOfBandRecord = asyncRecord;
        }
        else if (c == '~' || c == '@' || c == '&') {
            buffer.deleteCharAt(0);
            StreamRecord stream = null;
            switch (c) {
                case '~':
                    stream = new ConsoleStreamOutput();
                    break;
                case '@':
                    stream = new TargetStreamOutput();
                    break;
                case '&':
                    stream = new LogStreamOutput();
                    break;
                default:
                    assert false;
                    stream = new ConsoleStreamOutput();
            }
            if (buffer.length() > 0 && buffer.charAt(0) == '"')
                buffer.deleteCharAt(0);
            stream.setcString(parseCString(buffer));
            outOfBandRecord = stream;
        }
        else {
            // Unable to identify line, assume it's from the target
            StreamRecord stream = new TargetStreamOutput();
            stream.setcString(line + "\n");
            outOfBandRecord = stream;
        }
        return outOfBandRecord;
    }

    private int parseToken(StringBuffer buffer) {
        int token = -1;
        if (Character.isDigit(buffer.charAt(0))) {
            int i = 1;
            while (i < buffer.length() && Character.isDigit(buffer.charAt(i))) {
                i++;
            }
            String digits = buffer.substring(0, i);
            try {
                token = Integer.parseInt(digits);
            }
            catch (NumberFormatException ignored) {
            }
            buffer.delete(0, i);
        }
        return token;
    }

    public ResultRecord parseResultRecord(String line) {
        StringBuffer buffer = new StringBuffer(line);
        int token = parseToken(buffer);

        buffer.deleteCharAt(0); // delete '^'

        ResultRecord resultRecord = new ResultRecord();
        resultRecord.setToken(token);

        parseResultClass(buffer, resultRecord);

        if (buffer.length() > 0 && buffer.charAt(0) == ',') {
            buffer.deleteCharAt(0);
            Result[] results = parseResults(buffer);
            resultRecord.setResults(results);
        }

        return resultRecord;
    }

    private void parseResultClass(StringBuffer buffer, ResultRecord resultRecord) {
        if (buffer.toString().startsWith(ResultRecord.ResultClass.CONNECTED.getValue())) {
            resultRecord.setResultClass(ResultRecord.ResultClass.CONNECTED);
            buffer.delete(0, ResultRecord.ResultClass.CONNECTED.getValue().length());
        }
        else if (buffer.toString().startsWith(ResultRecord.ResultClass.DONE.getValue())) {
            resultRecord.setResultClass(ResultRecord.ResultClass.DONE);
            buffer.delete(0, ResultRecord.ResultClass.DONE.getValue().length());
        }
        else if (buffer.toString().startsWith(ResultRecord.ResultClass.ERROR.getValue())) {
            resultRecord.setResultClass(ResultRecord.ResultClass.ERROR);
            buffer.delete(0, ResultRecord.ResultClass.ERROR.getValue().length());
        }
        else if (buffer.toString().startsWith(ResultRecord.ResultClass.EXIT.getValue())) {
            resultRecord.setResultClass(ResultRecord.ResultClass.EXIT);
            buffer.delete(0, ResultRecord.ResultClass.EXIT.getValue().length());
        }
        else if (buffer.toString().startsWith(ResultRecord.ResultClass.RUNNING.getValue())) {
            resultRecord.setResultClass(ResultRecord.ResultClass.RUNNING);
            buffer.delete(0, ResultRecord.ResultClass.RUNNING.getValue().length());
        }
        else {
            throw new RuntimeException("Unexpected ResultRecord ResultClass");
        }
    }

    private Result[] parseResults(StringBuffer buffer) {
        List<Result> results = new ArrayList<>();
        Result result = parseResult(buffer);
        if (result != null)
            results.add(result);
        while (buffer.length() > 0 && buffer.charAt(0) == ',') {
            buffer.deleteCharAt(0);
            result = parseResult(buffer);
            if (result != null)
                results.add(result);
        }
        return results.toArray(new Result[results.size()]);
    }

    private Result parseResult(StringBuffer buffer) {
        Result result = new Result();
        int equal;
        if (buffer.length() > 0 && Character.isLetter(buffer.charAt(0)) && (equal = buffer.indexOf("=")) != -1) {
            String variable = buffer.substring(0, equal);
            result.setVariable(variable);
            buffer.delete(0, equal + 1);
            Value value = parseValue(buffer);
            result.setValue(value);
        }
        else {
            Value value = parseValue(buffer);
            if (value != null)
                result.setValue(value);
            else {
                result.setVariable(buffer.toString());
                result.setValue(new MIConst());
                buffer.setLength(0);
            }
        }
        return result;
    }

    private Value parseValue(StringBuffer buffer) {
        Value value = null;
        if (buffer.length() > 0) {
            if (buffer.charAt(0) == '{') {
                buffer.deleteCharAt(0);
                value = parseTuple(buffer);
            }
            else if (buffer.charAt(0) == '[') {
                buffer.deleteCharAt(0);
                value = parseList(buffer);
            }
            else if (buffer.charAt(0) == '"') {
                buffer.deleteCharAt(0);
                MIConst miConst = new MIConst();
                miConst.setcString(parseCString(buffer));
                value = miConst;
            }
        }
        return value;
    }

    private Tuple parseTuple(StringBuffer buffer) {
        Tuple tuple = new Tuple();
        List<Value> values = new ArrayList<>();
        List<Result> results = new ArrayList<>();

        while (buffer.length() > 0 && buffer.charAt(0) != '}') {
            Value value = parseValue(buffer);
            if (value != null)
                values.add(value);
            else {
                Result result = parseResult(buffer);
                if (result != null)
                    results.add(result);
            }
            if (buffer.length() > 0 && buffer.charAt(0) == ',')
                buffer.deleteCharAt(0);
        }

        if (buffer.length() > 0 && buffer.charAt(0) == '}')
            buffer.deleteCharAt(0);

        tuple.setValues(values.toArray(new Value[values.size()]));
        tuple.setResults(results.toArray(new Result[results.size()]));
        return tuple;
    }

    private MIList parseList(StringBuffer buffer) {
        MIList list = new MIList();
        List<Value> values = new ArrayList<>();
        List<Result> results = new ArrayList<>();

        while (buffer.length() > 0 && buffer.charAt(0) != ']') {
            Value value = parseValue(buffer);
            if (value != null)
                values.add(value);
            else {
                Result result = parseResult(buffer);
                if (result != null)
                    results.add(result);
            }
            if (buffer.length() > 0 && buffer.charAt(0) == ',')
                buffer.deleteCharAt(0);
        }

        if (buffer.length() > 0 && buffer.charAt(0) == ']')
            buffer.deleteCharAt(0);

        list.setValues(values.toArray(new Value[values.size()]));
        list.setResults(results.toArray(new Result[results.size()]));
        return list;
    }

    private String parseCString(StringBuffer buffer) {
        boolean escapeSeen = false;
        boolean endQuotesSeen = false;

        StringBuffer stringBuffer = new StringBuffer();

        int i = 0;
        for (; i < buffer.length() && !endQuotesSeen; i++) {
            char c = buffer.charAt(i);
            if (c == '\\') {
                if (escapeSeen) {
                    stringBuffer.append(c);
                    escapeSeen = false;
                }
                else
                    escapeSeen = true;
            }
            else if (c == '"') {
                if (escapeSeen) {
                    stringBuffer.append(c);
                    escapeSeen = false;
                }
                else
                    endQuotesSeen = true;
            }
            else {
                if (escapeSeen)
                    stringBuffer.append('\\');
                stringBuffer.append(c);
                escapeSeen = false;
            }
        }
        buffer.delete(0, i);
        return stringBuffer.toString();
    }

    public enum RecordType {
        OutOfBand,
        Result,
        Stream,
        Async,
        GDBPrompt
    }
}
