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

import io.infinitytools.wml.Configuration;
import io.infinitytools.wml.Globals;
import io.infinitytools.wml.icons.Icons;
import io.infinitytools.wml.mod.ModInfo;
import io.infinitytools.wml.mod.ini.ModIni;
import io.infinitytools.wml.mod.log.WeiduLog;
import io.infinitytools.wml.process.SysProc;
import io.infinitytools.wml.process.SysProcChangeEvent;
import io.infinitytools.wml.process.SysProcOutputEvent;
import io.infinitytools.wml.utils.R;
import io.infinitytools.wml.utils.SystemInfo;
import io.infinitytools.wml.utils.Utils;
import io.infinitytools.wml.weidu.Weidu;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.event.Event;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonBar;
import javafx.scene.control.ButtonType;
import javafx.scene.image.ImageView;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.stage.FileChooser;
import javafx.stage.FileChooser.ExtensionFilter;
import javafx.stage.Stage;
import javafx.stage.Window;
import javafx.stage.WindowEvent;
import org.tinylog.Logger;

import java.io.IOException;
import java.net.URL;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.CancellationException;
import java.util.concurrent.Future;
import java.util.stream.Collectors;

/**
 * Implementation of the main window of the application.
 */
public class MainWindow extends Application {
  /**
   * Colors for use with the visualization of process results.
   */
  private enum StateColors {
    SUCCESS(Color.GREEN, Color.LIGHTGREEN),
    WARNING(Color.ORANGE, Color.ORANGE),
    ERROR(Color.RED, Color.rgb(0xff, 0x3f, 0x3f)),
    DISABLED(Color.TRANSPARENT, Color.TRANSPARENT),
    ;

    private final Color lightColor;
    private final Color darkColor;

    StateColors(Color light, Color dark) {
      this.lightColor = light;
      this.darkColor = dark;
    }

    /**
     * Returns the {@link Color} for this value based on the current UI mode.
     */
    public Color getColor() {
      boolean isDark = Configuration.Key.DARK_UI_MODE.getDefaultValue();
      try {
        isDark = Configuration.getInstance().getOption(Configuration.Key.DARK_UI_MODE);
      } catch (Throwable ignored) {
      }
      return getColor(isDark);
    }

    /**
     * Returns the {@link Color} for the specified UI mode.
     */
    public Color getColor(boolean darkMode) {
      return (darkMode) ? darkColor : lightColor;
    }
  }

  /**
   * Path to the FXML definition file for this window.
   */
  private final static URL FXML_FILE = Objects.requireNonNull(MainWindow.class.getResource("main.fxml"));

  private static MainWindow instance;

  /**
   * Returns the current {@link MainWindow} instance.
   */
  public static MainWindow getInstance() {
    if (instance == null) {
      throw new NullPointerException("MainWindow is null");
    }
    return instance;
  }

  private MainWindowController controller;
  private Stage stage;
  private ModInfo modInfo;
  private DetailsWindow detailsWindow;
  private SysProc process;
  private Future<Integer> processResult;

  public MainWindow() {
    super();
    if (instance == null) {
      instance = this;
    } else {
      Logger.error("Duplicate instance of main window.");
    }
  }

  /**
   * Performs WeiDU binary download with UI feedback.
   *
   * @param overwrite Whether an existing binary should be overwritten.
   * @return Success state of the operation.
   * @throws IOException                  if the progress dialog could not be initialized.
   * @throws ProgressDialog.TaskException if the task was prematurely terminated by an error.
   */
  private boolean downloadWeidu(boolean overwrite) throws IOException, ProgressDialog.TaskException {
    return ProgressDialog.performTask(null, R.get("ui.checkWeidu.downloadProgress.title"),
        R.get("ui.checkWeidu.downloadProgress.message"),
        dlg -> {
          final String weiduFileName = Weidu.WEIDU_NAME + SystemInfo.EXE_SUFFIX;
          final Path targetDir = Globals.APP_DATA_PATH
              .resolve(Weidu.WEIDU_NAME)
              .resolve(SystemInfo.getPlatform().toString())
              .resolve(SystemInfo.getArchitecture().toString());
          Weidu.installWeidu(weiduFileName, targetDir, overwrite, (note, progress) -> {
            dlg.setNote(note);
            dlg.setProgress(progress);
            return !dlg.isCancelled();
          });

          if (overwrite) {
            // prefer new WeiDU executable
            Configuration.getInstance().setOption(Configuration.Key.WEIDU_PATH, null);
          }
        });
  }

  /**
   * Performs a WeiDU binary check and may attempt to download the binary or request it from the user if necessary.
   *
   * @throws Exception if the WeiDU binary could not be found on the system.
   */
  private Weidu checkWeidu() throws Exception {
    final ButtonType downloadButton = new ButtonType(R.get("ui.checkWeidu.download.button"), ButtonBar.ButtonData.YES);
    final ButtonType selectButton = new ButtonType(R.get("ui.checkWeidu.choose.button"), ButtonBar.ButtonData.NO);
    final ButtonType cancelButton = new ButtonType(R.get("ui.checkWeidu.cancel.button"), ButtonBar.ButtonData.CANCEL_CLOSE);
    final ButtonType keepButton = new ButtonType(R.get("ui.checkWeidu.keep.button"), ButtonBar.ButtonData.CANCEL_CLOSE);

    boolean updateBinary = false;
    String questionHeader = R.get("ui.checkWeidu.message.notFound.header");
    String questionContent = R.get("ui.checkWeidu.message.notFound.content");
    final ButtonType[] questionButtons = {downloadButton, selectButton, cancelButton};

    // 1. check local system
    final Exception exception; // may be thrown later
    try {
      final Weidu.Version versionFound = Weidu.getInstance().getVersion();
      if (versionFound != null) {
        final Weidu.Version versionRecommended = Weidu.Version.of(Weidu.getProperty(Weidu.PROP_WEIDU_VERSION));
        Logger.debug("WeiDU version (found: {}, recommended: {}, cmp value: {})",
            versionFound, versionRecommended, versionFound.compareTo(versionRecommended));

        if (versionFound.compareTo(versionRecommended) < 0) {
          // notify about outdated WeiDU version
          updateBinary = true;
          questionHeader = String.format(R.get("ui.checkWeidu.message.foundOutdated.header"),
              versionFound.major(), versionRecommended.major());
          questionContent = R.get("ui.checkWeidu.message.foundOutdated.content");
          questionButtons[questionButtons.length - 1] = keepButton;
          throw new UnsupportedOperationException("Outdated WeiDU version found: " + versionFound);
        }
      } else {
        Logger.debug("Not a valid WeiDU binary: {}", Weidu.getInstance().getWeidu());
      }
      return Weidu.getInstance();
    } catch (UnsupportedOperationException e) {
      Logger.debug(e, "Local WeiDU binary instance not found");
      exception = e;
    }

    // ask user before downloading binary
    final ButtonType result = Utils.showCustomDialog(null, Alert.AlertType.CONFIRMATION,
        R.QUESTION(), questionHeader, questionContent, questionButtons);

    if (result == cancelButton) {
      // Install: cancel install operation
      throw exception;
    }

    if (result == keepButton) {
      // Update: cancel update operation
      return Weidu.getInstance();
    }

    // Discard previously cached WeiDU executable
    Weidu.reset();

    if (result == downloadButton) {
      // Install/Update: download from WeiDU release page
      try {
        boolean retVal = downloadWeidu(updateBinary);
        Logger.debug("Download WeiDU result: {}", retVal);
      } catch (ProgressDialog.TaskException e) {
        Logger.error(e, "Download WeiDU terminated prematurely");
      } catch (Exception e) {
        Logger.warn(e, "Download WeiDU internal error: no further actions required");
      }

      try {
        return Weidu.getInstance();
      } catch (UnsupportedOperationException e) {
        Logger.debug(e, "After download: Local Weidu binary instance not found");
      }

      final ButtonType confirmType = Utils.showConfirmationDialog(null,
          R.get("ui.checkWeidu.message.choose.title"),
          R.get("ui.checkWeidu.message.choose.header"),
          R.get("ui.checkWeidu.message.choose.content"));
      if (confirmType != ButtonType.OK) {
        throw new Exception("WeiDU executable selection cancelled by the user.");
      }
    }

    // Install/Update: allow user to choose WeiDU binary path
    final Path binPath = Utils.chooseOpenFile(null, R.get("ui.checkWeidu.fileDialog.choose.title"),
        SystemInfo.getUserPath(),
        new FileChooser.ExtensionFilter(R.get("ui.checkWeidu.fileDialog.choose.filter.weidu"),
            Weidu.WEIDU_NAME + SystemInfo.EXE_SUFFIX),
        new FileChooser.ExtensionFilter(R.get("ui.checkWeidu.fileDialog.choose.filter.executable"),
            "*" + SystemInfo.EXE_SUFFIX));
    if (binPath != null) {
      Configuration.getInstance().setOption(Configuration.Key.WEIDU_PATH, binPath.toString());
      return Weidu.getInstance();
    } else {
      try {
        return Weidu.getInstance();
      } catch (UnsupportedOperationException e) {
        throw new Exception("WeiDU executable selection cancelled by the user.");
      }
    }
  }

  @Override
  public void start(Stage stage) throws Exception {
    // Loading configuration; must be done before the UI is initialized
    Configuration.getInstance().loadDefaults();
    try {
      Configuration.getInstance().load();
    } catch (NoSuchFileException e) {
      // not an error
      Logger.debug(e, "Loading configuration: configuration not found (expected)");
    } catch (Exception e) {
      Logger.error(e, "Loading configuration");
    }

    // Applying application language override (if available)
    final String languageOverride = Configuration.getInstance().getOption(Configuration.Key.UI_LANGUAGE_OVERRIDE);
    if (languageOverride != null) {
      final String[] items = languageOverride.split("[_-]");
      R.setLocale(items);
    }

    // Platform check
    if (!SystemInfo.isSystemSupported()) {
      Utils.showErrorDialog(null, R.ERROR(), R.get("ui.main.message.platformCheck.header"),
          String.format(R.get("ui.main.message.platformCheck.content"), SystemInfo.getPlatform(),
              SystemInfo.getArchitecture()));
      return;
    }

    // WeiDU binary check
    try {
      if (checkWeidu() == null) {
        throw new Exception("Weidu is null");
      }
    } catch (Exception e) {
      Logger.info(e, "WeiDU existence check failed");
      Utils.showErrorDialog(null, R.ERROR(), R.get("ui.main.message.weiduCheck.header"),
          R.get("ui.main.message.weiduCheck.content"));
      return;
    }

    // loading launcher arguments (must be performed after WeiDU binary check)
    Configuration.getInstance().loadArguments(getParameters().getRaw());

    // Setup mode: ModInfo initialization
    try {
      this.modInfo = loadModInfo(Configuration.getInstance().getMode(), true);
    } catch (Exception e) {
      Logger.error(e, "Loading mod information in setup mode");
      Configuration.getInstance().save();
      return;
    }

    // loading UI layout
    final FXMLLoader loader = new FXMLLoader(FXML_FILE, R.getBundle());
    final Scene scene = new CustomScene(loader.load());
    this.controller = loader.getController();
    this.stage = stage;
    setupUI(scene);
  }

  /**
   * Returns the {@link ModInfo} instance for the active mod if available, {@code null} otherwise.
   */
  public ModInfo getModInfo() {
    return modInfo;
  }

  /**
   * Returns the main {@link Stage} instance of this application.
   */
  public Stage getStage() {
    return stage;
  }

  /**
   * Provides access to the UI components of this window.
   */
  public MainWindowController getController() {
    return controller;
  }

  /**
   * Returns whether the "Quit on Enter" option is selected.
   */
  public boolean isAutoQuitEnabled() {
    return getController().autoQuitCheckItem.isSelected();
  }

  /**
   * Sets the selection state of the "Quit on Enter" option.
   */
  public void setAutoQuitEnabled(boolean newValue) {
    getController().autoQuitCheckItem.setSelected(newValue);
  }

  /**
   * Returns whether the "Visualize Result of WeiDU Operation" option is selected.
   */
  public boolean isVisualizeResultsEnabled() {
    return getController().visualizeResultCheckItem.isSelected();
  }

  /**
   * Sets the selection state of the "Visualize Result of WeiDU Operation" option.
   */
  public void setVisualizeResultsEnabled(boolean newValue) {
    getController().visualizeResultCheckItem.setSelected(newValue);
  }

  /**
   * Returns whether the "Warn about Mod Order Conflicts" option is selected.
   */
  public boolean isWarnModOrderEnabled() {
    return getController().warnModOrderCheckItem.isSelected();
  }

  /**
   * Sets the selection state of the "Warn about Mod Order Conflicts" option.
   */
  public void setWarnModOrderEnabled(boolean newValue) {
    getController().warnModOrderCheckItem.setSelected(newValue);
  }

  /**
   * Returns whether Dark Mode UI is enabled.
   */
  public boolean isDarkModeEnabled() {
    return getController().darkModeUiCheckItem.isSelected();
  }

  /**
   * Enables or disables Dark Mode UI.
   */
  public void setDarkModeEnabled(boolean newValue) {
    getController().darkModeUiCheckItem.setSelected(newValue);
  }

  /**
   * Returns whether the Details window is visible.
   */
  public boolean isDetailsWindowVisible() {
    return getController().detailsButton.isSelected();
  }

  /**
   * Turns on the Details window if it is available.
   */
  public void setDetailsWindowVisible(boolean newValue) {
    if (!getController().detailsButton.isDisabled()) {
      getController().detailsButton.setSelected(newValue);
    }
  }

  /**
   * Returns the current output buffer size.
   */
  public int getOutputBufferSize() {
    return getOutputBufferSize(getController().bufferSizeSlider.getValue());
  }

  /**
   * Sets the new output buffer size to the specified value.
   */
  public void setOutputBufferSize(int newSize) {
    getController().bufferSizeSlider.setValue(newSize);
  }

  /**
   * Returns the output buffer size based on the specified value.
   * <p>
   * Buffer size is limited to the slider range. Minimum value indicates using the default value. Maximum value
   * indicates no buffer limit.
   * </p>
   *
   * @return Output buffer size, in characters, rounded to the unit size of the slider.
   */
  public int getOutputBufferSize(double value) {
    final double major = getController().bufferSizeSlider.getMajorTickUnit();
    final int minor = Math.max(1, getController().bufferSizeSlider.getMinorTickCount());
    final double unit = Math.max(1.0, major / minor);
    int roundedValue = (int) (Math.round(Math.max(0.0, value) / unit) * unit);
    if (roundedValue == (int) getController().bufferSizeSlider.getMin()) {
      roundedValue = Configuration.Key.BUFFER_LIMIT.<Integer>getDefaultValue();
    } else if (roundedValue == (int) getController().bufferSizeSlider.getMax()) {
      roundedValue = Integer.MAX_VALUE;
    }
    return roundedValue;
  }

  /**
   * Returns the font size of the output text area, as specified in the output font size option.
   *
   * @return Font size, in pt.
   */
  public double getOutputAreaFontSize() {
    return getController().outputArea.getFont().getSize();
  }

  /**
   * Returns the content of the output text area.
   */
  public String getOutputText() {
    return getController().outputArea.getText();
  }

  /**
   * Adds the specified text string to the output text area and scrolls down to the bottom-most line of the content.
   *
   * @param text           string to add.
   * @param autoScrollDown Whether to scroll the text area down to the last line.
   */
  public void appendOutputText(String text, boolean autoScrollDown) {
    if (text != null) {
      setOutputText(getOutputText() + text, autoScrollDown);
    }
  }

  /**
   * Sets the specified text to the output text area and scrolls down to the bottom-most line of the content.
   *
   * @param text           string to set.
   * @param autoScrollDown Whether to scroll the text area down to the last line.
   */
  public void setOutputText(String text, boolean autoScrollDown) {
    int caretPos = getController().outputArea.getCaretPosition();

    clearOutputText();
    if (text != null) {
      getController().outputArea.appendText(ensureOutputTextLimit(text));

      if (!autoScrollDown) {
        getController().outputArea.positionCaret(caretPos);
      }
    }
  }

  /**
   * Removes all text content from the output text area.
   */
  public void clearOutputText() {
    getController().outputArea.clear();
  }

  /**
   * This method should be called whenever the visibility state of the Details window changes.
   */
  public void updateDetailsButtonSelected() {
    setDetailsWindowVisible(detailsWindow.isShowing());
  }

  /**
   * Called by listener of the Details toggle button when the selected state changes.
   */
  private void onDetailsButtonSelected(boolean newValue) {
    if (!getController().detailsButton.isDisabled()) {
      if (newValue) {
        detailsWindow.show();
        // Delay required to work around platform-specific window behavior
        Platform.runLater(() -> getStage().requestFocus());
      } else {
        detailsWindow.hide();
      }
      updateDetailsButtonSelected();
    } else {
      setDetailsWindowVisible(newValue);
    }
  }

  /**
   * Called by process event handler when a process starts or terminates.
   */
  private void onProcessStateChanged(SysProcChangeEvent event) {
    Platform.runLater(() -> {
      switch (event.getType()) {
        case Started -> setWeiduRunning();
        case Terminated -> {
          setWeiduTerminated();
          final Integer exitCode = getProcessResult();
          if (exitCode != null) {
            setVisualizedResult(isVisualizeResultsEnabled(), exitCode);
          }
        }
        default -> {
        }
      }
    });
  }

  /**
   * Called by process event handler whenever output data is available.
   */
  private void onProcessOutput(SysProcOutputEvent event) {
    Platform.runLater(() -> appendOutputText(event.getText(), true));
  }

  /**
   * Performs post-initializations when the window is about to become visible.
   */
  private void onShowing(WindowEvent event) {
  }

  /**
   * Performs post-initializations that are only possible when the window is visible.
   */
  private void onShown(WindowEvent event) {
  }

  /**
   * Performs clean up operations when the window is about to be hidden/closed.
   */
  private void onHiding(WindowEvent event) {
  }

  /**
   * Performs clean up operations when the window is hidden/closed.
   */
  private void onHidden(WindowEvent event) {
    // all secondary windows must be closed to terminate the application properly
    for (int idx = Window.getWindows().size() - 1; idx >= 0; idx--) {
      final Window wnd = Window.getWindows().get(idx);
      if (wnd != getStage()) {
        wnd.hide();
      }
    }
  }

  /**
   * Called if the user wants to close the launcher application.
   *
   * @param event Forwarded event that triggered the close action. Can be {@code null}.
   */
  private void onCloseApplication(Event event) {
    if (!quit(false)) {
      if (event != null) {
        // discard close request
        event.consume();
      }
    }
  }

  /**
   * Called whenever a key is pressed
   */
  private void onGlobalKeyPressed(KeyEvent event) {
    switch (event.getCode()) {
      case ENTER ->
        // send input text to WeiDU process
          sendInput(getController().inputField.getText(), true);
      case Q -> {
        if (event.isControlDown()) {
          // signal to quit the app
          onCloseApplication(event);
        }
      }
      default -> {
      }
    }
  }

  /**
   * Sets the window into the running state.
   */
  private void setWeiduRunning() {
    updateWindowTitle(true);
    getController().quitButton.setText(R.get("ui.main.terminate.button"));
    getController().inputQuitButton.setDisable(false);
    getController().inputAskButton.setDisable(false);
    getController().inputSkipButton.setDisable(false);
    getController().inputReinstallButton.setDisable(false);
    getController().inputUninstallButton.setDisable(false);
    getController().inputInstallButton.setDisable(false);
    getController().inputNoButton.setDisable(false);
    getController().inputYesButton.setDisable(false);
  }

  /**
   * Sets the window into the completed state.
   */
  private void setWeiduTerminated() {
    updateWindowTitle(true);
    getController().quitButton.setText(R.get("ui.main.quit.button"));
    getController().inputQuitButton.setDisable(true);
    getController().inputAskButton.setDisable(true);
    getController().inputSkipButton.setDisable(true);
    getController().inputReinstallButton.setDisable(true);
    getController().inputUninstallButton.setDisable(true);
    getController().inputInstallButton.setDisable(true);
    getController().inputNoButton.setDisable(true);
    getController().inputYesButton.setDisable(true);
  }

  /**
   * Returns whether a WeiDU process is currently being executed.
   */
  private boolean isProcessRunning() {
    return (process != null && process.isRunning());
  }

  /**
   * Returns the exit code of the WeiDU process.
   *
   * @return Exit code of a completed WeiDU process, {@link Integer#MIN_VALUE} if the process was forcefully terminated,
   * and {@code null} otherwise.
   */
  private Integer getProcessResult() {
    if (processResult != null && processResult.isDone()) {
      try {
        return processResult.get();
      } catch (CancellationException e) {
        Logger.debug(e, "WeiDU process cancelled");
        return Integer.MIN_VALUE;
      } catch (Exception e) {
        // don't really care
        Logger.warn(e, "WeiDU process error");
      }
    }
    return null;
  }

  /**
   * Sets or update the window title.
   */
  private void updateWindowTitle(boolean showState) {
    final Path tp2Path = Configuration.getInstance().getTp2Path();
    final String tp2Name = (tp2Path != null) ? tp2Path.getFileName().toString() : null;

    final ModIni ini = (modInfo != null) ? modInfo.getModIni() : null;
    final String modName = (ini != null && !ini.getName().isEmpty()) ? ini.getName() : null;

    String title;
    if (tp2Name != null && modName != null) {
      title = String.format("%s - %s (%s)", Globals.APP_TITLE, tp2Name, modName);
    } else if (tp2Name != null) {
      title = String.format("%s - %s", Globals.APP_TITLE, tp2Name);
    } else {
      title = Globals.APP_TITLE;
    }

    if (showState) {
      final String key = isProcessRunning() ? "ui.main.windowTitle.state.running" : "ui.main.windowTitle.state.completed";
      final String state = String.format(" [%s]", R.get(key));
      title += state;
    }

    stage.setTitle(title);
  }

  /**
   * Sets the font size of the output text area to the specified value.
   */
  private void setOutputAreaFontSize(double value) {
    final Font font = getController().outputArea.getFont();
    if (font.getSize() != value) {
      final String style = String.format("-fx-font-size: %f;", value);
      getController().outputArea.styleProperty().set(style);
    }
  }

  /**
   * Applies the specified font size to the font size spinner and optionally updates configuration option.
   *
   * @param fontSize     Font size to apply to the output text area.
   * @param updateConfig Whether to update the corresponding {@link Configuration} option.
   */
  private void applyOutputFontSize(double fontSize, boolean updateConfig) {
    // ensure that output font size is rounded to a multiple of 0.5
    fontSize = Math.floor(fontSize * 2.0) / 2.0;

    if (updateConfig) {
      Configuration.getInstance().setOption(Configuration.Key.OUTPUT_FONT_SIZE, fontSize);
    }

    // distinction is necessary to ensure that font size is set
    if (fontSize != getController().outputFontSizeValueFactory.getValue()) {
      getController().outputFontSizeValueFactory.setValue(fontSize);
    } else {
      setOutputAreaFontSize(getOutputAreaFontSize());
    }
  }

  /**
   * Updates the output buffer size label to the specified value.
   */
  private void setOutputBufferSizeLabel(double value) {
    final int roundedValue = getOutputBufferSize(value);
    final int defaultValue = Configuration.Key.BUFFER_LIMIT.<Integer>getDefaultValue();
    if (roundedValue == defaultValue) {
      final String strDefault = R.get("ui.main.menu.item.bufferSize.default");
      getController().bufferSizeValueLabel.setText(String.format("%,d (%s)", roundedValue, strDefault));
    } else if (roundedValue == Integer.MAX_VALUE) {
      final String strUnlimited = R.get("ui.main.menu.item.bufferSize.unlimited");
      getController().bufferSizeValueLabel.setText(strUnlimited);
    } else {
      getController().bufferSizeValueLabel.setText(String.format("%,d", roundedValue));
    }
  }

  /**
   * Updates the UI to match the specified color scheme.
   */
  private void applyDarkModeUi(boolean enable) {
    Configuration.getInstance().setOption(Configuration.Key.DARK_UI_MODE, enable);

    // updating window skins
    CustomScene.updateSceneCache();

    // updating Options menu icon
    if (getController().optionsButton.getGraphic() instanceof ImageView iv) {
      final Icons icon = enable ? Icons.OptionsDark32 : Icons.Options32;
      iv.setImage(icon.getImage());
    }
  }

  /**
   * Ensures that the output text length does not exceed the specified buffer size.
   * Excess characters will be removed from the beginning of the string if needed.
   *
   * @param text String to check
   * @return A string with a length that satisfies the buffer limit.
   */
  private String ensureOutputTextLimit(String text) {
    String retVal = text;

    if (text != null) {
      int maxLength = getOutputBufferSize();
      if (maxLength <= 0) {
        maxLength = Configuration.Key.BUFFER_LIMIT.getDefaultValue();
      }
      final int textLength = text.length();
      if (textLength > maxLength) {
        int numCharsToRemove = textLength - maxLength;
        int pos = Math.max(text.indexOf('\n', numCharsToRemove) + 1, numCharsToRemove);
        retVal = text.substring(pos);
      }
    }

    return retVal;
  }

  /**
   * This method sends the specified text to the current WeiDU process.
   *
   * @param text    The text to send to the WeiDU process.
   * @param cleanup Whether to clear the input field after the operation.
   */
  private void sendInput(String text, boolean cleanup) {
    final String inputText = text + SystemInfo.NEWLINE;
    if (isProcessRunning()) {
      process.setInput(inputText);
    }

    appendOutputText(inputText, true);

    if (cleanup) {
      getController().inputField.setText("");
    }

    if (isAutoQuitEnabled() && !isProcessRunning()) {
      quit(false);
    }

    setInputFocus();
  }

  /**
   * Sets focus to the input text field.
   */
  private void setInputFocus() {
    getController().inputField.requestFocus();
  }

  /**
   * Initializes the UI.
   */
  private void setupUI(Scene scene) {
    stage.setScene(Objects.requireNonNull(scene));

    // assigning application icon
    stage.getIcons().addAll(Icons.Icon.getImages());

    stage.focusedProperty().addListener((ob, ov, nv) -> {
      if (nv) setInputFocus();
    });
    stage.setOnCloseRequest(this::onCloseApplication);
    stage.setOnShowing(this::onShowing);
    stage.setOnShown(this::onShown);
    stage.setOnHiding(this::onHiding);
    stage.setOnHidden(this::onHidden);
    // global detection of pressing keys requires an explicit event filter
    stage.getScene().addEventFilter(KeyEvent.KEY_PRESSED, this::onGlobalKeyPressed);

    // add invisible border; required to reserve border space for a consistent look when border is enabled
    setVisualizedResult(false, 0);

    getController().autoQuitCheckItem.setOnAction(event -> Configuration.getInstance()
        .setOption(Configuration.Key.QUIT_ON_ENTER, isAutoQuitEnabled()));
    getController().visualizeResultCheckItem.setOnAction(event -> Configuration.getInstance()
        .setOption(Configuration.Key.VISUALIZE_RESULT, isVisualizeResultsEnabled()));
    getController().warnModOrderCheckItem.setOnAction(event -> Configuration.getInstance()
        .setOption(Configuration.Key.WARN_MOD_ORDER, isWarnModOrderEnabled()));
    getController().darkModeUiCheckItem.setOnAction(event -> applyDarkModeUi(isDarkModeEnabled()));
    getController().bufferSizeSlider.valueProperty().addListener((ob, ov, nv) -> setOutputBufferSizeLabel(nv.doubleValue()));
    getController().outputFontSizeValueFactory.valueProperty().addListener((ob, ov, nv) -> setOutputAreaFontSize(nv));

    getController().quitButton.setOnAction(this::onCloseApplication);
    getController().detailsButton.selectedProperty().addListener((ob, ov, nv) -> onDetailsButtonSelected(nv));
    getController().detailsButton.setOnAction(event -> setInputFocus());
    getController().aboutButton.setOnAction(event -> showAboutDialog());
    getController().sendButton.setOnAction(event -> sendInput(getController().inputField.getText(), true));

    // shortcut buttons
    getController().inputQuitButton.setOnAction(event -> {
      sendInput("q", false);
      event.consume();
    });
    getController().inputAskButton.setOnAction(event -> {
      sendInput("a", false);
      event.consume();
    });
    getController().inputSkipButton.setOnAction(event -> {
      sendInput("s", false);
      event.consume();
    });
    getController().inputReinstallButton.setOnAction(event -> {
      sendInput("r", false);
      event.consume();
    });
    getController().inputUninstallButton.setOnAction(event -> {
      sendInput("u", false);
      event.consume();
    });
    getController().inputInstallButton.setOnAction(event -> {
      sendInput("i", false);
      event.consume();
    });
    getController().inputNoButton.setOnAction(event -> {
      sendInput("n", false);
      event.consume();
    });
    getController().inputYesButton.setOnAction(event -> {
      sendInput("y", false);
      event.consume();
    });

    // setting initial window title
    updateWindowTitle(false);

    // preparing execution mode
    getController().detailsButton.setDisable(Configuration.getInstance().getMode() != Configuration.Mode.WEIDU_GUIDED);

    if (this.modInfo != null) {
      try {
        detailsWindow = new DetailsWindow(this.modInfo);
      } catch (Exception e) {
        Logger.error(e, "Could not create DetailsWindow instance");
        getController().detailsButton.setDisable(true);
      }
    }

    stage.show();
    stage.setMinWidth(stage.getWidth());
    stage.setMinHeight(stage.getHeight());

    // Applying configuration
    if (Configuration.getInstance().<Boolean>getOption(Configuration.Key.WINDOW_MAXIMIZED)) {
      stage.setMaximized(true);
    } else {
      final Integer x = Configuration.getInstance().<Integer>getOption(Configuration.Key.WINDOW_X);
      if (x != null) {
        stage.setX(x);
      }
      final Integer y = Configuration.getInstance().<Integer>getOption(Configuration.Key.WINDOW_Y);
      if (y != null) {
        stage.setY(y);
      }
      final Integer w = Configuration.getInstance().<Integer>getOption(Configuration.Key.WINDOW_WIDTH);
      if (w != null) {
        stage.setWidth(w);
      }
      final Integer h = Configuration.getInstance().<Integer>getOption(Configuration.Key.WINDOW_HEIGHT);
      if (h != null) {
        stage.setHeight(h);
      }
    }

    setAutoQuitEnabled(Configuration.getInstance().<Boolean>getOption(Configuration.Key.QUIT_ON_ENTER));
    setVisualizeResultsEnabled(Configuration.getInstance().<Boolean>getOption(Configuration.Key.VISUALIZE_RESULT));
    setWarnModOrderEnabled(Configuration.getInstance().<Boolean>getOption(Configuration.Key.WARN_MOD_ORDER));
    setDarkModeEnabled(Configuration.getInstance().<Boolean>getOption(Configuration.Key.DARK_UI_MODE));
    setOutputBufferSize(Configuration.getInstance().<Integer>getOption(Configuration.Key.BUFFER_LIMIT));

    applyOutputFontSize(Configuration.getInstance().getOption(Configuration.Key.OUTPUT_FONT_SIZE,
        getController().outputArea.getFont().getSize()), true);

    applyDarkModeUi(isDarkModeEnabled());

    onDetailsButtonSelected(Configuration.getInstance().getOption(Configuration.Key.SHOW_DETAILS));

    setInputFocus();
    execute();
  }

  private void runProcess(SysProc process) {
    if (this.process == null && process != null) {
      this.process = process;
      this.process.addChangeEventHandler(this::onProcessStateChanged);
      this.process.addOutputEventHandler(this::onProcessOutput);

      if (this.process.getWorkingDirectory() != null) {
        appendOutputText("Working directory: " + this.process.getWorkingDirectory().toString() + "\n", true);
      }
      // display executed command line in output area
      appendOutputText(this.process.getCommandLine() + "\n", true);

      try {
        processResult = this.process.execute();
      } catch (IOException e) {
        Logger.info(e, "WeiDU process execution error");
        this.process = null;
        this.processResult = null;
        handleProcessError(e.getMessage());
      }
    }
  }

  private void handleProcessError(String message) {
    Utils.showErrorDialog(getStage(), R.ERROR(), R.get("ui.main.message.process.header"),
        String.format("%s\n%s", R.get("ui.main.message.process.content"), message));
  }

  /**
   * Updates the {@link Configuration} instance with the current application state.
   */
  private void updateConfiguration() {
    final Configuration cfg = Configuration.getInstance();
    cfg.setOption(Configuration.Key.WINDOW_X, (int) getStage().getX());
    cfg.setOption(Configuration.Key.WINDOW_Y, (int) getStage().getY());
    cfg.setOption(Configuration.Key.WINDOW_WIDTH, (int) getStage().getWidth());
    cfg.setOption(Configuration.Key.WINDOW_HEIGHT, (int) getStage().getHeight());
    cfg.setOption(Configuration.Key.WINDOW_MAXIMIZED, getStage().isMaximized());
    cfg.setOption(Configuration.Key.SHOW_DETAILS, isDetailsWindowVisible());
    cfg.setOption(Configuration.Key.WARN_MOD_ORDER, isWarnModOrderEnabled());
    cfg.setOption(Configuration.Key.QUIT_ON_ENTER, isAutoQuitEnabled());
    cfg.setOption(Configuration.Key.VISUALIZE_RESULT, isVisualizeResultsEnabled());
    cfg.setOption(Configuration.Key.BUFFER_LIMIT, getOutputBufferSize());
    cfg.setOption(Configuration.Key.OUTPUT_FONT_SIZE, getOutputAreaFontSize());
    if (modInfo != null) {
      cfg.setOption(Configuration.Key.LAST_GAME_PATH, modInfo.getGamePath().toString());
      if (modInfo.getTp2File() != null) {
        cfg.setOption(Configuration.Key.LAST_MOD_PATH, modInfo.getTp2File().getParent().toString());
      }
    }
  }

  /**
   * Initializes a {@link ModInfo} instance in Setup Mode. Does nothing, otherwise.
   *
   * @param feedback Whether to show error messages to the user if something didn't work.
   * @throws Exception Thrown if the {@link ModInfo} instance could not be initialized.
   */
  private ModInfo loadModInfo(Configuration.Mode mode, boolean feedback) throws Exception {
    if (mode == Configuration.Mode.WEIDU_GUIDED) {
      // Interactive installation mode
      Path gamePath = ModInfo.findGamePath(Configuration.getInstance().getTp2Path());

      // request game path
      if (gamePath == null) {
        // try last used game directory first
        Path initialPath = null;
        String dir = Configuration.getInstance().getOption(Configuration.Key.LAST_GAME_PATH);
        if (dir != null) {
          dir = Utils.resolveExistingPath(Path.of(dir)).toString();
        }
        if (dir == null) {
          dir = SystemInfo.getLocalDataPath().toString();
        }
        if (!dir.isEmpty()) {
          initialPath = Path.of(dir);
        }

        // fall back to local data directory
        if (initialPath == null) {
          initialPath = SystemInfo.getLocalDataPath();
        }

        gamePath = Utils.chooseOpenFile(null, R.get("ui.main.modInfo.fileDialog.title"), initialPath,
            new ExtensionFilter(R.get("ui.main.modInfo.fileDialog.filter.key"), "*.key"));
      }

      if (gamePath == null) {
        // cancel launcher
        if (feedback) {
          Utils.showErrorDialog(null, R.ERROR(), R.get("ui.main.modInfo.message.gameNotFound.header"),
              R.get("ui.main.modInfo.message.gameNotFound.content"));
        }
        throw new Exception("Could not find game directory");
      }

      try {
        return new ModInfo(gamePath, Configuration.getInstance().getTp2Path());
      } catch (Exception e) {
        Logger.debug(e, "Error creating ModInfo instance");
        if (feedback) {
          Utils.showErrorDialog(null, R.ERROR(), R.get("ui.main.modInfo.message.modNotFound.header"),
              String.format("%s\n%s", R.get("ui.main.modInfo.message.modNotFound.content"),
                  Configuration.getInstance().getTp2Path()));
        }
        throw e;
      }
    }

    return null;
  }

  /**
   * Applies a border to the specified UI control.
   *
   * @param enabled  Whether the border should be visible.
   * @param exitCode A numeric value that is used to determine the color for the border.
   *                 Ignored if {@code enabled} is {@code false}.
   */
  private void setVisualizedResult(boolean enabled, int exitCode) {
    final Color color;
    if (enabled) {
      // exit codes according to WeiDU sources:
      // https://github.com/WeiDUorg/weidu/blob/d2441fb71f6e08b22e747684c3c53c0a3fcb123b/src/util.ml#L847
      color = switch (exitCode) {
        // StatusSuccess
        case 0 -> StateColors.SUCCESS.getColor();
        // StatusInstallWarning
        case 3 -> StateColors.WARNING.getColor();
        // everything else is considered an error
        default -> StateColors.ERROR.getColor();
      };
    } else {
      color = StateColors.DISABLED.getColor();
    }

    final BorderWidths bw = new BorderWidths(2.0);
    final BorderStroke bs = new BorderStroke(color, BorderStrokeStyle.SOLID, CornerRadii.EMPTY, bw);
    getController().outputArea.setBorder(new Border(bs));
  }

  /**
   * Checks if there are potential mod order conflicts.
   *
   * @param interactive Whether to allow the user to confirm or override the suggested action.
   * @return {@code true} if the mod can be installed, {@code false} to cancel the operation.
   */
  private boolean checkModOrder(boolean interactive) {
    boolean retVal = true;

    if (getModInfo() != null && getModInfo().getModIni() != null) {
      // getting mods that should be installed after this mod
      final ModIni ini = getModInfo().getModIni();
      List<String> modList = ini.getBeforeList();
      if (!modList.isEmpty()) {
        final Path gamePath = getModInfo().getGamePath();
        Set<String> mods = null;
        try {
          WeiduLog log = WeiduLog.load(gamePath.resolve(WeiduLog.WEIDU_FILENAME));
          mods = log.getEntries().stream().map(e -> e.getTp2Name().toLowerCase()).collect(Collectors.toSet());
        } catch (Exception e) {
          // WeiDU.log may not exist
          Logger.debug(e, "WeiDU.log not available or could not be parsed");
        }

        if (mods != null) {
          final List<String> conflicts = new ArrayList<>();
          for (final String modName : modList) {
            if (mods.contains(modName.toLowerCase())) {
              conflicts.add(modName);
            }
          }

          if (!conflicts.isEmpty()) {
            retVal = false;
            if (interactive) {
              final String modSequence = String.join(", ", conflicts);
              final String title = R.WARNING();
              final String header = R.get("ui.main.modOrder.message.header");
              final String modName;
              if (modInfo.getModIni() != null && !modInfo.getModIni().getName().isEmpty()) {
                modName = modInfo.getModIni().getName();
              } else {
                modName = modInfo.getTp2Name();
              }
              final String content = String.format(R.get("ui.main.modOrder.message.content"), modName, modSequence);
              final ButtonType type = Utils.showCustomDialog(getStage(), Alert.AlertType.CONFIRMATION, title, header,
                  content, ButtonType.YES, ButtonType.NO);
              retVal = (type == ButtonType.YES);
            }
          }
        }
      }
    }

    return retVal;
  }

  /**
   * Performs the execution of the prepared WeiDU process.
   */
  private void execute() {
    try {
      switch (Configuration.getInstance().getMode()) {
        case WEIDU_HELP -> showWeiduHelp();
        case WEIDU_GUIDED -> {
          boolean confirm = true;
          if (isWarnModOrderEnabled()) {
            confirm = checkModOrder(true);
          }
          if (confirm) {
            executeGuided();
          } else {
            final String fmt = String.format("*** %s ***\n", R.get("ui.main.execute.message.skip"));
            appendOutputText(String.format(fmt, getModInfo().getTp2Name()),
                false);
          }
        }
        default -> executeCustom();
      }
    } catch (Exception e) {
      Logger.debug(e, "Error executing WeiDU command");
      Utils.showErrorDialog(getStage(), R.ERROR(), R.get("ui.main.execute.message.error.header"), e.getMessage());
    }
  }

  private void showWeiduHelp() {
    try {
      setWeiduRunning();
      final String helpDesc = Weidu.getInstance().getHelp();
      if (helpDesc != null) {
        appendOutputText(helpDesc, false);
      } else {
        Utils.showErrorDialog(getStage(), R.ERROR(), R.get("ui.main.weiduHelp.message.header"), null);
      }
    } catch (Exception e) {
      Logger.debug(e, "Error showing WeiDU help");
      Utils.showErrorDialog(getStage(), R.ERROR(), R.get("ui.main.weiduHelp.message.header"), null);
    } finally {
      setWeiduTerminated();
    }
  }

  /**
   * Executes a custom WeiDU command.
   */
  private void executeCustom() throws IndexOutOfBoundsException, NullPointerException, UnsupportedOperationException {
    final List<String> command = new ArrayList<>(Configuration.getInstance().getWeiduArgs());
    command.add(0, Weidu.getInstance().getWeidu().toString());

    final SysProc sp = new SysProc(command.toArray(new String[0]));
    runProcess(sp);
  }

  /**
   * Executes a guided WeiDU command.
   */
  private void executeGuided() throws IllegalArgumentException, IndexOutOfBoundsException, NullPointerException {
    if (getModInfo() == null) {
      throw new NullPointerException("Mod information are unavailable.");
    }

    String gameLang = null;
    if (getModInfo().isEnhancedEdition() && !getModInfo().isWeiduConfAvailable()) {
      // interactive game language selection
      gameLang = GameLanguageDialog.select(getStage(), getModInfo());
    }

    Path tp2File = Configuration.getInstance().getTp2Path();
    if (tp2File == null) {
      throw new NullPointerException("No TP2 file specified.");
    }

    final Path workingDir = getModInfo().getGamePath();
    if (workingDir != null && tp2File.startsWith(workingDir)) {
      tp2File = workingDir.relativize(tp2File);
    }

    final String[] command = getWeiduCommand(gameLang, tp2File);

    final SysProc sp = new SysProc(workingDir, true, command);
    runProcess(sp);
  }

  /**
   * Assembles the WeiDU command line for a guided mod installation.
   */
  private String[] getWeiduCommand(String gameLang, Path tp2File) {
    final List<String> command = new ArrayList<>(Configuration.getInstance().getWeiduArgs());

    if (!command.contains("--log")) {
      final Path logFile = getModInfo().getLogFile();
      command.add(0, logFile.toString());
      command.add(0, "--log");
    }

    if (!command.contains("--game")) {
      final Path gamePath = getModInfo().getGamePath();
      command.add(0, gamePath.toString());
      command.add(0, "--game");
    }

    if (!command.contains("--use-lang") && gameLang != null) {
      command.add(0, gameLang);
      command.add(0, "--use-lang");
    }

    if (!command.contains("--no-exit-pause")) {
      command.add(0, "--no-exit-pause");
    }

    command.add(0, Utils.normalizePath(tp2File.toString()));
    command.add(0, Weidu.getInstance().getWeidu().toString());

    return command.toArray(new String[0]);
  }

  /**
   * Shows the About dialog as a modal dialog.
   */
  private void showAboutDialog() {
    try {
      AboutDialog.showAboutDialog(getStage());
    } catch (Exception e) {
      Logger.error(e, "Error creating About dialog");
      // Fall-back option
      Utils.showMessageDialog(stage, "About", Globals.APP_TITLE, "Version " + Globals.APP_VERSION);
    }
  }

  /**
   * Quits the launcher application.
   *
   * @param forced Whether to quit the application regardless of the current state. Set this to {@code true} to skip any
   *               confirmation dialogs.
   * @return {@code true} if quit is performed, {@code false} otherwise.
   */
  private boolean quit(boolean forced) {
    ButtonType result = ButtonType.OK;
    if (!forced && isProcessRunning()) {
      result = Utils.showConfirmationDialog(stage, R.get("ui.main.quit.message.title"),
          R.get("ui.main.quit.message.header"), R.get("ui.main.quit.message.content"));
    }

    if (result == ButtonType.OK) {
      // continue with closing the application
      if (process != null && process.isRunning()) {
        process.killProcess();
      }
      updateConfiguration();
      try {
        Configuration.getInstance().save();
      } catch (Exception e) {
        Logger.warn(e, "Error saving configuration");
      }
      stage.close();
      return true;
    }
    return false;
  }
}
