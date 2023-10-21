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
package io.infinitytools.wit.ini;

import org.tinylog.Logger;

import java.io.InputStream;
import java.io.Reader;
import java.net.URL;
import java.nio.CharBuffer;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.function.Predicate;

public class IniMap implements Iterable<IniMapSection> {
  /**
   * Available comment styles.
   */
  public enum Style {
    /**
     * C-style type of comment, starting with two consecutive slashes ({@code //}).
     */
    C("//"),
    /**
     * Windows-style type of comment, starting with a semicolon ({@code ;}).
     */
    Windows(";"),
    /**
     * Unix-style type of comment, starting with a hash sign ({@code #}).
     */
    Unix("#"),
    /**
     * Any of the available comment styles are supported.
     * {@link Style#Unix}-style types of comment are used for output.
     */
    Any("#", ";", "//"),
    ;

    private final String[] prefix;

    Style(String... prefix) {
      this.prefix = prefix;
    }

    /**
     * Returns the comment prefix as string. The first prefix is returned if more than one prefix is available.
     */
    public String getPrefix() {
      return (prefix.length > 0) ? prefix[0] : "";
    }

    /**
     * Returns all supported comment prefixes by this style.
     */
    public String[] getPrefixes() {
      return Arrays.copyOf(prefix, prefix.length);
    }

    @Override
    public String toString() {
      return getPrefix();
    }

    /**
     * Returns the default comment type for this application.
     */
    public static Style getDefault() {
      return Style.Unix;
    }
  }

  public enum Options {
    /**
     * This option enables a hack that allows "Description" key definitions to continue over multiple lines.
     */
    MultiLineDescription,
    /**
     * Specifying this option disables comment detection in the same line as key/value definitions.
     * This hack is primarily intended to allow special characters in metadata definitions.
     */
    StrictLineComments,
  }

  private final List<IniMapSection> sections = new ArrayList<>();

  private Style style;

  /**
   * Parses the data derived from the given {@code Path} into a INI map structure.
   * <p>UTF-8 encoding is used for string encoding.</p>
   *
   * @param ini A {@link Path} pointing to an INI file.
   * @return A fully initialized {@link IniMap} instance from the given INI data.
   * @throws Exception If the INI content cannot be parsed.
   */
  public static IniMap parse(Path ini, Style style, Options... options) throws Exception {
    return parse(ini, StandardCharsets.UTF_8, style, options);
  }

  /**
   * Parses the data derived from the given {@code Path} into a INI map structure.
   *
   * @param ini      A {@link Path} pointing to an INI file.
   * @param encoding {@link Charset} required to convert binary data into valid string content.
   * @return A fully initialized {@link IniMap} instance from the given INI data.
   * @throws Exception If the INI content cannot be parsed.
   */
  public static IniMap parse(Path ini, Charset encoding, Style style, Options... options) throws Exception {
    IniMap retVal = null;
    if (ini != null) {
      retVal = parse(Files.readString(ini, encoding), style, options);
    }
    return retVal;
  }

  public static IniMap parse(URL ini, Style style, Options... options) throws Exception {
    return parse(ini, StandardCharsets.UTF_8, style, options);
  }

  /**
   * Parses the data derived from the given {@code URL} into a INI map structure.
   *
   * @param ini      A {@link URL} pointing to INI data.
   * @param encoding {@link Charset} required to convert binary data into valid string content.
   * @return A fully initialized {@link IniMap} instance from the given INI data.
   * @throws Exception If the INI content cannot be parsed.
   */
  public static IniMap parse(URL ini, Charset encoding, Style style, Options... options) throws Exception {
    IniMap retVal = null;
    if (ini != null) {
      if (encoding == null) {
        encoding = StandardCharsets.UTF_8;
      }
      String s;
      try (final InputStream is = ini.openStream()) {
        final byte[] buf = is.readAllBytes();
        s = new String(buf, encoding);
      }
      retVal = parse(s, style, options);
    }
    return retVal;
  }

  /**
   * Parses the data derived from the given {@code Reader} instance into a INI map structure.
   *
   * @param ini A {@link Reader} instance referencing INI data.
   * @return A fully initialized {@link IniMap} instance from the given INI data.
   * @throws Exception If the INI content cannot be parsed.
   */
  public static IniMap parse(Reader ini, Style style, Options... options) throws Exception {
    IniMap retVal = null;
    if (ini != null) {
      final StringBuilder sb = new StringBuilder();
      final char[] buf = new char[512];
      for (int len = ini.read(buf); len >= 0; len = ini.read(buf)) {
        if (len > 0) {
          sb.append(buf, 0, len);
        }
      }
      retVal = parse(sb.toString(), style, options);
    }
    return retVal;
  }

  /**
   * Parses the given string into a INI map structure.
   *
   * @param ini String containing INI data.
   * @return A fully initialized {@link IniMap} instance from the given INI data.
   * @throws Exception If the INI content cannot be parsed.
   */
  public static IniMap parse(String ini, Style style, Options... options) throws Exception {
    IniMap retVal = null;
    if (ini != null) {
      retVal = parse(Arrays.asList(ini.split("\r?\n")), style, options);
    }
    return retVal;
  }

  /**
   * Parses the list of strings into a INI map structure.
   *
   * @param lines INI content as list of strings.
   * @return A fully initialized {@link IniMap} instance from the given INI data.
   * @throws Exception If the INI content cannot be parsed.
   */
  public static IniMap parse(List<String> lines, Style style, Options... options) throws Exception {
    final EnumSet<Options> optionSet;
    if (options.length == 0) {
      optionSet = EnumSet.noneOf(Options.class);
    } else {
      optionSet = EnumSet.copyOf(Arrays.asList(options));
    }

    final String commentPrefix;
    if (style != null) {
      // Style.Any includes multiple comment styles
      commentPrefix = String.join("", style.getPrefixes());
    } else {
      commentPrefix = Style.getDefault().getPrefix();
    }
    final Predicate<Character> fnComment = c -> (commentPrefix.indexOf(c) >= 0);
    final Predicate<Character> fnCommentSlash = c -> (commentPrefix.indexOf('/') >= 0 && c == '/');
    final Predicate<Character> fnEquals = c -> (c == '=');
    final Predicate<Character> fnWhiteSpace = Character::isSpaceChar;
    final Predicate<Character> fnSectionStart = c -> (c == '[');
    final Predicate<Character> fnSectionEnd = c -> (c == ']');
    final Predicate<Character> fnKeyStart = c -> (Character.isLetter(c) || c == '_');
    final Predicate<Character> fnKey = c -> (fnKeyStart.test(c) || Character.isDigit(c) || "-+".indexOf(c) >= 0);
    final Predicate<Character> fnValue = c -> (!fnComment.test(c));
    // Testing for byte order mark in unicode strings
    final Predicate<Character> fnBOM = c -> (c == '\uFEFF');
    final Predicate<Character> fnBOMAsciiFirst = c -> (c == 0xef);
    final Predicate<Character> fnBOMAsciiSecond = c -> (c == 0xbb);
    final Predicate<Character> fnBOMAsciiThird = c -> (c == 0xbf);

    enum State {
      Null, Section, Key, Value, Comment, Unknown,
    }

    IniMap curMap = new IniMap(style);
    IniMapSection curSection = curMap.getDefaultSection();
    IniEntry curEntry = null;
    for (int i = 0, count = lines.size(); i < count; i++) {
      final String line = lines.get(i);
      final int len = line.length();
      CharBuffer cb = CharBuffer.allocate(len);
      State state = State.Null;
      int pos = 0;
      while (pos < len) {
        final boolean escaped;
        final char ch;
        if (state != State.Comment && line.charAt(pos) == '\\') {
          if (pos + 1 < len) {
            final Character newChar = getEscapedChar(line.charAt(pos + 1), style);
            if (newChar != null) {
              pos++;
              ch = line.charAt(pos);
              escaped = true;
            } else {
              throw new Exception(String.format("Illegal escape sequence at line:%d, pos:%d", i + 1, pos + 1));
            }
          } else {
            throw new Exception(String.format("Incomplete escape sequence at line:%d, pos:%d", i + 1, pos + 1));
          }
        } else {
          ch = line.charAt(pos);
          escaped = false;
        }
        switch (state) {
          case Null:
            if (fnBOM.test(ch)) {
              // ignore unicode byte order mark
            } else if (fnBOMAsciiFirst.test(ch) &&
                pos + 2 < len &&
                fnBOMAsciiSecond.test(line.charAt(pos + 1)) &&
                fnBOMAsciiThird.test(line.charAt(pos + 2))) {
              // potential unicode byte order mark in ansi-encoded string?
              pos += 2;
            } else if (!escaped && fnComment.test(ch)) {
              // comment?
              if (fnCommentSlash.test(ch)) {
                if (pos + 1 < len && fnCommentSlash.test(line.charAt(pos + 1))) {
                  state = State.Comment;
                  pos++; // skip second character of comment prefix
                  cb.position(0);
                }
              } else {
                state = State.Comment;
                cb.position(0);
              }
            } else if (fnSectionStart.test(ch)) {
              // section?
              state = State.Section;
              cb.position(0);
            } else if (fnKeyStart.test(ch)) {
              // key?
              state = State.Key;
              cb.position(0).put(ch);
            } else if (fnWhiteSpace.test(ch)) {
              // no change
            } else {
              state = State.Unknown;
              Logger.debug("Skipping invalid characters at line:{}, pos:{}\n", i + 1, pos + 1);
            }
            break;
          case Section:
            if (fnSectionEnd.test(ch)) {
              final String name = cb.flip().toString();
              cb.clear();
//              final String name = line.substring(marker, pos);
              curSection = curMap.addSection(name);
              state = State.Null;
            } else {
              cb.put(ch);
            }
            break;
          case Key:
            if (fnEquals.test(ch)) {
              final String key = cb.flip().toString();
              cb.clear();
              curEntry = curSection.addEntry(key, "");
              state = State.Value;
            } else if (!fnKey.test(ch) && !fnWhiteSpace.test(ch)) {
              // Special exception: Handle "Description" entry more lenient and allow multiple lines
              if (optionSet.contains(Options.MultiLineDescription) &&
                  curEntry != null && curEntry.getKey().equalsIgnoreCase("Description")) {
                cb.put(ch);
                final int capacity = curEntry.getValue().length() + line.length() + 2;
                String curValue = curEntry.getValue() + ' ' + cb.flip();
                cb.clear();
                if (cb.capacity() < capacity) {
                  cb = CharBuffer.allocate(capacity);
                }
                cb.append(curValue);
                state = State.Value;
              } else {
                throw new Exception(String.format("Invalid character for entry key at line:%d, pos:%d", i + 1, pos + 1));
              }
            } else {
              cb.put(ch);
            }
            break;
          case Value: {
            boolean isComment = !escaped && fnComment.test(ch);
            boolean isValue = !isComment && (escaped || fnValue.test(ch));

            if (isComment && !optionSet.contains(Options.StrictLineComments)) {
              if (fnCommentSlash.test(ch)) {
                if (pos + 1 < len && fnCommentSlash.test(line.charAt(pos + 1))) {
                  state = State.Comment;
                  pos++;
                }
              } else {
                state = State.Comment;
              }
              if (state == State.Comment) {
                final String value = cb.flip().toString();
                cb.clear();
                curEntry.setValue(value);
              } else {
                cb.put(ch);
              }
            } else if (isValue || (optionSet.contains(Options.StrictLineComments) && isComment)) {
              cb.put(ch);
            } else {
              throw new Exception(String.format("Invalid character for entry value at line:%d, pos:%d", i + 1, pos + 1));
            }

            break;
          }
          case Comment:
            cb.put(ch);
            break;
          default:
        }
        pos++;
      }

      // cleaning up pending states
      switch (state) {
        case Section: {
          // invalid section?
          final String name = cb.flip().toString();
          cb.clear();
          curSection = curMap.addSection(name);
          Logger.debug("Expected closing bracket (]) at line:{}, pos:{}\n", i + 1, pos + 1);
          break;
        }
        case Key: {
          // entry is missing equals sign and value?
          final String key = cb.flip().toString();
          cb.clear();
          curEntry = curSection.addEntry(key, "");
          Logger.debug("Expected equals sign (=) at line:{}, pos:{}\n", i + 1, pos + 1);
          break;
        }
        case Value: {
          // updating ini entry value
          final String value = cb.flip().toString();
          cb.clear();
          curEntry.setValue(value);
          break;
        }
        case Comment: {
          final String comment = cb.flip().toString();
          cb.clear();
          curSection.addComment(comment);
          break;
        }
        default:
      }
    }

    return curMap;
  }

  /**
   * Returns the character code of an escaped character, or {@code null} if the character is not a valid escape
   * character.
   */
  private static Character getEscapedChar(char ch, Style style) {
    final String commentPrefix;
    if (style != null) {
      // Style.Any includes multiple comment styles
      commentPrefix = String.join("", style.getPrefixes());
    } else {
      commentPrefix = Style.getDefault().getPrefix();
    }

    if (commentPrefix.indexOf(ch) >= 0) {
      return ch;
    } else {
      switch (ch) {
        case '0':
          return '\0';
        case 't':
          return '\t';
        case 'r':
          return '\r';
        case 'n':
          return '\n';
        case 'f':
          return '\f';
        case 'b':
          return '\b';
        case '\\':
        case '\'':
        case '"':
          return ch;
        default:
      }
    }

    return null;
  }

  /**
   * Escapes characters with special meaning.
   */
  static String getEscapedString(String s, Style style) {
    // building escape sequence map
    if (style == null) {
      style = Style.getDefault();
    }
    final HashMap<String, String> table = new HashMap<>();
    String prefixes = String.join("", style.getPrefixes());
    prefixes += "\0\t\r\n\f\b'\"";
    for (int i = 0, len = prefixes.length(); i < len; i++) {
      final String ch = Character.toString(prefixes.charAt(i));
      switch (ch) {
        // only double-slashes are escaped
        case "/" -> table.put("//", "\\/\\/");
        case "\0" -> table.put(ch, "\\0");
        case "\t" -> table.put(ch, "\\t");
        case "\r" -> table.put(ch, "\\r");
        case "\n" -> table.put(ch, "\\n");
        case "\f" -> table.put(ch, "\\f");
        case "\b" -> table.put(ch, "\\b");
        default -> table.put(ch, "\\" + ch);
      }
    }

    // escape characters in string
    s = s.replace("\\", "\\\\");  // must be replaced first to prevent false positives
    for (final Map.Entry<String, String> entry : table.entrySet()) {
      final String search = entry.getKey();
      final String replace = entry.getValue();
      s = s.replace(search, replace);
    }

    return s;
  }

  /**
   * Constructs a new empty INI map with the default comment style.
   */
  public IniMap() {
    this(Style.getDefault());
  }

  /**
   * Constructs a new empty INI map with the specified comment style.
   */
  public IniMap(Style commentStyle) {
    setStyle(commentStyle);
    addSection(IniMapSection.getDefaultSection(this));
  }

  /**
   * Returns the currently used comment style.
   */
  public Style getStyle() {
    return style;
  }

  /**
   * Assigns a new comment style to the INI map.
   */
  public void setStyle(Style newStyle) {
    this.style = (newStyle != null) ? newStyle : Style.getDefault();
  }

  /**
   * Returns the {@link IniMapSection} instance of the section that matches the specified name.
   *
   * @param name Section name. Empty name or {@code null} indicates default section.
   * @return A {@link IniMapSection} instance if a section of the given name is found, {@code null} otherwise.
   */
  public IniMapSection getSection(String name) {
    if (name == null) {
      name = "";
    }
    final String n = name.strip();
    return sections.stream().filter(s -> s.getName().equalsIgnoreCase(n)).findAny().orElse(null);
  }

  /**
   * Returns the default section of this INI map.
   */
  public IniMapSection getDefaultSection() {
    return getSection("");
  }

  /**
   * Adds a new INI section to the map.
   *
   * @param name Name of the section.
   * @return {@link IniMapSection} instance of the added section.
   */
  public IniMapSection addSection(String name) {
    return addSection(new IniMapSection(this, name));
  }

  /**
   * Removes the specified INI section from the map. The default section is never removed.
   *
   * @param name Name of the section to remove.
   * @return {@code true} if a section of the given name exists and is removed, {@code false} otherwise.
   */
  public boolean removeSection(String name) {
    boolean retVal = false;
    if (name != null && !name.isBlank()) {
      final IniMapSection section = getSection(name);
      if (section != null) {
        retVal = sections.remove(section);
      }
    }
    return retVal;
  }

  private IniMapSection addSection(IniMapSection section) {
    IniMapSection retVal = section;
    if (section != null) {
      if (!sections.contains(section)) {
        sections.add(section);
      } else {
        retVal = getSection(section.getName());
      }
    }
    return retVal;
  }

  /**
   * Returns an iterator over an unmodifiable list of {@link IniMapSection} instances associated with this map.
   */
  @Override
  public Iterator<IniMapSection> iterator() {
    return Collections.unmodifiableList(sections).iterator();
  }

  @Override
  public int hashCode() {
    return Objects.hash(sections, style);
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj) {
      return true;
    }
    if (obj == null) {
      return false;
    }
    if (getClass() != obj.getClass()) {
      return false;
    }
    IniMap other = (IniMap) obj;
    return Objects.equals(sections, other.sections) && style == other.style;
  }

  @Override
  public String toString() {
    final StringBuilder sb = new StringBuilder();
    for (final IniMapSection section : sections) {
      if (!section.equals(getDefaultSection()) || !section.getNodes().isEmpty()) {
        sb.append(section).append('\n');
      }
    }
    return sb.toString();
  }
}
