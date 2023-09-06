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

import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;

/**
 * Controller class for the Game Language Selection UI. It is automatically initialized by the JavaFX FXML loader.
 */
public class LanguageWindowController {
  public Label promptLabel;
  public Label infoLabel;
  public Label notesLabel;
  public ComboBox<GameLanguageDialog.LanguageCode> languageBox;
  public Button okButton;
  public Button cancelButton;

  public LanguageWindowController() {
  }
}
