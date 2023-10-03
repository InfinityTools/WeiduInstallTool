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
import io.infinitytools.wml.mod.info.*;
import io.infinitytools.wml.mod.ini.ModIni;
import io.infinitytools.wml.utils.R;
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
import java.nio.charset.Charset;
import java.util.List;

/**
 * Provides detailed information about mod components and optional mod.ini content.
 */
public class DetailsWindow extends Stage {
  /**
   * Path to the FXML definition file for this window.
   */
  private static final URL FXML_FILE = MainWindow.class.getResource("details.fxml");

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

  private void onLanguageItemSelected(int newLanguageIndex) {
    Logger.debug("Selecting language index {}", newLanguageIndex);

    // repopulating components group list with entries in the new language
    try {
      final ComponentRoot components = modInfo.getComponentInfo(newLanguageIndex);
      final List<ComponentGroup> groupList = components.getGroups();

      final int curGroupIndex = Math.max(0, controller.groupComboBox.getSelectionModel().getSelectedIndex());
      controller.groupComboBox.getItems().clear();
      controller.groupComboBox.getItems().add(ComponentGroup.GROUP_NONE);
      controller.groupComboBox.getItems().addAll(groupList);
      controller.groupComboBox.getSelectionModel().select(curGroupIndex);
    } catch (IndexOutOfBoundsException e) {
      Logger.error(e, "Language item selected at index {}", newLanguageIndex);
    }

    onComponentsTreeChanged(newLanguageIndex, controller.groupComboBox.getSelectionModel().getSelectedItem());
  }

  private void onGroupItemSelected(ComponentGroup newGroup) {
    Logger.debug("Selecting component group {}", newGroup);
    onComponentsTreeChanged(controller.languageComboBox.getSelectionModel().getSelectedIndex(), newGroup);
  }

  /**
   * Called whenever the content of the mod components tree should be reinitialized.
   *
   * @param newLanguageIndex Language index to use
   * @param newGroup         Component Group to use
   */
  private void onComponentsTreeChanged(int newLanguageIndex, ComponentGroup newGroup) {
    Logger.debug("Components Tree changes (languageIndex={}, group={})", newLanguageIndex, newGroup);
    try {
      ComponentRoot components = modInfo.getComponentInfo(newLanguageIndex);

      if (newGroup == null) {
        newGroup = ComponentGroup.GROUP_NONE;
      }

      initComponentsTree(components, newGroup);
    } catch (IndexOutOfBoundsException e) {
      Logger.error(e, "Components Tree changed (languageIndex={}, group={})", newLanguageIndex, newGroup);
    }
  }

  /**
   * Rebuilds the mod components tree based on the given parameters.
   *
   * @param components {@link ComponentRoot} instance of the mod.
   * @param group      {@link ComponentGroup} filter.
   */
  private void initComponentsTree(final ComponentRoot components, final ComponentGroup group) {
    controller.componentsTree.setRoot(null);
    if (components == null) {
      controller.componentsTree.setDisable(true);
      return;
    }

    // simplifies filter checks
    final boolean isAnyGroup = (group == ComponentGroup.GROUP_NONE);

    final TreeItem<ComponentBase> rootItem = new TreeItem<>(components);
    for (final ComponentBase child : components.getChildren()) {
      if (child instanceof ComponentContainerBase container) {
        // checking whether subcomponent is included in the given group
        final boolean groupMatches = isAnyGroup ||
            container
                .getChildren()
                .stream()
                .anyMatch(cb -> cb instanceof ComponentInfo ci && ci.getGroups().contains(group));

        if (groupMatches) {
          final TreeItem<ComponentBase> parentItem = new TreeItem<>(container);
          for (final ComponentBase subChild : container.getChildren()) {
            parentItem.getChildren().add(new TreeItem<>(subChild));
          }
          rootItem.getChildren().add(parentItem);
        }
      } else if (child instanceof ComponentInfo ci) {
        if (isAnyGroup || ci.getGroups().contains(group)) {
          rootItem.getChildren().add(new TreeItem<>(ci));
        }
      }
    }

    expandTreeNodes(rootItem);
    controller.componentsTree.setRoot(rootItem);
  }

  /**
   * Reloads mod component information with the specified character set.
   *
   * @param charset The new character set to decode mod component information.
   */
  private void refreshModInfoCharset(Charset charset) {
    if (modInfo != null) {
      Logger.debug("New charset: {}", charset);
      modInfo.clearCache();
      modInfo.setCharsetOverride(charset);
      onLanguageItemSelected(controller.languageComboBox.getSelectionModel().getSelectedIndex());
    }
  }

  private void init() throws Exception {
    final FXMLLoader loader = new FXMLLoader(FXML_FILE, R.getBundle());
    final SplitPane splitter = loader.load();
    controller = loader.getController();

    if (modInfo != null) {
      setTitle(String.format("%s - %s", R.get("ui.details.title"), modInfo.getTp2File().getFileName().toString()));
    } else {
      setTitle(R.get("ui.details.title"));
    }

    if (modInfo != null) {
      // populating language list
      for (int i = 0; i < modInfo.getLanguageCount(); i++) {
        controller.languageComboBox.getItems().add(modInfo.getLanguage(i));
      }
    }
    controller.languageComboBox.setOnAction(
        event -> onLanguageItemSelected(controller.languageComboBox.getSelectionModel().getSelectedIndex()));
    controller.groupComboBox.setOnAction(
        event -> onGroupItemSelected(controller.groupComboBox.getSelectionModel().getSelectedItem()));
    controller.componentsCharsetMenu.selectedProperty().addListener((ob, ov, nv) -> {
      if (nv.getUserData() instanceof CharsetMenu.CharsetInfo ci) {
        refreshModInfoCharset(ci.charset());
      }
    });

    if (modInfo != null && modInfo.getModIni() != null) {
      Logger.debug("Initializing mod information");
      // initializing mod information tree
      final ModIni ini = modInfo.getModIni();
      final TreeItem<Node> root = new TreeItem<>(new Label(R.get("ui.details.modInfo.root")));
      if (!ini.getName().isEmpty()) {
        final TreeItem<Node> label = new TreeItem<>(new Label(R.get("ui.details.modInfo.name")));
        label.getChildren().add(new TreeItem<>(new Label(ini.getName())));
        root.getChildren().add(label);
      }

      if (!ini.getAuthorList().isEmpty()) {
        final TreeItem<Node> label = new TreeItem<>(new Label(R.get("ui.details.modInfo.authors")));
        for (final String author : ini.getAuthorList()) {
          label.getChildren().add(new TreeItem<>(new Label(author)));
        }
        root.getChildren().add(label);
      }

      if (!ini.getDescription().isEmpty()) {
        final TreeItem<Node> labelNode = new TreeItem<>(new Label(R.get("ui.details.modInfo.description")));
        labelNode.getChildren().add(new TreeItem<>(new Label(ini.getDescription())));
        root.getChildren().add(labelNode);
      }

      if (!ini.getReadmeList().isEmpty()) {
        final TreeItem<Node> label = new TreeItem<>(new Label(R.get("ui.details.modInfo.readme")));
        for (final URL link : ini.getReadmeList()) {
          final Hyperlink hl = new Hyperlink(link.toExternalForm());
          hl.setOnAction(event -> onHyperlinkClick(hl));
          label.getChildren().add(new TreeItem<>(hl));
        }
        root.getChildren().add(label);
      }

      if (!ini.getForumList().isEmpty()) {
        final TreeItem<Node> label = new TreeItem<>(new Label(R.get("ui.details.modInfo.forum")));
        for (final URL link : ini.getForumList()) {
          final Hyperlink hl = new Hyperlink(link.toExternalForm());
          hl.setOnAction(event -> onHyperlinkClick(hl));
          label.getChildren().add(new TreeItem<>(hl));
        }
        root.getChildren().add(label);
      }

      if (ini.getHomepage() != null) {
        final TreeItem<Node> label = new TreeItem<>(new Label(R.get("ui.details.modInfo.homepage")));
        final Hyperlink hl = new Hyperlink(ini.getHomepage().toExternalForm());
        hl.setOnAction(event -> onHyperlinkClick(hl));
        label.getChildren().add(new TreeItem<>(hl));
        root.getChildren().add(label);
      }

      if (!ini.getDownloadList().isEmpty()) {
        final TreeItem<Node> label = new TreeItem<>(new Label(R.get("ui.details.modInfo.download")));
        for (final URL link : ini.getDownloadList()) {
          final Hyperlink hl = new Hyperlink(link.toExternalForm());
          hl.setOnAction(event -> onHyperlinkClick(hl));
          label.getChildren().add(new TreeItem<>(hl));
        }
        root.getChildren().add(label);
      }

      if (!ini.getBeforeList().isEmpty()) {
        final TreeItem<Node> label = new TreeItem<>(new Label(R.get("ui.details.modInfo.installBefore")));
        for (final String name : ini.getBeforeList()) {
          label.getChildren().add(new TreeItem<>(new Label(name)));
        }
        root.getChildren().add(label);
      }

      if (!ini.getAfterList().isEmpty()) {
        final TreeItem<Node> label = new TreeItem<>(new Label(R.get("ui.details.modInfo.installAfter")));
        for (final String name : ini.getAfterList()) {
          label.getChildren().add(new TreeItem<>(new Label(name)));
        }
        root.getChildren().add(label);
      }

      expandTreeNodes(root);
      controller.iniTree.setRoot(root);
    } else {
      Logger.debug("Not mod information available");
      // initializing message for non-existing mod information
      final String msg = String.format("*** %s ***", R.get("ui.details.modInfo.message.unavailable"));
      final Label label = new Label(msg);
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
    if (!controller.languageComboBox.getItems().isEmpty() &&
        controller.languageComboBox.getSelectionModel().getSelectedIndex() < 0) {
      controller.languageComboBox.getSelectionModel().select(0);
      onLanguageItemSelected(controller.languageComboBox.getSelectionModel().getSelectedIndex());
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
      // adjustments should be preferably bound to the screen showing the main window
      final Stage stageMain = MainWindow.getInstance().getStage();
      final double mainStageRight = stageMain.getX() + stageMain.getWidth();
      Screen screen = null;
      final ObservableList<Screen> screens = Screen.getScreensForRectangle(stageMain.getX(), stageMain.getY(),
          stageMain.getWidth(), stageMain.getHeight());
      for (final Screen curScreen : screens) {
        if (mainStageRight >= curScreen.getBounds().getMinX() && mainStageRight < curScreen.getBounds().getMaxX()) {
          screen = curScreen;
          break;
        }
      }
      if (screen == null) {
        screen = Screen.getPrimary();
      }
      Logger.debug("Using screen: {}", screen);

      // adjusting initial window size and position
      final double height = Math.max(getMinHeight(), screen.getBounds().getHeight() * 0.8);
      Logger.debug("New height: {}", height);
      setHeight(height);

      final double width = Math.max(getWidth(), MainWindow.getInstance().getStage().getMinWidth() * .75);
      Logger.debug("New width: {}", width);
      setWidth(width);

      // keep window inside vertical screen boundary
      double y = Math.max(0.0, stageMain.getY() - (getHeight() - stageMain.getHeight()) / 2.0);
      y = Math.min(screen.getBounds().getMaxY() - getHeight(), y);
      Logger.debug("New y position: {}", y);
      setY(y);

      // try to place window right next to the main window
      final double x;
      if (mainStageRight + getWidth() > screen.getBounds().getMaxX()) {
        if (stageMain.getX() - getWidth() >= screen.getBounds().getMinX()) {
          // place window on the left side instead
          x = stageMain.getX() - getWidth();
        } else {
          // stay within screen bounds
          x = screen.getBounds().getMaxX() - getWidth();
        }
      } else {
        // place window on the right side
        x = mainStageRight;
      }
      Logger.debug("New x position: {}", x);
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
