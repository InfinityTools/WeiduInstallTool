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
package io.infinitytools.wml.utils;

import io.infinitytools.wml.WeiduModLauncher;
import org.apache.commons.text.StringEscapeUtils;
import org.tinylog.Logger;

import java.util.Arrays;
import java.util.Locale;
import java.util.MissingResourceException;
import java.util.ResourceBundle;

/**
 * Provides quick access to resource bundles content.
 */
public class R {
  /**
   * Base name of the Resource Bundle.
   */
  private static final String BUNDLE_PREFIX = "l10n/wml";

  private static ResourceBundle bundle = null;
  private static Locale currentLocale = setLocale();

  /**
   * Returns the currently used Resource Bundle instance.
   *
   * @return current {@link ResourceBundle} instance.
   */
  public static ResourceBundle getBundle() {
    if (bundle == null) {
      Logger.debug("New resource bundle instance for locale {}", currentLocale);
      bundle = ResourceBundle.getBundle(BUNDLE_PREFIX, currentLocale, WeiduModLauncher.class.getModule());
    }
    return bundle;
  }

  /**
   * Resets the {@link Locale} for the Resource Bundle. All subsequent string requests will use the new Locale
   * definition.
   *
   * @param params A variable number of arguments to narrow down the search for a matching Locale, in the following
   *               order: {@code language} code, {@code country} code, {@code variant} (variation)
   */
  public static Locale setLocale(String... params) {
    Logger.debug("Set locale for resource bundle: {}", Arrays.toString(params));
    bundle = null;
    return switch (params.length) {
      case 0 -> currentLocale = Locale.getDefault();
      case 1 -> currentLocale = Locale.of(params[0]);
      case 2 -> currentLocale = Locale.of(params[0], params[1]);
      default -> currentLocale = Locale.of(params[0], params[1], params[2]);
    };
  }

  /**
   * Gets a string from the Resource Bundle for the given key.
   *
   * @param key the key for the desired string.
   * @return the string for the given key with unescaped Java literals. Returns {@code null} if the string could not be
   * loaded.
   */
  public static String get(String key) {
    return get(key, null, true);
  }

  /**
   * Gets a string from the Resource Bundle for the given key.
   *
   * @param key      the key for the desired string.
   * @param defValue a default value to return if the string for the given {@code key} cannot be loaded.
   * @return the string for the given key with unescaped Java literals. Returns {@code defValue} if the string could not
   * be loaded.
   */
  public static String get(String key, String defValue) {
    return get(key, defValue, true);
  }

  /**
   * Gets a string from the Resource Bundle for the given key.
   *
   * @param key      the key for the desired string.
   * @param defValue a default value to return if the string for the given {@code key} cannot be loaded.
   * @param unescape indicates whether Java literals are unescaped in the returned string (e.g. {@code \n}, {@code \t}).
   * @return the string for the given key. Returns {@code defValue} if the string could not be loaded.
   */
  public static String get(String key, String defValue, boolean unescape) {
    String retVal;
    try {
      retVal = getBundle().getString(key);
    } catch (ClassCastException | MissingResourceException | NullPointerException e) {
      Logger.debug(e, "Could not get string from resource bundle for key: {}", key);
      retVal = defValue;
    }

    if (retVal != null && unescape) {
      retVal = StringEscapeUtils.unescapeJava(retVal);
    }

    return retVal;
  }

  /**
   * Returns the string "Error" for the currently selected language.
   *
   * @return String "Error" in the current language.
   */
  public static String ERROR() {
    return get("ui.message.error");
  }

  /**
   * Returns the string "Warning" for the currently selected language.
   *
   * @return String "Warning" in the current language.
   */
  public static String WARNING() {
    return get("ui.message.warning");
  }

  /**
   * Returns the string "Question" for the currently selected language.
   *
   * @return String "Question" in the current language.
   */
  public static String QUESTION() {
    return get("ui.message.question");
  }

  /**
   * Returns the string "Confirmation" for the currently selected language.
   *
   * @return String "Confirmation" in the current language.
   */
  public static String CONFIRMATION() {
    return get("ui.message.confirmation");
  }

  /**
   * Returns the string "Information" for the currently selected language.
   *
   * @return String "Information" in the current language.
   */
  public static String INFORMATION() {
    return get("ui.message.information");
  }
}
