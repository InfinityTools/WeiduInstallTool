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
package io.infinitytools.wml.ini;

import java.util.Objects;

/**
 * Represents a single INI file entry.
 */
public class IniEntry extends IniBaseNode {
  private final String key;

  private String value;

  /**
   * Creates a new INI file entry. This constructor is invoked internally by
   * {@link IniMapSection#addEntry(String, String)}.
   *
   * @param section INI section this entry is associated with. {@code null} is treated as the
   *                {@link IniMapSection} section.
   * @param key     Key name. Cannot be {@code null} or empty.
   * @param value   Value of the INI entry.
   */
  IniEntry(IniMapSection section, String key, String value) {
    super(section);
    if (key == null || key.isBlank()) {
      throw new IllegalArgumentException("Key cannot be empty");
    }
    this.key = key.strip();
    setValue(value);
  }

  /**
   * Returns the entry key.
   */
  public String getKey() {
    return key;
  }

  /**
   * Returns the entry value.
   */
  public String getValue() {
    return value;
  }

  /**
   * Convenience function to return the numeric representation of the entry value.
   * <p>
   * The entry value may contain:
   * <ul>
   * <li>A hexadecimal integer value, using prefix {@code 0x},</li>
   * <li>A decimal number up to double precision,</li>
   * <li>Or an integer value in decimal notation.</li>
   * </ul>
   * </p>
   *
   * @param defValue A default value that is returned if the entry value cannot be converted to a number.
   * @return Numeric representation of the entry value. Returns {@code defValue} if the value could not be converted.
   */
  public Number getNumericValue(Number defValue) {
    try {
      if (value.toLowerCase().contains("0x")) {
        return Long.parseLong(value.toLowerCase().replace("0x", ""), 16);
      } else if (value.indexOf('.') >= 0 || value.toLowerCase().indexOf('e') >= 0) {
        return Double.parseDouble(value);
      } else {
        return Long.parseLong(value);
      }
    } catch (NumberFormatException ignored) {
    }
    return defValue;
  }

  /**
   * Convenience function to return the boolean representation of the entry value.
   * <p>
   * The following values are detected:
   * <ul>
   * <li>{@code false}: "false", "no", "n", "0"</li>
   * <li>{@code true}: "true", "yes", "y", "1"</li>
   * </ul>
   * </p>
   *
   * @param defValue A default value that is returned if the entry value cannot be evaluated as a boolean value.
   * @return A boolean representation of the entry value. Returns {@code defValue} if the value could not be interpreted.
   */
  public boolean getBoolValue(boolean defValue) {
    final String curValue = value.toLowerCase();
    final String[] trueValues = {"true", "yes", "y", "1"};
    for (final String v : trueValues) {
      if (curValue.equals(v)) {
        return true;
      }
    }

    final String[] falseValues = {"false", "no", "n", "0"};
    for (final String v : falseValues) {
      if (curValue.equals(v)) {
        return false;
      }
    }

    return defValue;
  }

  /**
   * Assigns a new value to the entry.
   */
  public IniEntry setValue(String newValue) {
    this.value = (newValue != null) ? newValue.strip() : "";
    return this;
  }

  @Override
  public int compareTo(IniBaseNode o) {
    if (o instanceof IniEntry entry) {
      return key.compareToIgnoreCase(entry.key);
    } else {
      return 1;
    }
  }

  @Override
  public int hashCode() {
    return Objects.hash(key.toLowerCase(), value.toLowerCase());
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
    IniEntry other = (IniEntry) obj;
    return key.equalsIgnoreCase(other.key) && value.equalsIgnoreCase(other.value);
  }

  @Override
  public String toString() {
    final IniMap.Style style = getSection().getMap().getStyle();
    return String.format("%s = %s", IniMap.getEscapedString(key, style), IniMap.getEscapedString(value, style));
  }
}
