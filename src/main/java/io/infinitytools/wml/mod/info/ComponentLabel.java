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
 * Represents a descriptive label that is associated with a mod component.
 */
public class ComponentLabel extends ComponentBase {
  private final String label;

  public ComponentLabel(ComponentBase parent, String label) {
    super(parent);
    this.label = label;
  }

  /** Returns the label associated with the parent {@link ComponentBase} object. */
  public String getLabel() {
    return label;
  }

  @Override
  public int hashCode() {
    return Objects.hash(label);
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
    ComponentLabel other = (ComponentLabel) obj;
    return Objects.equals(label, other.label);
  }

  @Override
  public String toString() {
    return getLabel();
  }
}
