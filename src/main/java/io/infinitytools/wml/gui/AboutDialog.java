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
package io.infinitytools.wml.gui;

import io.infinitytools.wml.icons.Icons;
import io.infinitytools.wml.utils.R;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.control.Hyperlink;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.Window;
import javafx.stage.WindowEvent;
import org.tinylog.Logger;

import java.net.URL;

/**
 * A modal dialog that displays information about the launcher.
 */
public class AboutDialog extends Stage {
  /**
   * Path to the FXML definition file for this window.
   */
  private static final URL FXML_FILE = AboutDialog.class.getResource("about.fxml");

  private static AboutDialog instance;

  /**
   * Shows a modal dialog with information about the launcher.
   */
  public static void showAboutDialog(Window owner) throws Exception {
    if (instance == null) {
      instance = new AboutDialog(owner);
    }
    instance.showAndWait();
  }

  private AboutWindowController controller;

  private AboutDialog(Window owner) throws Exception {
    super();
    initOwner(owner);
    initModality(Modality.WINDOW_MODAL);
    init();
  }

  /**
   * Opens the specified URL in the default browser of the system.
   */
  private void openLink(String url) {
    if (url != null) {
      MainWindow.getInstance().getHostServices().showDocument(url);
    }
  }

  private void onHyperlinkClick(Hyperlink node) {
    if (node != null) {
      openLink(node.getText());
      node.setVisited(false);
      controller.okButton.requestFocus();
    }
  }

  private void onOkButtonClick() {
    hide();
  }

  private void onWindowShown(WindowEvent event) {
    if (getOwner() != null) {
      final double x = getOwner().getX() + (getOwner().getWidth() - instance.getWidth()) / 2.0;
      final double y = getOwner().getY() + (getOwner().getHeight() - instance.getHeight()) / 2.0;
      setX(x);
      setY(y);
      Logger.debug("About window position: {}, {}", x, y);
    } else {
      centerOnScreen();
    }
  }

  /**
   * Initializes the dialog content.
   */
  private void init() throws Exception {
    final FXMLLoader loader = new FXMLLoader(FXML_FILE, R.getBundle());
    final Scene scene = new CustomScene(loader.load());
    controller = loader.getController();
    controller.init();

    controller.projectLink.setOnAction(event -> onHyperlinkClick(controller.projectLink));
    controller.wikiLink.setOnAction(event -> onHyperlinkClick(controller.wikiLink));
    controller.weiduLink.setOnAction(event -> onHyperlinkClick(controller.weiduLink));
    controller.okButton.setOnAction(event -> onOkButtonClick());

    setScene(scene);
    setTitle(R.get("ui.about.title"));
    setResizable(false);
    controller.okButton.requestFocus();

    // assigning application icon
    getIcons().addAll(Icons.Icon.getImages());

    setOnShown(this::onWindowShown);
  }
}
