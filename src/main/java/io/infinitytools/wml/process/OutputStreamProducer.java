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

import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.locks.ReentrantLock;

/**
 * A thread that is responsible for fetching data from the standard input of the main process and write it to the
 * input stream of the child process.
 */
public class OutputStreamProducer implements Runnable {
  private final List<String> input = new ArrayList<>();
  private final ReentrantLock inputLock = new ReentrantLock();

  private final Process process;
  private final BufferedWriter writer;
  private final Thread runner;

  public OutputStreamProducer(Process process) {
    this(process, StandardCharsets.UTF_8);
  }

  public OutputStreamProducer(Process process, Charset charset) {
    this.process = Objects.requireNonNull(process);
    Charset charset1 = (charset != null) ? charset : StandardCharsets.UTF_8;
    this.writer = this.process.outputWriter(charset1);
    this.runner = Thread.ofVirtual().start(this);
  }

  /**
   * Sends the specified string to the underlying process at the next opportunity.
   * Does nothing if the process has already been terminated.
   */
  public void sendInput(String text) {
    if (text != null && !text.isEmpty() && runner.isAlive()) {
      try {
        inputLock.lock();
        input.add(text);
        runner.interrupt();
      } finally {
        inputLock.unlock();
      }
    }
  }

  /**
   * Returns the {@link Thread} instance that runs this producer.
   */
  public Thread getRunner() {
    return runner;
  }

  @Override
  public void run() {
    while (process.isAlive()) {
      forward(false);
      try {
        Thread.sleep(1000);
      } catch (InterruptedException e) {
        // woken up by external trigger
        Logger.trace(e, "Polling thread interrupted");
      }
    }

    // forwarding pending input data
    forward(true);
  }

  /**
   * Forwards currently available input data to the process.
   *
   * @param forceFlush Specify {@code true} to flush the process output stream unconditionally after the write
   *                   operation. Otherwise, a flush occurs only when a line break is detected.
   */
  private void forward(boolean forceFlush) {
    if (!input.isEmpty()) {
      // fetching input data
      final String s;
      try {
        inputLock.lock();
        s = String.join("", input);
        input.clear();
      } finally {
        inputLock.unlock();
      }

      // forwarding input to the process
      if (!s.isEmpty()) {
        s.chars().forEachOrdered(ch -> {
          try {
            writer.write(ch);

            // auto-flush at line break
            if (ch == 0x0a) {
              writer.flush();
            }
          } catch (IOException e) {
            Logger.error(e, "I/O error while writing input content to process stream");
          }
        });
      }
    }

    // unconditional flush as requested
    if (forceFlush) {
      try {
        writer.flush();
      } catch (IOException ignored) {
      }
    }
  }
}
