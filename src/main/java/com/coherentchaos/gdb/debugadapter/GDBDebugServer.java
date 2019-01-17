package com.coherentchaos.gdb.debugadapter;

import com.coherentchaos.gdb.debugadapter.mi.command.*;
import com.coherentchaos.gdb.debugadapter.mi.output.Output;
import com.coherentchaos.gdb.debugadapter.mi.output.OutputParser;
import com.coherentchaos.gdb.debugadapter.mi.parser.Parser;
import com.coherentchaos.gdb.debugadapter.mi.record.OutOfBandRecord;
import com.coherentchaos.gdb.debugadapter.mi.record.ResultRecord;
import com.coherentchaos.gdb.debugadapter.utils.Cantor;
import com.coherentchaos.gdb.debugadapter.utils.Logger;
import com.coherentchaos.gdb.debugadapter.utils.Utils;
import org.eclipse.lsp4j.debug.*;
import org.eclipse.lsp4j.debug.services.IDebugProtocolClient;
import org.eclipse.lsp4j.debug.services.IDebugProtocolServer;

import java.io.*;
import java.lang.Thread;
import java.util.*;
import java.util.concurrent.*;
import java.util.function.Supplier;

/**
 * GDB Server - implements DAP interface
 */
public class GDBDebugServer implements IDebugProtocolServer {
    /**
     * Commands that need to be processed
     */
    private final BlockingQueue<CommandWrapper> commandQueue = new LinkedBlockingQueue<>();
    /**
     * Commands that have been written to the GDB stream
     */
    private final List<CommandWrapper> writtenCommands = Collections.synchronizedList(new ArrayList<>());
    /**
     * Commands that have been read from the GDB stream
     */
    private final Map<Integer, CommandWrapper> readCommands = Collections.synchronizedMap(new HashMap<>());
    private ExecutionTarget executionTarget;
    private GDBBackend backend;
    private GDBReaderThread gdbReaderThread;
    private GDBWriterThread gdbWriterThread;
    private IDebugProtocolClient client;
    private ExecutorService asyncExecutor = Executors.newCachedThreadPool();
    private ExecutorService eventExecutor = Executors.newSingleThreadExecutor();
    private EventProcessor eventProcessor = new EventProcessor();
    /**
     * The initialized event will mark this as complete
     */
    private CompletableFuture<Void> initialized = new CompletableFuture<>();
    /**
     * Aligns commandWrapper requests with commandWrapper responses
     */
    private int tokenCounter = 0;
    /**
     * Maps variablesReference to a Long
     * where variablesReference = SCOPE_NAME concatonated with CANTOR_PAIR_NUMBER
     * where CANTOR_PAIR_NUMBER = cantor pair of threadId and frameId (See {@link Cantor})
     */
    private VariablesReferenceMap variablesReferenceMap = new VariablesReferenceMap();

    private GDBDebugServer() {
        // not allowed
    }

    public GDBDebugServer(ExecutionTarget executionTarget) {
        this.executionTarget = executionTarget;
    }

    public GDBDebugServer(ExecutionTarget executionTarget, IDebugProtocolClient client) {
        this.executionTarget = executionTarget;
        this.client = client;
        eventProcessor.setClient(client);
    }

    @Override
    public CompletableFuture<RunInTerminalResponse> runInTerminal(RunInTerminalRequestArguments args) {
        return CompletableFuture.completedFuture(null);
    }

    @Override
    public CompletableFuture<Capabilities> initialize(InitializeRequestArguments args) {
        Utils.debug("initialize");

        backend = new GDBBackend(executionTarget);
        backend.startGDB();

        gdbReaderThread = new GDBReaderThread(backend.getInputStream());
        gdbReaderThread.start();

        gdbWriterThread = new GDBWriterThread(backend.getOutputStream());
        gdbWriterThread.start();

        Capabilities capabilities = new Capabilities();
        capabilities.setSupportsFunctionBreakpoints(false);
        capabilities.setSupportsConditionalBreakpoints(false);

        return initialized.thenCompose((v) -> {
            return CompletableFuture.completedFuture(capabilities);
        });
    }

    @Override
    public CompletableFuture<Void> configurationDone(ConfigurationDoneArguments args) {
        return CompletableFuture.completedFuture(null);
    }

    @Override
    public CompletableFuture<Void> launch(Map<String, Object> args) {
        queueCommand(ExecRunCommand.of());
        return CompletableFuture.completedFuture(null);
    }

    @Override
    public CompletableFuture<Void> attach(Map<String, Object> args) {
        return CompletableFuture.completedFuture(null);
    }

    @Override
    public CompletableFuture<Void> restart(RestartArguments args) {
        return CompletableFuture.completedFuture(null);
    }

    @Override
    public CompletableFuture<Void> disconnect(DisconnectArguments args) {
        // TODO disconnect from debuggee
        // TODO kill debug adapter
        return CompletableFuture.completedFuture(null);
    }

    @Override
    public CompletableFuture<Void> terminate(TerminateArguments args) {
        queueCommand(GDBExitCommand.of());
        return CompletableFuture.completedFuture(null);
    }

    @Override
    public CompletableFuture<SetBreakpointsResponse> setBreakpoints(SetBreakpointsArguments args) {
        queueCommand(BreakDeleteCommand.of());

        List<Integer> tokens = new ArrayList<>();
        Source source = args.getSource();
        String path = source.getPath();
        for (SourceBreakpoint breakpoint : args.getBreakpoints()) {
            Long line = breakpoint.getLine();
            StringBuilder stringBuilder = new StringBuilder(path);
            stringBuilder.append(':').append(line);
            final int token = queueCommand(BreakInsertCommand.of(stringBuilder.toString()));
            tokens.add(token);
        }

        Supplier<SetBreakpointsResponse> supplier = setBreakpointsResponseSupplier(tokens, args);
        return CompletableFuture.supplyAsync(supplier, asyncExecutor);
    }

    private Supplier<SetBreakpointsResponse> setBreakpointsResponseSupplier(List<Integer> tokens, SetBreakpointsArguments args) {
        return () -> {
            // TODO start timer to flag any commandWrapper that doesn't get a response
            List<CommandWrapper> commandResponses = new ArrayList<>();
            while (commandResponses.size() != tokens.size()) {
                for (Integer token : tokens) {
                    if (readCommands.containsKey(token)) {
                        CommandWrapper commandWrapper = readCommands.remove(token);
                        commandResponses.add(commandWrapper);
                    }
                }
                try {
                    Thread.sleep(200);
                }
                catch (InterruptedException ignored) {
                }
            }
            return getSetBreakpointsResponse(commandResponses, args);
        };
    }

    private SetBreakpointsResponse getSetBreakpointsResponse(List<CommandWrapper> commandWrappers, SetBreakpointsArguments args) {
        SetBreakpointsResponse response = new SetBreakpointsResponse();
        List<Breakpoint> breakpoints = new ArrayList<>();
        for (CommandWrapper commandWrapper : commandWrappers) {
            CommandResponse commandResponse = commandWrapper.getCommandResponse();
            Output output = commandResponse.getOutput();
            Breakpoint breakpoint = OutputParser.parseBreakpointsResponse(output);
            if (breakpoint != null)
                breakpoints.add(breakpoint);
            Utils.debug(this.getClass().getSimpleName() + " " + commandWrapper.getToken() + commandWrapper.getCommand().constructCommand());
        }
        // TODO parse commandWrapper response into set breakpoint reponse maintaining same order as original args
        if (!breakpoints.isEmpty())
            response.setBreakpoints(breakpoints.toArray(new Breakpoint[breakpoints.size()]));
        return response;
    }

    @Override
    public CompletableFuture<SetFunctionBreakpointsResponse> setFunctionBreakpoints(SetFunctionBreakpointsArguments args) {
        return CompletableFuture.completedFuture(null);
    }

    @Override
    public CompletableFuture<Void> setExceptionBreakpoints(SetExceptionBreakpointsArguments args) {
        return CompletableFuture.completedFuture(null);
    }

    @Override
    public CompletableFuture<ContinueResponse> continue_(ContinueArguments args) {
        final int token = queueCommand(ExecContinueCommand.of(ExecutionContext.of(args.getThreadId())));
        Supplier<ContinueResponse> supplier = continueResponseSupplier(token);
        return CompletableFuture.supplyAsync(supplier, asyncExecutor);
    }

    private Supplier<ContinueResponse> continueResponseSupplier(int token) {
        return () -> {
            // TODO start timer to flag any commandWrapper that doesn't get a response
            while (true) {
                if (readCommands.containsKey(token)) {
                    synchronized (readCommands) {
                        CommandWrapper commandWrapper = readCommands.remove(token);
                        return getContinueResponse(commandWrapper);
                    }
                }
                try {
                    Thread.sleep(200);
                }
                catch (InterruptedException ignored) {
                }
            }
        };
    }

    private ContinueResponse getContinueResponse(CommandWrapper commandWrapper) {
        ContinueResponse response = new ContinueResponse();
        response.setAllThreadsContinued(true);
        return response;
    }

    @Override
    public CompletableFuture<Void> next(NextArguments args) {
        queueCommand(ExecNextCommand.of(ExecutionContext.of(args.getThreadId())));
        return CompletableFuture.completedFuture(null);
    }

    @Override
    public CompletableFuture<Void> stepIn(StepInArguments args) {
        queueCommand(ExecStepCommand.of(ExecutionContext.of(args.getThreadId())));
        return CompletableFuture.completedFuture(null);
    }

    @Override
    public CompletableFuture<Void> stepOut(StepOutArguments args) {
        queueCommand(ExecReturnCommand.of(ExecutionContext.of(args.getThreadId())));
        return CompletableFuture.completedFuture(null);
    }

    @Override
    public CompletableFuture<Void> stepBack(StepBackArguments args) {
        return CompletableFuture.completedFuture(null);
    }

    @Override
    public CompletableFuture<Void> reverseContinue(ReverseContinueArguments args) {
        return CompletableFuture.completedFuture(null);
    }

    @Override
    public CompletableFuture<Void> restartFrame(RestartFrameArguments args) {
        return CompletableFuture.completedFuture(null);
    }

    @Override
    public CompletableFuture<Void> goto_(GotoArguments args) {
        return CompletableFuture.completedFuture(null);
    }

    @Override
    public CompletableFuture<Void> pause(PauseArguments args) {
        return CompletableFuture.completedFuture(null);
    }

    @Override
    public CompletableFuture<StackTraceResponse> stackTrace(StackTraceArguments args) {
        final int token = queueCommand(StackListFramesCommand.of(ExecutionContext.of(args.getThreadId())));
        Supplier<StackTraceResponse> supplier = setStackTraceResponseSupplier(token, args.getThreadId());
        return CompletableFuture.supplyAsync(supplier, asyncExecutor);
    }

    private Supplier<StackTraceResponse> setStackTraceResponseSupplier(int token, Long threadId) {
        return () -> {
            // TODO start timer to flag any commandWrapper that doesn't get a response
            while (true) {
                if (readCommands.containsKey(token)) {
                    CommandWrapper commandWrapper = readCommands.remove(token);
                    return getStackTraceResponse(commandWrapper, threadId);
                }
                try {
                    Thread.sleep(200);
                }
                catch (InterruptedException ignored) {
                }
            }
        };
    }

    private StackTraceResponse getStackTraceResponse(CommandWrapper commandWrapper, Long threadId) {
        CommandResponse commandResponse = commandWrapper.getCommandResponse();
        Output output = commandResponse.getOutput();
        StackTraceResponse response = OutputParser.parseStackListFramesResponse(output);
        for (StackFrame stackFrame : response.getStackFrames()) {
            Long stackFrameId = stackFrame.getId();
            Long uniqueStackFrameId = Utils.createUniqueStackFrameId(threadId, stackFrameId);
            stackFrame.setId(uniqueStackFrameId);
        }
        return response;
    }

    @Override
    public CompletableFuture<ScopesResponse> scopes(ScopesArguments args) {
        ScopesResponse response = new ScopesResponse();
        List<Scope> scopes = new ArrayList<>();
        scopes.add(createScope("Locals", args.getFrameId()));
//        scopes.add(createScope("Arguments", args.getFrameId())); // TODO turn on one day
        response.setScopes(scopes.toArray(new Scope[scopes.size()]));
        return CompletableFuture.completedFuture(response);
    }

    private Scope createScope(String name, Long frameId) {
        Scope scope = new Scope();
        scope.setName(name);
        scope.setVariablesReference(variablesReferenceMap.create(String.format("%s_%s", name, frameId)));
        return scope;
    }

    @Override
    public CompletableFuture<VariablesResponse> variables(VariablesArguments args) {
        String variablesReference = variablesReferenceMap.get(args.getVariablesReference());
        String[] tmp = variablesReference.split("_");
        String type = tmp[0];
        Long uniqueStackFrameId = Long.valueOf(tmp[1]);
        long[] threadAndStackFrameIds = Utils.computeThreadAndStackFrameIds(uniqueStackFrameId);
        Long threadId = threadAndStackFrameIds[0];
        Long frameId = threadAndStackFrameIds[1];

        if ("Locals".equals(type)) {
            final int token = queueCommand(StackListLocalsCommand.of(ExecutionContext.of(threadId, frameId)));
            Supplier<VariablesResponse> supplier = setVariablesResponseSupplier(token, args.getVariablesReference());
            return CompletableFuture.supplyAsync(supplier, asyncExecutor);
        }
        else if ("Arguments".equals(type)) {
            // TODO
        }
        throw new IllegalStateException("naughty naughty");
    }

    private Supplier<VariablesResponse> setVariablesResponseSupplier(int token, Long variablesReference) {
        return () -> {
            // TODO start timer to flag any commandWrapper that doesn't get a response
            while (true) {
                if (readCommands.containsKey(token)) {
                    CommandWrapper commandWrapper = readCommands.remove(token);
                    return getVariablesResponse(commandWrapper, variablesReference);
                }
                try {
                    Thread.sleep(200);
                }
                catch (InterruptedException ignored) {
                }
            }
        };
    }

    private VariablesResponse getVariablesResponse(CommandWrapper commandWrapper, Long variablesReference) {
        CommandResponse commandResponse = commandWrapper.getCommandResponse();
        Output output = commandResponse.getOutput();
        VariablesResponse response = OutputParser.parseVariablesResponse(output);
        for (Variable variable : response.getVariables())
            variable.setVariablesReference(variablesReference);
        return response;
    }

    @Override
    public CompletableFuture<SetVariableResponse> setVariable(SetVariableArguments args) {
        return CompletableFuture.completedFuture(null);
    }

    @Override
    public CompletableFuture<SourceResponse> source(SourceArguments args) {
        return CompletableFuture.completedFuture(null);
    }

    @Override
    public CompletableFuture<ThreadsResponse> threads() {
        final int token = queueCommand(ThreadsInfoCommand.of());
        Supplier<ThreadsResponse> supplier = setThreadsResponseSupplier(token);
        return CompletableFuture.supplyAsync(supplier, asyncExecutor);
    }

    private Supplier<ThreadsResponse> setThreadsResponseSupplier(int token) {
        return () -> {
            // TODO start timer to flag any commandWrapper that doesn't get a response
            while (true) {
                if (readCommands.containsKey(token)) {
                    CommandWrapper commandWrapper = readCommands.remove(token);
                    return getThreadsResponse(commandWrapper);
                }
                try {
                    Thread.sleep(200);
                }
                catch (InterruptedException ignored) {
                }
            }
        };
    }

    private ThreadsResponse getThreadsResponse(CommandWrapper commandWrapper) {
        CommandResponse commandResponse = commandWrapper.getCommandResponse();
        Output output = commandResponse.getOutput();
        ThreadsResponse response = OutputParser.parseThreadsResponse(output);
        return response;
    }

    @Override
    public CompletableFuture<Void> terminateThreads(TerminateThreadsArguments args) {
        return CompletableFuture.completedFuture(null);
    }

    @Override
    public CompletableFuture<ModulesResponse> modules(ModulesArguments args) {
        return CompletableFuture.completedFuture(null);
    }

    @Override
    public CompletableFuture<LoadedSourcesResponse> loadedSources(LoadedSourcesArguments args) {
        return CompletableFuture.completedFuture(null);
    }

    @Override
    public CompletableFuture<EvaluateResponse> evaluate(EvaluateArguments args) {
        return CompletableFuture.completedFuture(null);
    }

    @Override
    public CompletableFuture<SetExpressionResponse> setExpression(SetExpressionArguments args) {
        return CompletableFuture.completedFuture(null);
    }

    @Override
    public CompletableFuture<StepInTargetsResponse> stepInTargets(StepInTargetsArguments args) {
        return CompletableFuture.completedFuture(null);
    }

    @Override
    public CompletableFuture<GotoTargetsResponse> gotoTargets(GotoTargetsArguments args) {
        return CompletableFuture.completedFuture(null);
    }

    @Override
    public CompletableFuture<CompletionsResponse> completions(CompletionsArguments args) {
        return CompletableFuture.completedFuture(null);
    }

    @Override
    public CompletableFuture<ExceptionInfoResponse> exceptionInfo(ExceptionInfoArguments args) {
        return CompletableFuture.completedFuture(null);
    }

    public void setRemoteProxy(IDebugProtocolClient client) {
        this.client = client;
        eventProcessor.setClient(client);
    }

    private int getNewToken() {
        int newTokenCounter = ++tokenCounter;
        if (newTokenCounter <= 0)
            newTokenCounter = tokenCounter = 1;
        return newTokenCounter;
    }

    private int queueCommand(Command command) {
        int token = -1;
        final CommandWrapper commandWrapper = new CommandWrapper(command);
        if (commandWrapper.getCommand().isRequiresResponse()) {
            commandWrapper.generateToken();
            token = commandWrapper.getToken();
        }
        commandQueue.add(commandWrapper);
        Utils.debug("queued.. " + token + commandWrapper.getCommand().constructCommand());
        return token;
    }

    private void notifyClientOfInitialized() {
        if (!initialized.isDone() || eventProcessor == null)
            return;
        eventExecutor.execute(() -> eventProcessor.notifyClientOfInitialized());
    }

    private int processNextQueuedCommand() {
//        int token = -1;
//        if (!commandQueue.isEmpty()) {
//            final CommandWrapper commandWrapper = commandQueue.remove(0);
//            if (commandWrapper != null) {
//                if (commandWrapper.getCommand().isRequiresResponse()) {
//                    commandWrapper.generateToken();
//                    token = commandWrapper.getToken();
//                }
////                writtenCommands.add(commandWrapper);
//                Utils.dsp(commandWrapper.getCommand().constructCommand() + " processed..");
//            }
//        }
//        return token;
        return -1;
    }

    private void processEvent(Output output) {
        // do not send events until initialized event has been sent to client
        if (!initialized.isDone())
            return;
        eventExecutor.execute(() -> eventProcessor.eventReceived(output));
    }

    private CommandWrapper getWrittenCommand(int token) {
        synchronized (writtenCommands) {
            for (CommandWrapper commandWrapper : writtenCommands) {
                if (commandWrapper.getToken() == token)
                    return commandWrapper;
            }
        }
        return null;
    }

    /**
     * Track variablesReference
     */
    private static class VariablesReferenceMap {
        private static final Long START_VARIABLES_REFERENCE = 100L;
        private Map<Long, String> map = new HashMap<>();
        private Long nextVariablesReference = START_VARIABLES_REFERENCE;

        Long create(String variablesReference) {
            Long next = nextVariablesReference++;
            map.put(next, variablesReference);
            return next;
        }

        public String get(Long variablesReference) {
            return map.get(variablesReference);
        }

        public void reset() {
            map = new HashMap<>();
            nextVariablesReference = START_VARIABLES_REFERENCE;
        }
    }

    /**
     * Handles MI commands that are written to the GDB stream
     */
    private class GDBWriterThread extends Thread {
        private OutputStream outputStream;

        GDBWriterThread(OutputStream outputStream) {
            super("GDB Writer Thread");
            this.outputStream = outputStream;
        }

        @Override
        public void run() {
            CommandWrapper commandWrapper;

            while (true) {
                try {
                    commandWrapper = commandQueue.take();
                }
                catch (InterruptedException e) {
                    break;
                }

                writtenCommands.add(commandWrapper);

                StringBuilder commandBuilder = new StringBuilder();
                if (commandWrapper.getCommand().isRequiresResponse())
                    commandBuilder.append(commandWrapper.getToken());
                commandBuilder.append(commandWrapper.getCommand().constructCommand());

                try {
                    outputStream.write(commandBuilder.toString().getBytes());
                    outputStream.flush();
                    Utils.debug(commandBuilder.toString().trim() + " written..");
                }
                catch (IOException e) {
                    break;
                }
            }
            try {
                if (outputStream != null)
                    outputStream.close();
            }
            catch (IOException ignored) {
            }
        }
    }

    /**
     * Handles MI output from the GDB stream
     */
    private class GDBReaderThread extends Thread {
        private InputStream inputStream;
        private Parser parser;

        GDBReaderThread(InputStream inputStream) {
            super("GDB Reader Thread");
            this.inputStream = inputStream;
            parser = new Parser();
        }

        @Override
        public void run() {
            BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream));
            try {
                String line;
                while ((line = reader.readLine()) != null) {
                    if (line.length() != 0) {
                        Utils.debug("reading line: " + line);
                        handleOutput(line);
                    }
                }
                Utils.debug("GDBReaderThread while FINISHED");
            }
            catch (IOException | RejectedExecutionException ignored) {
                ignored.printStackTrace();
            }
            try {
                if (inputStream != null)
                    inputStream.close();
            }
            catch (IOException ignored) {
            }
        }

        private void handleOutput(String line) {
            Parser.RecordType recordType = parser.getRecordType(line);

            if (recordType == Parser.RecordType.Result) {
                ResultRecord resultRecord = parser.parseResultRecord(line);
                int token = resultRecord.getToken();
                CommandWrapper commandWrapper = getWrittenCommand(token);
                if (commandWrapper != null) {
                    writtenCommands.remove(commandWrapper);

                    if (!commandWrapper.getCommand().isIgnoreResponse()) {
                        Output output = new Output(resultRecord);

                        CommandResponse commandResponse = new CommandResponse(output);
                        commandWrapper.setCommandResponse(commandResponse);

                        readCommands.put(token, commandWrapper);
                        Utils.debug(commandWrapper.getToken() + commandWrapper.getCommand().constructCommand().trim() + " received..");
                    }
                }
                else {
                    // treat response as an event
                    Output output = new Output(resultRecord);
                    processEvent(output);
                }
            }
            else if (recordType == Parser.RecordType.GDBPrompt) {
                if (!initialized.isDone()) {
                    initialized.complete(null);
                    notifyClientOfInitialized();
                }
                // TODO should fire off output event to client
            }
            else if (recordType == Parser.RecordType.OutOfBand) {
                OutOfBandRecord outOfBandRecord = parser.parseOutOfBandRecord(line);
                Output output = new Output(outOfBandRecord);
                processEvent(output);
            }
        }
    }

    /**
     * Wrapper for handling commandWrapper requests and responses
     */
    private class CommandWrapper {
        private Command command;
        private CommandResponse commandResponse;
        private int token;
        private int hashCode;

        CommandWrapper(Command command) {
            this.command = command;
            token = -1;
        }

        public Command getCommand() {
            return command;
        }

        public void setCommand(Command command) {
            this.command = command;
        }

        int getToken() {
            return token;
        }

        public void setToken(int token) {
            this.token = token;
        }

        void generateToken() {
            token = getNewToken();
        }

        CommandResponse getCommandResponse() {
            return commandResponse;
        }

        void setCommandResponse(CommandResponse commandResponse) {
            this.commandResponse = commandResponse;
        }

        @Override
        public boolean equals(Object obj) {
            if (obj instanceof CommandWrapper) {
                CommandWrapper other = (CommandWrapper) obj;
                return Objects.equals(command, other.getCommand()) && token == other.getToken();
            }
            return false;
        }

        @Override
        public int hashCode() {
            int result = hashCode;
            if (result == 0) {
                result = Objects.hash(command, token);
            }
            hashCode = result;
            return result;
        }
    }

    /**
     * Timer for detecting written commands that don't receive a response
     */
    private class CommandResponseTimer extends TimerTask {
        private CommandWrapper commandWrapper;

        public CommandResponseTimer(CommandWrapper commandWrapper) {
            this.commandWrapper = commandWrapper;
        }

        @Override
        public void run() {
            CommandWrapper writtenCommand = getWrittenCommand(commandWrapper.getToken());
            if (writtenCommand != null) {
                String msg = this.getClass().getSimpleName() + " " + writtenCommand.getCommand().constructCommand() + " did not received a response in time";
                Logger.getInstance().warning(msg);
                throw new RuntimeException(msg);
            }
        }
    }
}
