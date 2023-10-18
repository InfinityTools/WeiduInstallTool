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
package io.infinitytools.wml.mod.info;

import java.util.*;

/**
 * Represents a single instance of metadata that is associated with a mod component.
 */
public class ComponentMetaData extends ComponentBase {
  /**
   * Indicates where this component is position in the relative mod order.
   */
  public enum OrderType {
    /**
     * Mod component can be positioned anywhere in the stack of installed mods.
     */
    ANY,
    /**
     * Mod component should be installed <strong>before</strong> the specified list of mods.
     */
    BEFORE,
    /**
     * Mod component should be installed <strong>after</strong> the specified list of mods.
     */
    AFTER
  }

  /**
   * Defines a hint about the relative position of this mod component within the list of installed mods.
   *
   * @param type Specifies the relative position of this mod component.
   * @param mods List of mods that are relevant for the relative position of this mod component.
   */
  public record OrderHint(OrderType type, List<String> mods) {
    @Override
    public String toString() {
      return type + ": " + mods;
    }

    /**
     * Returns a {@link OrderType} enum based on the specified metadata content.
     *
     * @param data Unparsed metadata as string.
     * @return An initialized {@link OrderType} enum value.
     */
    public static OrderHint create(String data) {
      if (data != null) {
        final String line = data.strip().toLowerCase(Locale.ROOT);
        if (line.matches("^before\\s*=.+")) {
          int pos = line.indexOf('=');
          final String[] seq = line.substring(pos + 1).strip().split("\\s*,\\s*");
          return new OrderHint(OrderType.BEFORE, Arrays.asList(seq));
        } else if (line.matches("^after[ \t]*=.+")) {
          int pos = line.indexOf('=');
          final String[] seq = line.substring(pos + 1).strip().split("\\s*,\\s*");
          return new OrderHint(OrderType.AFTER, Arrays.asList(seq));
        }
      }

      return new OrderHint(OrderType.ANY, Collections.emptyList());
    }
  }

  private final String data;
  private final OrderHint orderHint;

  public ComponentMetaData(ComponentBase parent, String data) {
    super(parent);
    this.data = data;
    this.orderHint = OrderHint.create(this.data);
  }

  /**
   * Returns the metadata associated with the parent {@link ComponentBase} object.
   */
  public String getData() {
    return data;
  }

  /**
   * Returns the metadata content parsed as {@link OrderHint} object.
   *
   * @return {@link OrderHint} representation of the metadata.
   */
  public OrderHint getOrderHint() {
    return orderHint;
  }

  @Override
  public int hashCode() {
    return Objects.hash(data);
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
    ComponentMetaData other = (ComponentMetaData) obj;
    return Objects.equals(data, other.data);
  }

  @Override
  public String toString() {
    return getData();
  }
}
