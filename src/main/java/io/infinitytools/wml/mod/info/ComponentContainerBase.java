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

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

/**
 * Specialized base class for component info classes with child elements.
 */
public class ComponentContainerBase extends ComponentBase {
  private final List<ComponentBase> children = new ArrayList<>();

  /**
   * Initializes the object.
   *
   * @param parent   Reference to the parent {@link ComponentBase} object.
   * @param children Optional list of child elements to be added to the child list.
   */
  protected ComponentContainerBase(ComponentBase parent, ComponentBase... children) {
    super(parent);

    for (final ComponentBase child : children) {
      add(child);
    }
  }

  /** Returns an unmodifiable list of all child elements associated with this object. */
  @Override
  public List<ComponentBase> getChildren() {
    return Collections.unmodifiableList(children);
  }

  /** Adds the specified {@link ComponentBase} object to the child list. Returns the added element. */
  @Override
  public ComponentBase add(ComponentBase child) {
    int index = children.indexOf(child);
    if (index >= 0) {
      children.set(index, child);
    } else {
      children.add(child);
    }
    return child;
  }

  /**
   * Removes the specified {@link ComponentBase} object from the child list. Returns {@code true} if an element was found and
   * removed.
   */
  @Override
  public boolean remove(ComponentBase child) {
    return children.remove(child);
  }

  /**
   * Removes the {@link ComponentBase} object from the child list at the specified index. Returns {@code true} if the element
   * existed and was removed.
   */
  @Override
  public boolean remove(int index) {
    if (index >= 0 && index < children.size()) {
      children.remove(index);
      return true;
    }
    return false;
  }

  @Override
  public int hashCode() {
    final int prime = 31;
    int result = super.hashCode();
    result = prime * result + Objects.hash(children);
    return result;
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj) {
      return true;
    }
    if (!super.equals(obj)) {
      return false;
    }
    if (getClass() != obj.getClass()) {
      return false;
    }
    ComponentContainerBase other = (ComponentContainerBase) obj;
    return Objects.equals(children, other.children);
  }
}
