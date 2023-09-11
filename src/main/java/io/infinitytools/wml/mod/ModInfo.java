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
package io.infinitytools.wml.mod;

import io.infinitytools.wml.mod.info.ComponentRoot;
import io.infinitytools.wml.mod.ini.ModIni;
import io.infinitytools.wml.utils.SystemInfo;
import io.infinitytools.wml.utils.Utils;
import io.infinitytools.wml.weidu.Weidu;
import org.json.JSONArray;
import org.tinylog.Logger;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Provides detailed information about a single mod.
 */
public class ModInfo {
  /**
   * Name of the default language.
   */
  public static final String DEFAULT_LANGUAGE = "(Default)";

  /**
   * Path to the game's installation directory.
   */
  private final Path gamePath;

  /**
   * Path to the mod's tp2 file.
   */
  private final Path tp2File;

  /**
   * Normalized mod name (tp2 filename without "setup-" prefix and file extension).
   */
  private final String tp2Name;

  /**
   * List of available mod languages (as defined by the mod).
   */
  private final List<String> languages = new ArrayList<>();

  /**
   * Detailed mod component information, available for all languages. The hash map key references a language string
   * listed in {@link #languages}. The hash map value contains a list of all available mod components.
   */
  private final List<ComponentRoot> components = new ArrayList<>();

  private ModIni modIni;
  private boolean isEE;
  private boolean weiduConfExists;
  private List<String> gameLanguages;

  public ModInfo(Path gamePath, Path tp2File) throws Exception {
    // Checking game directory
    gamePath = Objects.requireNonNull(gamePath, "Game path is null").toAbsolutePath();
    if (!Files.isDirectory(gamePath)) {
      gamePath = gamePath.getParent();
      if (!Files.isDirectory(gamePath)) {
        throw new Exception("Game path does not exist: " + gamePath);
      }
    }

    // Checking tp2 file path
    tp2File = Objects.requireNonNull(tp2File, "TP2 file is null").toAbsolutePath();
    if (!Files.isRegularFile(tp2File)) {
      throw new Exception("TP2 file path does not point to an existing file");
    }
    if (!tp2File.getFileName().toString().toLowerCase().endsWith(".tp2")) {
      throw new Exception("Unsupported file extension for tp2 file: " + tp2File.getFileName());
    }

    this.gamePath = gamePath;
    this.tp2File = tp2File;
    this.tp2Name = stripModName(tp2File, true);

    init();
  }

  /**
   * Returns the full path to the game where this mod should be installed.
   */
  public Path getGamePath() {
    return gamePath;
  }

  /**
   * Returns the full path to the tp2 file of the mod.
   */
  public Path getTp2File() {
    return tp2File;
  }

  /**
   * Returns the normalized tp2 name without "setup-" prefix and file extension.
   */
  public String getTp2Name() {
    return tp2Name;
  }

  /**
   * Returns the full path to the log file of the mod.
   */
  public Path getLogFile() {
    return getGamePath().resolve("setup-" + getTp2Name() + ".debug");
  }

  /**
   * Returns the number of available mod languages.
   */
  public int getLanguageCount() {
    return languages.size();
  }

  /**
   * Returns the language name at the specified language index.
   */
  public String getLanguage(int index) throws IndexOutOfBoundsException {
    return languages.get(index);
  }

  /**
   * Returns mod component information in the language specified by the language index.
   *
   * @param languageIndex Index of the language for the requested mod component information.
   * @return {@code List<BaseInfo>} instance with component information. Returns {@code null} if the information are not
   * available.
   * @throws IndexOutOfBoundsException if the language index is out of range.
   */
  public ComponentRoot getComponentInfo(int languageIndex) {
    if (languageIndex < 0 || languageIndex >= components.size()) {
      throw new IndexOutOfBoundsException(languageIndex);
    }

    try {
      ensureComponentExists(languageIndex);
    } catch (Exception e) {
      // component information could not be retrieved
      return null;
    }

    return components.get(languageIndex);
  }

  /**
   * Returns additional mod information from the associated ini file.
   * Returns {@code null} if this information is not available.
   */
  public ModIni getModIni() {
    return modIni;
  }

  /**
   * Returns whether the game directory points to an Enhanced Edition game.
   */
  public boolean isEnhancedEdition() {
    return isEE;
  }

  /**
   * Returns whether the file "weidu.conf" exists in the game directory.
   *
   * @return {@code true} if the game is an Enhanced Edition game and the "weidu.conf" file exists in the game
   * directory, {@code false} otherwise.
   */
  public boolean isWeiduConfAvailable() {
    return weiduConfExists;
  }

  /**
   * Returns a list of available game languages as ISO language codes for Enhanced Edition games.
   *
   * @return List if ISO language codes for Enhanced Edition games, {@code null} otherwise.
   */
  public List<String> getGameLanguages() {
    if (gameLanguages != null) {
      return Collections.unmodifiableList(gameLanguages);
    }
    return null;
  }

  /**
   * Returns the ISO language code defined in the "weidu.conf" file of the game.
   *
   * @return ISO language code if available, {@code null} otherwise.
   */
  public String getWeiduConfValue() {
    String retVal = null;

    final Path confPath = this.gamePath.resolve("weidu.conf");
    if (Files.isRegularFile(confPath)) {
      try {
        final String content = Files.readString(confPath);
        final Matcher m = Pattern.compile("lang_dir[ \t]*=[ \t]*(.._..)").matcher(content);
        if (m.find()) {
          retVal = m.group(1);
        }
      } catch (IOException ignored) {
      }
    }

    return retVal;
  }

  /**
   * Creates a new "weidu.conf" file in the Enhanced Edition game directory with the specified ISO language
   * {@code code}.
   *
   * @param code      The ISO language code.
   * @param overwrite Whether an existing "weidu.conf" file should be overwritten.
   * @return {@code true} if the "weidu.conf" was created successfully, {@code false} otherwise. Always returns
   * {@code false} for non-EE games.
   */
  public boolean writeWeiduConf(String code, boolean overwrite) {
    boolean retVal = false;

    if (isEE && !Objects.requireNonNull(code).matches(".._..")) {
      final Path confPath = this.gamePath.resolve("weidu.conf");
      if (overwrite || !Files.exists(confPath)) {
        try {
          Files.writeString(confPath, String.format("lang_dir = %s\n", code.toLowerCase()), StandardOpenOption.CREATE,
              StandardOpenOption.WRITE);
          retVal = true;
        } catch (IOException e) {
          Logger.debug("Error writing weidu.conf", e);
        }
      }
    }

    return retVal;
  }

  private void ensureComponentExists(int languageIndex) throws Exception {
    if (languageIndex >= 0 && languageIndex < components.size()) {
      if (components.get(languageIndex) == null) {
        ComponentRoot entry = retrieveComponentInfo(languageIndex);

        // no success? try default language instead
        if (entry == null && languageIndex > 0) {
          entry = retrieveComponentInfo(0);
        }

        if (entry != null) {
          components.set(languageIndex, entry);
        } else {
          throw new Exception("Could not retrieve mod component information");
        }
      }
    }
  }

  /**
   * Retrieves component information from cache. Failing that, component information is retrieved from WeiDU.
   */
  private ComponentRoot retrieveComponentInfo(int languageIndex) {
    ComponentRoot retVal = null;

    if (languageIndex >= 0 && languageIndex < components.size()) {
      retVal = components.get(languageIndex);
      if (retVal == null) {
        try {
          final JSONArray json = Weidu.getInstance().getModComponentInfo(getTp2File(), languageIndex);
          if (json != null) {
            retVal = ComponentRoot.parse(tp2File.getFileName().toString(), json);
          }
        } catch (Exception e) {
          Logger.debug("Error retrieving component information", e);
        }
      }
    }

    return retVal;
  }

  /**
   * Initializes available mod languages and components.
   *
   * @throws FileNotFoundException if the tp2 file is not available.
   */
  private void init() throws FileNotFoundException {
    // retrieving available mod languages
    final String[] languages = Weidu.getInstance().getModLanguages(getTp2File());
    if (languages != null && languages.length > 0) {
      this.languages.addAll(Arrays.asList(languages));
    } else {
      // at least one language entry must exist
      this.languages.add(DEFAULT_LANGUAGE);
    }

    // filling components list with placeholders
    for (int i = 0; i < this.languages.size(); i++) {
      this.components.add(null);
    }

    // retrieving additional mod information from associated ini file
    this.modIni = ModIni.load(tp2File);

    initEE();
  }

  /**
   * Initializes EE-specific properties.
   */
  private void initEE() {
    final Path langDir = this.gamePath.resolve("lang");
    if (!Files.isDirectory(langDir)) {
      return;
    }

    // getting list of available language codes for the game
    List<String> languages = new ArrayList<>(
        SystemInfo.findDirectories(langDir, ".._..", false, false)
            .stream()
            .filter(path -> Files.isRegularFile(path.resolve("dialog.tlk")))
            .map(path -> path.getFileName().toString().toLowerCase())
            .toList());
    if (languages.isEmpty()) {
      return;
    }

    // English languages should be first entry (for convenience)
    final int idx = languages.indexOf("en_us");
    if (idx > 0) {
      final String item = languages.remove(idx);
      languages.add(0, item);
    }

    // checking existance of "weidu.conf"
    final Path weiduConf = this.gamePath.resolve("weidu.conf");
    boolean weiduConfExists = Files.isRegularFile(weiduConf)
        && Utils.requireResult(() -> Files.readString(weiduConf), "").contains("lang_dir");

    this.isEE = true;
    this.gameLanguages = languages;
    this.weiduConfExists = weiduConfExists;
  }

  /**
   * Returns the full path to the log file for the specified mod.
   *
   * @param gamePath Path of the game where the mod is or should be installed.
   * @param tp2File  File path of the TP2 file of the mod.
   * @return Full path of the log file for the mod.
   */
  public static Path getLogFile(Path gamePath, Path tp2File) {
    if (gamePath == null) {
      throw new NullPointerException("Game path is null");
    }
    if (tp2File == null) {
      throw new NullPointerException("TP2 file path is null");
    }

    if (!Files.isDirectory(gamePath) && gamePath.getParent() != null) {
      gamePath = gamePath.getParent();
    }

    final String tp2Name = stripModName(tp2File, true);
    return gamePath.resolve("setup-" + tp2Name + ".debug");
  }

  /**
   * A helper method that returns a normalized mod name.
   * <p>
   * A normalized mod name is trimmed, (optionally) lowercased and does not contain "setup-" prefix or ".tp2" extension.
   * </p>
   *
   * @param tp2Name   tp2 filename with or without path.
   * @param lowerCase Whether the return value should be in lowercase.
   * @return normalized mod name. Returns {@code null} if {@code modName} is {@code null}.
   */
  public static String stripModName(String tp2Name, boolean lowerCase) {
    if (tp2Name != null) {
      return stripModName(Path.of(tp2Name), lowerCase);
    }
    return null;
  }

  /**
   * A helper method that returns a normalized mod name.
   * <p>
   * A normalized mod name is trimmed, (optionally) lowercased and does not contain "setup-" prefix or ".tp2" extension.
   * </p>
   *
   * @param tp2File   {@link Path} to a tp2 filename.
   * @param lowerCase Whether the return value should be in lowercase.
   * @return normalized mod name. Returns {@code null} if {@code modName} is {@code null}.
   */
  public static String stripModName(Path tp2File, boolean lowerCase) {
    String retVal = null;

    if (tp2File != null) {
      String name = tp2File.getFileName().toString();

      if (lowerCase) {
        name = name.toLowerCase();
      }

      final String prefix = "setup-";
      if (name.startsWith(prefix)) {
        name = name.substring(prefix.length());
      }

      final String ext = ".tp2";
      if (name.endsWith(ext)) {
        name = name.substring(0, name.length() - ext.length());
      }

      retVal = name;
    }

    return retVal;
  }

  /**
   * Attempts to detect a game directory in one of the parent directories of the specified tp2 file path.
   *
   * @param tp2File Path to a mod's tp2 file.
   * @return {@link Path} to a game directory if available, {@code null} if game directory could not be determined.
   * @throws InvalidPathException If the specified file path cannot be resolved.
   */
  public static Path findGamePath(Path tp2File) throws InvalidPathException {
    Path retVal = null;

    if (tp2File == null) {
      throw new NullPointerException("TP2 file path is null");
    }

    final String chitinFile = "chitin.key";
    for (Path curDir = tp2File.getParent(); curDir != null; curDir = curDir.getParent()) {
      if (!Files.isDirectory(curDir)) {
        continue;
      }

      if (Files.isRegularFile(curDir.resolve(chitinFile))) {
        retVal = curDir;
        break;
      }

      if (SystemInfo.IS_LINUX) {
        // Linux filesystem may be case-sensitive: check every file individually
        final Path file = SystemInfo.findFile(curDir, chitinFile, false, true);
        if (file != null) {
          retVal = file.getParent();
        }
      }
    }

    return retVal;
  }
}
