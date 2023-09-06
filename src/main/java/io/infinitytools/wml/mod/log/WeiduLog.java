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
package io.infinitytools.wml.mod.log;

import io.infinitytools.wml.mod.ModInfo;
import org.tinylog.Logger;

import java.io.InputStream;
import java.net.URL;
import java.nio.ByteBuffer;
import java.nio.charset.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;

/**
 * Provides details about installed mods from the WeiDU.log file.
 */
public class WeiduLog implements Iterable<WeiduLogEntry> {
  /** Name of the WeiDU.log file. */
  public static final String WEIDU_FILENAME = "WeiDU.log";

  /** Character sets to try out when loading WeiDU.log file content. */
  private static final Charset[] CHARSETS = {
      StandardCharsets.UTF_8, StandardCharsets.ISO_8859_1, StandardCharsets.US_ASCII
  };

  /**
   * Parses data derived from the given {@link Path} into a WeiDU log structure.
   *
   * @param log A {@link Path} pointing to the WeiDU.log.
   * @return A fully initialized {@link WeiduLog} instance from the given WeiDU.log data. Returns {@code null} if
   *         {@code log} is null.
   * @throws Exception If log content could not be parsed.
   */
  public static WeiduLog load(Path log) throws Exception {
    if (log != null) {
      String content = null;
      for (final Charset cs : CHARSETS) {
        try {
          content = Files.readString(log, cs);
          break;
        } catch (CharacterCodingException e) {
          Logger.debug("Incompatible character encoding", e);
        }
      }
      if (content != null) {
        return new WeiduLog(Arrays.asList(content.split("\r?\n")));
      }
    }
    return null;
  }

  /**
   * Parses data derived from the given {@link URL} into a WeiDU log structure.
   *
   * @param log A {@link URL} pointing to the WeiDU.log.
   * @return A fully initialized {@link WeiduLog} instance from the given WeiDU.log data. Returns {@code null} if
   *         {@code log} is null.
   * @throws Exception If log content could not be parsed.
   */
  public static WeiduLog load(URL log) throws Exception {
    if (log != null) {
      byte[] buf;
      try (final InputStream is = log.openStream()) {
        buf = is.readAllBytes();
      }

      if (buf != null) {
        String content = null;
        for (final Charset cs : CHARSETS) {
          try {
            final CharsetDecoder decoder = cs
                .newDecoder()
                .onMalformedInput(CodingErrorAction.REPORT)
                .onUnmappableCharacter(CodingErrorAction.REPORT);
            content = decoder.decode(ByteBuffer.wrap(buf)).toString();
            break;
          } catch (CharacterCodingException e) {
            Logger.debug("Incompatible character encoding", e);
          }
        }

        if (content != null) {
          return new WeiduLog(Arrays.asList(content.split("\r?\n")));
        }
      }
    }
    return null;
  }

  /**
   * Parses data derived from the given string into a WeiDU log structure.
   *
   * @param log String containing WeiDU.log data.
   * @return A fully initialized {@link WeiduLog} instance from the given WeiDU.log data. Returns {@code null} if
   *         {@code log} is null.
   * @throws Exception If log content could not be parsed.
   */
  public static WeiduLog load(String log) throws Exception {
    if (log != null) {
      return new WeiduLog(Arrays.asList(log.split("\r?\n")));
    }
    return null;
  }

  private final List<WeiduLogEntry> entries = new ArrayList<>();

  private WeiduLog(List<String> lines) throws Exception {
    init(lines);
  }

  /**
   * Returns a collection of all log entries.
   *
   * @return Collection of available log entries.
   */
  public Collection<WeiduLogEntry> getEntries() {
    return Collections.unmodifiableList(entries);
  }

  /**
   * Returns a collection of all log entries matching the specified mod name.
   *
   * @param modName Name of the mod to filter.
   * @return Collection of matching log entries.
   */
  public Collection<WeiduLogEntry> getEntries(String modName) {
    final String search = ModInfo.stripModName(modName, true);
    return entries.stream().filter(e -> e.getTp2Name().equals(search)).toList();
  }

  /** Returns the number of available log entries. */
  public int getEntryCount() {
    return entries.size();
  }

  /**
   * Returns the {@link WeiduLogEntry} at the specified index.
   *
   * @param index Index of the log entry.
   * @return {@link WeiduLogEntry} instance at the specified index.
   * @throws IndexOutOfBoundsException If the index is out of range.
   */
  public WeiduLogEntry getEntry(int index) throws IndexOutOfBoundsException {
    return entries.get(index);
  }

  /**
   * Returns whether the WeiDU.log contains entries of the specified mod.
   *
   * @param modName Name of the mod to search.
   * @return {@code true} if one or more entries are found, {@code false} otherwise.
   */
  public boolean contains(String modName) {
    boolean retVal = false;

    if (modName != null) {
      final String search = ModInfo.stripModName(modName, true);
      retVal = entries.stream().anyMatch(e -> e.getTp2Name().equalsIgnoreCase(search));
    }

    return retVal;
  }

  @Override
  public Iterator<WeiduLogEntry> iterator() {
    return Collections.unmodifiableList(entries).iterator();
  }

  @Override
  public String toString() {
    final StringBuilder sb = new StringBuilder();
    sb.append("// Log of Currently Installed WeiDU Mods").append('\n');
    sb.append("// The top of the file is the 'oldest' mod").append('\n');
    sb.append("// ~TP2_File~ #language_number #component_number").append('\n');
    for (final WeiduLogEntry entry : entries) {
      sb.append(entry.toString()).append('\n');
    }
    return sb.toString();
  }

  private void init(List<String> lines) throws Exception {
    if (lines != null) {
      for (final String line : lines) {
        final String s = line.replaceFirst("//.*", "").strip();
        if (!s.isEmpty()) {
          final WeiduLogEntry entry = new WeiduLogEntry(s);
          entries.add(entry);
        }
      }
    }
  }
}
