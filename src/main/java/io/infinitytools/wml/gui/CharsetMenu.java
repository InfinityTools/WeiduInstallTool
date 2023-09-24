/*
 * Copyright (c) 2023 Argent77
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
package io.infinitytools.wml.gui;

import io.infinitytools.wml.utils.R;
import javafx.beans.property.ReadOnlyObjectProperty;
import javafx.beans.property.ReadOnlyObjectWrapper;
import javafx.scene.control.Menu;
import javafx.scene.control.MenuItem;
import javafx.scene.control.RadioMenuItem;
import javafx.scene.control.ToggleGroup;

import java.nio.charset.Charset;
import java.util.*;

/**
 * Handles a preconfigured set of character encoding menu options.
 */
public class CharsetMenu {
  /**
   * Storage of a character set definition.
   *
   * @param label   The menu item label for display.
   * @param charset {@link Charset} instance associated with the label. {@code null} indicates to use default values.
   */
  public record CharsetInfo(String label, Charset charset) {
    /**
     * Convenience function for creating a new {@link CharsetInfo} record from a label and a character set name.
     *
     * @param label       Menu item label.
     * @param charsetName Name of the {@link Charset}.
     * @return Initialized {@link CharsetInfo} instance.
     * @throws IllegalArgumentException if a charset of the given name could not be created.
     */
    public static CharsetInfo of(String label, String charsetName) throws IllegalArgumentException {
      return new CharsetInfo(label, Charset.forName(charsetName));
    }

    @Override
    public boolean equals(Object o) {
      if (this == o) return true;
      if (o == null || getClass() != o.getClass()) return false;
      CharsetInfo that = (CharsetInfo) o;
      return Objects.equals(charset, that.charset);
    }

    @Override
    public int hashCode() {
      return Objects.hash(charset);
    }

    @Override
    public String toString() {
      return label;
    }
  }

  /**
   * Available language regions.
   */
  public enum Region {
    /**
     * Indicates charset entries without region-specific origin (placed on the root level of the menu.)
     */
    DEFAULT,
    /**
     * Character sets primarily used in western or central europe.
     */
    WESTERN_EUROPE(R.get("ui.charsets.westernEuropean")),
    /**
     * Character sets primarily used in eastern europe.
     */
    CENTRAL_EUROPE(R.get("ui.charsets.centralEuropean")),
    /**
     * Character sets used in countries with cyrillic alphabets.
     */
    CYRILLIC(R.get("ui.charsets.cyrillic")),
    /**
     * Character sets used throughout various regions in Asia.
     */
    ASIAN(R.get("ui.charsets.asian")),
    /**
     * Turkish character sets.
     */
    TURKISH(R.get("ui.charsets.turkish")),
    ;

    private final String label;

    Region() {
      this(null);
    }

    Region(String label) {
      this.label = Objects.nonNull(label) ? label : "";
    }

    /**
     * Returns the name of the Region. {@link #DEFAULT} returns an empty string.
     */
    @Override
    public String toString() {
      return label;
    }
  }

  /**
   * Available character set definitions.
   */
  public enum CharsetEntry {
    /**
     * Indicates to perform default character decoding.
     */
    DEFAULT(new CharsetInfo(R.get("ui.charsets.default"), null), Region.DEFAULT),
    CP850(CharsetInfo.of(String.format("CP850 (DOS, %s)", R.get("ui.charsets.westernEuropean")), "IBM850"), Region.WESTERN_EUROPE),
    CP860(CharsetInfo.of(String.format("CP860 (DOS, %s)", R.get("ui.charsets.portuguese")), "IBM860"), Region.WESTERN_EUROPE),
    CP437(CharsetInfo.of(String.format("CP437 (DOS, %s)", R.get("ui.charsets.english")), "IBM437"), Region.WESTERN_EUROPE),
    WINDOWS_1252(CharsetInfo.of(String.format("Windows-1252 (Windows, %s)", R.get("ui.charsets.westernEuropean")), "windows-1252"), Region.WESTERN_EUROPE),
    CP852(CharsetInfo.of(String.format("CP852 (DOS, %s)", R.get("ui.charsets.centralEuropean")), "IBM852"), Region.CENTRAL_EUROPE),
    ISO_8859_2(CharsetInfo.of(String.format("ISO-8859-2 (ISO, %s)", R.get("ui.charsets.centralEuropean")), "ISO-8859-2"), Region.CENTRAL_EUROPE),
    WINDOWS_1250(CharsetInfo.of(String.format("Windows-1250 (Windows, %s)", R.get("ui.charsets.centralEuropean")), "windows-1250"), Region.CENTRAL_EUROPE),
    CP855(CharsetInfo.of(String.format("CP855 (DOS, %s)", R.get("ui.charsets.cyrillic")), "IBM855"), Region.CYRILLIC),
    CP866(CharsetInfo.of(String.format("CP866 (DOS, %s)", R.get("ui.charsets.russian")), "IBM866"), Region.CYRILLIC),
    KOI8_R(CharsetInfo.of(String.format("KOI8-R (%s)", R.get("ui.charsets.russian")), "KOI8-R"), Region.CYRILLIC),
    ISO_8859_5(CharsetInfo.of(String.format("ISO-8859-5 (ISO, %s)", R.get("ui.charsets.cyrillic")), "ISO-8859-5"), Region.CYRILLIC),
    WINDOWS_1251(CharsetInfo.of(String.format("Windows-1251 (Windows, %s)", R.get("ui.charsets.cyrillic")), "windows-1251"), Region.CYRILLIC),
    WINDOWS_936(CharsetInfo.of(String.format("Windows-936 (Windows, %s)", R.get("ui.charsets.simplifiedChinese")), "GBK"), Region.ASIAN),
    GB2312(CharsetInfo.of(String.format("GB2312 (%s)", R.get("ui.charsets.simplifiedChinese")), "GB2312"), Region.ASIAN),
    BIG5(CharsetInfo.of(String.format("Big5 (%s)", R.get("ui.charsets.traditionalChinese")), "Big5"), Region.ASIAN),
    Big5_HKSCS(CharsetInfo.of(String.format("Big5-HKSCS (%s)", R.get("ui.charsets.hongkongChinese")), "Big5-HKSCS"), Region.ASIAN),
    Shift_JIS(CharsetInfo.of(String.format("Shift-JIS (%s)", R.get("ui.charsets.japanese")), "Shift_JIS"), Region.ASIAN),
    EUC_JP(CharsetInfo.of(String.format("EUC-JP (%s)", R.get("ui.charsets.japanese")), "EUC-JP"), Region.ASIAN),
    EUC_KR(CharsetInfo.of(String.format("EUC-KR (%s)", R.get("ui.charsets.korean")), "EUC-KR"), Region.ASIAN),
    WINDOWS_949(CharsetInfo.of(String.format("Windows-949 (Windows, %s)", R.get("ui.charsets.korean")), "x-windows-949"), Region.ASIAN),
    CP857(CharsetInfo.of(String.format("CP857 (DOS, %s)", R.get("ui.charsets.turkish")), "IBM857"), Region.TURKISH),
    ISO_8859_3(CharsetInfo.of(String.format("ISO-8859-3 (ISO, %s)", R.get("ui.charsets.southernEuropean")), "ISO-8859-3"), Region.TURKISH),
    ISO_8859_9(CharsetInfo.of(String.format("ISO-8859-9 (ISO, %s)", R.get("ui.charsets.turkish")), "ISO-8859-9"), Region.TURKISH),
    WINDOWS_1254(CharsetInfo.of(String.format("Windows-1254 (Windows, %s)", R.get("ui.charsets.turkish")), "windows-1254"), Region.TURKISH),
    ;

    private final Region region;
    private final CharsetInfo info;

    CharsetEntry(CharsetInfo info, Region region) {
      this.region = region;
      this.info = info;
    }

    public CharsetInfo getInfo() {
      return info;
    }

    public Region getRegion() {
      return region;
    }

    @Override
    public String toString() {
      return info.toString();
    }
  }

  private final List<MenuItem> menuList = new ArrayList<>();
  private final ToggleGroup itemGroup = new ToggleGroup();
  private final ReadOnlyObjectWrapper<RadioMenuItem> selected = new ReadOnlyObjectWrapper<>();

  /**
   * Creates a character encoding menu structure without top-levem menu that can be added to a
   * {@link javafx.scene.control.ContextMenu} or {@link Menu}.
   */
  public CharsetMenu() {
    this(null);
  }

  /**
   * Creates a character encoding menu structure with a top-level {@link Menu} of the given label that can be added to
   * a {@link javafx.scene.control.ContextMenu} or {@link Menu}.
   *
   * @param label Text of the top-level {@link Menu} entry.
   */
  public CharsetMenu(String label) {
    init(label);
  }

  /**
   * Selects the {@code RadioMenuItem} associated with the specified {@link CharsetInfo} instance.
   */
  public void selectItem(CharsetInfo info) {
    selectItem(findMenuItem(info));
  }

  /**
   * Selects the specified {@link RadioMenuItem} instance.
   */
  public void selectItem(RadioMenuItem value) {
    itemGroup.selectToggle(value);
  }

  /**
   * Gets the selected {@link RadioMenuItem}.
   */
  public RadioMenuItem getSelectedItem() {
    if (itemGroup.getSelectedToggle() instanceof RadioMenuItem rmi) {
      return rmi;
    }
    return null;
  }

  /**
   * The selected {@link RadioMenuItem}.
   */
  public ReadOnlyObjectProperty<RadioMenuItem> selectedProperty() {
    return selected.getReadOnlyProperty();
  }

  /**
   * Returns the charset menu structure as a list of {@link MenuItem}s.
   *
   * @return {@link List} of menu items.
   */
  public List<MenuItem> getItems() {
    return Collections.unmodifiableList(menuList);
  }

  /**
   * Returns the menu item associated with the specified {@link CharsetEntry}.
   *
   * @param entry {@link CharsetEntry} associated with the menu item.
   * @return {@link RadioMenuItem} instance if available, {@code null} otherwise.
   */
  public RadioMenuItem findMenuItem(CharsetEntry entry) {
    if (entry != null) {
      return findMenuItem(entry.getInfo());
    }
    return null;
  }

  /**
   * Returns the menu item associated with the specified {@link CharsetInfo}.
   *
   * @param info {@link CharsetInfo} associated with the menu item.
   * @return {@link RadioMenuItem} instance if available, {@code null} otherwise.
   */
  public RadioMenuItem findMenuItem(CharsetInfo info) {
    if (info != null) {
      return findMenuItem(info, null);
    }
    return null;
  }

  /**
   * Searches the menu list recursively for a matching {@link RadioMenuItem}.
   *
   * @param info {@link CharsetInfo} of the requested menu item.
   * @param item Menu item to check. Specify {@code null} to check the whole list.
   * @return Matching {@link RadioMenuItem} instance if found, {@code null} otherwise.
   */
  private RadioMenuItem findMenuItem(CharsetInfo info, MenuItem item) {
    if (item instanceof RadioMenuItem rmi) {
      if (Objects.equals(info, rmi.getUserData())) {
        return rmi;
      } else {
        return null;
      }
    }

    final List<MenuItem> list;
    if (item == null) {
      list = menuList;
    } else if (item instanceof Menu menu) {
      list = menu.getItems();
    } else {
      return null;
    }

    for (final MenuItem mi : list) {
      RadioMenuItem rmi = findMenuItem(info, mi);
      if (rmi != null) {
        return rmi;
      }
    }

    return null;
  }

  /**
   * Initializes the menu item structure.
   */
  private void init(String label) {
    final HashMap<Region, Menu> menuCache = new HashMap<>();

    final List<MenuItem> list;
    if (label != null) {
      final Menu rootMenu = new Menu(label);
      menuList.add(rootMenu);
      list = rootMenu.getItems();
    } else {
      list = menuList;
    }

    for (final CharsetEntry entry : CharsetEntry.values()) {
      final RadioMenuItem rmi = new RadioMenuItem(entry.getInfo().label());
      rmi.setUserData(entry.getInfo());
      rmi.setToggleGroup(itemGroup);

      final Region region = entry.getRegion();
      if (region.toString().isEmpty()) {
        list.add(rmi);
      } else {
        Menu menu = menuCache.get(region);
        if (menu == null) {
          menu = new Menu(region.toString());
          list.add(menu);
          menuCache.put(region, menu);
        }
        menu.getItems().add(rmi);
      }
    }

    itemGroup.selectedToggleProperty().addListener((ob, ov, nv) -> {
      if (nv instanceof RadioMenuItem rmi) {
        selected.set(rmi);
      }
    });

    // selected by default
    findMenuItem(CharsetEntry.DEFAULT.getInfo()).setSelected(true);
  }
}
