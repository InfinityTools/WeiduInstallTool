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
package io.infinitytools.wml.utils;

import javafx.geometry.Bounds;
import javafx.scene.text.Font;
import javafx.scene.text.Text;

/**
 * A helper class for calculating text sizes based on a specific font.
 */
public class FontMetrics {
  private final Text internal;
  private final double ascent;
  private final double descent;
  private final double lineHeight;

  public FontMetrics(Font f) {
    this.internal = new Text();
    if (f != null) {
      this.internal.setFont(f);
    }

    final Bounds b = internal.getLayoutBounds();
    this.lineHeight = b.getHeight();
    this.ascent = -b.getMinY();
    this.descent = b.getMaxY();
  }

  public double getAscent() {
    return ascent;
  }

  public double getDescent() {
    return descent;
  }

  public double getLineHeight() {
    return lineHeight;
  }

  /**
   * Returns the width of the specified string.
   *
   * @param text string value.
   * @return width in pixel.
   */
  public double computeStringWidth(String text) {
    double retVal = 0.0;

    if (text != null) {
      internal.setText(text);
      retVal = internal.getLayoutBounds().getWidth();
    }

    return retVal;
  }
}
