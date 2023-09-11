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
package io.infinitytools.wml;

import io.infinitytools.wml.utils.SystemInfo;
import org.tinylog.Logger;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Path;
import java.util.Objects;
import java.util.Properties;

/**
 * Global definitions used by the application.
 */
public class Globals {
  /**
   * Current application version. (Loaded from property file.)
   */
  public static final String APP_VERSION = loadProperty("version", "1.0-SNAPSHOT");

  /**
   * The full application title. (Loaded from property file.)
   */
  public static final String APP_TITLE = Objects.requireNonNull(loadProperty("title"));

  /**
   * URL to the project sources. (Loaded from property file.)
   */
  public static final String PROJECT_URL = Objects.requireNonNull(loadProperty("project"));

  /**
   * URL to the WeiDU sources. (Loaded from property file.)
   */
  public static final String WEIDU_URL = Objects.requireNonNull(loadProperty("weidu"));

  /**
   * Folder name for storing and retrieving application data.
   */
  public static final String APP_FOLDER_NAME = Objects.requireNonNull(loadProperty("config_name"));

  /**
   * Filename for storing and retrieving application options.
   */
  public static final String APP_CONFIG_FILE = APP_FOLDER_NAME + ".ini";

  /**
   * {@link Path} to the user's appdata folder for this application.
   */
  public static final Path APP_DATA_PATH = SystemInfo.getLocalDataPath().resolve(APP_FOLDER_NAME);

  private Globals() {
  }

  /**
   * Loads a property from the resource {@code globals.properties}.
   *
   * @param name Name of the property.
   * @return The requested property value if successful, {@code null} otherwise.
   */
  private static String loadProperty(String name) {
    return loadProperty(name, null);
  }

  /**
   * Loads a property from the resource {@code globals.properties}.
   *
   * @param name     Name of the property.
   * @param defValue A default value to return if the request property could not be loaded.
   * @return The requested property value if successful, {@code defValue} otherwise.
   */
  private static String loadProperty(String name, String defValue) {
    String retVal = defValue;

    if (name != null) {
      final Properties prop = new Properties();
      try (final InputStream is = Globals.class.getClassLoader().getResourceAsStream("globals.properties")) {
        prop.load(is);
        final String value = prop.getProperty(name);
        if (value != null) {
          retVal = value;
        }
      } catch (IOException e) {
        Logger.debug("Error loading \"globals.properties\" entry", e);
      }
    }

    return retVal;
  }
}
