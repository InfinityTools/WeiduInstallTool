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
package io.infinitytools.wit.fonts;

import javafx.scene.text.Font;
import org.tinylog.Logger;

import java.net.URL;
import java.util.HashMap;
import java.util.Map;

/**
 * List of custom fonts provided by the application.
 */
public enum Fonts {
  LiberationMonoRegular("fonts/liberation-mono/LiberationMono-Regular.ttf"),
  DejaVuSansMononRegular("fonts/dejavu-sans-mono/DejaVuSansMono.ttf"),
  ;

  private final Map<Double, Font> fontMap = new HashMap<>();
  private final String relPath;

  Fonts(String relPath) {
    this.relPath = relPath;
  }

  /**
   * Returns the font of default size (12.0).
   *
   * @return {@link Font} instance if available, {@code null} otherwise.
   */
  public Font getFont() {
    return getFont(12.0);
  }

  /**
   * Returns the font of specified point size.
   *
   * @param size Font size.
   * @return {@link Font} instance if available, {@code null} otherwise.
   */
  public Font getFont(double size) {
    return fontMap.computeIfAbsent(size, this::createFont);
  }

  private Font createFont(double size) {
    Font retVal = null;
    final URL url = Fonts.class.getResource(relPath);
    if (url != null) {
      retVal = Font.loadFont(url.toExternalForm(), size);
    }
    Logger.debug("Font for size {} loaded: {}", size, retVal);
    return retVal;
  }
}
