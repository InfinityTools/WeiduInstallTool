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
package io.infinitytools.wml.icons;

import javafx.scene.image.Image;
import org.tinylog.Logger;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * Available icons and graphics resources.
 */
public enum Icons {
  /**
   * Application icon
   */
  Icon256("icon/256x256.png"),
  /**
   * Application icon
   */
  Icon128("icon/128x128.png"),
  /**
   * Application icon
   */
  Icon64("icon/64x64.png"),
  /**
   * Application icon
   */
  Icon32("icon/32x32.png"),
  /**
   * Application icon
   */
  Icon22("icon/22x22.png"),
  /**
   * Application icon
   */
  Icon16("icon/16x16.png"),
  /**
   * Collection of application icons in all available dimensions.
   */
  Icon(Icon256.getFileName(), Icon128.getFileName(), Icon64.getFileName(), Icon32.getFileName(), Icon22.getFileName(),
      Icon16.getFileName()),

  /**
   * Application icon (running process)
   */
  IconRunning256("icon-running/256x256.png"),
  /**
   * Application icon (running process)
   */
  IconRunning128("icon-running/128x128.png"),
  /**
   * Application icon (running process)
   */
  IconRunning64("icon-running/64x64.png"),
  /**
   * Application icon (running process)
   */
  IconRunning32("icon-running/32x32.png"),
  /**
   * Application icon (running process)
   */
  IconRunning22("icon-running/22x22.png"),
  /**
   * Application icon (running process)
   */
  IconRunning16("icon-running/16x16.png"),
  /**
   * Collection of application icons in all available dimensions (running process).
   */
  IconRunning(IconRunning256.getFileName(), IconRunning128.getFileName(), IconRunning64.getFileName(),
      IconRunning32.getFileName(), IconRunning22.getFileName(), IconRunning16.getFileName()),

  /**
   * Application icon (Dark Mode UI version)
   */
  IconDark256("icon-dark/256x256.png"),
  /**
   * Application icon (Dark Mode UI version)
   */
  IconDark128("icon-dark/128x128.png"),
  /**
   * Application icon (Dark Mode UI version)
   */
  IconDark64("icon-dark/64x64.png"),
  /**
   * Application icon (Dark Mode UI version)
   */
  IconDark32("icon-dark/32x32.png"),
  /**
   * Application icon (Dark Mode UI version)
   */
  IconDark22("icon-dark/22x22.png"),
  /**
   * Application icon (Dark Mode UI version)
   */
  IconDark16("icon-dark/16x16.png"),
  /**
   * Collection of application icons in all available dimensions. (Dark Mode UI version)
   */
  IconDark(IconDark256.getFileName(), IconDark128.getFileName(), IconDark64.getFileName(), IconDark32.getFileName(),
      IconDark22.getFileName(), IconDark16.getFileName()),

  /**
   * Application icon (Dark Mode UI version, running process)
   */
  IconRunningDark256("icon-running-dark/256x256.png"),
  /**
   * Application icon (Dark Mode UI version, running process)
   */
  IconRunningDark128("icon-running-dark/128x128.png"),
  /**
   * Application icon (Dark Mode UI version, running process)
   */
  IconRunningDark64("icon-running-dark/64x64.png"),
  /**
   * Application icon (Dark Mode UI version, running process)
   */
  IconRunningDark32("icon-running-dark/32x32.png"),
  /**
   * Application icon (Dark Mode UI version, running process)
   */
  IconRunningDark22("icon-running-dark/22x22.png"),
  /**
   * Application icon (Dark Mode UI version, running process)
   */
  IconRunningDark16("icon-running-dark/16x16.png"),
  /**
   * Collection of application icons in all available dimensions. (Dark Mode UI version, running process)
   */
  IconRunningDark(IconRunningDark256.getFileName(), IconRunningDark128.getFileName(), IconRunningDark64.getFileName(),
      IconRunningDark32.getFileName(), IconRunningDark22.getFileName(), IconRunningDark16.getFileName()),

  /**
   * Transparent application logo
   */
  Logo256("logo/256x256.png"),
  /**
   * Transparent application logo
   */
  Logo128("logo/128x128.png"),
  /**
   * Transparent application logo
   */
  Logo64("logo/64x64.png"),
  /**
   * Transparent application logo
   */
  Logo32("logo/32x32.png"),
  /**
   * Transparent application logo
   */
  Logo16("logo/16x16.png"),
  /**
   * Collection of application logos in all available dimensions.
   */
  Logo(Logo256.getFileName(), Logo128.getFileName(), Logo64.getFileName(), Logo32.getFileName(), Logo16.getFileName()),

  /**
   * Icon for "Open Mod: button
   */
  OpenMod256("icon-tp2/256x256.png"),
  /**
   * Icon for "Open Mod: button
   */
  OpenMod128("icon-tp2/128x128.png"),
  /**
   * Icon for "Open Mod: button
   */
  OpenMod64("icon-tp2/64x64.png"),
  /**
   * Icon for "Open Mod: button
   */
  OpenMod32("icon-tp2/32x32.png"),
  /**
   * Collection of "Open Mod" button icons in all available dimensions.
   */
  OpenMod(OpenMod256.getFileName(), OpenMod128.getFileName(), OpenMod64.getFileName(), OpenMod32.getFileName()),

  /**
   * Icon for "Open Mod: button (Dark Mode UI version)
   */
  OpenModDark256("icon-tp2-dark/256x256.png"),
  /**
   * Icon for "Open Mod: button (Dark Mode UI version)
   */
  OpenModDark128("icon-tp2-dark/128x128.png"),
  /**
   * Icon for "Open Mod: button (Dark Mode UI version)
   */
  OpenModDark64("icon-tp2-dark/64x64.png"),
  /**
   * Icon for "Open Mod: button (Dark Mode UI version)
   */
  OpenModDark32("icon-tp2-dark/32x32.png"),
  /**
   * Collection of "Open Mod" button icons in all available dimensions (Dark Mode UI version).
   */
  OpenModDark(OpenModDark256.getFileName(), OpenModDark128.getFileName(), OpenModDark64.getFileName(),
      OpenModDark32.getFileName()),

  /**
   * Icon for Options menu
   */
  Options256("options/256x256.png"),
  /**
   * Icon for Options menu
   */
  Options128("options/128x128.png"),
  /**
   * Icon for Options menu
   */
  Options64("options/64x64.png"),
  /**
   * Icon for Options menu
   */
  Options32("options/32x32.png"),
  /**
   * Collection of options menu icons in all available dimensions.
   */
  Options(Options256.getFileName(), Options128.getFileName(), Options64.getFileName(), Options32.getFileName()),

  /**
   * Icon for Options menu (Dark Mode UI version)
   */
  OptionsDark256("options-dark/256x256.png"),
  /**
   * Icon for Options menu (Dark Mode UI version)
   */
  OptionsDark128("options-dark/128x128.png"),
  /**
   * Icon for Options menu (Dark Mode UI version)
   */
  OptionsDark64("options-dark/64x64.png"),
  /**
   * Icon for Options menu (Dark Mode UI version)
   */
  OptionsDark32("options-dark/32x32.png"),
  /**
   * Collection of options menu icons in all available dimensions (Dark Mode UI version).
   */
  OptionsDark(OptionsDark256.getFileName(), OptionsDark128.getFileName(), OptionsDark64.getFileName(),
      OptionsDark32.getFileName()),
  ;

  private final List<String> fileNames = new ArrayList<>();

  private final List<Image> images = new ArrayList<>();

  Icons(String... names) {
    this.fileNames.addAll(Arrays.asList(names));
    for (int i = 0; i < this.fileNames.size(); i++) {
      this.images.add(null);
    }
  }

  /**
   * Returns the filename of the icon. For icon groups the filename of the first available icon is returned.
   */
  public String getFileName() {
    return fileNames.get(0);
  }

  /**
   * Returns the filenames of all available icons associated with the enum value.
   */
  public List<String> getFileNames() {
    return Collections.unmodifiableList(fileNames);
  }

  /**
   * Returns the {@link Image} object of the icon. For icon groups the image of the first available icon is returned.
   */
  public Image getImage() {
    // lazy initialization
    ensureImage(0);
    return images.get(0);
  }

  /**
   * Returns the {@link Image} objects of all available icons associated with the enum value.
   */
  public List<Image> getImages() {
    // lazy initialization
    for (int i = 0; i < images.size(); i++) {
      ensureImage(i);
    }
    return Collections.unmodifiableList(images);
  }

  /**
   * Ensures that the image at the specified list index is initialized.
   */
  private void ensureImage(int index) {
    if (index >= 0 && index < images.size()) {
      if (images.get(index) == null) {
        images.set(index, getImage(Icons.class, fileNames.get(index)));
      }
    }
  }

  /**
   * A static method for loading an image from an embedded graphics resource.
   * <p>
   * The following image formats are supported: BMP, GIF, JPEG, PNG.
   * </p>
   *
   * @param cls      {@code Class} object is used to determine the root path within the Java archive. Specify
   *                 {@code null} to use {@code Icons.class} as root for the image resource.
   * @param fileName Filename of the image resource (optionally, prepended by a relative path) relative to the root
   *                 path.
   * @return {@link Image} object if successful, {@code null} otherwise.
   */
  public static Image getImage(Class<?> cls, String fileName) {
    Image retVal = null;

    if (fileName == null) {
      return null;
    }

    final Class<?> baseClass = (cls != null) ? cls : Icons.class;
    try (final InputStream is = baseClass.getResourceAsStream(fileName)) {
      assert is != null;
      retVal = new Image(is);
    } catch (IOException e) {
      Logger.error(e, "Image not accessible: {}", fileName);
    }

    return retVal;
  }
}
