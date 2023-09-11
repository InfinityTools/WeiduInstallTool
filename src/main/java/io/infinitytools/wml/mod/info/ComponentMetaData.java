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
 * Represents a single instance of metadata that is associated with a mod component.
 */
public class ComponentMetaData extends ComponentBase {
  private final String data;

  public ComponentMetaData(ComponentBase parent, String data) {
    super(parent);
    this.data = data;
  }

  /**
   * Returns the metadata associated with the parent {@link ComponentBase} object.
   */
  public String getData() {
    return data;
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
