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
package io.infinitytools.wit.mod.info;

import io.infinitytools.wit.utils.R;

import java.util.Objects;

/**
 * A mod group includes one or more components. (See WeiDU {@code GROUP} directive.)
 */
public class ComponentGroup extends ComponentContainerBase {
  /**
   * An independent meta-group that can be used as filter.
   */
  public static final ComponentGroup GROUP_NONE = new ComponentGroup(null, R.get("ui.details.group.entry.none"));

  private final String name;

  public ComponentGroup(ComponentBase parent, String name, ComponentBase... children) {
    super(parent, children);
    this.name = (name != null) ? name.strip() : "";
  }

  /**
   * Returns the name of the group.
   */
  public String getName() {
    return name;
  }

  @Override
  public int hashCode() {
    final int prime = 31;
    int result = super.hashCode();
    result = prime * result + Objects.hash(name);
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
    ComponentGroup other = (ComponentGroup) obj;
    return Objects.equals(name, other.name);
  }

  @Override
  public String toString() {
    return getName();
  }
}
