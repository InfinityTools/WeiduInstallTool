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

import java.util.Objects;

/**
 * Represents the top-level element of a multiple choice component. Child elements represent the available choices.
 */
public class ComponentSubGroup extends ComponentContainerBase {
  private final String name;

  public ComponentSubGroup(ComponentBase parent, String name, ComponentBase... children) {
    super(parent, children);
    this.name = Objects.requireNonNull(name).strip();
  }

  /**
   * Returns the name of the subgroup.
   */
  public String getName() {
    return name;
  }

  @Override
  public int hashCode() {
    return Objects.hash(name);
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
    ComponentSubGroup other = (ComponentSubGroup) obj;
    return Objects.equals(name, other.name);
  }

  @Override
  public String toString() {
    return "?: " + getName();
  }
}
