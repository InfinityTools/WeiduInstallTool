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
import java.io.InputStream;
import java.util.Objects;
import java.util.concurrent.locks.ReentrantLock;

/**
 * A thread that is responsible for fetching data from the standard output of a child process and print it to the
 * standard output of the main process.
 */
public class InputStreamConsumer implements Runnable {
  private final ByteArrayOutputStream buffer = new ByteArrayOutputStream(16384);
  private final ReentrantLock bufferLock = new ReentrantLock();
  private final ByteArrayOutputStream baos = new ByteArrayOutputStream(4096);

  private final Process process;
  private final Thread runner;

  /**
   * Creates a new consumer that monitors a given {@link Process} and fetches process output as raw byte data.
   *
   * @param process The underlying {@link Process}.
   */
  public InputStreamConsumer(Process process) {
    this.process = Objects.requireNonNull(process);
    this.runner = Thread.ofVirtual().start(this);
  }

  /**
   * Returns the raw bytes that have been accumulated since the last call of this method or {@link #getBuffer()}.
   */
  public byte[] getBufferedBytes() {
    try {
      bufferLock.lock();
      final byte[] retVal = buffer.toByteArray();
      buffer.reset();
      return retVal;
    } finally {
      bufferLock.unlock();
    }
  }

  /**
   * Returns the buffered data that has been accumulated since the last call of this method or
   * {@link #getBufferedBytes()}.
   */
  public byte[] getBuffer() {
    try {
      bufferLock.lock();
      final byte[] retVal = buffer.toByteArray();
      buffer.reset();
      return retVal;
    } finally {
      bufferLock.unlock();
    }
  }

  /**
   * Returns the {@link Thread} instance that runs this consumer.
   */
  public Thread getRunner() {
    return runner;
  }

  @Override
  public void run() {
    try {
      final long minDuration = 20;
      final long maxDuration = 100;
      long duration = minDuration;
      boolean busy;
      while (process.isAlive()) {
        // forwarding process output
        busy = (forward() != 0);

        // reducing cpu load
        if (!busy) {
          duration = Math.min(duration + 10, maxDuration);
        } else {
          duration = minDuration;
        }

        try {
          Thread.sleep(duration);
        } catch (InterruptedException e) {
          // no feedback; may be intentional
          Logger.trace(e, "Polling thread interrupted");
        }
      }

      // forward pending process output
      forward();
    } catch (IOException e) {
      Logger.error(e, "Error in polling loop");
    }
  }

  private int forward() throws IOException {
    int len = 0;

    final InputStream is = process.getInputStream();
    while (is.available() > 0) {
      int value = is.read();
      if (value == -1) {
        break;
      }

      baos.write(value);
    }

    if (baos.size() > 0) {
      final byte[] outBuf = baos.toByteArray();
      try {
        try {
          bufferLock.lock();
          buffer.write(outBuf);
        } finally {
          bufferLock.unlock();
        }
        baos.reset();
      } catch (IOException e) {
        Logger.debug(e, "Writing buffer data");
      }
    }

    return len;
  }
}
