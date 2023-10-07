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
import io.infinitytools.wml.utils.FontMetrics;
import io.infinitytools.wml.utils.R;
import io.infinitytools.wml.utils.Utils;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.layout.VBox;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.Window;
import javafx.stage.WindowEvent;
import org.tinylog.Logger;

import java.net.URL;
import java.util.List;
import java.util.Objects;

/**
 * A modal dialog that displays a language selection dialog.
 */
public class GameLanguageDialog extends Stage {
  /**
   * Path to the FXML definition file for this window.
   */
  private static final URL FXML_FILE = GameLanguageDialog.class.getResource("language.fxml");

  public static String select(Window owner, ModInfo modInfo) {
    String retVal = null;

    try {
      final GameLanguageDialog dialog = new GameLanguageDialog(owner, modInfo);
      dialog.showAndWait();
      retVal = dialog.getSelectedLanguage();
    } catch (IllegalArgumentException e) {
      // ignored; expected exception
      Logger.debug("Select game language: {}", e);
    } catch (Exception e) {
      Logger.error(e, "Select game language");
    }

    return retVal;
  }

  private LanguageWindowController controller;
  private String selectedLanguage;

  private GameLanguageDialog(Window owner, ModInfo modInfo) throws Exception {
    super();
    initOwner(owner);
    initModality(Modality.WINDOW_MODAL);
    if (modInfo == null || modInfo.getGameLanguages() == null || modInfo.getGameLanguages().isEmpty()) {
      throw new IllegalArgumentException("Game language list not available");
    }
    init(modInfo);
  }

  /**
   * Returns the ISO language code of the selected language.
   *
   * @return ISO language code of the selected language. Returns {@code null} if no selection was made.
   */
  public String getSelectedLanguage() {
    return selectedLanguage;
  }

  /**
   * Positions the dialog at the center of the parent window.
   */
  private void onWindowShown(WindowEvent event) {
    if (getOwner() != null) {
      final double x = getOwner().getX() + (getOwner().getWidth() - this.getWidth()) / 2.0;
      final double y = getOwner().getY() + (getOwner().getHeight() - this.getHeight()) / 2.0;
      Logger.debug("Dialog position: {}, {}", x, y);
      setX(x);
      setY(y);
    } else {
      centerOnScreen();
    }
  }

  /**
   * Called when the dialog should be closed.
   */
  private void onClose(boolean assignSelected) {
    if (assignSelected) {
      selectedLanguage = controller.languageBox.getSelectionModel().getSelectedItem().getCode();
    }
    hide();
  }

  private void init(ModInfo modInfo) throws Exception {
    final FXMLLoader loader = new FXMLLoader(FXML_FILE, R.getBundle());
    final VBox vbox = loader.load();
    controller = loader.getController();

    final FontMetrics fm = new FontMetrics(controller.notesLabel.getFont());
    final double maxWidth = fm.computeStringWidth(controller.notesLabel.getText()) / 1.5;
    vbox.setMaxWidth(maxWidth);

    // initializing language list
    final List<LanguageCode> items = modInfo
        .getGameLanguages()
        .stream()
        .map(LanguageCode::new)
        .toList();
    controller.languageBox.getItems().addAll(items);
    controller.languageBox.getSelectionModel().select(0);

    controller.cancelButton.setOnAction(event -> onClose(false));
    controller.okButton.setOnAction(event -> onClose(true));

    final Scene scene = new CustomScene(vbox);
    setScene(scene);
    setTitle(R.get("ui.language.title"));
    setResizable(false);
    controller.languageBox.requestFocus();

    // assigning application icon
    getIcons().addAll(Icons.Icon.getImages());

    setOnShown(this::onWindowShown);
  }


  /**
   * Helper class for ISO language codes to provide code and description strings.
   */
  public static class LanguageCode {
    private final String code;
    private final String name;
    private final String nameNative;

    public LanguageCode(String code) {
      this.code = Objects.requireNonNull(code).toLowerCase();

      final String[] items = Utils.getLanguageName(code);
      this.nameNative = items[0];
      this.name = items[1];
    }

    /**
     * Returns the ISO code of the language.
     */
    public String getCode() {
      return code;
    }

    /**
     * Returns the English translation of the language name.
     */
    public String getName() {
      return name;
    }

    /**
     * Returns the language name in the native language.
     */
    public String getNativeName() {
      return nameNative;
    }

    @Override
    public int hashCode() {
      return Objects.hash(code);
    }

    @Override
    public boolean equals(Object obj) {
      if (this == obj) {
        return true;
      }
      if (obj == null) {
        return false;
      }
      if (getClass() != obj.getClass()) {
        return false;
      }
      LanguageCode other = (LanguageCode) obj;
      return Objects.equals(code, other.code);
    }

    @Override
    public String toString() {
      return (getName().equals(getNativeName())) ? getName() : String.format("%s (%s)", getName(), getNativeName());
    }
  }
}
