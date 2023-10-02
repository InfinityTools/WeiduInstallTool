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

import io.infinitytools.wml.utils.CustomLogWriter;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import org.tinylog.Level;

import java.net.URL;
import java.util.ResourceBundle;
import java.util.Set;

/**
 * Controller class for the Log window. It is automatically initialized by the JavaFX FXML loader.
 */
public class LogWindowController implements Initializable {
  public CheckBox errorLevelCheckBox;
  public CheckBox warningLevelCheckBox;
  public CheckBox infoLevelCheckBox;
  public CheckBox debugLevelCheckBox;
  public CheckBox traceLevelCheckBox;
  public Button saveButton;
  public TextArea logArea;
  public ContextMenu logContextMenu;
  public MenuItem logCopyMenuItem;
  public MenuItem logSelectAllMenuItem;
  public CheckMenuItem logWrapCheckItem;

  public LogWindowController() {
  }

  @Override
  public void initialize(URL location, ResourceBundle resources) {
    // initializing default text area actions
    logCopyMenuItem.setOnAction(event -> logArea.copy());
    logSelectAllMenuItem.setOnAction(event -> logArea.selectAll());
    logWrapCheckItem.setOnAction(event -> logArea.setWrapText(logWrapCheckItem.isSelected()));
    logContextMenu.setOnShowing(event -> logCopyMenuItem.setDisable(logArea.getSelection().getLength() == 0));

    if (CustomLogWriter.getInstance() != null) {
      final Set<Level> levels = CustomLogWriter.getInstance().getLevels();
      traceLevelCheckBox.setDisable(!levels.contains(Level.TRACE));
      debugLevelCheckBox.setDisable(!levels.contains(Level.DEBUG));
      infoLevelCheckBox.setDisable(!levels.contains(Level.INFO));
      warningLevelCheckBox.setDisable(!levels.contains(Level.WARN));
      errorLevelCheckBox.setDisable(!levels.contains(Level.ERROR));
    }

    logArea.textProperty().addListener((ob, ov, nv) -> saveButton.setDisable(logArea.getLength() == 0));
  }
}
