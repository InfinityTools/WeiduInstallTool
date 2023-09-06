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

import java.util.List;
import java.util.Objects;

/**
 * Common base for all mod component info classes.
 */
public class ComponentBase {
  private final ComponentBase parent;

  /**
   * Initializes the object.
   *
   * @param parent Reference to the parent {@link ComponentBase} object.
   */
  protected ComponentBase(ComponentBase parent) {
    this.parent = parent;
  }

  /**
   * Returns the reference to the parent {@link ComponentBase} object.
   *
   * @return Parent {@link ComponentBase} instance. {@code null} for the top-level object.
   */
  public ComponentBase getParent() {
    return parent;
  }

  /** Returns an unmodifiable list of all child elements. Empty method in the {@code BaseInfo} class. */
  protected List<ComponentBase> getChildren() {
    return null;
  }

  /** Associated the specified child with this object. Empty method in the {@code BaseInfo} class. */
  protected ComponentBase add(ComponentBase child) {
    return null;
  }

  /** Removes the specified child from the list. Empty method in the {@code BaseInfo} class. */
  protected boolean remove(ComponentBase child) {
    return false;
  }

  /** Removes the child at the specified index from the list. Empty method in the {@code BaseInfo} class. */
  protected boolean remove(int index) {
    return false;
  }

  @Override
  public int hashCode() {
    return Objects.hash(parent);
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
    ComponentBase other = (ComponentBase) obj;
    return Objects.equals(parent, other.parent);
  }
}
