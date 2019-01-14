package com.coherentchaos.gdb.debugadapter;

import org.eclipse.lsp4j.debug.*;
import org.eclipse.lsp4j.debug.launch.DSPLauncher;
import org.eclipse.lsp4j.debug.services.IDebugProtocolClient;
import org.eclipse.lsp4j.debug.services.IDebugProtocolServer;
import org.eclipse.lsp4j.jsonrpc.Launcher;
import org.eclipse.lsp4j.jsonrpc.services.ServiceEndpoints;
import org.junit.Assert;

import java.io.File;
import java.io.IOException;
import java.io.PipedInputStream;
import java.io.PipedOutputStream;
import java.lang.Thread;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.*;

public class GDBDebugServerTest {
    private static final int THREE_SECONDS = 3000;
    private static final int TWO_SECONDS = 2000;
    private static final int ONE_SECOND = 1000;
    private static final int HALF_SECOND = 500;
    private static final String[] makeCmdLine = {"make", "-f", "makefile"};
    private final String TEST_CASE_DIR = this.getClass().getResource("/helloworld").getPath();
    private final String SOURCE_FILENAME = this.getClass().getResource("/helloworld/helloworld.cpp").getPath();
    private final String TARGET_FILENAME = String.format("%s/helloworld", TEST_CASE_DIR);
    private Target target = new Target(TARGET_FILENAME);
    private GDBDebugServer server;
    private Launcher<IDebugProtocolClient> serverLauncher;
    private Future<?> serverListening;

    private TestDebugProtocolClient client;
    private Launcher<IDebugProtocolServer> clientLauncher;
    private Future<?> clientListening;

    private void compileHelloWorldExe() {
        try {
            ProcessBuilder processBuilder = new ProcessBuilder(makeCmdLine);
            processBuilder.directory(new File(TEST_CASE_DIR));
            processBuilder.start();
        }
        catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * Exercise initialize handshake with server
     *
     * @throws Exception
     */
    @org.junit.Before
    public void setUp() throws Exception {
        compileHelloWorldExe();

        PipedInputStream inClient = new PipedInputStream();
        PipedOutputStream outClient = new PipedOutputStream();
        PipedInputStream inServer = new PipedInputStream();
        PipedOutputStream outServer = new PipedOutputStream();

        inClient.connect(outServer);
        outClient.connect(inServer);

        server = new GDBDebugServer(target);
        serverLauncher = DSPLauncher.createServerLauncher(server, inServer, outServer);
        serverListening = serverLauncher.startListening();

        client = new TestDebugProtocolClient();
        server.setRemoteProxy(client);
        clientLauncher = DSPLauncher.createClientLauncher(client, inClient, outClient);
        clientListening = clientLauncher.startListening();

        InitializeRequestArguments initializeRequestArguments = new InitializeRequestArguments();
        initializeRequestArguments.setClientID("com.boris.dsp");
        initializeRequestArguments.setAdapterID("adapterId");
        initializeRequestArguments.setPathFormat("path");
        initializeRequestArguments.setSupportsVariableType(true);
        initializeRequestArguments.setSupportsVariablePaging(true);
        initializeRequestArguments.setLinesStartAt1(true);
        initializeRequestArguments.setColumnsStartAt1(true);
        initializeRequestArguments.setSupportsRunInTerminalRequest(true);

        Capabilities result = new Capabilities();
        result.setSupportsFunctionBreakpoints(false);
        result.setSupportsConditionalBreakpoints(false);

        CompletableFuture<?> future = server.initialize(initializeRequestArguments);
        Assert.assertEquals(result.toString(), future.get(TWO_SECONDS, TimeUnit.MILLISECONDS).toString());
        Thread.sleep(HALF_SECOND);
        Assert.assertTrue(client.isInitializedExercised());

        Thread.sleep(TWO_SECONDS);
    }

    @org.junit.After
    public void tearDown() throws Exception {
        clientListening.cancel(true);
        serverListening.cancel(true);
        Thread.sleep(HALF_SECOND);
    }

    /**
     * Set a single breakpoint on a single source
     *
     * @throws InterruptedException
     * @throws TimeoutException
     * @throws ExecutionException
     */
    @org.junit.Test
    public void setBreakpoints() throws InterruptedException, TimeoutException, ExecutionException {
        // The test
        Source source = new Source();
        source.setPath(SOURCE_FILENAME);
        source.setName(new File(SOURCE_FILENAME).getName());

        SourceBreakpoint sourceBreakpoint = new SourceBreakpoint();
        sourceBreakpoint.setLine(Long.valueOf(9));

        SetBreakpointsArguments setBreakpointsArguments = new SetBreakpointsArguments();
        setBreakpointsArguments.setSource(source);
        setBreakpointsArguments.setBreakpoints(new SourceBreakpoint[]{sourceBreakpoint});

        SetBreakpointsResponse setBreakpointsResponse = new SetBreakpointsResponse();
        List<Breakpoint> breakpoints = new ArrayList<>();
        Breakpoint breakpoint = new Breakpoint();
        breakpoint.setSource(source);
        breakpoint.setLine(Long.valueOf(9));
        breakpoints.add(breakpoint);
        setBreakpointsResponse.setBreakpoints(breakpoints.toArray(new Breakpoint[breakpoints.size()]));

        CompletableFuture<SetBreakpointsResponse> future = server.setBreakpoints(setBreakpointsArguments);
        Assert.assertEquals(setBreakpointsResponse.toString(), future.get(TWO_SECONDS, TimeUnit.MILLISECONDS).toString());
        Thread.sleep(HALF_SECOND);
    }

    /**
     * Launch target and verify executable exits cleanly
     *
     * @throws InterruptedException
     * @throws TimeoutException
     * @throws ExecutionException
     */
    @org.junit.Test
    public void launch() throws InterruptedException, TimeoutException, ExecutionException {
        // The test
        CompletableFuture<Void> future = server.launch(new HashMap<>());
        Assert.assertEquals(null, future.get(TWO_SECONDS, TimeUnit.MILLISECONDS));

        Thread.sleep(TWO_SECONDS);
        Assert.assertEquals(0, client.exitedCleanly());
    }

    /**
     * Set a single breakpoint, launch target, and verify breakpoint is hit and stopped
     *
     * @throws InterruptedException
     * @throws TimeoutException
     * @throws ExecutionException
     */
    @org.junit.Test
    public void breakpointHitStopped() throws InterruptedException, TimeoutException, ExecutionException {
        Source source = new Source();
        source.setPath(SOURCE_FILENAME);
        source.setName(new File(SOURCE_FILENAME).getName());

        SourceBreakpoint sourceBreakpoint = new SourceBreakpoint();
        sourceBreakpoint.setLine(Long.valueOf(9));

        SetBreakpointsArguments setBreakpointsArguments = new SetBreakpointsArguments();
        setBreakpointsArguments.setSource(source);
        setBreakpointsArguments.setBreakpoints(new SourceBreakpoint[]{sourceBreakpoint});

        SetBreakpointsResponse setBreakpointsResponse = new SetBreakpointsResponse();
        List<Breakpoint> breakpoints = new ArrayList<>();
        Breakpoint breakpoint = new Breakpoint();
        breakpoint.setSource(source);
        breakpoint.setLine(Long.valueOf(9));
        breakpoints.add(breakpoint);
        setBreakpointsResponse.setBreakpoints(breakpoints.toArray(new Breakpoint[breakpoints.size()]));

        CompletableFuture<?> future = server.setBreakpoints(setBreakpointsArguments);
        Assert.assertEquals(setBreakpointsResponse.toString(), future.get(TWO_SECONDS, TimeUnit.MILLISECONDS).toString());
        Thread.sleep(HALF_SECOND);

        future = server.launch(new HashMap<>());
        Assert.assertEquals(null, future.get(TWO_SECONDS, TimeUnit.MILLISECONDS));

        Thread.sleep(TWO_SECONDS);
        Assert.assertTrue(client.isStopped());

        // The test
        StoppedEventArguments stoppedArgs = new StoppedEventArguments();
        stoppedArgs.setReason(StoppedEventArgumentsReason.BREAKPOINT + ";bkptno=1");
        stoppedArgs.setThreadId(Long.valueOf(1));
        stoppedArgs.setAllThreadsStopped(true);
        Assert.assertEquals(stoppedArgs.toString(), client.getStoppedEventArguments().toString());
    }

    @org.junit.Test
    public void threads() throws InterruptedException, TimeoutException, ExecutionException {
        Source source = new Source();
        source.setPath(SOURCE_FILENAME);
        source.setName(new File(SOURCE_FILENAME).getName());

        SourceBreakpoint sourceBreakpoint = new SourceBreakpoint();
        sourceBreakpoint.setLine(Long.valueOf(9));

        SetBreakpointsArguments setBreakpointsArguments = new SetBreakpointsArguments();
        setBreakpointsArguments.setSource(source);
        setBreakpointsArguments.setBreakpoints(new SourceBreakpoint[]{sourceBreakpoint});

        SetBreakpointsResponse setBreakpointsResponse = new SetBreakpointsResponse();
        List<Breakpoint> breakpoints = new ArrayList<>();
        Breakpoint breakpoint = new Breakpoint();
        breakpoint.setSource(source);
        breakpoint.setLine(Long.valueOf(9));
        breakpoints.add(breakpoint);
        setBreakpointsResponse.setBreakpoints(breakpoints.toArray(new Breakpoint[breakpoints.size()]));

        CompletableFuture<?> future = server.setBreakpoints(setBreakpointsArguments);
        Assert.assertEquals(setBreakpointsResponse.toString(), future.get(TWO_SECONDS, TimeUnit.MILLISECONDS).toString());
        Thread.sleep(HALF_SECOND);

        future = server.launch(new HashMap<>());
        Assert.assertEquals(null, future.get(TWO_SECONDS, TimeUnit.MILLISECONDS));

        Thread.sleep(TWO_SECONDS);
        Assert.assertTrue(client.isStopped());

        StoppedEventArguments stoppedArgs = new StoppedEventArguments();
        stoppedArgs.setReason(StoppedEventArgumentsReason.BREAKPOINT + ";bkptno=1");
        stoppedArgs.setThreadId(Long.valueOf(1));
        stoppedArgs.setAllThreadsStopped(true);
        Assert.assertEquals(stoppedArgs.toString(), client.getStoppedEventArguments().toString());

        // The test
        ThreadsResponse threadsResponse = new ThreadsResponse();
        org.eclipse.lsp4j.debug.Thread thread = new org.eclipse.lsp4j.debug.Thread();
        thread.setName("helloworld");
        thread.setId(Long.valueOf(1));
        threadsResponse.setThreads(new org.eclipse.lsp4j.debug.Thread[]{thread});

        future = server.threads();
        Assert.assertEquals(threadsResponse.toString(), future.get(TWO_SECONDS, TimeUnit.MILLISECONDS).toString());
    }

    @org.junit.Test
    public void stackTrace() throws InterruptedException, TimeoutException, ExecutionException {
        Source source = new Source();
        source.setPath(SOURCE_FILENAME);
        source.setName(new File(SOURCE_FILENAME).getName());

        SourceBreakpoint sourceBreakpoint = new SourceBreakpoint();
        sourceBreakpoint.setLine(Long.valueOf(22));

        SetBreakpointsArguments setBreakpointsArguments = new SetBreakpointsArguments();
        setBreakpointsArguments.setSource(source);
        setBreakpointsArguments.setBreakpoints(new SourceBreakpoint[]{sourceBreakpoint});

        SetBreakpointsResponse setBreakpointsResponse = new SetBreakpointsResponse();
        List<Breakpoint> breakpoints = new ArrayList<>();
        Breakpoint breakpoint = new Breakpoint();
        breakpoint.setSource(source);
        breakpoint.setLine(Long.valueOf(22));
        breakpoints.add(breakpoint);
        setBreakpointsResponse.setBreakpoints(breakpoints.toArray(new Breakpoint[breakpoints.size()]));

        CompletableFuture<?> future = server.setBreakpoints(setBreakpointsArguments);
        Assert.assertEquals(setBreakpointsResponse.toString(), future.get(TWO_SECONDS, TimeUnit.MILLISECONDS).toString());
        Thread.sleep(HALF_SECOND);

        future = server.launch(new HashMap<>());
        Assert.assertEquals(null, future.get(TWO_SECONDS, TimeUnit.MILLISECONDS));

        Thread.sleep(TWO_SECONDS);
        Assert.assertTrue(client.isStopped());

        StoppedEventArguments stoppedArgs = new StoppedEventArguments();
        stoppedArgs.setReason(StoppedEventArgumentsReason.BREAKPOINT + ";bkptno=1");
        stoppedArgs.setThreadId(Long.valueOf(1));
        stoppedArgs.setAllThreadsStopped(true);
        Assert.assertEquals(stoppedArgs.toString(), client.getStoppedEventArguments().toString());

        // The test
        StackTraceArguments stackTraceArguments = new StackTraceArguments();
        stackTraceArguments.setThreadId(Long.valueOf(1));

        StackTraceResponse stackTraceResponse = new StackTraceResponse();
        List<StackFrame> stackFrames = new ArrayList<>();

        source = new Source();
        source.setName("helloworld.cpp");
        source.setPath(SOURCE_FILENAME);

        StackFrame stackFrame = new StackFrame();
        stackFrame.setId(Long.valueOf(0));
        stackFrame.setLine(Long.valueOf(22));
        stackFrame.setName("foo");
        stackFrame.setSource(source);
        stackFrames.add(stackFrame);

        stackFrame = new StackFrame();
        stackFrame.setId(Long.valueOf(1));
        stackFrame.setLine(Long.valueOf(10));
        stackFrame.setName("main");
        stackFrame.setSource(source);
        stackFrames.add(stackFrame);

        stackTraceResponse.setStackFrames(stackFrames.toArray(new StackFrame[stackFrames.size()]));

        future = server.stackTrace(stackTraceArguments);
        Assert.assertEquals(stackTraceResponse.toString(), future.get(TWO_SECONDS, TimeUnit.MILLISECONDS).toString());
    }

    @org.junit.Test
    public void scopes() throws InterruptedException, TimeoutException, ExecutionException {
        Source source = new Source();
        source.setPath(SOURCE_FILENAME);
        source.setName(new File(SOURCE_FILENAME).getName());

        SourceBreakpoint sourceBreakpoint = new SourceBreakpoint();
        sourceBreakpoint.setLine(Long.valueOf(22));

        SetBreakpointsArguments setBreakpointsArguments = new SetBreakpointsArguments();
        setBreakpointsArguments.setSource(source);
        setBreakpointsArguments.setBreakpoints(new SourceBreakpoint[]{sourceBreakpoint});

        SetBreakpointsResponse setBreakpointsResponse = new SetBreakpointsResponse();
        List<Breakpoint> breakpoints = new ArrayList<>();
        Breakpoint breakpoint = new Breakpoint();
        breakpoint.setSource(source);
        breakpoint.setLine(Long.valueOf(22));
        breakpoints.add(breakpoint);
        setBreakpointsResponse.setBreakpoints(breakpoints.toArray(new Breakpoint[breakpoints.size()]));

        CompletableFuture<?> future = server.setBreakpoints(setBreakpointsArguments);
        Assert.assertEquals(setBreakpointsResponse.toString(), future.get(TWO_SECONDS, TimeUnit.MILLISECONDS).toString());
        Thread.sleep(HALF_SECOND);

        future = server.launch(new HashMap<>());
        Assert.assertEquals(null, future.get(TWO_SECONDS, TimeUnit.MILLISECONDS));

        Thread.sleep(TWO_SECONDS);
        Assert.assertTrue(client.isStopped());

        StoppedEventArguments stoppedArgs = new StoppedEventArguments();
        stoppedArgs.setReason(StoppedEventArgumentsReason.BREAKPOINT + ";bkptno=1");
        stoppedArgs.setThreadId(Long.valueOf(1));
        stoppedArgs.setAllThreadsStopped(true);
        Assert.assertEquals(stoppedArgs.toString(), client.getStoppedEventArguments().toString());

        // The test
        ScopesArguments scopesArguments = new ScopesArguments();
        scopesArguments.setFrameId(1L);
        future = server.scopes(scopesArguments);
        ScopesResponse scopesResponse = (ScopesResponse) future.get(TWO_SECONDS, TimeUnit.MILLISECONDS);
        Assert.assertEquals(2, scopesResponse.getScopes().length);
        Assert.assertEquals("Locals", scopesResponse.getScopes()[0].getName());
        Assert.assertEquals(Long.valueOf(100), scopesResponse.getScopes()[0].getVariablesReference());
        Assert.assertEquals("Arguments", scopesResponse.getScopes()[1].getName());
        Assert.assertEquals(Long.valueOf(101), scopesResponse.getScopes()[1].getVariablesReference());
    }

    @org.junit.Test
    public void variables() throws InterruptedException, TimeoutException, ExecutionException {
        Source source = new Source();
        source.setPath(SOURCE_FILENAME);
        source.setName(new File(SOURCE_FILENAME).getName());

        SourceBreakpoint sourceBreakpoint = new SourceBreakpoint();
        sourceBreakpoint.setLine(Long.valueOf(22));

        SetBreakpointsArguments setBreakpointsArguments = new SetBreakpointsArguments();
        setBreakpointsArguments.setSource(source);
        setBreakpointsArguments.setBreakpoints(new SourceBreakpoint[]{sourceBreakpoint});

        SetBreakpointsResponse setBreakpointsResponse = new SetBreakpointsResponse();
        List<Breakpoint> breakpoints = new ArrayList<>();
        Breakpoint breakpoint = new Breakpoint();
        breakpoint.setSource(source);
        breakpoint.setLine(Long.valueOf(22));
        breakpoints.add(breakpoint);
        setBreakpointsResponse.setBreakpoints(breakpoints.toArray(new Breakpoint[breakpoints.size()]));

        CompletableFuture<?> future = server.setBreakpoints(setBreakpointsArguments);
        Assert.assertEquals(setBreakpointsResponse.toString(), future.get(TWO_SECONDS, TimeUnit.MILLISECONDS).toString());
        Thread.sleep(HALF_SECOND);

        future = server.launch(new HashMap<>());
        Assert.assertEquals(null, future.get(TWO_SECONDS, TimeUnit.MILLISECONDS));

        Thread.sleep(TWO_SECONDS);
        Assert.assertTrue(client.isStopped());

        StoppedEventArguments stoppedArgs = new StoppedEventArguments();
        stoppedArgs.setReason(StoppedEventArgumentsReason.BREAKPOINT + ";bkptno=1");
        stoppedArgs.setThreadId(Long.valueOf(1));
        stoppedArgs.setAllThreadsStopped(true);
        Assert.assertEquals(stoppedArgs.toString(), client.getStoppedEventArguments().toString());

        ScopesArguments scopesArguments = new ScopesArguments();
        scopesArguments.setFrameId(0L);
        future = server.scopes(scopesArguments);
        ScopesResponse scopesResponse = (ScopesResponse) future.get(TWO_SECONDS, TimeUnit.MILLISECONDS);

        // The test (it is required that scopes is called before calling variables)
        VariablesResponse variablesResponse = new VariablesResponse();
        Variable variable = new Variable();
        variable.setName("numPlanets");
        variable.setValue(String.valueOf(8));
        variablesResponse.setVariables(new Variable[]{variable});

        VariablesArguments variablesArguments = new VariablesArguments();
        variablesArguments.setVariablesReference(scopesResponse.getScopes()[0].getVariablesReference());
        future = server.variables(variablesArguments);

        Assert.assertEquals(variablesResponse.toString(), future.get(TWO_SECONDS, TimeUnit.MILLISECONDS).toString());
    }

    @org.junit.Test
    public void continue_() throws InterruptedException, TimeoutException, ExecutionException {
        Source source = new Source();
        source.setPath(SOURCE_FILENAME);
        source.setName(new File(SOURCE_FILENAME).getName());

        SourceBreakpoint sourceBreakpoint = new SourceBreakpoint();
        sourceBreakpoint.setLine(Long.valueOf(22));

        SetBreakpointsArguments setBreakpointsArguments = new SetBreakpointsArguments();
        setBreakpointsArguments.setSource(source);
        setBreakpointsArguments.setBreakpoints(new SourceBreakpoint[]{sourceBreakpoint});

        SetBreakpointsResponse setBreakpointsResponse = new SetBreakpointsResponse();
        List<Breakpoint> breakpoints = new ArrayList<>();
        Breakpoint breakpoint = new Breakpoint();
        breakpoint.setSource(source);
        breakpoint.setLine(Long.valueOf(22));
        breakpoints.add(breakpoint);
        setBreakpointsResponse.setBreakpoints(breakpoints.toArray(new Breakpoint[breakpoints.size()]));

        CompletableFuture<?> future = server.setBreakpoints(setBreakpointsArguments);
        Assert.assertEquals(setBreakpointsResponse.toString(), future.get(TWO_SECONDS, TimeUnit.MILLISECONDS).toString());
        Thread.sleep(HALF_SECOND);

        future = server.launch(new HashMap<>());
        Assert.assertEquals(null, future.get(TWO_SECONDS, TimeUnit.MILLISECONDS));

        Thread.sleep(TWO_SECONDS);
        Assert.assertTrue(client.isStopped());

        StoppedEventArguments stoppedArgs = new StoppedEventArguments();
        stoppedArgs.setReason(StoppedEventArgumentsReason.BREAKPOINT + ";bkptno=1");
        stoppedArgs.setThreadId(Long.valueOf(1));
        stoppedArgs.setAllThreadsStopped(true);
        Assert.assertEquals(stoppedArgs.toString(), client.getStoppedEventArguments().toString());

        // The test
        ContinueResponse continueResponse = new ContinueResponse();
        continueResponse.setAllThreadsContinued(true);

        ContinueArguments continueArguments = new ContinueArguments();
        future = server.continue_(continueArguments);

        Assert.assertEquals(continueResponse.toString(), future.get(TWO_SECONDS, TimeUnit.MILLISECONDS).toString());
    }
}