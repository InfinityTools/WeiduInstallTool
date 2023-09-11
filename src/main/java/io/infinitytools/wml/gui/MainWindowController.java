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

import javafx.scene.control.*;

public class MainWindowController {
  public TextArea outputArea;
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

  public MainWindowController() {
  }

}
