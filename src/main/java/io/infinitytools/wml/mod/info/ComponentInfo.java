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
 * Represents an actual mod component. For multiple choice components this is a child of a {@link ComponentSubGroup}
 * instance.
 */
public class ComponentInfo extends ComponentBase {
  /** List of {@link ComponentGroup} instances this component is associated with. */
  private final List<ComponentGroup> groupList = new ArrayList<>();

  /** List of available {@link ComponentLabel} instances for this component. */
  private final List<ComponentLabel> labelList = new ArrayList<>();

  /** List of available {@link ComponentMetaData} instances for this component. */
  private final List<ComponentMetaData> metaList = new ArrayList<>();

  private final int index;
  private final int id;
  private final String name;
  private final boolean forced;

  public ComponentInfo(ComponentBase parent, int index, int id, String name, boolean forced) {
    super(parent);
    this.index = index;
    this.id = id;
    this.name = Objects.requireNonNull(name, "Name cannot be null").strip();
    this.forced = forced;
  }

  /** Returns the sequential index, as the component is defined in the tp2 script. */
  public int getIndex() {
    return index;
  }

  /** Returns the numeric identifier of the component, which is unique for the whole mod. */
  public int getId() {
    return id;
  }

  /**
   * Returns the name of the mod component. If this is an option of a multiple choice component then this
   * name represents the name of the component option.
   *
   * @return Component name as string.
   */
  public String getName() {
    return name;
  }

  /** Returns whether this is a forced component. */
  public boolean isForced() {
    return forced;
  }

  /** Returns an unmodifiable list of all associated groups. */
  public List<ComponentGroup> getGroups() {
    return Collections.unmodifiableList(groupList);
  }

  /** Write access to the group list for internal purposes. */
  List<ComponentGroup> getGroupList() {
    return groupList;
  }

  /** Returns an unmodifiable list of all available labels for this component. */
  public List<ComponentLabel> getLabels() {
    return Collections.unmodifiableList(labelList);
  }

  /** Write access to the label list for internal purposes. */
  List<ComponentLabel> getLabelList() {
    return labelList;
  }

  /** Returns an unmodifiable list of all available metadata information for this component. */
  public List<ComponentMetaData> getMetadata() {
    return Collections.unmodifiableList(metaList);
  }

  /** Write access to the metadata list for internal purposes. */
  List<ComponentMetaData> getMetadataList() {
    return metaList;
  }

  @Override
  public int hashCode() {
    return Objects.hash(id);
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
    ComponentInfo other = (ComponentInfo) obj;
    return id == other.id;
  }

  @Override
  public String toString() {
    if (isForced()) {
      return String.format("%d: %s [f]", id, name);
    } else {
      return String.format("%d: %s", id, name);
    }
  }
}
