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
package io.infinitytools.wml.io;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;

/**
 * A specialized {@link InputStream} class which allows reading from byte data that is dynamically added to the
 * underlying buffer as needed.
 */
public class DynamicByteInputStream extends InputStream {
  /**
   * Contains buffered data for reading.
   */
  private final List<byte[]> buffers = new ArrayList<>();

  /**
   * Reference to the current byte array.
   */
  private byte[] curBuffer;

  /**
   * Current offset in the byte array.
   */
  private int offset;

  public DynamicByteInputStream() {
    this.curBuffer = null;
    this.offset = -1;
  }

  /**
   * Adds a copy of the specified byte array to the internal buffer of the input stream for consumption.
   *
   * @param buffer Byte array to add to the internal read buffer.
   * @throws NullPointerException if {@code buffer} is {@code null}.
   */
  public void putBytes(byte[] buffer) {
    if (buffer == null) {
      throw new NullPointerException();
    }

    buffers.add(Arrays.copyOf(buffer, buffer.length));
  }

  @Override
  public synchronized int read() {
    if (curBuffer == null) {
      return -1;
    }

    final int retVal = curBuffer[offset] & 0xff;
    offset++;

    updateBuffer();

    return retVal;
  }

  @Override
  public synchronized int read(byte[] b, int off, int len) throws IOException {
    Objects.checkFromIndexSize(off, len, b.length);

    int avail = available();
    if (avail == 0) {
      return -1;
    }

    if (len > avail) {
      len = avail;
    }
    if (len <= 0) {
      return 0;
    }

    int written = 0;
    while (written < len) {
      int curLen = Math.min(len - written, curBuffer.length - offset);
      System.arraycopy(curBuffer, offset, b, off + written, curLen);
      written += curLen;
      offset += curLen;
      updateBuffer();
    }

    return written;
  }

  @Override
  public synchronized long skip(long n) throws IOException {
    int avail = available();
    if (avail < n) {
      n = avail;
    }

    long skipped = 0;
    while (skipped < avail) {
      long curLen = Math.min(n - skipped, curBuffer.length - offset);
      skipped += curLen;
      offset += (int) curLen;
      updateBuffer();
    }

    return skipped;
  }

  @Override
  public synchronized int available() throws IOException {
    int retVal = 0;

    // add remainder of current buffer
    if (curBuffer != null) {
      retVal = curBuffer.length - offset;
    }

    // add total size of remaining buffers
    for (final byte[] buffer : buffers) {
      retVal += buffer.length;
    }

    return retVal;
  }

  /**
   * Ensures that buffer and offset are properly initialized.
   */
  private void updateBuffer() {
    if (curBuffer != null && offset >= curBuffer.length) {
      curBuffer = null;
      offset = -1;
    }

    if (curBuffer == null && !buffers.isEmpty()) {
      curBuffer = buffers.remove(0);
      offset = 0;
    }
  }
}
