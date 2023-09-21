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

import io.infinitytools.wml.utils.R;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import org.tinylog.Logger;

import java.net.URL;
import java.util.ResourceBundle;

public class MainWindowController implements Initializable {
  public TextArea outputArea;
  public ContextMenu outputContextMenu;
  public MenuItem outputCopyMenuItem;
  public MenuItem outputSelectAllMenuItem;
  public MenuItem outputScrollTopMenuItem;
  public MenuItem outputScrollBottomMenuItem;
  public TextField inputField;
  public Button sendButton;
  public Button aboutButton;
  public Button inputQuitButton;
  public Button inputAskButton;
  public Button inputSkipButton;
  public Button inputReinstallButton;
  public Button inputUninstallButton;
  public Button inputInstallButton;
  public Button inputNoButton;
  public Button inputYesButton;
  public Button quitButton;
  public ToggleButton detailsButton;
  public MenuButton optionsButton;
  public CheckMenuItem autoQuitCheckItem;
  public CheckMenuItem visualizeResultCheckItem;
  public CheckMenuItem warnModOrderCheckItem;
  public CheckMenuItem darkModeUiCheckItem;
  public CustomMenuItem bufferSizeMenuItem;
  public CustomMenuItem outputFontSizeMenuItem;
  public Spinner<Double> outputFontSizeSpinner;
  public SpinnerValueFactory.DoubleSpinnerValueFactory outputFontSizeValueFactory;
  public Slider bufferSizeSlider;
  public Label bufferSizeValueLabel;

  public CharsetMenu outputCharsetMenu;

  public MainWindowController() {
  }

  @Override
  public void initialize(URL location, ResourceBundle resources) {
    initContextMenu();
  }

  /**
   * Initializes the context menu of the output text area.
   */
  private void initContextMenu() {
    // initializing default text area actions
    outputCopyMenuItem.setOnAction(event -> outputArea.copy());
    outputSelectAllMenuItem.setOnAction(event -> outputArea.selectAll());
    outputScrollTopMenuItem.setOnAction(event -> outputArea.setScrollTop(0.0));
    outputScrollBottomMenuItem.setOnAction(event -> outputArea.setScrollTop(Double.MAX_VALUE));
    outputContextMenu.setOnShowing(event -> outputCopyMenuItem.setDisable(outputArea.getSelection().getLength() == 0));

    // extending context menu by character encoding options
    // binding is initialized in the MainWindow class
    outputCharsetMenu = new CharsetMenu(R.get("ui.charsets.title"));

    // Adapting default menu entry
    final RadioMenuItem rmi = outputCharsetMenu.findMenuItem(CharsetMenu.CharsetEntry.DEFAULT);
    if (rmi != null) {
      rmi.setText(R.get("ui.charsets.default.utf8"));
    }

    outputArea.getContextMenu().getItems().add(new SeparatorMenuItem());
    outputArea.getContextMenu().getItems().addAll(outputCharsetMenu.getItems());
  }
}
