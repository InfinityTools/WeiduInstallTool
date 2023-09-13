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
import io.infinitytools.wml.mod.ModInfo;
import io.infinitytools.wml.mod.info.ComponentBase;
import io.infinitytools.wml.mod.info.ComponentContainerBase;
import io.infinitytools.wml.mod.info.ComponentRoot;
import io.infinitytools.wml.mod.ini.ModIni;
import javafx.collections.ObservableList;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Rectangle2D;
import javafx.scene.Node;
import javafx.scene.control.Hyperlink;
import javafx.scene.control.Label;
import javafx.scene.control.SplitPane;
import javafx.scene.control.TreeItem;
import javafx.stage.Modality;
import javafx.stage.Screen;
import javafx.stage.Stage;
import javafx.stage.WindowEvent;
import org.tinylog.Logger;

import java.net.URL;

/**
 * Provides detailed information about mod components and optional mod.ini content.
 */
public class DetailsWindow extends Stage {
  /**
   * Path to the FXML definition file for this window.
   */
  private final static URL FXML_FILE = MainWindow.class.getResource("details.fxml");

  private static final String TITLE = "Details";

  private final ModInfo modInfo;

  private DetailsWindowController controller;
  private Rectangle2D windowRect;
  private boolean windowMaximized;

  public DetailsWindow(ModInfo modInfo) throws Exception {
    super();
    this.modInfo = modInfo;
    initModality(Modality.NONE);
    init();
  }

  /**
   * Called by listener of the language combobox when the item selection changes.
   */
  private void onLanguageItemSelected(int newIndex) {
    try {
      ComponentRoot components = modInfo.getComponentInfo(newIndex);
      initComponentsTree(components);
    } catch (IndexOutOfBoundsException e) {
      Logger.error(e, "Language item selected at index {}", newIndex);
    }
  }

  private void initComponentsTree(ComponentRoot components) {
    controller.componentsTree.setRoot(null);

    if (components != null) {
      TreeItem<ComponentBase> rootItem = new TreeItem<>(components);

      for (final ComponentBase child : components.getChildren()) {
        if (child instanceof ComponentContainerBase container) {
          final TreeItem<ComponentBase> parentItem = new TreeItem<>(container);
          for (final ComponentBase subChild : container.getChildren()) {
            parentItem.getChildren().add(new TreeItem<>(subChild));
          }
          rootItem.getChildren().add(parentItem);
        } else {
          rootItem.getChildren().add(new TreeItem<>(child));
        }
      }

      expandTreeNodes(rootItem);
      controller.componentsTree.setRoot(rootItem);
    } else {
      controller.componentsTree.setDisable(true);
    }
  }

  private void init() throws Exception {
    final FXMLLoader loader = new FXMLLoader(FXML_FILE);
    final SplitPane splitter = loader.load();
    controller = loader.getController();
    controller.init();

    if (modInfo != null) {
      setTitle(String.format("%s - %s", TITLE, modInfo.getTp2File().getFileName().toString()));
    } else {
      setTitle(TITLE);
    }

    // populating language list
    if (modInfo != null) {
      for (int i = 0; i < modInfo.getLanguageCount(); i++) {
        controller.languageChoiceBox.getItems().add(modInfo.getLanguage(i));
      }
    }
    controller.languageChoiceBox.setOnAction(
        event -> onLanguageItemSelected(controller.languageChoiceBox.getSelectionModel().getSelectedIndex()));

    if (modInfo != null && modInfo.getModIni() != null) {
      // initializing mod information tree
      final ModIni ini = modInfo.getModIni();
      final TreeItem<Node> root = new TreeItem<>(new Label("Mod information"));
      if (!ini.getName().isEmpty()) {
        final TreeItem<Node> label = new TreeItem<>(new Label("Name"));
        label.getChildren().add(new TreeItem<>(new Label(ini.getName())));
        root.getChildren().add(label);
      }

      if (!ini.getAuthorList().isEmpty()) {
        final TreeItem<Node> label = new TreeItem<>(new Label("Author(s)"));
        for (final String author : ini.getAuthorList()) {
          label.getChildren().add(new TreeItem<>(new Label(author)));
        }
        root.getChildren().add(label);
      }

      if (!ini.getDescription().isEmpty()) {
        final TreeItem<Node> labelNode = new TreeItem<>(new Label("Description"));
        labelNode.getChildren().add(new TreeItem<>(new Label(ini.getDescription())));
        root.getChildren().add(labelNode);
      }

      if (!ini.getReadmeList().isEmpty()) {
        final TreeItem<Node> label = new TreeItem<>(new Label("Readme"));
        for (final URL link : ini.getReadmeList()) {
          final Hyperlink hl = new Hyperlink(link.toExternalForm());
          hl.setOnAction(event -> onHyperlinkClick(hl));
          label.getChildren().add(new TreeItem<>(hl));
        }
        root.getChildren().add(label);
      }

      if (!ini.getForumList().isEmpty()) {
        final TreeItem<Node> label = new TreeItem<>(new Label("Forum"));
        for (final URL link : ini.getForumList()) {
          final Hyperlink hl = new Hyperlink(link.toExternalForm());
          hl.setOnAction(event -> onHyperlinkClick(hl));
          label.getChildren().add(new TreeItem<>(hl));
        }
        root.getChildren().add(label);
      }

      if (ini.getHomepage() != null) {
        final TreeItem<Node> label = new TreeItem<>(new Label("Homepage"));
        final Hyperlink hl = new Hyperlink(ini.getHomepage().toExternalForm());
        hl.setOnAction(event -> onHyperlinkClick(hl));
        label.getChildren().add(new TreeItem<>(hl));
        root.getChildren().add(label);
      }

      if (!ini.getDownloadList().isEmpty()) {
        final TreeItem<Node> label = new TreeItem<>(new Label("Download"));
        for (final URL link : ini.getDownloadList()) {
          final Hyperlink hl = new Hyperlink(link.toExternalForm());
          hl.setOnAction(event -> onHyperlinkClick(hl));
          label.getChildren().add(new TreeItem<>(hl));
        }
        root.getChildren().add(label);
      }

      if (!ini.getBeforeList().isEmpty()) {
        final TreeItem<Node> label = new TreeItem<>(new Label("Install before"));
        for (final String name : ini.getBeforeList()) {
          label.getChildren().add(new TreeItem<>(new Label(name)));
        }
        root.getChildren().add(label);
      }

      if (!ini.getAfterList().isEmpty()) {
        final TreeItem<Node> label = new TreeItem<>(new Label("Install after"));
        for (final String name : ini.getAfterList()) {
          label.getChildren().add(new TreeItem<>(new Label(name)));
        }
        root.getChildren().add(label);
      }

      expandTreeNodes(root);
      controller.iniTree.setRoot(root);
    } else {
      // initializing message for non-existing mod information
      final Label label = new Label("*** No information available for this mod ***");
      label.setStyle("-fx-font-weight: bold;");
      final TreeItem<Node> root = new TreeItem<>(label);
      controller.iniTree.setRoot(root);

      controller.iniTree.setShowRoot(true);
      controller.iniTree.setFocusTraversable(false);
      splitter.setDividerPositions(0.85);
    }

    setScene(new CustomScene(splitter));

    // assigning application icon
    getIcons().addAll(Icons.Icon.getImages());

    setOnShowing(this::onShowing);
    setOnShown(this::onShown);
    setOnHiding(this::onHiding);
    setOnHidden(this::onHidden);
  }

  /**
   * Performs post-initializations when the window is about to become visible.
   */
  private void onShowing(WindowEvent event) {
    // one-time initialization
    if (!controller.languageChoiceBox.getItems().isEmpty() &&
        controller.languageChoiceBox.getSelectionModel().getSelectedIndex() < 0) {
      controller.languageChoiceBox.getSelectionModel().select(0);
    }
  }

  /**
   * Performs post-initializations that are only possible when the window is visible.
   */
  private void onShown(WindowEvent event) {
    restoreSettings();
  }

  /**
   * Performs clean up operations when the window is about to be hidden/closed.
   */
  private void onHiding(WindowEvent event) {
    storeSettings();
  }

  /**
   * Performs clean up operations when the window is hidden/closed.
   */
  private void onHidden(WindowEvent event) {
    MainWindow.getInstance().updateDetailsButtonSelected();
  }

  /**
   * Opens the specified link with the associated application.
   */
  private void onHyperlinkClick(Hyperlink node) {
    if (node != null) {
      MainWindow.getInstance().getHostServices().showDocument(node.getText());
    }
  }

  /**
   * Stores the current window size, position and state.
   */
  private void storeSettings() {
    // we need to store window size and position, since hiding the window is equivalent to closing the window
    if (isMaximized()) {
      windowMaximized = true;
    } else {
      windowRect = new Rectangle2D(getX(), getY(), getWidth(), getHeight());
      windowMaximized = false;
    }
  }

  /**
   * Restores or initializes window size, position and state.
   */
  private void restoreSettings() {
    if (windowRect != null) {
      // restoring last known window size and position
      setX(windowRect.getMinX());
      setY(windowRect.getMinY());
      setWidth(windowRect.getWidth());
      setHeight(windowRect.getHeight());
      setMaximized(windowMaximized);
    } else {
      // adjusting initial window size and position
      double width = Math.max(getWidth(), MainWindow.getInstance().getStage().getMinWidth() * .75);
      setWidth(width);

      // try to place window right next to the main window
      final Stage stageMain = MainWindow.getInstance().getStage();
      double x = stageMain.getX() + stageMain.getWidth();
      final ObservableList<Screen> screens = Screen.getScreensForRectangle(stageMain.getX(), stageMain.getY(),
          stageMain.getWidth(), stageMain.getHeight());
      if (!screens.isEmpty()) {
        final Screen screen = screens.get(0);
        double maxX = screen.getBounds().getMaxX();
        if (x + getWidth() > maxX) {
          x = maxX - getWidth();
        }
      }
      setX(x);
    }
    storeSettings();
  }

  /**
   * Expands the specified tree node and all child nodes recursively.
   */
  private static <T> void expandTreeNodes(TreeItem<T> node) {
    if (node != null) {
      node.setExpanded(true);
      for (final TreeItem<T> child : node.getChildren()) {
        expandTreeNodes(child);
      }
    }
  }

  /**
   * Collapses the specified tree node and all child nodes recursively.
   */
  @SuppressWarnings("unused")
  private static <T> void collapseTreeNode(TreeItem<T> node) {
    if (node != null) {
      for (final TreeItem<T> child : node.getChildren()) {
        collapseTreeNode(child);
      }
      node.setExpanded(false);
    }
  }
}
