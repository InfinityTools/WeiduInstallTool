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

import io.infinitytools.wml.icons.Icons;
import io.infinitytools.wml.utils.CustomLogWriter;
import io.infinitytools.wml.utils.LogEvent;
import io.infinitytools.wml.utils.R;
import io.infinitytools.wml.utils.Utils;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.VBox;
import javafx.stage.FileChooser;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.WindowEvent;
import org.tinylog.Level;
import org.tinylog.Logger;
import org.tinylog.core.LogEntry;

import java.io.IOException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.HashSet;
import java.util.Set;

public class LogWindow extends Stage {
  private static final URL FXML_FILE = LogWindow.class.getResource("log.fxml");

  private static final String LOG_FILENAME = "wml-log.txt";

  private static LogWindow instance;

  private LogWindowController controller;

  /**
   * Provides access to the {@link LogWindow} instance. Window is automatically created if it doesn't exist.
   *
   * @return the {@link LogWindow} instance.
   * @throws Exception if the window could not be created.
   */
  public static LogWindow getInstance() throws Exception {
    if (instance == null) {
      instance = new LogWindow();
    }
    return instance;
  }

  /**
   * Displays the {@link LogWindow} on the screen. Window is automatically created if it doesn't exist.
   *
   * @throws Exception if the window could not be created.
   */
  public static void open() throws Exception {
    if (!getInstance().isShowing()) {
      getInstance().show();
    }
    if (getInstance().isIconified()) {
      getInstance().setIconified(false);
    }
    getInstance().requestFocus();
  }

  private LogWindow() throws Exception {
    super();
    instance = this;
    initModality(Modality.NONE);
    init();
  }

  /**
   * Returns the current content of the log text area.
   */
  public String getLogOutput() {
    return controller.logArea.getText();
  }

  private void onShown(WindowEvent event) {
    updateLog();
  }

  private void onHidden(WindowEvent event) {
    MainWindow.getInstance().getController().showLogCheckItem.setSelected(false);
  }

  private void onGlobalKeyPressed(KeyEvent event) {
    boolean isModifierDown = event.isAltDown() || event.isControlDown() || event.isMetaDown() || event.isShiftDown();
    if (event.getCode() == KeyCode.ESCAPE && !isModifierDown) {
      close();
    }
  }

  /**
   * Handles events triggered by the {@link CustomLogWriter} instance.
   */
  private void onLogEvent(LogEvent event) {
    switch (event.type()) {
      case ADD -> onAddLog(event.writer(), event.logEntry());
      case CLEAR -> onClearLog();
    }
  }

  private void onAddLog(CustomLogWriter writer, LogEntry logEntry) {
    String s = writer.render(logEntry);
    appendText(s);
  }

  private void onClearLog() {
    updateLog();
  }

  private void init() throws Exception {
    final FXMLLoader loader = new FXMLLoader(FXML_FILE, R.getBundle());
    final VBox vbox = loader.load();
    controller = loader.getController();

    // initializing filter checkboxes and save button
    controller.saveButton.setOnAction(event -> saveLogAs());
    controller.errorLevelCheckBox.setOnAction(event -> updateLog());
    controller.warningLevelCheckBox.setOnAction(event -> updateLog());
    controller.infoLevelCheckBox.setOnAction(event -> updateLog());
    controller.debugLevelCheckBox.setOnAction(event -> updateLog());
    controller.traceLevelCheckBox.setOnAction(event -> updateLog());

    // initializing listener to CustomLogWriter instance
    CustomLogWriter.getInstance().addEventHandler(this::onLogEvent);

    final Scene scene = new CustomScene(vbox);
    setScene(scene);
    setTitle(R.get("ui.log.title"));
    setResizable(true);
    getIcons().addAll(Icons.Icon.getImages());

    getScene().addEventHandler(KeyEvent.KEY_PRESSED, this::onGlobalKeyPressed);
    setOnShown(this::onShown);
    setOnHidden(this::onHidden);
  }

  private void appendText(String text) {
    if (text != null && !text.isEmpty()) {
      controller.logArea.appendText(text);
      controller.logArea.positionCaret(controller.logArea.getLength());
    }
  }

  /**
   * Recreates the log output based on the current filter settings.
   */
  private void updateLog() {
    controller.logArea.clear();

    final Set<Level> set = getSelectedLevels();
    if (!set.isEmpty()) {
      final CustomLogWriter writer = CustomLogWriter.getInstance();
      if (writer != null) {
        writer.getFiltered(set).forEach(e -> appendText(writer.render(e)));
      }
    }
  }

  /**
   * Saves the current log output to a user-defined text file.
   */
  private void saveLogAs() {
    Path initialFolder = null;
    if (MainWindow.getInstance().getModInfo() != null) {
      initialFolder = MainWindow.getInstance().getModInfo().getGamePath();
    }
    if (initialFolder == null) {
      initialFolder = Path.of(".");
    }

    final Path saveFile = Utils.chooseSaveFile(this, R.get("ui.log.fileDialog.txt.title"),
        initialFolder.resolve(LOG_FILENAME),
        new FileChooser.ExtensionFilter(R.get("ui.log.fileDialog.txt.filter.txt"), "*.txt"),
        new FileChooser.ExtensionFilter(R.get("ui.fileDialog.filter.allFiles"), "*.*"));

    if (saveFile != null) {
      try {
        Files.writeString(saveFile, getLogOutput(), StandardOpenOption.CREATE);
        Utils.showMessageDialog(this, R.INFORMATION(), R.get("ui.log.save.message.success.header"), null);
      } catch (IOException e) {
        Logger.warn(e, "Could not save log file");
        Utils.showErrorDialog(this, R.ERROR(), R.get("ui.log.save.message.error.header"), null);
      }
    }
  }

  /**
   * Returns a set of all selected log levels.
   */
  private Set<Level> getSelectedLevels() {
    final Set<Level> retVal = new HashSet<>();

    if (controller.errorLevelCheckBox.isSelected()) {
      retVal.add(Level.ERROR);
    }
    if (controller.warningLevelCheckBox.isSelected()) {
      retVal.add(Level.WARN);
    }
    if (controller.infoLevelCheckBox.isSelected()) {
      retVal.add(Level.INFO);
    }
    if (controller.debugLevelCheckBox.isSelected()) {
      retVal.add(Level.DEBUG);
    }
    if (controller.traceLevelCheckBox.isSelected()) {
      retVal.add(Level.TRACE);
    }

    return retVal;
  }
}
