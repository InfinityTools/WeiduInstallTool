/*
 * Copyright 2023 Argent77
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.infinitytools.wml.process;

import org.tinylog.Logger;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.*;
import java.util.concurrent.locks.ReentrantLock;
import java.util.stream.Collectors;

/**
 * Handles execution of a single system process.
 */
public class SysProc {
  /**
   * Storage for the total output of raw byte data since the start of the process.
   */
  private final ByteArrayOutputStream output = new ByteArrayOutputStream(65536);

  /**
   * Temporary buffer for input text that is sent to the process.
   */
  private final ConcurrentLinkedQueue<byte[]> input = new ConcurrentLinkedQueue<>();

  /**
   * Stores all SysProcOutputEvent handlers registered for this instance.
   */
  private final List<SysProcEventHandler<SysProcOutputEvent>> outputHandlers = new ArrayList<>();

  /**
   * Stores all SysProcChangeEvent handlers registered for this instance.
   */
  private final List<SysProcEventHandler<SysProcChangeEvent>> changeHandlers = new ArrayList<>();

  /**
   * Used internally to synchronize SysProcOutputEvents.
   */
  private final ReentrantLock outputEventLock = new ReentrantLock();

  /**
   * Used internally to synchronize SysProcChangeEvents.
   */
  private final ReentrantLock changeEventLock = new ReentrantLock();

  /**
   * Used internally to synchronize handling output data.
   */
  private final ReentrantLock outputLock = new ReentrantLock();

  private final List<String> commands = new ArrayList<>();

  private Path workingDir;
  private boolean includeError;

  /**
   * Internally used to prevent executing the process multiple times.
   */
  private boolean executed;

  /**
   * {@link Process} instance of the currently running process, {@code null} otherwise.
   */
  private Process process;

  /**
   * Initializes a new process that can be executed by the {@link #execute()} method. Error stream is redirected to the
   * output stream.
   *
   * @param command Array of strings containing the program and optional arguments.
   * @throws NullPointerException      if the program string is {@code null}.
   * @throws IndexOutOfBoundsException if the command array is empty.
   */
  public SysProc(String... command) {
    this(null, true, command);
  }

  /**
   * Initializes a new process that can be executed by the {@link #execute()} method. Error stream is redirected to the
   * output stream.
   *
   * @param workingDir The working directory where the process should be invoked. Specify {@code null} to ignore.
   * @param command    Array of strings containing the program and optional arguments.
   * @throws NullPointerException      if the program string is {@code null}.
   * @throws IndexOutOfBoundsException if the command array is empty.
   */
  public SysProc(Path workingDir, String... command) {
    this(workingDir, true, command);
  }

  /**
   * Initializes a new process that can be executed by the {@link #execute()} method.
   *
   * @param workingDir   The working directory where the process should be invoked. Specify {@code null} to ignore.
   * @param includeError Whether to include the error stream in the output stream.
   * @param command      Array of strings containing the program and optional arguments.
   * @throws NullPointerException      if the program string is {@code null}.
   * @throws IndexOutOfBoundsException if the command array is empty.
   */
  public SysProc(Path workingDir, boolean includeError, String... command) {
    if (command.length == 0 || command[0] == null) {
      throw new NullPointerException();
    }

    this.commands.addAll(Arrays.asList(command));
    setWorkingDirectory(workingDir);
    setIncludeError(includeError);
  }

  /**
   * Executes the process. The process can only be executed once. Subsequent attempts to execute the process will throw
   * an {@link UnsupportedOperationException}.
   *
   * <p>
   * This method blocks until the process has either started to run or has been terminated prematurely.
   * </p>
   *
   * @return A {@link Future} object containing the exit code of the process when it has terminated.
   * @throws UnsupportedOperationException if the process is executed a second time.
   * @throws IOException                   if an I/O error occurs.
   */
  public Future<Integer> execute() throws IOException {
    if (executed) {
      throw new UnsupportedOperationException("Process has already been executed");
    }
    executed = true;

    final ExecutorService executor = Executors.newSingleThreadExecutor();
    final Future<Integer> retVal = executor.submit(new Runner());

    // ExecutorService must be closed for the application to terminate properly
    Thread.ofVirtual().start(executor::close);

    // blocks until the process is running
    while (process == null && !retVal.isDone()) {
      try {
        Thread.sleep(1);
      } catch (InterruptedException e) {
        Logger.trace(e, "Blocking loop interrupted");
      }
    }

    return retVal;
  }

  /**
   * Returns whether the {@link #execute()} method has already been called for this instance.
   */
  public boolean isExecuted() {
    return executed;
  }

  /**
   * Returns whether the process is currently being executed.
   *
   * @return {@code true} if the process is currently running, {@code false} otherwise.
   */
  public boolean isRunning() {
    if (process != null) {
      return process.isAlive();
    }
    return false;
  }

  /**
   * Kills the currently running process.
   */
  public void killProcess() {
    if (process != null && process.isAlive()) {
      process.destroy();
    }
  }

  /**
   * Returns the working directory for the external process. Returns {@code null} if no explicit working directory is
   * defined.
   */
  public Path getWorkingDirectory() {
    return workingDir;
  }

  /**
   * Defines the working directory for the external process. Does nothing if the process is already being executed.
   *
   * @param workingDir Working directory for the process. Specify {@code null} to use the default working directory.
   */
  public void setWorkingDirectory(Path workingDir) {
    if (!isExecuted()) {
      if (workingDir != null) {
        if (!Files.isDirectory(workingDir)) {
          workingDir = workingDir.getParent();
        }
      }
      this.workingDir = workingDir;
    }
  }

  /**
   * Returns an unmodifiable list of the command line for this process.
   */
  public List<String> getCommand() {
    return Collections.unmodifiableList(commands);
  }

  /**
   * Returns a string representation of the command that is executed by this process.
   */
  public String getCommandLine() {
    return commands.stream().map(c -> {
      if (c.indexOf(' ') >= 0 && (c.charAt(0) != '"')) {
        return "\"" + c + "\"";
      } else {
        return c;
      }
    }).collect(Collectors.joining(" "));
  }

  /**
   * Returns whether the error stream of the external process is redirected to the output stream.
   */
  public boolean isIncludeError() {
    return includeError;
  }

  /**
   * Defined whether the error stream of the external process is redirected to the output stream.
   * Does nothing if the process is already being executed.
   *
   * @param value {@code true} to merge error output with the standard output of the process.
   */
  public void setIncludeError(boolean value) {
    if (!isExecuted()) {
      this.includeError = value;
    }
  }

  /**
   * Registers the specified event handler for {@link SysProcOutputEvent}s.
   */
  public void addOutputEventHandler(SysProcEventHandler<SysProcOutputEvent> handler) {
    if (handler != null) {
      try {
        outputEventLock.lock();
        outputHandlers.add(handler);
      } finally {
        outputEventLock.unlock();
      }
    }
  }

  /**
   * Unregisters the specified event handler.
   */
  public boolean removeOutputEventHandler(SysProcEventHandler<SysProcOutputEvent> handler) {
    if (handler != null) {
      try {
        outputEventLock.lock();
        return outputHandlers.remove(handler);
      } finally {
        outputEventLock.unlock();
      }
    }
    return false;
  }

  /**
   * Returns an unmodifiable list of registered handlers for {@link SysProcOutputEvent}s.
   */
  public List<SysProcEventHandler<SysProcOutputEvent>> getOutputEventHandler() {
    return Collections.unmodifiableList(outputHandlers);
  }

  /**
   * Registers the specified event handler for {@link SysProcChangeEvent}s.
   */
  public void addChangeEventHandler(SysProcEventHandler<SysProcChangeEvent> handler) {
    if (handler != null) {
      try {
        changeEventLock.lock();
        changeHandlers.add(handler);
      } finally {
        changeEventLock.unlock();
      }
    }
  }

  /**
   * Unregisters the specified event handler.
   */
  public boolean removeChangeEventHandler(SysProcEventHandler<SysProcChangeEvent> handler) {
    if (handler != null) {
      try {
        changeEventLock.lock();
        return changeHandlers.remove(handler);
      } finally {
        changeEventLock.unlock();
      }
    }
    return false;
  }

  /**
   * Returns an unmodifiable list of registered handlers for {@link SysProcChangeEvent}s.
   */
  public List<SysProcEventHandler<SysProcChangeEvent>> getChangeEventHandler() {
    return Collections.unmodifiableList(changeHandlers);
  }

  /**
   * Specifies input data as raw byte array that is sent to the process at the next occasion.
   */
  public void setInput(byte[] data) {
    if (data != null) {
      input.add(data);
    }
  }

  /**
   * Returns the total output that has been accumulated since the start of the process.
   */
  public byte[] getOutput() {
    try {
      outputLock.lock();
      return output.toByteArray();
    } finally {
      outputLock.unlock();
    }
  }

  /**
   * Adds the specified data to the total output data.
   */
  private void putBuffer(byte[] processOutput) {
    if (processOutput != null) {
      try {
        outputLock.lock();
        output.write(processOutput);
      } catch (IOException e) {
        Logger.error(e, "Writing process data to output buffer");
      } finally {
        outputLock.unlock();
      }
    }
  }

  /**
   * Fetches all available output data from the current process and notifies registered event handlers.
   *
   * @param consumer {@link InputStreamConsumer} object that handles process output.
   * @return Whether output data was available.
   */
  private boolean pollProcessOutput(InputStreamConsumer consumer) {
    // consuming process output
    boolean retVal = false;
    final byte[] processOutput = consumer.getBuffer();
    if (processOutput != null && processOutput.length > 0) {
      runAsync(() -> fireOutputEvent(processOutput));
      putBuffer(processOutput);
      retVal = true;
    }
    return retVal;
  }

  /**
   * Sends available user data to the process.
   *
   * @param producer {@link OutputStreamProducer} object that handles the process input stream.
   * @return Whether input data was available.
   */
  private boolean sendUserInput(OutputStreamProducer producer) {
    boolean retVal = !input.isEmpty();
    while (!input.isEmpty()) {
      final byte[] data = input.poll();
      if (data != null) {
        producer.sendInput(data);
      }
    }
    return retVal;
  }

  /**
   * A helper method that runs the specified {@link Runnable} object asynchronously.
   */
  private void runAsync(Runnable runnable) {
    if (runnable != null) {
      try (ExecutorService executor = Executors.newVirtualThreadPerTaskExecutor()) {
        executor.execute(runnable);
      }
    }
  }

  /**
   * Used internally to fire a {@link SysProcOutputEvent}.
   */
  private void fireOutputEvent(byte[] processOutput) {
    try {
      outputEventLock.lock();
      final SysProcOutputEvent event = new SysProcOutputEvent(this, processOutput);
      for (final SysProcEventHandler<SysProcOutputEvent> handler : outputHandlers) {
        handler.handle(event);
      }
    } finally {
      outputEventLock.unlock();
    }
  }

  /**
   * Used internally to fire a {@link SysProcChangeEvent}.
   */
  private void fireChangeEvent(SysProcChangeEvent.Type type) {
    try {
      changeEventLock.lock();
      final SysProcChangeEvent event = new SysProcChangeEvent(this, type);
      for (final SysProcEventHandler<SysProcChangeEvent> handler : changeHandlers) {
        handler.handle(event);
      }
    } finally {
      changeEventLock.unlock();
    }
  }

  /**
   * Thread is implemented as private inner class to prevent exposing the call() method to the public.
   */
  private class Runner implements Callable<Integer> {
    public Runner() {
    }

    @Override
    public Integer call() throws Exception {
      ProcessBuilder pb = new ProcessBuilder(commands);
      if (includeError) {
        pb.redirectErrorStream(true);
      }

      if (workingDir != null) {
        pb.directory(workingDir.toFile());
      }


      process = pb.start();
      final InputStreamConsumer consumer = new InputStreamConsumer(process);
      final OutputStreamProducer producer = new OutputStreamProducer(process);

      // process execution has started
      runAsync(() -> fireChangeEvent(SysProcChangeEvent.Type.Started));

      final long minDuration = 20;
      final long maxDuration = 100;
      long duration = minDuration;
      boolean busy;
      while (process.isAlive()) {
        busy = pollProcessOutput(consumer);
        busy |= sendUserInput(producer);

        if (!busy) {
          duration = Math.min(duration + 10, maxDuration);
        } else {
          duration = minDuration;
        }

        try {
          Thread.sleep(duration);
        } catch (InterruptedException e) {
          // thread was interrupted by external event
          Logger.trace(e, "Polling loop interrupted");
        }
      }

      producer.getRunner().interrupt();
      producer.getRunner().join(500);
      consumer.getRunner().interrupt();
      consumer.getRunner().join(500);

      // fetch remaining output data, if any
      pollProcessOutput(consumer);

      // process execution has been terminated
      runAsync(() -> fireChangeEvent(SysProcChangeEvent.Type.Terminated));

      int retVal;
      try {
        retVal = process.exitValue();
      } catch (IllegalStateException e) {
        retVal = -1;
      }

      process = null;

      return retVal;
    }
  }
}
