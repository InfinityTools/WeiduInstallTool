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
import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.charset.*;
import java.util.Objects;
import java.util.concurrent.locks.ReentrantLock;

/**
 * A thread that is responsible for fetching data from the standard output of a child process and print it to the
 * standard output of the main process.
 */
public class InputStreamConsumer implements Runnable {
  private final StringBuilder buffer = new StringBuilder(16384);
  private final ReentrantLock bufferLock = new ReentrantLock();
  private final ByteArrayOutputStream baos = new ByteArrayOutputStream(4096);

  private final Process process;
  private final CharsetDecoder decoder;
  private final Thread runner;

  /**
   * Bytes remaining after a string conversion pass.
   */
  private byte[] remaining;

  /**
   * Creates a new consumer that monitors a given {@link Process} and fetches process output as text in
   * UTF-8.
   *
   * @param process The underlying {@link Process}.
   */
  public InputStreamConsumer(Process process) {
    this(process, StandardCharsets.UTF_8);
  }

  /**
   * Creates a new consumer that monitors a given {@link Process} and fetches process output as text in the specified
   * character encoding.
   *
   * @param process The underlying {@link Process}.
   * @param charset {@link Charset} used to convert process raw output data into text.
   */
  public InputStreamConsumer(Process process, Charset charset) {
    this.process = Objects.requireNonNull(process);

    final Charset charset1 = (charset != null) ? charset : StandardCharsets.UTF_8;
    this.decoder = charset1.newDecoder();

    this.decoder.replaceWith("\ufffd"); // default replacement character
    this.decoder.onMalformedInput(CodingErrorAction.REPLACE);
    this.decoder.onUnmappableCharacter(CodingErrorAction.REPLACE);

    this.runner = Thread.ofVirtual().start(this);
  }

  /**
   * Returns the content that has been accumulated since the last call of this method.
   */
  public String getBufferedContent() {
    try {
      bufferLock.lock();
      final String retVal = buffer.toString();
      buffer.delete(0, buffer.length());
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
      final byte[] outBuf;
      if (remaining != null) {
        final byte[] osBuf = baos.toByteArray();
        outBuf = new byte[remaining.length + osBuf.length];
        System.arraycopy(remaining, 0, outBuf, 0, remaining.length);
        System.arraycopy(osBuf, 0, outBuf, remaining.length, osBuf.length);
      } else {
        outBuf = baos.toByteArray();
      }

      try {
        final ByteBuffer bb = ByteBuffer.wrap(outBuf);
        final CharBuffer cb = decoder.decode(bb);
        if (cb.limit() > 0) {
          try {
            bufferLock.lock();
            buffer.append(cb);
          } finally {
            bufferLock.unlock();
          }
        }
        baos.reset();

        // remaining undecodable bytes (if any) are stored for the next pass
        if (bb.limit() < bb.capacity()) {
          bb.position(bb.limit());
          bb.limit(bb.capacity());
          remaining = new byte[bb.remaining()];
          bb.get(remaining, 0, remaining.length);
        } else {
          remaining = null;
        }
      } catch (CharacterCodingException e) {
        Logger.debug(e, "Incompatible character encoding");
      }
    }

    return len;
  }
}
