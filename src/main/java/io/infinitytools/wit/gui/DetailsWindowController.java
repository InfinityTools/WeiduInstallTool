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

import io.infinitytools.wit.mod.info.ComponentBase;
import io.infinitytools.wit.mod.info.ComponentGroup;
import io.infinitytools.wit.utils.R;
import javafx.fxml.Initializable;
import javafx.scene.Node;
import javafx.scene.control.*;
import org.tinylog.Logger;

import java.net.URL;
import java.util.ResourceBundle;

/**
 * Controller class for the DetailsWindow UI. It is automatically initialized by the JavaFX FXML loader.
 */
public class DetailsWindowController implements Initializable {
  public ComboBox<String> languageComboBox;
  public ComboBox<ComponentGroup> groupComboBox;
  public TreeView<ComponentBase> componentsTree;
  public Label iniLabel;
  public TreeView<Node> iniTree;

  public CharsetMenu componentsCharsetMenu;

  public DetailsWindowController() {
  }

  @Override
  public void initialize(URL location, ResourceBundle resources) {
    try {
      componentsTree.getSelectionModel().setSelectionMode(SelectionMode.SINGLE);
      iniTree.getSelectionModel().setSelectionMode(SelectionMode.SINGLE);
    } catch (NullPointerException e) {
      Logger.error(e, "Components are null (componentsTree={}, iniTree={})", componentsTree, iniTree);
    }

    // add context menu with character encoding options
    // binding is initialized in the DetailsWindow class
    componentsCharsetMenu = new CharsetMenu();

    // Adapting default menu entry
    final RadioMenuItem rmi = componentsCharsetMenu.findMenuItem(CharsetMenu.CharsetEntry.DEFAULT);
    if (rmi != null) {
      rmi.setText(R.get("ui.charsets.default.autodetect"));
    }

    final ContextMenu cm = new ContextMenu();
    cm.getItems().addAll(componentsCharsetMenu.getItems());
    componentsTree.setContextMenu(cm);
  }
}
