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

import java.nio.file.Path;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Represents a single WeiDU.log mod component entry.
 */
public class WeiduLogEntry implements Comparable<WeiduLogEntry> {
  private Path tp2File;
  private int language;
  private int component;

  /**
   * Parses the specified line for mod component information.
   *
   * @param line Line from WeiDU.log with mod component information.
   * @throws Exception If the parsing process failed.
   */
  public WeiduLogEntry(String line) throws Exception {
    if (line == null) {
      throw new NullPointerException("String is null");
    }
    parse(line);
  }

  /**
   * Returns the TP2 file path defined by this log entry.
   */
  public Path getTp2File() {
    return tp2File;
  }

  /**
   * Returns the language number defined by this log entry.
   */
  public int getLanguage() {
    return language;
  }

  /**
   * Returns the component number defined by this log entry.
   */
  public int getComponent() {
    return component;
  }

  /**
   * Returns the base name of the TP2 file without the path, "setup-" prefix and file extension part.
   */
  public String getTp2Name() {
    return ModInfo.stripModName(getTp2File().getFileName().toString(), true);
  }

  @Override
  public int compareTo(WeiduLogEntry o) {
    if (o != null) {
      return getTp2Name().compareTo(o.getTp2Name());
    }
    return 1;
  }

  @Override
  public String toString() {
    return String.format("~%s~ #%d #%d", getTp2File().toString(), language, component);
  }

  /**
   * Parses the specified line and initializes the {@link WeiduLogEntry}.
   */
  private void parse(String line) throws Exception {
    final Pattern p = Pattern.compile("~([^~]*)~\\s+#(\\d+)\\s+#(\\d+)");
    final Matcher m = p.matcher(strip(line));
    if (m.find()) {
      if (m.groupCount() >= 3) {
        String tp2File = m.group(1).toLowerCase();
        if (!tp2File.endsWith(".tp2")) {
          throw new Exception("Not a valid TP2 file: " + tp2File);
        }
        this.tp2File = Path.of(tp2File);
        this.language = toNumber(m.group(2), "Not a valid language number");
        this.component = toNumber(m.group(3), "Not a valid component number");
      } else {
        throw new Exception("Not a valid log entry");
      }
    } else {
      throw new Exception("Not a valid log entry");
    }
  }

  /**
   * Helper method to throw a customized exception message if the string could not be converted to a number.
   */
  private int toNumber(String s, String error) throws Exception {
    try {
      return Integer.parseInt(s);
    } catch (NumberFormatException e) {
      throw new Exception(error);
    }
  }

  /**
   * Removes comments as well as leading and trailing whitespace from the string.
   */
  private static String strip(String s) {
    if (s != null) {
      return s.replaceFirst("//.*", "").strip();
    }
    return null;
  }
}
