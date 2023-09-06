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

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.tinylog.Logger;

import java.io.InputStream;
import java.io.Reader;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.function.Function;

/**
 * Represents the root element of a mod component tree.
 */
public class ComponentRoot extends ComponentContainerBase {
  /**
   * Parses JSON data with mod component information derived from the given {@code Path}.
   *
   * @param modName Name of the mod.
   * @param json A {@link Path} pointing to a JSON file.
   * @return {@link ComponentRoot} instance containing all group and component elements retrieved from the JSON data..
   * @throws Exception If the JSON data could not be parsed.
   */
  public static ComponentRoot parse(String modName, Path json) throws Exception {
    return parse(modName, Files.readString(json));
  }

  /**
   * Parses JSON data with mod component information derived from the given {@code URL}.
   *
   * @param modName Name of the mod.
   * @param json A {@link URL} instance pointing to a JSON file.
   * @return {@link ComponentRoot} instance containing all group and component elements retrieved from the JSON data..
   * @throws Exception If the JSON data could not be parsed.
   */
  public static ComponentRoot parse(String modName, URL json) throws Exception {
    try (final InputStream is = json.openStream()) {
      final byte[] buf = is.readAllBytes();
      return parse(modName, new String(buf));
    }
  }

  /**
   * Parses JSON data with mod component information derived from the given {@code Reader}.
   *
   * @param modName Name of the mod.
   * @param json A {@link Reader} instance referencing JSON data.
   * @return {@link ComponentRoot} instance containing all group and component elements retrieved from the JSON data..
   * @throws Exception If the JSON data could not be parsed.
   */
  public static ComponentRoot parse(String modName, Reader json) throws Exception {
    final StringBuilder sb = new StringBuilder();
    final char[] buf = new char[512];
    for (int len = json.read(buf); len >= 0; len = json.read(buf)) {
      if (len > 0) {
        sb.append(buf, 0, len);
      }
    }
    return parse(modName, sb.toString());
  }

  /**
   * Parses the given JSON string with mod component information.
   *
   * @param modName Name of the mod.
   * @param json String containing JSON data.
   * @return {@link ComponentRoot} instance containing all group and component elements retrieved from the JSON data..
   * @throws JSONException If the JSON data could not be parsed.
   */
  public static ComponentRoot parse(String modName, String json) throws JSONException {
    return parse(modName, new JSONArray(json));
  }

  /**
   * Parses the specified JSON structure with mod component information.
   *
   * @param modName Name of the mod.
   * @param root The JSON content from WeiDU's {@code --list-components-json} call as {@link JSONArray} object.
   * @return {@link ComponentRoot} instance containing all group and component elements retrieved from the JSON data..
   * @throws JSONException If the JSON data could not be parsed.
   */
  public static ComponentRoot parse(String modName, JSONArray root) throws JSONException {
    if (root == null) {
      throw new NullPointerException("JSON data is null");
    }

    enum Keys {
      Index("index"),
      Number("number"),
      Forced("forced"),
      Name("name"),
      Label("label"),
      SubGroup("subgroup"),
      Group("group"),
      MetaData("metadata"),
      ;
      final private String name;
      Keys(String name) {
        this.name = name;
      }
      @Override
      public String toString() {
        return name;
      }
    }

    final List<ComponentBase> children = new ArrayList<>();
    final List<ComponentGroup> groups = new ArrayList<>();

    // Returns a SubGroupInfo object with the specified name
    final Function<String, ComponentSubGroup> fnSubgroup = name -> (ComponentSubGroup) children.stream()
        .filter(bi -> (bi instanceof ComponentSubGroup) && ((ComponentSubGroup) bi).getName().equals(name)).findAny()
        .orElse(null);

    // Returns a GroupInfo object with the specified name
    final Function<String, ComponentGroup> fnGroup = name -> groups.stream().filter(bi -> bi.getName().equals(name))
        .findAny().orElse(null);

    for (final Object o : root) {
      if (o instanceof JSONObject component) {
        // handling groups
        try {
          final JSONArray array = component.getJSONArray(Keys.Group.toString());
          for (int i = 0, len = array.length(); i < len; i++) {
            final String name = array.getString(i);
            if (name != null) {
              final ComponentGroup gi = fnGroup.apply(name);
              if (gi == null) {
                groups.add(new ComponentGroup(null, name));
              }
            }
          }
        } catch (JSONException e) {
          Logger.debug("Parsing mod info JSON group (ignored)", e);
        }

        // handling subgroup
        ComponentSubGroup sgi = null;
        try {
          final String name = component.getString(Keys.SubGroup.toString());
          if (name != null) {
            sgi = fnSubgroup.apply(name);
            if (sgi == null) {
              sgi = new ComponentSubGroup(null, name);
              children.add(sgi);
            }
          }
        } catch (JSONException e) {
          Logger.debug("Parsing mod info JSON subgroup (ignored)", e);
        }

        // handling component
        ComponentInfo ci = null;
        try {
          final int index = component.getInt(Keys.Index.toString());
          final int id = component.getInt(Keys.Number.toString());
          final String name = component.getString(Keys.Name.toString());
          final boolean forced = component.getBoolean(Keys.Forced.toString());
          if (name != null) {
            ci = new ComponentInfo(sgi, index, id, name, forced);
            if (sgi != null) {
              sgi.add(ci);
            } else {
              children.add(ci);
            }
          }
        } catch (JSONException e) {
          Logger.debug("Parsing mod info JSON component (ignored)", e);
        }

        // handling component groups
        if (ci != null) {
          try {
            final JSONArray array = component.getJSONArray(Keys.Group.toString());
            for (int i = 0, len = array.length(); i < len; i++) {
              final String name = array.getString(i);
              final ComponentGroup gi = fnGroup.apply(name);
              if (gi != null) {
                gi.add(ci);
                ci.getGroupList().add(gi);
              }
            }
          } catch (JSONException e) {
            Logger.debug("Parsing mod info JSON component group (ignored)", e);
          }
        }

        // handling labels
        if (ci != null) {
          try {
            final JSONArray array = component.getJSONArray(Keys.Label.toString());
            for (int i = 0, len = array.length(); i < len; i++) {
              final String label = array.getString(i);
              final ComponentLabel li = new ComponentLabel(ci, label);
              ci.getLabelList().add(li);
            }
          } catch (JSONException e) {
            Logger.debug("Parsing mod info JSON component label (ignored)", e);
          }
        }

        // handling metadata
        if (ci != null) {
          try {
            final JSONArray array = component.getJSONArray(Keys.MetaData.toString());
            for (int i = 0, len = array.length(); i < len; i++) {
              final String data = array.getString(i);
              final ComponentMetaData mi = new ComponentMetaData(ci, data);
              ci.getMetadataList().add(mi);
            }
          } catch (JSONException e) {
            Logger.debug("Parsing mod info JSON component metadata (ignored)", e);
          }
        }
      }
    }

    return new ComponentRoot(modName, children, groups);
  }


  private final List<ComponentGroup> groups = new ArrayList<>();

  private final String modName;

  protected ComponentRoot(String modName, List<ComponentBase> components, List<ComponentGroup> groups) {
    super(null, Objects.requireNonNull(components).toArray(new ComponentBase[0]));
    this.modName = (modName != null) ? modName : "<unknown>";
    if (groups != null) {
      this.groups.addAll(groups);
    }
  }

  /** Returns the name of the mod. */
  public String getModName() {
    return modName;
  }

  /** Returns an unmodifiable list of all available component groups. */
  public List<ComponentGroup> getGroups() {
    return Collections.unmodifiableList(groups);
  }

  /** Adds the specified {@link ComponentGroup} object to the groups list. Returns the added element. */
  public ComponentGroup addGroup(ComponentGroup group) {
    int index = groups.indexOf(group);
    if (index >= 0) {
      groups.set(index, group);
    } else {
      groups.add(group);
    }
    return group;
  }

  /**
   * Removes the specified {@link ComponentGroup} object from the groups list. Returns {@code true} if an element was
   * found and removed.
   */
  public boolean remove(ComponentGroup group) {
    return groups.remove(group);
  }

  /**
   * Removes the {@link ComponentGroup} object from the groups list at the specified index. Returns {@code true} if the
   * element existed and was removed.
   */
  public boolean remove(int index) {
    if (index >= 0 && index < groups.size()) {
      groups.remove(index);
      return true;
    }
    return false;
  }

  @Override
  public int hashCode() {
    final int prime = 31;
    int result = super.hashCode();
    result = prime * result + Objects.hash(groups, modName);
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
    ComponentRoot other = (ComponentRoot) obj;
    return Objects.equals(groups, other.groups) && Objects.equals(modName, other.modName);
  }

  @Override
  public String toString() {
    return getModName();
  }
}
