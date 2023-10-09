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

import io.infinitytools.wml.ini.IniEntry;
import io.infinitytools.wml.ini.IniMap;
import io.infinitytools.wml.ini.IniMapSection;
import io.infinitytools.wml.utils.R;
import io.infinitytools.wml.utils.SystemInfo;
import io.infinitytools.wml.utils.Utils;
import javafx.stage.FileChooser;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.*;

/**
 * Manages application-wide configurations.
 */
public class Configuration {
  /**
   * Collection of support options.
   */
  public enum Key {
    /**
     * Specifies the x coordinate of the application main window.
     */
    WINDOW_X("Window X", Integer.class, null),
    /**
     * Specifies the y coordinate of the application main window.
     */
    WINDOW_Y("Window Y", Integer.class, null),
    /**
     * Specifies the width of the application main window.
     */
    WINDOW_WIDTH("Window Width", Integer.class, null),
    /**
     * Specifies the height of the application main window.
     */
    WINDOW_HEIGHT("Window Height", Integer.class, null),
    /**
     * Indicates whether the main window should be maximized.
     */
    WINDOW_MAXIMIZED("Window Maximized", Boolean.class, false),
    /**
     * Indicates whether dark mode coloring is enabled.
     */
    DARK_UI_MODE("Dark UI Mode", Boolean.class, false),
    /**
     * Specifies font size, in pt, of the output text area.
     */
    OUTPUT_FONT_SIZE("Output Font Size", Double.class, null),
    /**
     * Indicates whether the Details window should be visible on launch.
     */
    SHOW_DETAILS("Show Details Window", Boolean.class, true),
    /**
     * Indicates whether a warning should be shown if the preferred mod order is violated.
     */
    WARN_MOD_ORDER("Warn Mod Order", Boolean.class, true),
    /**
     * Indicates whether pressing the Enter key closes the application when the WeiDU process is completed.
     * <p>
     * Enabled by default, except on Linux because of incomplete single app instance functionality.
     * </p>
     */
    QUIT_ON_ENTER("Quit On Enter", Boolean.class, !SystemInfo.IS_LINUX),
    /**
     * Indicates whether the output area should display a colored frame to visualize whether the WeiDU
     * process terminated successfully or as error.
     */
    VISUALIZE_RESULT("Visualize Result", Boolean.class, true),
    /**
     * Specifies the max. number of characters shown in the output area of the main window.
     */
    BUFFER_LIMIT("Output Buffer Limit", Integer.class, 500_000),
    /**
     * Indicates whether the application is running in single instance mode.
     * <p>
     * Enabled by default except for macOS (which already provides a similar behavior).
     * </p>
     */
    SINGLE_INSTANCE("Single Instance Mode", Boolean.class, !SystemInfo.IS_MACOS),
    /**
     * Indicates whether the system tray notification about hiding the main window was shown to the user.
     */
    TRAY_HINT_SHOWN("Tray Hint Shown", Boolean.class, false),
    /**
     * Indicates whether the tray icon should change depending on the running state of the WeiDU process.
     */
    TRAY_ICON_FEEDBACK("Tray Icon Feedback", Boolean.class, true),
    /**
     * Specifies a custom path to the WeiDU binary.
     */
    WEIDU_PATH("Weidu Path", String.class, null),
    /**
     * Specifies a SHA256 hash of the selected WeiDU binary.
     */
    WEIDU_HASH("Weidu Hash", String.class, null),
    /**
     * Specifies the path to the last used game directory.
     */
    LAST_GAME_PATH("Last Game Path", String.class, null),
    /**
     * Specifies the directory to the last used mod's tp2 file.
     */
    LAST_MOD_PATH("Last Mod Path", String.class, null),
    /**
     * Specifies the language in which all text of the application should be displayed.
     * System language is used if this option is not defined.
     * <p>
     * Language can be defined as two-letter ISO language code with an optional two-letter country code, separated by
     * an underscore or hyphen.
     * </p>
     */
    UI_LANGUAGE_OVERRIDE("UI Language Override", String.class, null),
    ;

    private final Object defValue;
    private final Class<?> type;
    private final String name;

    <T> Key(String name, Class<T> type, T defValue) {
      this.name = name;
      this.type = type;
      this.defValue = defValue;
    }

    /**
     * Returns the option key name.
     */
    public String getName() {
      return name;
    }

    /**
     * Returns the option type.
     */
    public Class<?> getType() {
      return type;
    }

    /**
     * Returns the default value for the current option that can be used if no explicit value exists.
     *
     * @param <T> Return type of the default value.
     * @return Default value of the appropriate type. Returns {@code true} if no default value is defined.
     */
    @SuppressWarnings("unchecked")
    public <T> T getDefaultValue() {
      return (T) type.cast(defValue);
    }

    /**
     * Attempts to find a {@link Key} enum matching the given string.
     *
     * @param name Name of the option key.
     * @return A {@link Key} item if match is found, {@code null} otherwise.
     */
    public Key findKey(String name) {
      return Arrays
          .stream(Key.values())
          .filter(k -> k.name.equalsIgnoreCase(name))
          .findAny()
          .orElse(null);
    }

    @Override
    public String toString() {
      return getName();
    }
  }

  /**
   * Available operation modes for this application.
   */
  public enum Mode {
    /**
     * Show WeiDU help in the application output area.
     */
    WEIDU_HELP,
    /**
     * Perform an assisted mod installation.
     */
    WEIDU_GUIDED,
    /**
     * Perform a custom WeiDU operation.
     */
    WEIDU_CUSTOM,
    ;

    /**
     * Returns the default mode of the application ({@link Mode#WEIDU_HELP}).
     */
    public static Mode getDefaultMode() {
      return WEIDU_HELP;
    }
  }

  private static Configuration instance;

  /**
   * Returns the singleton instance of {@link Configuration}.
   */
  public static Configuration getInstance() {
    if (instance == null) {
      instance = new Configuration();
    }
    return instance;
  }

  private final EnumMap<Key, Object> options = new EnumMap<>(Key.class);
  private final List<String> weiduArgs = new ArrayList<>();
  private final Path configPath;

  private Mode mode;
  private Path tp2File;

  private Configuration() {
    this.configPath = Globals.APP_DATA_PATH.resolve(Globals.APP_CONFIG_FILE);
    this.mode = Mode.getDefaultMode();
    loadDefaults();
  }

  /**
   * Returns the current application mode.
   */
  public Mode getMode() {
    return mode;
  }

  /**
   * Sets a new application mode.
   */
  private void setMode(Mode newMode) {
    this.mode = (newMode != null) ? newMode : Mode.getDefaultMode();
  }

  /**
   * Returns the mod's TP2 file path in {@link Mode#WEIDU_GUIDED} mode, {@code null} otherwise.
   */
  public Path getTp2Path() {
    return tp2File;
  }

  /**
   * Sets the mod's TP2 file path.
   */
  private void setTp2Path(Path tp2Path) {
    this.tp2File = tp2Path;
  }

  /**
   * Returns the {@link Path} to the application's configuration file.
   */
  public Path getConfigFile() {
    return configPath;
  }

  /**
   * Returns the value associated with the specified option.
   *
   * @param <T> Expected type of the return value.
   * @param key The option {@link Key}.
   * @return Value associated with the key. {@code null} if option doesn't exist.
   */
  public <T> T getOption(Key key) {
    return getOption(key, (key != null) ? key.getDefaultValue() : null);
  }

  /**
   * Returns the value associated with the specified option.
   *
   * @param <T>      Expected type of the return value.
   * @param key      The option {@link Key}.
   * @param defValue Default value to return if the option does not exist.
   * @return Value associated with the key. {@code defValue} if option doesn't exist.
   */
  @SuppressWarnings("unchecked")
  public <T> T getOption(Key key, T defValue) {
    if (key != null) {
      return (T) key.getType().cast(options.getOrDefault(key, defValue));
    }
    return defValue;
  }

  /**
   * Assigns a new value to the specified option.
   *
   * @param <T>   Expected value type of the option.
   * @param key   The option {@link Key}.
   * @param value The value to assign. Specify {@code null} to reset the option to the default value.
   */
  public <T> void setOption(Key key, T value) {
    if (key != null) {
      if (value != null) {
        options.put(key, value);
      } else if (key.getDefaultValue() != null) {
        options.put(key, key.getDefaultValue());
      } else {
        options.remove(key);
      }
    }
  }

  /**
   * Returns an unmodifiable list of arguments that are forwarded as parameters to the WeiDU process.
   * <p>
   * This list is only available after a call of {@link Configuration#loadArguments(List)}.
   * </p>
   */
  public List<String> getWeiduArgs() {
    return Collections.unmodifiableList(weiduArgs);
  }

  /**
   * Assigns default values to all options.
   */
  public void loadDefaults() {
    options.clear();
    for (final Key key : Key.values()) {
      if (key.getDefaultValue() != null) {
        options.put(key, key.getDefaultValue());
      }
    }
  }

  /**
   * Loads options from the application's configuration file.
   *
   * @throws Exception if the configuration file could not be loaded.
   */
  public void load() throws Exception {
    final IniMap ini = IniMap.parse(getConfigFile(), IniMap.Style.getDefault());
    final IniMapSection section = ini.getDefaultSection();
    for (final Key key : Key.values()) {
      final IniEntry entry = section.getEntry(key.getName());
      if (entry != null) {
        if (Boolean.class.isAssignableFrom(key.getType())) {
          setOption(key, entry.getBoolValue(key.getDefaultValue()));
        } else if (Integer.class.isAssignableFrom(key.getType())) {
          setOption(key, entry.getNumericValue(key.getDefaultValue()).intValue());
        } else if (Double.class.isAssignableFrom(key.getType())) {
          setOption(key, entry.getNumericValue(key.getDefaultValue()).doubleValue());
        } else {
          setOption(key, entry.getValue());
        }
      }
    }
  }

  /**
   * Evaluates the specified command line arguments and applies valid arguments to the current configuration.
   *
   * @param args Arguments as string list.
   */
  public void loadArguments(List<String> args) {
    if (args != null) {
      weiduArgs.clear();
      weiduArgs.addAll(args);

      // determine application mode
      initWeiduMode();
    }
  }

  /**
   * Saves the current options to the configuration file.
   *
   * @throws Exception if the configuration could not be written.
   */
  public void save() throws Exception {
    if (!Files.exists(getConfigFile())) {
      // Create path and file
      final Path configDir = getConfigFile().getParent();
      if (!Files.exists(configDir)) {
        Files.createDirectories(configDir);
      }
    }

    final IniMap ini = new IniMap(IniMap.Style.getDefault());
    final IniMapSection section = ini.getDefaultSection();
    for (final Key key : Key.values()) {
      final Object value = getOption(key);
      if (value != null) {
        section.addEntry(key.getName(), value.toString());
      }
    }

    Files.writeString(getConfigFile(), ini.toString(), StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING,
        StandardOpenOption.WRITE);
  }

  /**
   * Determines the right application mode based on the current state of the WeiDU parameter list.
   */
  private void initWeiduMode() {
    Mode mode;
    Path tp2File = null;

    if (weiduArgs.isEmpty()) {
      // try guided mode first
      String path = getOption(Key.LAST_MOD_PATH);
      if (path != null) {
        path = Utils.resolveExistingPath(Path.of(path)).toString();
      }

      if (path == null) {
        path = getOption(Key.LAST_GAME_PATH);
      }

      if (path != null) {
        path = Utils.resolveExistingPath(Path.of(path)).toString();
      }

      if (path == null) {
        final Path dataPath = SystemInfo.getLocalDataPath();
        if (dataPath != null) {
          path = dataPath.toString();
        }
      }

      final Path initialPath = (path != null) ? Path.of(path) : null;
      tp2File = Utils.chooseOpenFile(null, R.get("ui.configuration.fileDialog.tp2.title"), initialPath,
          new FileChooser.ExtensionFilter(R.get("ui.configuration.fileDialog.tp2.filter.tp2"), "*.tp2"),
          new FileChooser.ExtensionFilter(R.get("ui.fileDialog.filter.allFiles"), "*.*"));
      if (tp2File != null) {
        mode = Mode.WEIDU_GUIDED;
      } else {
        // falling back to help mode
        mode = Mode.WEIDU_HELP;
      }
    } else if (weiduArgs.contains("--help") || weiduArgs.contains("-help")) {
      mode = Mode.WEIDU_HELP;
    } else {
      mode = Mode.WEIDU_CUSTOM;
      final String arg = weiduArgs.get(0).strip();
      if (!arg.startsWith("--") && arg.toLowerCase().endsWith(".tp2")) {
        mode = Mode.WEIDU_GUIDED;
        // moving tp2 path argument to dedicated attribute
        weiduArgs.remove(0);
        tp2File = Path.of(arg);
      }
    }

    setMode(mode);
    setTp2Path(tp2File);
  }
}
