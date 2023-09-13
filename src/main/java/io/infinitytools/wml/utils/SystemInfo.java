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
package io.infinitytools.wml.utils;

import io.infinitytools.wml.process.ProcessUtils;
import org.tinylog.Logger;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;
import java.util.stream.Stream;

/**
 * Provides static methods and attributes to query platform and architecture-related information.
 */
public class SystemInfo {
  /**
   * Available operating systems.
   */
  public enum Platform {
    WINDOWS("windows"),
    LINUX("linux"),
    MACOS("macos"),
    UNKNOWN("unknown"),
    ;

    private final String label;

    Platform(String label) {
      this.label = label;
    }

    @Override
    public String toString() {
      return label;
    }
  }

  /**
   * Available system architectures.
   */
  public enum Architecture {
    /**
     * Intel/AMD 32-bit architecture.
     */
    X86("x86", true),
    /**
     * Intel/AMD 64-bit architecture.
     */
    X86_64("x86_64", true),
    /**
     * Arm's 64-bit architecture.
     */
    AARCH64("aarch64", false),
    /**
     * Unknown or unsupported architecture.
     */
    UNKNOWN("unknown", false),
    ;

    private final String label;
    private final boolean supported;

    Architecture(String label, boolean supported) {
      this.label = label;
      this.supported = supported;
    }

    /**
     * Returns whether the architecture is supported by the WeiDU launcher.
     */
    public boolean isSupported() {
      return supported;
    }

    @Override
    public String toString() {
      return label;
    }
  }

  /**
   * Returns the major version number of the active Java Runtime.
   */
  public final static int JAVA_VERSION = getJavaVersionMajor();

  /**
   * Whether this is a Microsoft Windows system.
   */
  public final static boolean IS_WINDOWS = (getPlatform() == Platform.WINDOWS);
  /**
   * Whether this is a GNU/Linux operating system.
   */
  public final static boolean IS_LINUX = (getPlatform() == Platform.LINUX);
  /**
   * Whether this an Apple macOS system.
   */
  public final static boolean IS_MACOS = (getPlatform() == Platform.MACOS);

  /**
   * Returns the symbol for separating individual path definitions from each other for the current platform
   * (e.g. colon for unix-like systems).
   */
  public final static String PATH_SEPARATOR = System.getProperty("path.separator");

  /**
   * Returns the system-dependent path element separator character as string for the current platform
   * (e.g. slash for unix-like systems).
   */
  public final static String SEPARATOR = System.getProperty("file.separator");

  /**
   * Returns the line break native to the current operating system.
   */
  public final static String NEWLINE = (getPlatform() == Platform.WINDOWS) ? "\r\n" : "\n";

  /**
   * Platform-specific suffix for executable files.
   *
   * <p>
   * For Windows this is {@code .exe}. Other platforms may return an empty string.
   * </p>
   */
  public final static String EXE_SUFFIX = (getPlatform() == Platform.WINDOWS) ? ".exe" : "";

  /**
   * Determines the current operating system.
   *
   * @return {@link Platform} value of the current operating system.
   */
  public static Platform getPlatform() {
    final String name = System.getProperty("os.name").toLowerCase();
    if (name.contains("mac") || name.contains("darwin")) {
      return Platform.MACOS;
    } else if (name.contains("nix") || name.contains("nux")) {
      return Platform.LINUX;
    } else if (name.contains("win")) {
      return Platform.WINDOWS;
    } else {
      return Platform.UNKNOWN;
    }
  }

  /**
   * Determines the current system architecture.
   *
   * @return {@link Architecture} value compatible with the current operating system.
   */
  public static Architecture getArchitecture() {
    String name = System.getProperty("os.arch").toLowerCase();
    return switch (name) {
      case "x86", "i386", "i486", "i586", "i686" -> Architecture.X86;
      case "amd64", "x86_64" -> Architecture.X86_64;
      case "aarch64", "arm64" -> Architecture.AARCH64;
      default -> Architecture.UNKNOWN;
    };
  }

  /**
   * Returns whether the current system is supported by the WeiDU launcher.
   */
  public static boolean isSystemSupported() {
    return (getPlatform() != Platform.UNKNOWN) && getArchitecture().isSupported();
  }

  /**
   * Returns the path to the user directory.
   *
   * @return {@code Path} of the user directory. {@code null} if the path could not be determined.
   */
  public static Path getUserPath() {
    Path retVal = null;
    try {
      retVal = Path.of(System.getProperty("user.home"));
    } catch (InvalidPathException e) {
      Logger.debug(e, "User path not found");
    }
    return retVal;
  }

  /**
   * Attempts to determine the user's "Application Data" directory.
   *
   * @return {@code Path} of the user's local data directory. {@code null} if the path could not be determined.
   */
  public static Path getLocalDataPath() {
    Path retVal = null;

    String userPrefix = System.getProperty("user.home");
    Path dataRoot = null;
    Platform platform = getPlatform();
    if (platform == Platform.WINDOWS) {
      String appData = System.getenv("LOCALAPPDATA");
      if (appData != null) {
        try {
          dataRoot = Path.of(appData);
        } catch (Throwable ignored) {
        }
      }

      if (dataRoot == null) {
        // Fallback solution
        final String output = ProcessUtils.getProcessOutput("reg", "query",
            "HKCU\\Software\\Microsoft\\Windows\\CurrentVersion\\Explorer\\Shell Folders", "/v", "local appdata");
        final String[] splitted = output.split("\\s\\s+");
        userPrefix = splitted[splitted.length - 1];
        dataRoot = Path.of(userPrefix);
      }
    } else if (platform == Platform.MACOS) {
      dataRoot = Path.of(userPrefix, "Library", "Application Support");
    } else if (platform == Platform.LINUX) {
      dataRoot = Path.of(userPrefix, ".local", "share");
    }

    if (dataRoot != null && Files.isDirectory(dataRoot)) {
      retVal = dataRoot;
    }

    return retVal;
  }

  /**
   * Returns the Java Runtime major version.
   */
  public static int getJavaVersionMajor() {
    int retVal = 8;

    String[] versionString = System.getProperty("java.specification.version").split("\\.");
    try {
      int major = Integer.parseInt(versionString[0]);
      if (major <= 1 && versionString.length > 1) {
        major = Integer.parseInt(versionString[1]);
      }
      retVal = major;
    } catch (NumberFormatException ignored) {
    }

    return retVal;
  }

  /**
   * Returns the first available matching file of the specified filename pattern.
   *
   * @param dir           Directory to search for files matching the given pattern.
   * @param pattern       File name pattern as regular expression.
   * @param caseSensitive Whether the matches are case-sensitive.
   * @param literal       Whether to check the literal string instead of a regular expression.
   * @return {@link Path} of the file if found, {@code null} otherwise.
   */
  public static Path findFile(Path dir, String pattern, boolean caseSensitive, boolean literal) {
    final List<Path> retVal = findDirectoryContent(dir, pattern, caseSensitive, literal, true, false, true);
    return !retVal.isEmpty() ? retVal.get(0) : null;
  }

  /**
   * Returns a list of files in the given dir, matching the specified filename pattern.
   *
   * @param dir           Directory to search for files matching the given pattern.
   * @param pattern       File name pattern as regular expression.
   * @param caseSensitive Whether the matches are case-sensitive.
   * @param literal       Whether to check the literal string instead of a regular expression.
   * @return List of files matching the specified pattern.
   */
  public static List<Path> findFiles(Path dir, String pattern, boolean caseSensitive, boolean literal) {
    return findDirectoryContent(dir, pattern, caseSensitive, literal, true, false, false);
  }

  /**
   * Returns the first available matching directory of the specified filename pattern.
   *
   * @param dir           Directory to search for files matching the given pattern.
   * @param pattern       File name pattern as regular expression.
   * @param caseSensitive Whether the matches are case-sensitive.
   * @param literal       Whether to check the literal string instead of a regular expression.
   * @return {@link Path} of the directory if found, {@code null} otherwise.
   */
  public static Path findDirectory(Path dir, String pattern, boolean caseSensitive, boolean literal) {
    final List<Path> retVal = findDirectoryContent(dir, pattern, caseSensitive, literal, false, true, true);
    return !retVal.isEmpty() ? retVal.get(0) : null;
  }

  /**
   * Returns a list of directories in the given dir, matching the specified filename pattern.
   *
   * @param dir           Directory to search for files matching the given pattern.
   * @param pattern       File name pattern as regular expression.
   * @param caseSensitive Whether the matches are case-sensitive.
   * @param literal       Whether to check the literal string instead of a regular expression.
   * @return List of directories matching the specified pattern.
   */
  public static List<Path> findDirectories(Path dir, String pattern, boolean caseSensitive, boolean literal) {
    return findDirectoryContent(dir, pattern, caseSensitive, literal, false, true, false);
  }

  /**
   * Returns a list of files in the given dir, matching the specified filename pattern.
   *
   * @param dir                Directory to search for files matching the given pattern.
   * @param pattern            File name pattern as regular expression.
   * @param caseSensitive      Whether the matches are case-sensitive.
   * @param literal            Whether to check the literal string instead of a regular expression.
   * @param includeFiles       Whether to include regular files in the list.
   * @param includeDirectories Whether to include directories in the list.
   * @param findFirst          Whether to return the result after finding the first match.
   * @return List of files matching the specified pattern.
   */
  private static List<Path> findDirectoryContent(Path dir, String pattern, boolean caseSensitive, boolean literal,
                                                 boolean includeFiles, boolean includeDirectories, boolean findFirst) {
    final List<Path> retVal = new ArrayList<>();

    if (dir == null) {
      dir = Path.of(".");
    }

    if (!Files.isDirectory(dir)) {
      return retVal;
    }

    Pattern regex;
    try {
      int flags = 0;
      if (caseSensitive) {
        flags |= Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE;
      }
      if (literal) {
        flags |= Pattern.LITERAL;
      }
      regex = Pattern.compile(pattern, flags);
    } catch (PatternSyntaxException e) {
      regex = Pattern.compile(Pattern.quote(pattern));
    }

    final Pattern r = regex;
    try (Stream<Path> stream = Files.list(dir)) {
      Stream<Path> curStream = stream.filter(path -> {
        if (includeFiles && Files.isRegularFile(path)) {
          return r.matcher(path.getFileName().toString()).matches();
        } else if (includeDirectories && Files.isDirectory(path)) {
          return r.matcher(path.getFileName().toString()).matches();
        }
        return false;
      });

      if (findFirst) {
        // short-circuit after the first match
        curStream.findFirst().ifPresent(retVal::add);
      } else {
        final List<Path> list = curStream.toList();
        if (!list.isEmpty()) {
          retVal.addAll(list);
        }
      }
    } catch (IOException ignored) {
    }

    return retVal;
  }

  private SystemInfo() {
  }
}
