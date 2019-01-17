package com.coherentchaos.gdb.debugadapter.mi.command;

import com.coherentchaos.gdb.debugadapter.ExecutionContext;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

public class Command {
    private static String[] EMPTY = new String[]{};
    private String operation;
    private String[] parameters = EMPTY;
    private String[] options = EMPTY;
    private boolean requiresResponse;
    private boolean ignoreResponse;
    private int hashCode;
    Optional<ExecutionContext> executionContext;

    public Command(String operation, Optional<ExecutionContext> executionContext) {
        this.operation = operation;
        this.executionContext = executionContext;
    }

    public Command(String operation, Optional<ExecutionContext> executionContext, String[] parameters) {
        this.operation = operation;
        this.parameters = parameters;
        this.executionContext = executionContext;
    }

    public boolean isRequiresResponse() {
        return requiresResponse;
    }

    public void setRequiresResponse(boolean requiresResponse) {
        this.requiresResponse = requiresResponse;
    }

    public boolean isIgnoreResponse() {
        return ignoreResponse;
    }

    public void setIgnoreResponse(boolean ignoreResponse) {
        this.ignoreResponse = ignoreResponse;
    }

    public String getOperation() {
        return operation;
    }

    public void setOperation(String operation) {
        this.operation = operation;
    }

    public String[] getParameters() {
        return parameters;
    }

    public void setParameters(String[] parameters) {
        this.parameters = parameters;
    }

    public String[] getOptions() {
        return options;
    }

    public void setOptions(String[] options) {
        this.options = options;
    }

    private String executionContextToString() {
        StringBuilder stringBuilder = new StringBuilder();
        if (!executionContext.isPresent())
            return stringBuilder.toString();

        ExecutionContext ec = executionContext.get();
        if (ec.getThreadId().isPresent()) {
            stringBuilder.append("--thread ").append(ec.getThreadId().get());
            if (ec.getFrameId().isPresent())
                stringBuilder.append(" --frame ").append(ec.getFrameId().get());
        }

        return stringBuilder.toString();
    }

    public String constructCommand() {
        StringBuilder command = new StringBuilder(getOperation());

        String executionContext = executionContextToString();
        if (!executionContext.isEmpty())
            command.append(' ').append(executionContext);

        String options = optionsToString();
        if (!options.isEmpty())
            command.append(' ').append(options);

        String parameters = parametersToString();
        if (!parameters.isEmpty())
            command.append(' ').append(parameters);

        command.append('\n');
        return command.toString();
    }

    private String optionsToString() {
        StringBuilder stringBuilder = new StringBuilder();
        for (String option : options)
            stringBuilder.append(' ').append(option);
        return stringBuilder.toString().trim();
    }

    private String parametersToString() {
        StringBuilder stringBuilder = new StringBuilder();
        for (String parameter : parameters)
            stringBuilder.append(' ').append(parameter);
        return stringBuilder.toString().trim();
    }

    @Override
    public boolean equals(Object obj) {
        if (obj instanceof Command) {
            Command other = (Command) obj;
            return Objects.equals(operation, other.operation)
                    && Objects.equals(parameters, other.getParameters())
                    && Objects.equals(options, other.getOptions())
                    && requiresResponse == other.isRequiresResponse()
                    && ignoreResponse == other.isIgnoreResponse();
        }
        return false;
    }

    @Override
    public int hashCode() {
        int result = hashCode;
        if (result == 0) {
            result = Objects.hash(operation, parameters, options, requiresResponse, ignoreResponse);
        }
        hashCode = result;
        return result;
    }
}
