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
package io.infinitytools.wit.mod.ini;

import io.infinitytools.wit.ini.IniEntry;
import io.infinitytools.wit.ini.IniMap;
import io.infinitytools.wit.ini.IniMapSection;
import io.infinitytools.wit.mod.ModInfo;
import org.tinylog.Logger;

import java.io.FileNotFoundException;
import java.net.URI;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Loads information from the mod's .ini file if available.
 */
public class ModIni {
  /**
   * Returns a {@link ModIni} object containing additional information about the mod.
   *
   * @param tp2File Path to the mod's .tp2 file.
   * @return {@link ModIni} object if the .ini file is available, {@code null} otherwise.
   */
  public static ModIni load(Path tp2File) {
    try {
      return new ModIni(tp2File);
    } catch (FileNotFoundException e) {
      Logger.debug(e, "Parsing metadata file");
    }
    return null;
  }

  private final List<String> authorList = new ArrayList<>();
  private final List<URL> readmeList = new ArrayList<>();
  private final List<URL> forumList = new ArrayList<>();
  private final List<URL> downloadList = new ArrayList<>();
  private final List<String> beforeList = new ArrayList<>();
  private final List<String> afterList = new ArrayList<>();

  private String name;
  private String desc;
  private URL homepage;
  private String labelType;

  private ModIni(Path tp2File) throws FileNotFoundException {
    // two possible ini filename variant are possible: with or without "setup-" prefix
    final String modName = ModInfo.stripModName(tp2File, false);
    final Path modDir = tp2File.getParent();
    final Path[] iniPaths = {modDir.resolve(modName + ".ini"), modDir.resolve("setup-" + modName + ".ini")};
    Path iniFile = null;
    for (final Path curPath : iniPaths) {
      if (Files.exists(curPath)) {
        iniFile = curPath;
        break;
      }
    }

    if (iniFile == null) {
      throw new FileNotFoundException("Could not find ini file.");
    }

    try {
      IniMap ini = IniMap.parse(iniFile, IniMap.Style.Any, IniMap.Options.MultiLineDescription,
          IniMap.Options.StrictLineComments);
      IniMapSection section = ini.getSection("Metadata");
      if (section == null) {
        throw new Exception();
      }

      for (final IniEntry entry : section.getEntries()) {
        switch (entry.getKey().toLowerCase()) {
          case "name" -> name = entry.getValue().strip();
          case "author" -> authorList.addAll(Arrays.stream(entry.getValue().split(",")).map(String::strip)
              .filter(s -> !s.isEmpty()).toList());
          case "description" -> desc = entry.getValue().strip();
          case "readme" -> readmeList.addAll(stringArrayToUrlList(entry.getValue().split(",")));
          case "forum" -> forumList.addAll(stringArrayToUrlList(entry.getValue().split(",")));
          case "homepage" -> {
            try {
              homepage = new URI(entry.getValue()).toURL();
            } catch (Exception e) {
              // ignore
              Logger.info(e, "Mod Ini: invalid homepage URL: {}", entry.getValue());
            }
          }
          case "download" -> downloadList.addAll(stringArrayToUrlList(entry.getValue().split(",")));
          case "labeltype" -> labelType = entry.getValue();
          case "before" ->
              beforeList.addAll(Arrays.stream(entry.getValue().split(",")).map(String::strip).filter(s -> !s.isEmpty())
                  .toList());
          case "after" ->
              afterList.addAll(Arrays.stream(entry.getValue().split(",")).map(String::strip).filter(s -> !s.isEmpty())
                  .toList());
          default -> {
          }
        }
      }
    } catch (Exception e) {
      throw new FileNotFoundException("Could not parse ini file.");
    }
  }

  /**
   * Returns the name of the mod. Returns an empty string if this property is not available.
   */
  public String getName() {
    return (name != null) ? name : "";
  }

  /**
   * Returns a list of authors associated with the mod. Returns an empty list if this property is not available.
   */
  public List<String> getAuthorList() {
    return Collections.unmodifiableList(authorList);
  }

  /**
   * Returns a short description of the mod. Returns an empty string if this property is not available.
   */
  public String getDescription() {
    return (desc != null) ? desc : "";
  }

  /**
   * Returns the {@link URL} of the mod's homepage. Returns {@code null} if this property is not available.
   */
  public URL getHomepage() {
    return homepage;
  }

  /**
   * Returns the label type used by the mod. Returns an empty string if this property is not available.
   */
  public String getLabelType() {
    return (labelType != null) ? labelType : "";
  }

  /**
   * Returns a list of readme {@link URL}s associated with the mod. Returns an empty list if this property is not
   * available.
   */
  public List<URL> getReadmeList() {
    return Collections.unmodifiableList(readmeList);
  }

  /**
   * Returns a list of forum {@link URL}s associated with the mod. Returns an empty list if this property is not
   * available.
   */
  public List<URL> getForumList() {
    return Collections.unmodifiableList(forumList);
  }

  /**
   * Returns a list of download link {@link URL}s associated with the mod. Returns an empty list if this property is not
   * available.
   */
  public List<URL> getDownloadList() {
    return Collections.unmodifiableList(downloadList);
  }

  /**
   * Returns a list of mod names this mod should be installed before. Returns an empty list if this property is not
   * available.
   */
  public List<String> getBeforeList() {
    return Collections.unmodifiableList(beforeList);
  }

  /**
   * Returns a list of mod names this mod should be installed after. Returns an empty list if this property is not
   * available.
   */
  public List<String> getAfterList() {
    return Collections.unmodifiableList(afterList);
  }

  private static List<URL> stringArrayToUrlList(String[] items) {
    return Arrays.stream(items).map(item -> {
      if (!item.isBlank()) {
        try {
          return new URI(item.strip()).toURL();
        } catch (Exception e) {
          // ignore
          Logger.debug(e, "Mod Ini: invalid URL entry: {}", item);
        }
      }
      return null;
    }).filter(Objects::nonNull).collect(Collectors.toList());
  }
}
