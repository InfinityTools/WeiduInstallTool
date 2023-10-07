/*
 * Copyright (c) 2023 Argent77
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
import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.charset.*;
import java.util.Arrays;
import java.util.Objects;
import java.util.concurrent.locks.ReentrantLock;

/**
 * A utility class that takes care of converting raw byte data into text using any desired character set.
 */
public class BufferConvert {
  /**
   * Stores the result of a text decoding operation.
   *
   * @param decoded   The decoded text content.
   * @param remaining Remaining undecodable byte data.
   */
  public record DecodedData(String decoded, byte[] remaining) {
  }

  /**
   * Storage of all accumulated byte data.
   */
  private final ByteArrayOutputStream baos = new ByteArrayOutputStream(65536);
  private final StringBuilder text = new StringBuilder(65536);
  private final ReentrantLock bufferLock = new ReentrantLock();

  private CharsetDecoder decoder;
  private boolean failOnError;
  private String lastText;
  private byte[] remaining;

  /**
   * Decodes the specified raw byte data into a string. The data is assumed to be UTF-8 encoded.
   *
   * @param data Raw byte array.
   * @return A {@link DecodedData} record consisting of the decoded text data and, optionally, remaining undecoded bytes.
   * @throws NullPointerException if {@code data} is {@code null}.
   */
  public static DecodedData decodeBytes(byte[] data) {
    return decodeBytes(data, StandardCharsets.UTF_8);
  }

  /**
   * Decodes the specified raw byte data with the given character set into a string.
   *
   * @param data    Raw byte array.
   * @param charset {@link Charset} used to decode bytes into characters. Specify {@code null} to use the default
   *                charset (UTF-8).
   * @return A {@link DecodedData} record consisting of the decoded text data and, optionally, remaining undecoded bytes.
   * @throws NullPointerException if {@code data} is {@code null}.
   */
  public static DecodedData decodeBytes(byte[] data, Charset charset) {
    try {
      return decodeBytes(data, charset, false);
    } catch (IOException e) {
      Logger.debug("Decoding data: {}", e);
    }
    return new DecodedData("", null);
  }

  /**
   * Decodes the specified raw byte data with the given character set into a string.
   *
   * @param data        Raw byte array.
   * @param charset     {@link Charset} used to decode bytes into characters. Specify {@code null} to use the default
   *                    charset (UTF-8).
   * @param failOnError Specify {@code true} to throw an exception when undecodable data is encountered. Specify
   *                    {@code false} to drop the faulty data and add a replacement character instead.
   * @return A {@link DecodedData} record consisting of the decoded text data and, optionally, remaining undecoded bytes.
   * @throws IOException          If {@code failOnError} is {@code true} and a coding error occurs.
   * @throws NullPointerException if {@code data} is {@code null}.
   */
  public static DecodedData decodeBytes(byte[] data, Charset charset, boolean failOnError) throws IOException {
    Objects.requireNonNull(data, "data is null");
    if (charset == null) {
      charset = StandardCharsets.UTF_8;
    }

    final CharsetDecoder decoder = charset.newDecoder();
    final CodingErrorAction action = failOnError ? CodingErrorAction.REPORT : CodingErrorAction.REPLACE;
    decoder.onUnmappableCharacter(action);
    decoder.onMalformedInput(action);

    return decodeData(data, decoder);
  }

  /**
   * Encodes the specified text into a raw byte array through the UTF-8 charset encoder.
   *
   * @param text        Text to convert.
   * @return Byte array representation of the text string.
   * @throws NullPointerException if {@code text} is {@code null}.
   */
  public static byte[] encodeBytes(String text) {
    return encodeBytes(text, StandardCharsets.UTF_8);
  }

  /**
   * Encodes the specified text with the given character set into a raw byte array.
   *
   * @param text        Text to convert.
   * @param charset     {@link Charset} used to encode characters into bytes. Specify {@code null} to use the default
   *                    charset (UTF-8).
   * @return Byte array representation of the text string.
   * @throws NullPointerException if {@code text} is {@code null}.
   */
  public static byte[] encodeBytes(String text, Charset charset) {
    try {
      return encodeBytes(text, charset, false);
    } catch (IOException e) {
      Logger.debug(e, "Encoding data (ignored)");
    }
    return new byte[0];
  }

  /**
   * Encodes the specified text with the given character set into a raw byte array.
   *
   * @param text        Text to convert.
   * @param charset     {@link Charset} used to encode characters into bytes. Specify {@code null} to use the default
   *                    charset (UTF-8).
   * @param failOnError Specify {@code true} to throw an exception when undecodable characters are encountered. Specify
   *                    {@code false} to skip the character instead.
   * @return Byte array representation of the text string.
   * @throws IOException          If {@code failOnError} is {@code true}} and a coding error occurs.
   * @throws NullPointerException if {@code text} is {@code null}.
   */
  public static byte[] encodeBytes(String text, Charset charset, boolean failOnError) throws IOException {
    Objects.requireNonNull(text, "string is null");
    if (charset == null) {
      charset = StandardCharsets.UTF_8;
    }

    final CharsetEncoder encoder = charset.newEncoder();
    final CodingErrorAction action = failOnError ? CodingErrorAction.REPORT : CodingErrorAction.REPLACE;
    encoder.onUnmappableCharacter(action);
    encoder.onMalformedInput(action);

    final CharBuffer cb = CharBuffer.wrap(text);
    final ByteBuffer bb = encoder.encode(cb);

    final byte[] retVal = new byte[bb.limit()];
    if (bb.limit() > 0) {
      bb.get(0, retVal);
    }

    return retVal;
  }

  /**
   * Used internally to decode byte data into text.
   *
   * @param data    byte data to convert.
   * @param decoder {@link CharsetDecoder} instance for decoding byte data to text.
   * @return A {@link DecodedData} structure that contains both decoded text and remaining undecodable bytes.
   * @throws IOException If a decoding error occurs.
   */
  private static DecodedData decodeData(byte[] data, CharsetDecoder decoder) throws IOException {
    String text = "";
    byte[] remaining = null;

    if (data != null) {
      final ByteBuffer bb = ByteBuffer.wrap(data);
      final CharBuffer cb = decoder.decode(bb);

      // remaining undecodable bytes are stored for the next pass
      if (bb.limit() < bb.capacity()) {
        remaining = Arrays.copyOfRange(data, bb.limit(), bb.capacity());
      }

      // decoded text can be retrieved until the next call of this method
      text = cb.toString();
    }

    return new DecodedData(text, remaining);
  }

  public BufferConvert() {
    this(StandardCharsets.UTF_8, false);
  }

  public BufferConvert(Charset charset) {
    this(charset, false);
  }

  public BufferConvert(Charset charset, boolean failOnError) {
    this.failOnError = failOnError;
    this.lastText = "";
    setCharset(charset);
  }

  public boolean isFailOnError() {
    return failOnError;
  }

  public void setFailOnError(boolean value) {
    if (value != failOnError) {
      failOnError = value;
      setCharset(getCharset());
    }
  }

  public Charset getCharset() {
    return decoder.charset();
  }

  /**
   * Switches to the specified charset.
   * <p>
   * All accumulated text content will be discarded and rebuilt with the new charset.
   * </p>
   *
   * @param charset The new {@link Charset} to use. Specify {@code null} to fall back to the default charset (UTF-8).
   */
  public void setCharset(Charset charset) {
    if (charset == null) {
      charset = StandardCharsets.UTF_8;
    }

    try {
      bufferLock.lock();

      boolean reset = false;
      if (decoder == null || !decoder.charset().equals(charset)) {
        decoder = charset.newDecoder();
        reset = true;
      }
      CodingErrorAction action = isFailOnError() ? CodingErrorAction.REPORT : CodingErrorAction.REPLACE;
      decoder.onMalformedInput(action);
      decoder.onUnmappableCharacter(action);

      if (reset) {
        // rebuilding text content
        try {
          final DecodedData cd = decodeData(baos.toByteArray(), decoder);
          lastText = cd.decoded();
          remaining = cd.remaining();
          text.delete(0, text.length());
          text.append(lastText);
        } catch (IOException e) {
          Logger.warn(e, "Setting charset: {}", charset);
        }
      }
    } finally {
      bufferLock.unlock();
    }
  }

  /**
   * Returns all accumulated content as a string.
   */
  public String getText() {
    return text.toString();
  }

  /**
   * Returns the text content since the the last call of {@link #decode(byte[])}. Returns an empty string if
   * no content is available.
   */
  public String getLastText() {
    return lastText;
  }

  /**
   * Returns all accumulated content as raw byte data.
   */
  public byte[] getBuffer() {
    return baos.toByteArray();
  }

  /**
   * Returns the remaining byte data from the last conversion operation that could not be decoded into
   * textual content. Returns an empty array if no remaining bytes are available.
   */
  public byte[] getRemaining() {
    if (remaining != null) {
      return Arrays.copyOf(remaining, remaining.length);
    } else {
      return new byte[0];
    }
  }

  /**
   * Decodes the specified byte buffer into a text string.
   *
   * @param data byte data to convert using the current {@link Charset}.
   * @return A string with the decoded buffer content.
   * @throws CharacterCodingException if byte data could not be decoded into valid characters. This exception is only
   *                                  thrown if {@link #isFailOnError()} returns {@code true}.
   * @throws IOException              if an I/O error occurs.
   */
  public String decode(byte[] data) throws IOException {
    try {
      bufferLock.lock();
      if (data != null) {
        final byte[] buf;
        if (remaining != null && remaining.length > 0) {
          buf = new byte[remaining.length + data.length];
          System.arraycopy(remaining, 0, buf, 0, remaining.length);
          System.arraycopy(data, 0, buf, remaining.length, data.length);
        } else {
          buf = data;
        }

        final DecodedData cd = decodeData(buf, decoder);
        remaining = cd.remaining();
        lastText = cd.decoded();
        text.append(lastText);

        // full data block is added to the total buffer instead of just decodable data
        baos.write(data);
      } else {
        lastText = "";
      }
    } finally {
      bufferLock.unlock();
    }

    return lastText;
  }

  /**
   * Encodes the specified string into a byte array, using the current {@link Charset}, and adds it to the
   * accumulated content class of this instance.
   * <p>
   * A subsequent call of {@link #getLastText()} will return the text encoded by this method, optionally prepended by
   * content from the remaining bytes of the last {@link #decode(byte[])} operation.
   * </p>
   *
   * @param text Text to convert using the current {@link Charset}.
   * @return Byte sequence of the encoded text string.
   * @throws IOException if an I/O error occurs.
   */
  public byte[] encode(String text) throws IOException {
    final byte[] retVal = encodeBytes(text, getCharset(), isFailOnError());
    decode(retVal);
    return retVal;
  }

  /**
   * Discards any remaining byte data that hasn't been decoded to character data yet.
   */
  public void discardRemaining() {
    remaining = null;
  }

  /**
   * Clears all accumulated byte data.
   */
  public void discardAll() {
    try {
      bufferLock.lock();
      discardRemaining();
      baos.reset();
      text.delete(0, text.length());
    } finally {
      bufferLock.unlock();
    }
  }
}
