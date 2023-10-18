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

import java.util.*;
import java.util.stream.Collectors;

/**
 * Represents an INI file section.
 */
public class IniMapSection implements Iterable<IniBaseNode> {
  private final IniMap map;
  private final String name;
  private final List<IniBaseNode> entries = new ArrayList<>();

  /**
   * Returns a new instance of the default section for the INI map.
   */
  public static IniMapSection getDefaultSection(IniMap map) {
    return new IniMapSection(map, null);
  }

  /**
   * Creates an empty INI section. This constructor is invoked internally by {@link IniMap#addSection(String)}.
   *
   * @param map  The associated {@link IniMap} instance.
   * @param name Name of the section. Specify {@code null} or an empty string to define a default section.
   */
  IniMapSection(IniMap map, String name) {
    this.map = Objects.requireNonNull(map);
    this.name = (name != null) ? name.strip() : "";
  }

  /**
   * Returns the {@link IniMap} instance this section is associated with.
   */
  public IniMap getMap() {
    return map;
  }

  /**
   * Returns the name of the section. The default (or implied) section of the INI file returns an empty string.
   */
  public String getName() {
    return name;
  }

  /**
   * Returns whether this instance defines a default section.
   */
  public boolean isDefaultSection() {
    return name.isEmpty();
  }

  /**
   * Returns a collection of the keys of all INI entries associated with this section.
   *
   * @return Collection of available INI entry keys.
   */
  public Collection<String> getKeys() {
    return entries.stream().filter(e -> e instanceof IniEntry).map(e -> ((IniEntry) e).getKey())
        .collect(Collectors.toList());
  }

  /**
   * Returns a collection of all INI entries associated with this section.
   *
   * @return Collection of available INI entries.
   */
  public Collection<IniEntry> getEntries() {
    return entries.stream().filter(e -> e instanceof IniEntry).map(e -> (IniEntry) e).collect(Collectors.toList());
  }

  /**
   * Returns a collection of all INI comments associated with this section.
   *
   * @return Collection of available INI comments.
   */
  public Collection<IniComment> getComments() {
    return entries.stream().filter(e -> e instanceof IniComment).map(e -> (IniComment) e).collect(Collectors.toList());
  }

  /**
   * Returns an unmodifiable collection of all child elements associated with this section.
   *
   * @return Collection of available child nodes.
   */
  public Collection<IniBaseNode> getNodes() {
    return Collections.unmodifiableList(entries);
  }

  /**
   * Returns whether an INI entry matching the specified key exists in this section.
   *
   * @param key Key name of the INI entry.
   * @return {@code true} if a match is found, {@code false} otherwise.
   */
  public boolean containsEntry(String key) {
    boolean retVal = false;
    if (key != null) {
      final String k = key.strip();
      retVal = entries.stream().filter(e -> e instanceof IniEntry).map(e -> (IniEntry) e)
          .anyMatch(e -> e.getKey().equalsIgnoreCase(k));
    }
    return retVal;
  }

  /**
   * Returns the {@link IniEntry} instance that matches the given key.
   *
   * @param key Key name of the INI entry.
   * @return {@link IniEntry} object if a match is found, {@code null} otherwise.
   */
  public IniEntry getEntry(String key) {
    IniEntry retVal = null;

    if (key != null) {
      final String k = key.strip();
      retVal = (IniEntry) entries.stream()
          .filter(e -> (e instanceof IniEntry) && ((IniEntry) e).getKey().equalsIgnoreCase(k)).findAny().orElse(null);
    }

    return retVal;
  }

  /**
   * Adds a new INI entry to this section.
   *
   * @param key   Key name. Cannot be {@code null} or empty.
   * @param value Value of the INI entry.
   * @return {@link IniEntry} instance of the added entry.
   */
  public IniEntry addEntry(String key, String value) {
    final IniEntry retVal = new IniEntry(this, key, value);
    entries.add(retVal);
    return retVal;
  }

  /**
   * Removes the specified INI entry from this section.
   *
   * @param key Key name. Cannot be {@code null} or empty.
   * @return {@code true} if an INI entry of the given name exists and is removed, {@code false} otherwise.
   */
  public boolean removeEntry(String key) {
    boolean retVal = false;
    if (key != null) {
      final String k = key.strip();
      retVal = entries.removeIf(e -> (e instanceof IniEntry) && ((IniEntry) e).getKey().equalsIgnoreCase(k));
    }
    return retVal;
  }

  /**
   * Adds a new INI comment to this section.
   *
   * @param comment The comment string.
   * @return {@link IniComment} instance of the added comment.
   */
  public IniComment addComment(String comment) {
    final IniComment retVal = new IniComment(this, comment);
    entries.add(retVal);
    return retVal;
  }

  /**
   * Removes the comment node at the specified index from this section.
   *
   * @param index Index of the comment node to remove. Only comment nodes are considered by the indexer.
   * @return {@code true} if the INI comment at the specified index exists and is removed, {@code false} otherwise.
   */
  public boolean removeComment(int index) {
    boolean retVal = false;

    final IniBaseNode node = entries.stream().filter(e -> e instanceof IniComment).skip(index).findFirst().orElse(null);
    if (node != null) {
      retVal = entries.remove(node);
    }

    return retVal;
  }

  /**
   * Returns an iterator over an unmodifiable list of {@link IniEntry} instances associated with this section.
   */
  @Override
  public Iterator<IniBaseNode> iterator() {
    return Collections.unmodifiableList(entries).iterator();
  }

  @Override
  public int hashCode() {
    return Objects.hash(name.toLowerCase());
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
    IniMapSection other = (IniMapSection) obj;
    return name.equalsIgnoreCase(other.name);
  }

  @Override
  public String toString() {
    final StringBuilder sb = new StringBuilder();
    if (!isDefaultSection()) {
      sb.append(String.format("[%s]", IniMap.getEscapedString(name, getMap().getStyle()))).append('\n');
    }
    for (final IniBaseNode node : entries) {
      sb.append(node.toString()).append('\n');
    }
    return sb.toString();
  }
}
