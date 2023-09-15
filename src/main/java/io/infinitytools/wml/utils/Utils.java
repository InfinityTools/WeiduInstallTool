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

import io.infinitytools.wml.gui.CustomScene;
import javafx.scene.control.Alert;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.control.ButtonType;
import javafx.stage.FileChooser;
import javafx.stage.Window;
import org.tinylog.Logger;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeFormatterBuilder;
import java.time.format.DateTimeParseException;
import java.util.*;

public class Utils {
  /**
   * ISO date-time formatter that formats or parses a date-time with an offset, such as '2011-12-03T10:15:30+01:00'.
   * <p>
   * This returns an immutable formatter capable of formatting and parsing the ISO-8601 extended offset date-time format.
   * The format consists of:
   * <ul>
   * <li>The {@link DateTimeFormatter#ISO_LOCAL_DATE_TIME}
   * <li>The {@link java.time.ZoneOffset#getId() offset ID}. If the offset has seconds then they will be handled even
   * though this is not part of the ISO-8601 standard. Parsing is case insensitive.
   * </ul>
   * </p>
   */
  public static final DateTimeFormatter ISO_DATE_TIME = new DateTimeFormatterBuilder()
      .parseCaseInsensitive()
      .parseLenient()
      .append(DateTimeFormatter.ISO_OFFSET_DATE_TIME)
      .toFormatter();

  @FunctionalInterface
  public interface Producer<T> {
    /**
     * Produces a result. May throw any kinds of exceptions.
     */
    T produce() throws Throwable;
  }

  /**
   * Attempts to convert the given string into a numeric value.
   *
   * @param s        String containing a numeric value. Use {@code 0x} prefix to convert hexadecimal numbers.
   * @param defValue Returned if the string does not contain valid numeric data.
   * @return Numeric representation if {@code s} if successful, {@code defValue} otherwise.
   */
  public static int parseInt(String s, int defValue) {
    int retVal = defValue;
    if (s != null) {
      int radix = 10;
      if (s.toLowerCase().startsWith("0x")) {
        s = s.substring(2);
        radix = 16;
      }
      try {
        retVal = Integer.parseInt(s, radix);
      } catch (NumberFormatException ignored) {
      }
    }
    return retVal;
  }

  /**
   * Attempts to convert the given string into a numeric value.
   *
   * @param s        String containing a numeric value. Use {@code 0x} prefix to convert hexadecimal numbers.
   * @param defValue Returned if the string does not contain valid numeric data.
   * @return Numeric representation if {@code s} if successful, {@code defValue} otherwise.
   */
  public static long parseLong(String s, long defValue) {
    long retVal = defValue;
    if (s != null) {
      int radix = 10;
      if (s.toLowerCase().startsWith("0x")) {
        s = s.substring(2);
        radix = 16;
      }
      try {
        retVal = Long.parseLong(s, radix);
      } catch (NumberFormatException ignored) {
      }
    }
    return retVal;
  }

  /**
   * Attempts to convert the given string into a numeric value.
   *
   * @param s        String containing a numeric value.
   * @param defValue Returned if the string does not contain valid numeric data.
   * @return Numeric representation if {@code s} if successful, {@code defValue} otherwise.
   */
  public static float parseDouble(String s, float defValue) {
    float retVal = defValue;
    if (s != null) {
      try {
        retVal = Float.parseFloat(s);
      } catch (NumberFormatException ignored) {
      }
    }
    return retVal;
  }

  /**
   * Attempts to convert the given string into a numeric value.
   *
   * @param s        String containing a numeric value.
   * @param defValue Returned if the string does not contain valid numeric data.
   * @return Numeric representation if {@code s} if successful, {@code defValue} otherwise.
   */
  public static double parseDouble(String s, double defValue) {
    double retVal = defValue;
    if (s != null) {
      try {
        retVal = Double.parseDouble(s);
      } catch (NumberFormatException ignored) {
      }
    }
    return retVal;
  }

  /**
   * Attempts to convert the specified date-time string into a {@link OffsetDateTime} object.
   *
   * @param dateTimeString String containing date-time information.
   * @param defDateTime    A default {@link OffsetDateTime} object that is returned if the {@code dateTimeString}
   *                       could not be successfully converted.
   * @return {@link OffsetDateTime} object with the parsed date-time information from the given string if successful,
   * {@code defDateTime} otherwise.
   */
  public static OffsetDateTime parseDateTime(String dateTimeString, OffsetDateTime defDateTime) {
    if (dateTimeString == null) {
      return defDateTime;
    }

    final DateTimeFormatter[] formatters = {
        ISO_DATE_TIME,
        DateTimeFormatter.ISO_OFFSET_DATE_TIME,
        DateTimeFormatter.ISO_ZONED_DATE_TIME,
        DateTimeFormatter.ISO_LOCAL_DATE_TIME,
        DateTimeFormatter.ISO_DATE_TIME,
        DateTimeFormatter.RFC_1123_DATE_TIME
    };

    for (final DateTimeFormatter fmt : formatters) {
      try {
        return OffsetDateTime.parse(dateTimeString, fmt);
      } catch (DateTimeParseException e) {
        Logger.debug(e, "DateTimeParser error");
      }
    }

    return defDateTime;
  }

  /**
   * Returns the string representation of the given {@link OffsetDateTime} argument.
   *
   * @param odt A {@link OffsetDateTime} object to convert. If {@code null} is specified then the current date-time
   *            from the system clock is used.
   * @return String representation of the specified {@code OffsetDateTime} argument.
   */
  public static String dateTimeToString(OffsetDateTime odt) {
    if (odt == null) {
      odt = OffsetDateTime.now();
    }
    return odt.format(ISO_DATE_TIME);
  }

  /**
   * Executes the specified operation and returns the result. Returns a default value if the operation throws an
   * exception.
   *
   * @param <T>      Return type.
   * @param op       Function to generate a return value.
   * @param defValue Default value that is returned if the function throws an exception.
   * @return Result of {@code op} if successful, {@code defValue} otherwise.
   */
  public static <T> T requireResult(Producer<T> op, T defValue) {
    if (op != null) {
      try {
        return op.produce();
      } catch (Throwable ignored) {
      }
    }
    return defValue;
  }

  /**
   * Loads the content of a {@code .properties} file and returns it as a {@link Properties} instance.
   *
   * @param cls          Class to use as base for the resource path.
   * @param resourceName Properties resource name, including {@code .properties} extension.
   * @return A fully initialized {@link Properties} instance.
   * @throws IOException If the properties resource could not be loaded.
   */
  public static Properties loadProperties(Class<?> cls, String resourceName) throws IOException {
    final Properties retVal = new Properties();
    try (final InputStream is = cls.getClassLoader().getResourceAsStream(resourceName)) {
      retVal.load(is);
    }
    return retVal;
  }

  /**
   * Returns a display name for the specified ISO language code
   *
   * @param code ISO language code in the format {@code language_country} or {@code language}. Separator between
   *             language and country may be an underscore or hyphen. Examples: {@code en_us}, {@code de_de},
   *             {@code zh_cn}.
   * @return A string array with two elements. The first element contains the language name in the language specified by
   * the ISO language code. The second element contains the English translation of the name.
   * @throws NullPointerException if {@code code} is {@code null}.
   */
  public static String[] getLanguageName(String code) {
    if (code == null) {
      throw new NullPointerException();
    }

    final String[] retVal = new String[2];
    final String[] items = code.split("[_-]");
    final String language = (items.length > 0) ? items[0] : "";
    final String country = (items.length > 1) ? items[1] : "";
    final Locale locale = Locale.of(language, country);
    retVal[0] = locale.getDisplayLanguage(locale);
    retVal[1] = locale.getDisplayLanguage(Locale.ENGLISH);

    return retVal;
  }

  /**
   * Returns a normalized path where all path elements are separated by a slash ({@code /}).
   */
  public static String normalizePath(String path) {
    String retVal = path;

    if (SystemInfo.IS_WINDOWS && path != null) {
      retVal = path.replace('\\', '/');
    }

    return retVal;
  }

  /**
   * Attempts to resolve the specified path to an existing file or directory by looking at the parent path at each
   * unsuccessful pass.
   *
   * @param path {@link Path} instance.
   * @return First {@link Path} or parent path that points to an existing filesystem entry. Returns {@code null} if no
   * existing path could be found.
   */
  public static Path resolveExistingPath(Path path) {
    Path retVal = path;
    while (retVal != null && !Files.exists(retVal)) {
      retVal = retVal.getParent();
    }
    return retVal;
  }

  /**
   * Returns a path string to the specified file relative to the given class.
   *
   * @param cls      {@link Class} instance that acts as the base location for the specified file.
   * @param fileName File name as string.
   * @return A path string to the specified file.
   * @throws NullPointerException if either of the parameters is {@code null}.
   */
  public static String getClassPath(Class<?> cls, String fileName) {
    if (cls == null || fileName == null) {
      throw new NullPointerException();
    }

    return Objects.requireNonNull(cls.getResource(fileName)).toExternalForm();
  }

  /**
   * Displays a modal information dialog with the specified parameters and an OK button.
   *
   * @param owner   Owner window for this dialog.
   * @param title   The dialog title. (Default: Information)
   * @param header  Text for the header area. Default: {@code title}
   * @param content Text for the content area. This is where you should add more descriptive text for the message dialog.
   */
  public static void showMessageDialog(Window owner, String title, String header, String content) {
    showCustomDialog(owner, AlertType.INFORMATION, title, header, content);
  }

  /**
   * Displays a modal confirmation dialog with the specified parameters and two buttons: OK, CANCEL.
   *
   * @param owner   Owner window for this dialog.
   * @param title   The dialog title. (Default: Information)
   * @param header  Text for the header area. Default: {@code title}
   * @param content Text for the content area. This is where you should add more descriptive text for the message dialog.
   * @return {@link ButtonType} of the button clicked by the user.
   */
  public static ButtonType showConfirmationDialog(Window owner, String title, String header, String content) {
    return showCustomDialog(owner, AlertType.CONFIRMATION, title, header, content);
  }

  /**
   * Displays a modal error dialog with the specified parameters and an OK button.
   *
   * @param owner   Owner window for this dialog.
   * @param title   The dialog title. (Default: Information)
   * @param header  Text for the header area. Default: {@code title}
   * @param content Text for the content area. This is where you should add more descriptive text for the message dialog.
   */
  public static void showErrorDialog(Window owner, String title, String header, String content) {
    showCustomDialog(owner, AlertType.ERROR, title, header, content);
  }

  /**
   * Shows a customized dialog with the specified parameters.
   *
   * @param owner     Owner window for this dialog.
   * @param alertType {@link AlertType} defines basic properties of the dialog window.
   * @param title     The dialog title. (Default: depends on {@code alertType})
   * @param header    Text for the header area. Default: {@code title}
   * @param content   Text for the content area. This is where you should add more descriptive text for the message dialog.
   * @param buttons   Buttons to add to the dialog. Omit to use default buttons.
   * @return {@link ButtonType} of the button clicked by the user.
   */
  public static ButtonType showCustomDialog(Window owner, AlertType alertType, String title, String header,
                                            String content, ButtonType... buttons) {
    if (alertType == null) {
      alertType = AlertType.NONE;
    }

    if (title == null) {
      title = switch (alertType) {
        case CONFIRMATION -> R.CONFIRMATION();
        case ERROR -> R.ERROR();
        case WARNING -> R.WARNING();
        default -> R.INFORMATION();
      };
    }

    if (header == null) {
      header = title;
    }

    if (content == null) {
      content = "";
    }

    final Alert alert = new Alert(alertType, content, buttons);
    if (owner != null) {
      alert.initOwner(owner);
    }
    alert.setTitle(title);
    alert.setHeaderText(header);

    CustomScene.setDarkMode(alert.getDialogPane().getScene());

    ButtonType defButtonType = null;
    for (final ButtonType type : new ButtonType[]{
        ButtonType.CANCEL, ButtonType.CLOSE, ButtonType.OK, ButtonType.NO}) {
      if (alert.getButtonTypes().contains(type)) {
        defButtonType = type;
        break;
      }
    }
    if (defButtonType == null) {
      defButtonType = alert.getButtonTypes().get(0);
    }

    return alert.showAndWait().orElse(defButtonType);
  }

  /**
   * Shows an open file dialog with the specified parameters.
   *
   * @param owner       Owner window for this dialog.
   * @param title       Title for the file dialog.
   * @param initialPath Initial directory to display in the file dialog. Can be {@code null}, a path to an existing
   *                    directory
   * @param filters     List of extension filters.
   * @return {@link Path} of the selected file. Returns {@code null} if the user did not select a file.
   */
  public static Path chooseOpenFile(Window owner, String title, Path initialPath, FileChooser.ExtensionFilter... filters) {
    final List<Path> retVal = chooseFile(owner, title, initialPath, false, false, filters);
    if (!retVal.isEmpty()) {
      return retVal.get(0);
    }
    return null;
  }

  /**
   * Shows an open file dialog with the specified parameters in which multiple files can be selected.
   *
   * @param owner       Owner window for this dialog.
   * @param title       Title for the file dialog.
   * @param initialPath Initial directory to display in the file dialog. Can be {@code null}, a path to an existing
   *                    directory.
   * @param filters     List of extension filters.
   * @return List of selected file {@link Path} instances.. Returns an empty list if the user did not select a file.
   */
  public static List<Path> chooseOpenFiles(Window owner, String title, Path initialPath, FileChooser.ExtensionFilter... filters) {
    return chooseFile(owner, title, initialPath, true, false, filters);
  }

  /**
   * Shows a save file dialog with the specified parameters.
   *
   * @param owner       Owner window for this dialog.
   * @param title       Title for the file dialog.
   * @param initialPath Initial directory to display in the file dialog. Can be {@code null}, a path to an existing
   *                    directory.
   * @param filters     List of extension filters.
   * @return {@link Path} of the selected file. Returns {@code null} if the user did not select a file.
   */
  public static Path chooseSaveFile(Window owner, String title, Path initialPath, FileChooser.ExtensionFilter... filters) {
    final List<Path> retVal = chooseFile(owner, title, initialPath, false, true, filters);
    if (!retVal.isEmpty()) {
      return retVal.get(0);
    }
    return null;
  }

  /**
   * Opens a file selection dialog with the specified parameters.
   *
   * @param owner          Owner window for this dialog.
   * @param title          Title for the file dialog.
   * @param initialPath    Initial directory to display in the file dialog. Can be {@code null}, a path to an existing
   *                       directory.
   * @param selectMultiple Indicates whether multiple files can be selected. Only considered if the parameter
   *                       {@code saveDialog} is {@code false}.
   * @param saveDialog     Specify {@code true} to show a save dialog. Specify {@code false} to show an open dialog.
   * @param filters        List of extension filters.
   * @return A list of selected file {@link Path} instances. Returns an empty list if the user did not select a file.
   */
  private static List<Path> chooseFile(Window owner, String title, Path initialPath, boolean selectMultiple,
                                       boolean saveDialog, FileChooser.ExtensionFilter... filters) {
    List<Path> retVal = null;

    final FileChooser fc = new FileChooser();

    if (title != null) {
      fc.setTitle(title);
    }

    if (initialPath != null) {
      final Path directory;
      final String fileName;
      if (Files.isDirectory(initialPath)) {
        directory = initialPath;
        fileName = null;
      } else if (Files.isDirectory(initialPath.getParent())) {
        directory = initialPath.getParent();
        fileName = initialPath.getFileName().toString();
      } else if (Files.exists(initialPath)) {
        directory = initialPath;
        fileName = null;
      } else {
        directory = null;
        fileName = null;
      }

      if (directory != null) {
        fc.setInitialDirectory(directory.toFile());
      }

      if (fileName != null) {
        fc.setInitialFileName(fileName);
      }
    }

    fc.getExtensionFilters().addAll(filters);
    if (!fc.getExtensionFilters().isEmpty()) {
      fc.setSelectedExtensionFilter(fc.getExtensionFilters().get(0));
    }

    if (saveDialog || !selectMultiple) {
      retVal = new ArrayList<>();
    }

    if (saveDialog) {
      final File selectedFile = fc.showOpenDialog(owner);
      if (selectedFile != null) {
        retVal.add(selectedFile.toPath());
      }
    } else {
      if (selectMultiple) {
        final List<File> selectedFiles = fc.showOpenMultipleDialog(owner);
        if (selectedFiles != null) {
          retVal = new ArrayList<>(selectedFiles.stream().map(File::toPath).toList());
        }
      } else {
        final File selectedFile = fc.showOpenDialog(owner);
        if (selectedFile != null) {
          retVal.add(selectedFile.toPath());
        }
      }
    }

    return retVal;
  }
}
