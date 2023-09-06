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

import io.infinitytools.wml.mod.info.ComponentBase;
import javafx.scene.Node;
import javafx.scene.control.ChoiceBox;
import javafx.scene.control.Label;
import javafx.scene.control.SelectionMode;
import javafx.scene.control.TreeView;
import org.tinylog.Logger;

/**
 * Controller class for the DetailsWindow UI. It is automatically initialized by the JavaFX FXML loader.
 */
public class DetailsWindowController {
  public ChoiceBox<String> languageChoiceBox;
  public TreeView<ComponentBase> componentsTree;
  public Label iniLabel;
  public TreeView<Node> iniTree;

  public DetailsWindowController() {
  }

  /** Initializes the UI components. This method should be called after the UI has been loaded. */
  public void init() {
    try {
      componentsTree.getSelectionModel().setSelectionMode(SelectionMode.SINGLE);
      iniTree.getSelectionModel().setSelectionMode(SelectionMode.SINGLE);
    } catch (NullPointerException e) {
      Logger.error(String.format("Components are null (componentsTree=%s, iniTree=%s)", componentsTree, iniTree), e);
    }
  }
}
