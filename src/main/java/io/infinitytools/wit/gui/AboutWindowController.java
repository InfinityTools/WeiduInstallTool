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
package io.infinitytools.wit.gui;

import io.infinitytools.wit.Globals;
import io.infinitytools.wit.utils.R;
import javafx.scene.control.Button;
import javafx.scene.control.Hyperlink;
import javafx.scene.control.Label;
import javafx.scene.control.Labeled;
import javafx.scene.image.ImageView;
import org.tinylog.Logger;

public class AboutWindowController {
  public ImageView logoImage;
  public Label titleLabel;
  public Label versionLabel;
  public Hyperlink projectLink;
  public Hyperlink wikiLink;
  public Hyperlink weiduLink;
  public Label descLabel;
  public Label copyLabel;
  public Button okButton;

  public AboutWindowController() {
  }

  /**
   * Initializes the UI components. This method should be called after the UI has been loaded.
   */
  public void init() {
    try {
      titleLabel.setText(Globals.APP_TITLE);
      versionLabel.setText(String.format("%s %s", R.get("ui.about.version.label"), Globals.APP_VERSION));
      updateLink(Globals.PROJECT_URL, projectLink);
      updateLink(Globals.WIKI_URL, wikiLink);
      updateLink(Globals.WEIDU_URL, weiduLink);
    } catch (NullPointerException e) {
      Logger.error(e);
    }
  }

  /**
   * Used internally to update URLs in UI controls.
   *
   * @param url     URL string.
   * @param labeled The UI control to update.
   */
  private static void updateLink(String url, Labeled labeled) {
    if (labeled == null) {
      return;
    }

    if (url != null) {
      labeled.setText(url);
    } else {
      labeled.setDisable(true);
    }
  }
}
