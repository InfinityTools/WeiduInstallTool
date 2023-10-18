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
package io.infinitytools.wit.net;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * Class providing methods for encoding and decoding content for simple network communication through sockets.
 */
public class NetData {
  public enum Type {
    /**
     * Ping request sent by the client. Content provides a boolean value as string (true/false) which indicates
     * whether the app should be made visible and receive window focus.
     */
    REQ_PING(0, 1),
    /**
     * Ping acknowledgement sent by the server. Requires no further content arguments.
     */
    ACK_PING(1, 0),
    /**
     * Request for command execution sent by the client. Content provides the command line arguments that are to be
     * passed to the WeiDU process.
     */
    REQ_EXEC(2, -1),
    /**
     * Command execution acknowledgement sent by the client. Content provides a boolean value as string (true/false)
     * that indicates whether the command is accepted by the app.
     */
    ACK_EXEC(3, 1),
    /**
     * Request for server termination. Requires no further content arguments.
     */
    REQ_TERM(9, 0),
    /**
     * Server termination acknowledgement sent by the server. Requires no further content arguments.
     */
    ACK_TERM(10, 0),
    ;

    /**
     * Returns the {@link Type} enum associated with the given id. Returns {@code null} if no matching Type enum
     * could be found.
     */
    public static Type parse(int id) {
      Type retVal = null;
      for (final Type type : Type.values()) {
        if (type.getId() == id) {
          retVal = type;
          break;
        }
      }
      return retVal;
    }

    private final int id;
    private final int numArgs;

    Type(int id, int numArgs) {
      this.id = id;
      this.numArgs = numArgs;
    }

    /**
     * Returns the numeric identifier of the data type.
     */
    public int getId() {
      return id;
    }

    /**
     * Returns the number of expected arguments for this data type. {@code -1} indicates to expect a variable number of
     * arguments.
     */
    public int getNumArgs() {
      return numArgs;
    }
  }

  /**
   * Identifier for a {@link NetData} record.
   */
  public static final String ID = "wit";

  /**
   * Symbolic placeholder for a semicolon character.
   */
  private static final String SEMICOLON = ":semicolon:";

  private final List<String> content = new ArrayList<>();

  private final Type type;

  /**
   * A convenience method that encodes the given arguments into a data string.
   *
   * @param type    {@link Type} of the {@link NetData} structure.
   * @param content Variable number of strings associated with the data type.
   * @return String with encoded data.
   * @throws IllegalArgumentException if incompatible {@code content} is specified for the given {@code type}.
   * @throws NullPointerException     if type is {@code null}.
   */
  public static String encode(Type type, String... content) throws IllegalArgumentException, NullPointerException {
    return new NetData(type, content).toString();
  }

  /**
   * A convenience method that decodes the given string into a {@link NetData} instance.
   *
   * @param data Encoded data as string.
   * @return Decoded {@link NetData} instance.
   * @throws IllegalArgumentException If {@code data} could not be decoded.
   * @throws NullPointerException     If data is {@code null}.
   */
  public static NetData decode(String data) throws IllegalArgumentException, NullPointerException {
    return new NetData(data);
  }

  /**
   * Encodes the content string by replacing all literal semicolons to a placeholder.
   * Returns an empty string if the string argument is {@code null}.
   */
  private static String encodeString(String s) {
    if (s != null) {
      return s.replace(";", SEMICOLON);
    }
    return "";
  }

  /**
   * Decodes the content string by replacing all semicolon placeholders by the literal character.
   * Returns an empty string if the string argument is {@code null}.
   */
  private static String decodeString(String s) {
    if (s != null) {
      return s.replace(SEMICOLON, ";");
    }
    return "";
  }

  /**
   * Creates a {@link NetData} instance with the specified parameters.
   *
   * @param type    {@link Type} of the {@link NetData} structure.
   * @param content Variable number of strings associated with the data type.
   * @throws IllegalArgumentException if incompatible {@code content} is specified for the given {@code type}.
   * @throws NullPointerException     if type is {@code null}.
   */
  public NetData(Type type, String... content) throws IllegalArgumentException, NullPointerException {
    if (type == null) {
      throw new NullPointerException("type is null");
    }

    if (content.length < type.getNumArgs()) {
      throw new IllegalArgumentException(String.format("Unexpected number of arguments (expected: %d, found: %d)",
          type.getNumArgs(), content.length));
    }

    this.type = type;
    Arrays.stream(content).forEach(item -> NetData.this.content.add(decodeString(item)));
  }

  /**
   * Creates a {@link NetData} instance from the specified encoded data string.
   *
   * @param data Encoded data as string.
   * @throws IllegalArgumentException If {@code data} could not be decoded.
   * @throws NullPointerException     If data is {@code null}.
   */
  public NetData(String data) throws IllegalArgumentException, NullPointerException {
    final String[] items = getDataItems(data);

    final int id;
    try {
      id = Integer.parseInt(items[1]);
    } catch (NumberFormatException e) {
      throw new IllegalArgumentException("Invalid type definition found");
    }

    final Type type = Type.parse(id);
    if (type == null) {
      throw new IllegalArgumentException("Unsupported type definition: " + id);
    }

    if (items.length - 2 < type.getNumArgs()) {
      throw new IllegalArgumentException(String.format("Unexpected number of content entries (expected: %d, found: %d)",
          type.getNumArgs(), items.length - 2));
    }

    this.type = type;
    Arrays.stream(items).skip(2).forEach(item -> NetData.this.content.add(decodeString(item)));
  }

  /**
   * Returns the data type.
   */
  public Type getType() {
    return type;
  }

  /**
   * Returns the data content as a list of strings.
   */
  public List<String> getContent() {
    return Collections.unmodifiableList(content);
  }

  /**
   * Returns the {@link NetData} instance encoded into a {@code String}.
   */
  public String getEncoded() {
    StringBuilder sb = new StringBuilder();
    sb.append(ID).append(';');
    sb.append(type.getId()).append(';');

    int numArgs = type.getNumArgs() >= 0 ? type.getNumArgs() : content.size();
    for (int i = 0; i < numArgs; i++) {
      if (i < content.size()) {
        final String s = encodeString(content.get(i));
        sb.append(s);
      }
      sb.append(';');
    }

    sb.append('\n');
    return sb.toString();
  }

  /**
   * Splits the given data string into individual data fields.
   *
   * @param data Encoded data as string.
   * @return Array of data fields.
   * @throws IllegalArgumentException If {@code data} could not be split.
   */
  private static String[] getDataItems(String data) throws IllegalArgumentException {
    if (data == null) {
      throw new NullPointerException("data is null");
    }

    // discarding unneeded characters
    String dataStripped = data.strip();
    while (dataStripped.endsWith(";")) {
      dataStripped = dataStripped.substring(0, dataStripped.length() - 1);
    }

    final String[] items = dataStripped.split(";");
    if (items.length < 2) {
      throw new IllegalArgumentException("Not enough data fields available");
    }

    if (!ID.equals(items[0])) {
      throw new IllegalArgumentException("Invalid identifier found");
    }
    return items;
  }

  @Override
  public String toString() {
    return getEncoded();
  }
}
