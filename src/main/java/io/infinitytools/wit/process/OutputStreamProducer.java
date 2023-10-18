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
package io.infinitytools.wit.process;

import org.tinylog.Logger;

import java.io.IOException;
import java.io.OutputStream;
import java.util.Objects;
import java.util.concurrent.ConcurrentLinkedQueue;

/**
 * A thread that is responsible for fetching data from the standard input of the main process and write it to the
 * input stream of the child process.
 */
public class OutputStreamProducer implements Runnable {
  private final ConcurrentLinkedQueue<byte[]> input = new ConcurrentLinkedQueue<>();

  private final Process process;
  private final Thread runner;

  public OutputStreamProducer(Process process) {
    this.process = Objects.requireNonNull(process);
    this.runner = Thread.ofVirtual().start(this);
  }

  /**
   * Sends the specified raw byte data to the underlying process at the next opportunity.
   * Does nothing if the process has already been terminated.
   */
  public void sendInput(byte[] data) {
    if (data != null && data.length > 0 && runner.isAlive()) {
      input.add(data);
      runner.interrupt();
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
    final OutputStream os = process.getOutputStream();
    if (!input.isEmpty()) {
      // forwarding input to the process
      while (!input.isEmpty()) {
        final byte[] data = input.poll();
        for (final byte datum : data) {
          final int value = datum & 0xff;
          try {
            os.write(value);
            if (value == 0x0a) {
              os.flush();
            }
          } catch (IOException e) {
            Logger.error(e, "I/O error while writing input data to process output stream");
          }
        }
      }
    }

    // unconditional flush as requested
    if (forceFlush) {
      try {
        os.flush();
      } catch (IOException ignored) {
      }
    }
  }
}
