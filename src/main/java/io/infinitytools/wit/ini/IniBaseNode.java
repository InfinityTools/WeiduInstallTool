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
package io.infinitytools.wit.ini;

import java.util.Objects;

/**
 * Common base class for INI entry and comment nodes.
 */
public abstract class IniBaseNode implements Comparable<IniBaseNode> {
  private final IniMapSection section;

  /**
   * Initializes base attributes of an INI node.
   *
   * @param section INI section this comment is associated with.
   */
  protected IniBaseNode(IniMapSection section) {
    this.section = Objects.requireNonNull(section);
  }

  /**
   * Returns the {@link IniMapSection} instance this node is associated with.
   */
  public IniMapSection getSection() {
    return section;
  }
}
