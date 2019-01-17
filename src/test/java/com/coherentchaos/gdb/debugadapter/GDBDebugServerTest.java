package com.coherentchaos.gdb.debugadapter;

import com.coherentchaos.gdb.debugadapter.utils.Cantor;
import com.coherentchaos.gdb.debugadapter.utils.Utils;
import org.eclipse.lsp4j.debug.*;
import org.eclipse.lsp4j.debug.launch.DSPLauncher;
import org.eclipse.lsp4j.debug.services.IDebugProtocolClient;
import org.eclipse.lsp4j.debug.services.IDebugProtocolServer;
import org.eclipse.lsp4j.jsonrpc.Launcher;
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
    private final String TEST_CASE_DIR = this.getClass().getResource("/helloworld").getPath();
    private final String SOURCE_FILENAME = this.getClass().getResource("/helloworld/helloworld.cpp").getPath();
    private final String TARGET_FILENAME = String.format("%s/helloworld", TEST_CASE_DIR);
    private ExecutionTarget executionTarget = new ExecutionTarget(TARGET_FILENAME);
    private GDBDebugServer server;
    private Launcher<IDebugProtocolClient> serverLauncher;
    private Future<?> serverListening;

    private TestDebugProtocolClient client;
    private Launcher<IDebugProtocolServer> clientLauncher;
    private Future<?> clientListening;

    /**
     * Exercise initialize handshake with server
     *
     * @throws Exception
     */
    @org.junit.Before
    public void setUp() throws Exception {
        TestUtils.compileExecutionTarget(TEST_CASE_DIR);
        Assert.assertTrue(new File(TARGET_FILENAME).exists());

        PipedInputStream inClient = new PipedInputStream();
        PipedOutputStream outClient = new PipedOutputStream();
        PipedInputStream inServer = new PipedInputStream();
        PipedOutputStream outServer = new PipedOutputStream();

        inClient.connect(outServer);
        outClient.connect(inServer);

        server = new GDBDebugServer(executionTarget);
        serverLauncher = DSPLauncher.createServerLauncher(server, inServer, outServer);
        serverListening = serverLauncher.startListening();

        client = new TestDebugProtocolClient();
        server.setRemoteProxy(client);
        clientLauncher = DSPLauncher.createClientLauncher(client, inClient, outClient);
        clientListening = clientLauncher.startListening();

        InitializeRequestArguments initializeRequestArguments = new InitializeRequestArguments();
        initializeRequestArguments.setClientID("com.test.debug.protocol.client");
        initializeRequestArguments.setAdapterID("com.coherentchaos.gdb.debugadapter");
        initializeRequestArguments.setPathFormat("path");
        initializeRequestArguments.setSupportsVariableType(true);
        initializeRequestArguments.setSupportsVariablePaging(true);
        initializeRequestArguments.setLinesStartAt1(true);
        initializeRequestArguments.setColumnsStartAt1(true);
        initializeRequestArguments.setSupportsRunInTerminalRequest(true);

        Capabilities result = new Capabilities();
        result.setSupportsFunctionBreakpoints(false);
        result.setSupportsConditionalBreakpoints(false);

        // Initialize test
        CompletableFuture<?> future = server.initialize(initializeRequestArguments);
        Assert.assertEquals(result.toString(), future.get(TWO_SECONDS, TimeUnit.MILLISECONDS).toString());
        Thread.sleep(HALF_SECOND);
        Assert.assertTrue(client.isInitializedExercised());
    }

    @org.junit.After
    public void tearDown() throws Exception {
        clientListening.cancel(false);
        serverListening.cancel(false);
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
    public void testSetBreakpoints() throws InterruptedException, TimeoutException, ExecutionException {
        // The test
        Long breakpointLine = 17L;
        Source source = new Source();
        source.setPath(SOURCE_FILENAME);
        source.setName(new File(SOURCE_FILENAME).getName());

        SourceBreakpoint sourceBreakpoint = new SourceBreakpoint();
        sourceBreakpoint.setLine(breakpointLine);

        SetBreakpointsArguments setBreakpointsArguments = new SetBreakpointsArguments();
        setBreakpointsArguments.setSource(source);
        setBreakpointsArguments.setBreakpoints(new SourceBreakpoint[]{sourceBreakpoint});

        SetBreakpointsResponse setBreakpointsResponse = new SetBreakpointsResponse();
        List<Breakpoint> breakpoints = new ArrayList<>();
        Breakpoint breakpoint = new Breakpoint();
        breakpoint.setSource(source);
        breakpoint.setLine(breakpointLine);
        breakpoints.add(breakpoint);
        setBreakpointsResponse.setBreakpoints(breakpoints.toArray(new Breakpoint[breakpoints.size()]));

        CompletableFuture<SetBreakpointsResponse> future = server.setBreakpoints(setBreakpointsArguments);
        Assert.assertEquals(setBreakpointsResponse.toString(), future.get(TWO_SECONDS, TimeUnit.MILLISECONDS).toString());
    }

    /**
     * Launch executionTarget and verify executable exits cleanly
     *
     * @throws InterruptedException
     * @throws TimeoutException
     * @throws ExecutionException
     */
    @org.junit.Test
    public void testLaunch() throws InterruptedException, TimeoutException, ExecutionException {
        // The test
        CompletableFuture<Void> future = server.launch(new HashMap<>());
        Assert.assertEquals(null, future.get(TWO_SECONDS, TimeUnit.MILLISECONDS));

        Thread.sleep(TWO_SECONDS);
        Assert.assertEquals(0, client.exitedCleanly());
    }

    /**
     * Set a single breakpoint, testLaunch executionTarget, and verify breakpoint is hit and stopped
     *
     * @throws InterruptedException
     * @throws TimeoutException
     * @throws ExecutionException
     */
    @org.junit.Test
    public void testBreakpointHitStopped() throws InterruptedException, TimeoutException, ExecutionException {
        Source source = new Source();
        source.setPath(SOURCE_FILENAME);
        source.setName(new File(SOURCE_FILENAME).getName());

        Long breakpointLine = 27L;
        SourceBreakpoint sourceBreakpoint = new SourceBreakpoint();
        sourceBreakpoint.setLine(breakpointLine);

        SetBreakpointsArguments setBreakpointsArguments = new SetBreakpointsArguments();
        setBreakpointsArguments.setSource(source);
        setBreakpointsArguments.setBreakpoints(new SourceBreakpoint[]{sourceBreakpoint});

        SetBreakpointsResponse setBreakpointsResponse = new SetBreakpointsResponse();
        List<Breakpoint> breakpoints = new ArrayList<>();
        Breakpoint breakpoint = new Breakpoint();
        breakpoint.setSource(source);
        breakpoint.setLine(breakpointLine);
        breakpoints.add(breakpoint);
        setBreakpointsResponse.setBreakpoints(breakpoints.toArray(new Breakpoint[breakpoints.size()]));

        CompletableFuture<?> future = server.setBreakpoints(setBreakpointsArguments);
        Assert.assertEquals(setBreakpointsResponse.toString(), future.get(TWO_SECONDS, TimeUnit.MILLISECONDS).toString());

        future = server.launch(new HashMap<>());
        Assert.assertEquals(null, future.get(TWO_SECONDS, TimeUnit.MILLISECONDS));

        Thread.sleep(TWO_SECONDS);
        Assert.assertTrue(client.isStopped());

        // The test
        StoppedEventArguments stoppedArgs = new StoppedEventArguments();
        stoppedArgs.setReason(StoppedEventArgumentsReason.BREAKPOINT + ";bkptno=1");
        stoppedArgs.setThreadId(1L);
        stoppedArgs.setAllThreadsStopped(true);
        Assert.assertEquals(stoppedArgs.toString(), client.getStoppedEventArguments().toString());
    }

    @org.junit.Test
    public void testThreads() throws InterruptedException, TimeoutException, ExecutionException {
        Source source = new Source();
        source.setPath(SOURCE_FILENAME);
        source.setName(new File(SOURCE_FILENAME).getName());

        Long breakpointLine = 27L;
        SourceBreakpoint sourceBreakpoint = new SourceBreakpoint();
        sourceBreakpoint.setLine(breakpointLine);

        SetBreakpointsArguments setBreakpointsArguments = new SetBreakpointsArguments();
        setBreakpointsArguments.setSource(source);
        setBreakpointsArguments.setBreakpoints(new SourceBreakpoint[]{sourceBreakpoint});

        SetBreakpointsResponse setBreakpointsResponse = new SetBreakpointsResponse();
        List<Breakpoint> breakpoints = new ArrayList<>();
        Breakpoint breakpoint = new Breakpoint();
        breakpoint.setSource(source);
        breakpoint.setLine(breakpointLine);
        breakpoints.add(breakpoint);
        setBreakpointsResponse.setBreakpoints(breakpoints.toArray(new Breakpoint[breakpoints.size()]));

        CompletableFuture<?> future = server.setBreakpoints(setBreakpointsArguments);
        Assert.assertEquals(setBreakpointsResponse.toString(), future.get(TWO_SECONDS, TimeUnit.MILLISECONDS).toString());

        future = server.launch(new HashMap<>());
        Assert.assertEquals(null, future.get(TWO_SECONDS, TimeUnit.MILLISECONDS));

        Thread.sleep(TWO_SECONDS);
        Assert.assertTrue(client.isStopped());

        StoppedEventArguments stoppedArgs = new StoppedEventArguments();
        stoppedArgs.setReason(StoppedEventArgumentsReason.BREAKPOINT + ";bkptno=1");
        stoppedArgs.setThreadId(1L);
        stoppedArgs.setAllThreadsStopped(true);
        Assert.assertEquals(stoppedArgs.toString(), client.getStoppedEventArguments().toString());

        // The test
        ThreadsResponse threadsResponse = new ThreadsResponse();
        org.eclipse.lsp4j.debug.Thread thread = new org.eclipse.lsp4j.debug.Thread();
        thread.setName("helloworld");
        thread.setId(1L);
        threadsResponse.setThreads(new org.eclipse.lsp4j.debug.Thread[]{thread});

        future = server.threads();
        Assert.assertEquals(threadsResponse.toString(), future.get(TWO_SECONDS, TimeUnit.MILLISECONDS).toString());
    }

    @org.junit.Test
    public void testStackTrace() throws InterruptedException, TimeoutException, ExecutionException {
        Source source = new Source();
        source.setPath(SOURCE_FILENAME);
        source.setName(new File(SOURCE_FILENAME).getName());

        Long breakpointLine = 27L;
        SourceBreakpoint sourceBreakpoint = new SourceBreakpoint();
        sourceBreakpoint.setLine(breakpointLine);

        SetBreakpointsArguments setBreakpointsArguments = new SetBreakpointsArguments();
        setBreakpointsArguments.setSource(source);
        setBreakpointsArguments.setBreakpoints(new SourceBreakpoint[]{sourceBreakpoint});

        SetBreakpointsResponse setBreakpointsResponse = new SetBreakpointsResponse();
        List<Breakpoint> breakpoints = new ArrayList<>();
        Breakpoint breakpoint = new Breakpoint();
        breakpoint.setSource(source);
        breakpoint.setLine(breakpointLine);
        breakpoints.add(breakpoint);
        setBreakpointsResponse.setBreakpoints(breakpoints.toArray(new Breakpoint[breakpoints.size()]));

        CompletableFuture<?> future = server.setBreakpoints(setBreakpointsArguments);
        Assert.assertEquals(setBreakpointsResponse.toString(), future.get(TWO_SECONDS, TimeUnit.MILLISECONDS).toString());

        future = server.launch(new HashMap<>());
        Assert.assertEquals(null, future.get(TWO_SECONDS, TimeUnit.MILLISECONDS));

        Thread.sleep(TWO_SECONDS);
        Assert.assertTrue(client.isStopped());

        StoppedEventArguments stoppedArgs = new StoppedEventArguments();
        stoppedArgs.setReason(StoppedEventArgumentsReason.BREAKPOINT + ";bkptno=1");
        stoppedArgs.setThreadId(1L);
        stoppedArgs.setAllThreadsStopped(true);
        Assert.assertEquals(stoppedArgs.toString(), client.getStoppedEventArguments().toString());

        // The test
        StackTraceArguments stackTraceArguments = new StackTraceArguments();
        stackTraceArguments.setThreadId(1L);

        StackTraceResponse stackTraceResponse = new StackTraceResponse();
        List<StackFrame> stackFrames = new ArrayList<>();

        source = new Source();
        source.setName("helloworld.cpp");
        source.setPath(SOURCE_FILENAME);

        Long threadId = 1L;
        StackFrame stackFrame = new StackFrame();
        stackFrame.setId(Utils.createUniqueStackFrameId(threadId, 0L));
        stackFrame.setLine(breakpointLine);
        stackFrame.setName("foo()");
        stackFrame.setSource(source);
        stackFrames.add(stackFrame);

        stackFrame = new StackFrame();
        stackFrame.setId(Utils.createUniqueStackFrameId(threadId, 1L));
        stackFrame.setLine(10L);
        stackFrame.setName("main()");
        stackFrame.setSource(source);
        stackFrames.add(stackFrame);

        stackTraceResponse.setStackFrames(stackFrames.toArray(new StackFrame[stackFrames.size()]));

        future = server.stackTrace(stackTraceArguments);
        Assert.assertEquals(stackTraceResponse.toString(), future.get(TWO_SECONDS, TimeUnit.MILLISECONDS).toString());
    }

    @org.junit.Test
    public void testScopes() throws InterruptedException, TimeoutException, ExecutionException {
        Source source = new Source();
        source.setPath(SOURCE_FILENAME);
        source.setName(new File(SOURCE_FILENAME).getName());

        Long breakpointLine = 22L;
        SourceBreakpoint sourceBreakpoint = new SourceBreakpoint();
        sourceBreakpoint.setLine(breakpointLine);

        SetBreakpointsArguments setBreakpointsArguments = new SetBreakpointsArguments();
        setBreakpointsArguments.setSource(source);
        setBreakpointsArguments.setBreakpoints(new SourceBreakpoint[]{sourceBreakpoint});

        SetBreakpointsResponse setBreakpointsResponse = new SetBreakpointsResponse();
        List<Breakpoint> breakpoints = new ArrayList<>();
        Breakpoint breakpoint = new Breakpoint();
        breakpoint.setSource(source);
        breakpoint.setLine(breakpointLine);
        breakpoints.add(breakpoint);
        setBreakpointsResponse.setBreakpoints(breakpoints.toArray(new Breakpoint[breakpoints.size()]));

        CompletableFuture<?> future = server.setBreakpoints(setBreakpointsArguments);
        Assert.assertEquals(setBreakpointsResponse.toString(), future.get(TWO_SECONDS, TimeUnit.MILLISECONDS).toString());

        future = server.launch(new HashMap<>());
        Assert.assertEquals(null, future.get(TWO_SECONDS, TimeUnit.MILLISECONDS));

        Thread.sleep(TWO_SECONDS);
        Assert.assertTrue(client.isStopped());

        Long threadId = 1L;
        StoppedEventArguments stoppedArgs = new StoppedEventArguments();
        stoppedArgs.setReason(StoppedEventArgumentsReason.BREAKPOINT + ";bkptno=1");
        stoppedArgs.setThreadId(threadId);
        stoppedArgs.setAllThreadsStopped(true);
        Assert.assertEquals(stoppedArgs.toString(), client.getStoppedEventArguments().toString());

        // The test
        ScopesArguments scopesArguments = new ScopesArguments();
        scopesArguments.setFrameId(Utils.createUniqueStackFrameId(threadId, 0L));
        future = server.scopes(scopesArguments);
        ScopesResponse scopesResponse = (ScopesResponse) future.get(TWO_SECONDS, TimeUnit.MILLISECONDS);
        Assert.assertEquals(1, scopesResponse.getScopes().length);
        Assert.assertEquals("Locals", scopesResponse.getScopes()[0].getName());
        Assert.assertEquals(Long.valueOf(100), scopesResponse.getScopes()[0].getVariablesReference());
    }

    @org.junit.Test
    public void testVariables() throws InterruptedException, TimeoutException, ExecutionException {
        Source source = new Source();
        source.setPath(SOURCE_FILENAME);
        source.setName(new File(SOURCE_FILENAME).getName());

        Long breakpointLine = 17L;
        SourceBreakpoint sourceBreakpoint = new SourceBreakpoint();
        sourceBreakpoint.setLine(breakpointLine);

        SetBreakpointsArguments setBreakpointsArguments = new SetBreakpointsArguments();
        setBreakpointsArguments.setSource(source);
        setBreakpointsArguments.setBreakpoints(new SourceBreakpoint[]{sourceBreakpoint});

        SetBreakpointsResponse setBreakpointsResponse = new SetBreakpointsResponse();
        List<Breakpoint> breakpoints = new ArrayList<>();
        Breakpoint breakpoint = new Breakpoint();
        breakpoint.setSource(source);
        breakpoint.setLine(breakpointLine);
        breakpoints.add(breakpoint);
        setBreakpointsResponse.setBreakpoints(breakpoints.toArray(new Breakpoint[breakpoints.size()]));

        CompletableFuture<?> future = server.setBreakpoints(setBreakpointsArguments);
        Assert.assertEquals(setBreakpointsResponse.toString(), future.get(TWO_SECONDS, TimeUnit.MILLISECONDS).toString());

        future = server.launch(new HashMap<>());
        Assert.assertEquals(null, future.get(TWO_SECONDS, TimeUnit.MILLISECONDS));

        Thread.sleep(TWO_SECONDS);
        Assert.assertTrue(client.isStopped());

        Long threadId = 1L;
        StoppedEventArguments stoppedArgs = new StoppedEventArguments();
        stoppedArgs.setReason(StoppedEventArgumentsReason.BREAKPOINT + ";bkptno=1");
        stoppedArgs.setThreadId(threadId);
        stoppedArgs.setAllThreadsStopped(true);
        Assert.assertEquals(stoppedArgs.toString(), client.getStoppedEventArguments().toString());

        ScopesArguments scopesArguments = new ScopesArguments();
        scopesArguments.setFrameId(Utils.createUniqueStackFrameId(threadId, 0L));
        future = server.scopes(scopesArguments);
        ScopesResponse scopesResponse = (ScopesResponse) future.get(TWO_SECONDS, TimeUnit.MILLISECONDS);

        // The test (it is required that scopes is called before calling variables)
        VariablesResponse variablesResponse = new VariablesResponse();
        Variable variable = new Variable();
        variable.setName("numFubars");
        variable.setValue(String.valueOf(-42));
        variablesResponse.setVariables(new Variable[]{variable});
        variable.setVariablesReference(100L);

        VariablesArguments variablesArguments = new VariablesArguments();
        variablesArguments.setVariablesReference(scopesResponse.getScopes()[0].getVariablesReference());
        future = server.variables(variablesArguments);

        Assert.assertEquals(variablesResponse.toString(), future.get(TWO_SECONDS, TimeUnit.MILLISECONDS).toString());
    }

    @org.junit.Test
    public void testContinue_() throws InterruptedException, TimeoutException, ExecutionException {
        Source source = new Source();
        source.setPath(SOURCE_FILENAME);
        source.setName(new File(SOURCE_FILENAME).getName());

        Long breakpointLine = 22L;
        SourceBreakpoint sourceBreakpoint = new SourceBreakpoint();
        sourceBreakpoint.setLine(breakpointLine);

        SetBreakpointsArguments setBreakpointsArguments = new SetBreakpointsArguments();
        setBreakpointsArguments.setSource(source);
        setBreakpointsArguments.setBreakpoints(new SourceBreakpoint[]{sourceBreakpoint});

        SetBreakpointsResponse setBreakpointsResponse = new SetBreakpointsResponse();
        List<Breakpoint> breakpoints = new ArrayList<>();
        Breakpoint breakpoint = new Breakpoint();
        breakpoint.setSource(source);
        breakpoint.setLine(breakpointLine);
        breakpoints.add(breakpoint);
        setBreakpointsResponse.setBreakpoints(breakpoints.toArray(new Breakpoint[breakpoints.size()]));

        CompletableFuture<?> future = server.setBreakpoints(setBreakpointsArguments);
        Assert.assertEquals(setBreakpointsResponse.toString(), future.get(TWO_SECONDS, TimeUnit.MILLISECONDS).toString());

        future = server.launch(new HashMap<>());
        Assert.assertEquals(null, future.get(TWO_SECONDS, TimeUnit.MILLISECONDS));

        Thread.sleep(TWO_SECONDS);
        Assert.assertTrue(client.isStopped());

        Long threadId = 1L;
        StoppedEventArguments stoppedArgs = new StoppedEventArguments();
        stoppedArgs.setReason(StoppedEventArgumentsReason.BREAKPOINT + ";bkptno=1");
        stoppedArgs.setThreadId(threadId);
        stoppedArgs.setAllThreadsStopped(true);
        Assert.assertEquals(stoppedArgs.toString(), client.getStoppedEventArguments().toString());

        // The test
        ContinueResponse continueResponse = new ContinueResponse();
        continueResponse.setAllThreadsContinued(true);

        ContinueArguments continueArguments = new ContinueArguments();
        future = server.continue_(continueArguments);

        Assert.assertEquals(continueResponse.toString(), future.get(TWO_SECONDS, TimeUnit.MILLISECONDS).toString());

        // TODO test threads are running again, look for continued event I think
    }
}