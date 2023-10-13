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
import io.infinitytools.wml.utils.SystemInfo;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import org.tinylog.Logger;

import java.lang.reflect.Field;
import java.net.URL;
import java.util.HashSet;
import java.util.Locale;
import java.util.ResourceBundle;

public class MainWindowController implements Initializable {
  public TextArea outputArea;
  public ContextMenu outputContextMenu;
  public MenuItem outputCopyMenuItem;
  public MenuItem outputSelectAllMenuItem;
  public MenuItem outputSearchMenuItem;
  public MenuItem outputScrollTopMenuItem;
  public MenuItem outputScrollBottomMenuItem;
  public TextField inputField;
  public Button sendButton;
  public Button aboutButton;
  public Button inputDigit0Button;
  public Button inputDigit1Button;
  public Button inputDigit2Button;
  public Button inputDigit3Button;
  public Button inputDigit4Button;
  public Button inputDigit5Button;
  public Button inputDigit6Button;
  public Button inputDigit7Button;
  public Button inputDigit8Button;
  public Button inputDigit9Button;
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
  public CheckMenuItem singleInstanceCheckItem;
  public CheckMenuItem trayIconFeedbackCheckItem;
  public CheckMenuItem showLogCheckItem;
  public CustomMenuItem bufferSizeMenuItem;
  public CustomMenuItem outputFontSizeMenuItem;
  public Spinner<Double> outputFontSizeSpinner;
  public SpinnerValueFactory.DoubleSpinnerValueFactory outputFontSizeValueFactory;
  public Slider bufferSizeSlider;
  public Label bufferSizeValueLabel;

  /**
   * Set of all quick input button instances for automated access.
   * Initialized in the {@link #initInputButtons()} method.
   */
  public final HashSet<Button> inputButtons = new HashSet<>();

  public CharsetMenu outputCharsetMenu;

  public MainWindowController() {
  }

  @Override
  public void initialize(URL location, ResourceBundle resources) {
    initContextMenu();
    initInputButtons();
    initOptions();
  }

  /**
   * Configures availability of application options.
   */
  private void initOptions() {
    // single instance mode is already the default behavior on macOS
    singleInstanceCheckItem.setDisable(SystemInfo.IS_MACOS);
    singleInstanceCheckItem.setVisible(!SystemInfo.IS_MACOS);

    // system tray is only available on selected platforms
    trayIconFeedbackCheckItem.setDisable(!MainWindow.Tray.IS_SUPPORTED);
    trayIconFeedbackCheckItem.setVisible(MainWindow.Tray.IS_SUPPORTED);
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

  /**
   * Configures quick input buttons.
   */
  private void initInputButtons() {
    // assigning user data to quick input letters
    final String[] keys = {"Quit", "Ask", "Skip", "Reinstall", "Uninstall", "Install", "No", "Yes"};
    for (final String key : keys) {
      try {
        final Field field = MainWindowController.class.getField("input" + key + "Button");
        if (field.get(this) instanceof Button b) {
          inputButtons.add(b);
          // Shortcut key: first letter of the command string
          final String letter = Character.toString(key.charAt(0)).toLowerCase(Locale.ROOT);
          b.setUserData(letter);
        }
      } catch (IllegalAccessException | NoSuchFieldException e) {
        Logger.debug(e, "Setting user data for input letter buttons");
      }
    }

    // assigning tooltips and user data to quick input digits
    final String fmt = R.get("ui.main.inputDigit.button.tooltip");
    for (int i = 0; i < 10; i++) {
      try {
        final Field field = MainWindowController.class.getField("inputDigit" + i + "Button");
        if (field.get(this) instanceof Button b) {
          inputButtons.add(b);
          b.setTooltip(new Tooltip(String.format(fmt, i)));
          // Shortcut key: digit
          b.setUserData(Integer.toString(i));
        }
      } catch (IllegalAccessException | NoSuchFieldException e) {
        Logger.debug(e, "Setting tooltip and user data for input digit buttons");
      }
    }
  }
}
