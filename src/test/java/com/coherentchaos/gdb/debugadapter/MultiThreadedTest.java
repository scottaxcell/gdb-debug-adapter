package com.coherentchaos.gdb.debugadapter;

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

public class MultiThreadedTest {
    private static final int THREE_SECONDS = 3000;
    private static final int TWO_SECONDS = 2000;
    private static final int ONE_SECOND = 1000;
    private static final int HALF_SECOND = 500;
    private final String TEST_CASE_DIR = this.getClass().getResource("/multithreaded").getPath();
    private final String SOURCE_FILENAME = this.getClass().getResource("/multithreaded/multithreaded.cpp").getPath();
    private final String TARGET_FILENAME = String.format("%s/multithreaded", TEST_CASE_DIR);
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
        Source source = new Source();
        source.setPath(SOURCE_FILENAME);
        source.setName(new File(SOURCE_FILENAME).getName());

        List<SourceBreakpoint> sourceBreakpoints = new ArrayList<>();
        SourceBreakpoint sourceBreakpoint = new SourceBreakpoint();
        sourceBreakpoint.setLine(29L);
        sourceBreakpoints.add(sourceBreakpoint);
        sourceBreakpoint = new SourceBreakpoint();
        sourceBreakpoint.setLine(56L);
        sourceBreakpoints.add(sourceBreakpoint);

        SetBreakpointsArguments setBreakpointsArguments = new SetBreakpointsArguments();
        setBreakpointsArguments.setSource(source);
        setBreakpointsArguments.setBreakpoints(sourceBreakpoints.toArray(new SourceBreakpoint[sourceBreakpoints.size()]));

        SetBreakpointsResponse setBreakpointsResponse = new SetBreakpointsResponse();
        List<Breakpoint> breakpoints = new ArrayList<>();
        Breakpoint breakpoint = new Breakpoint();
        breakpoint.setSource(source);
        breakpoint.setLine(29L);
        breakpoints.add(breakpoint);

        breakpoint = new Breakpoint();
        breakpoint.setSource(source);
        breakpoint.setLine(56L);
        breakpoints.add(breakpoint);

        setBreakpointsResponse.setBreakpoints(breakpoints.toArray(new Breakpoint[breakpoints.size()]));

        CompletableFuture<SetBreakpointsResponse> future = server.setBreakpoints(setBreakpointsArguments);
        Assert.assertEquals(setBreakpointsResponse.toString(), future.get(TWO_SECONDS, TimeUnit.MILLISECONDS).toString());
    }

    /**
     * Select a single thread to run
     *
     * @throws InterruptedException
     * @throws TimeoutException
     * @throws ExecutionException
     */
    @org.junit.Test
    public void testTypicalFlow() throws InterruptedException, TimeoutException, ExecutionException {
        Source source = new Source();
        source.setPath(SOURCE_FILENAME);
        source.setName(new File(SOURCE_FILENAME).getName());

        List<SourceBreakpoint> sourceBreakpoints = new ArrayList<>();
        SourceBreakpoint sourceBreakpoint = new SourceBreakpoint();
        sourceBreakpoint.setLine(29L);
        sourceBreakpoints.add(sourceBreakpoint);
        sourceBreakpoint = new SourceBreakpoint();
        sourceBreakpoint.setLine(56L);
        sourceBreakpoints.add(sourceBreakpoint);

        SetBreakpointsArguments setBreakpointsArguments = new SetBreakpointsArguments();
        setBreakpointsArguments.setSource(source);
        setBreakpointsArguments.setBreakpoints(sourceBreakpoints.toArray(new SourceBreakpoint[sourceBreakpoints.size()]));

        SetBreakpointsResponse setBreakpointsResponse = new SetBreakpointsResponse();
        List<Breakpoint> breakpoints = new ArrayList<>();
        Breakpoint breakpoint = new Breakpoint();
        breakpoint.setSource(source);
        breakpoint.setLine(29L);
        breakpoints.add(breakpoint);

        breakpoint = new Breakpoint();
        breakpoint.setSource(source);
        breakpoint.setLine(56L);
        breakpoints.add(breakpoint);

        setBreakpointsResponse.setBreakpoints(breakpoints.toArray(new Breakpoint[breakpoints.size()]));

        CompletableFuture<?> future = server.setBreakpoints(setBreakpointsArguments);
        Assert.assertEquals(setBreakpointsResponse.toString(), future.get(TWO_SECONDS, TimeUnit.MILLISECONDS).toString());

        future = server.launch(new HashMap<>());
        Assert.assertEquals(null, future.get(TWO_SECONDS, TimeUnit.MILLISECONDS));

        Thread.sleep(THREE_SECONDS);
        Assert.assertTrue(client.isStopped());

        List<org.eclipse.lsp4j.debug.Thread> threads = new ArrayList<>();
        org.eclipse.lsp4j.debug.Thread thread = new org.eclipse.lsp4j.debug.Thread();
        thread.setId(1L);
        thread.setName("multithreaded");
        threads.add(thread);
        thread = new org.eclipse.lsp4j.debug.Thread();
        thread.setId(2L);
        thread.setName("multithreaded");
        threads.add(thread);
        thread = new org.eclipse.lsp4j.debug.Thread();
        thread.setId(3L);
        thread.setName("multithreaded");
        threads.add(thread);

        ThreadsResponse threadsResponse = new ThreadsResponse();
        threadsResponse.setThreads(threads.toArray(new org.eclipse.lsp4j.debug.Thread[threads.size()]));
        future = server.threads();
        Assert.assertEquals(threadsResponse.toString(), future.get(TWO_SECONDS, TimeUnit.MILLISECONDS).toString());


        StackTraceArguments stackTraceArguments = new StackTraceArguments();
        stackTraceArguments.setThreadId(2L);
        stackTraceArguments.setLevels(1L);

        Long threadId = 2L;
        List<StackFrame> stackFrames = new ArrayList<>();
        StackFrame stackFrame = new StackFrame();
        stackFrame.setId(Utils.createUniqueStackFrameId(threadId, 0L));
        stackFrame.setLine(56L);
        stackFrame.setName("foo()");
        stackFrame.setSource(source);
        stackFrames.add(stackFrame);

        StackTraceResponse stackTraceResponse = new StackTraceResponse();
        stackTraceResponse.setStackFrames(stackFrames.toArray(new StackFrame[stackFrames.size()]));

        future = server.stackTrace(stackTraceArguments);
        Assert.assertEquals(stackTraceResponse.toString(), future.get(TWO_SECONDS, TimeUnit.MILLISECONDS).toString());

        NextArguments nextArguments = new NextArguments();
        nextArguments.setThreadId(2L);
        future = server.next(nextArguments);
        Assert.assertEquals(null, future.get(THREE_SECONDS, TimeUnit.MILLISECONDS));

        // TODO add scopes and variables tests
    }
}

