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
import io.infinitytools.wml.utils.R;
import io.infinitytools.wml.utils.Utils;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Bounds;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.IndexRange;
import javafx.scene.control.TextArea;
import javafx.scene.layout.VBox;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import javafx.stage.WindowEvent;
import org.tinylog.Logger;

import java.net.URL;
import java.util.HashMap;
import java.util.Locale;
import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

public class SearchDialog extends Stage {
  private static final URL FXML_FILE = SearchDialog.class.getResource("search.fxml");
  private static final HashMap<TextArea, SearchDialog> INSTANCES = new HashMap<>();

  /**
   * Returns a {@link SearchDialog} instance associated with the specified {@link TextArea}.
   *
   * @param textArea {@link TextArea} instance associated with the search dialog.
   * @return The requested {@link SearchDialog} instance. A new instance is created automatically if it doesn't exist.
   * @throws Exception if dialog initialization fails.
   */
  public static SearchDialog getInstance(TextArea textArea) throws Exception {
    if (textArea == null) {
      throw new NullPointerException();
    }

    SearchDialog retVal = INSTANCES.get(textArea);
    if (retVal == null) {
      retVal = new SearchDialog(textArea);
      INSTANCES.put(textArea, retVal);
    }

    return retVal;
  }

  /**
   * The associated {@link TextArea} instance.
   */
  private final TextArea textArea;

  private SearchDialogController controller;
  /**
   * Contains the current regular expression pattern for the search string.
   */
  private Pattern inputPattern;
  /**
   * Tracks the current search position within the associated text area string.
   */
  private int position = -1;
  /**
   * Indicates whether the dialog has been fully initialized.
   */
  private boolean initialized;

  public SearchDialog(TextArea textArea) throws Exception {
    super();
    this.textArea = Objects.requireNonNull(textArea);

    final Scene scene = this.textArea.getScene();
    if (scene != null) {
      initOwner(scene.getWindow());
    }

    initModality(Modality.NONE);
    initStyle(StageStyle.UTILITY);

    init();
  }

  /**
   * Returns the associated {@link TextArea} node.
   */
  public TextArea getTextArea() {
    return textArea;
  }

  /**
   * Returns the search text.
   */
  public String getInputText() {
    return controller.inputField.getText();
  }

  /**
   * Returns whether search should be case-sensitive.
   */
  public boolean isMatchCase() {
    return controller.matchCaseCheckBox.isSelected();
  }

  /**
   * Returns whether the search text should be parsed as regular expression.
   */
  public boolean isRegularExpression() {
    return controller.regexCheckBox.isSelected();
  }

  /**
   * Returns whether the search should automatically start from the beginning when the end is reached.
   */
  public boolean isWrapAround() {
    return controller.wrapAroundCheckBox.isSelected();
  }

  /**
   * Centers the dialog on the specified {@link Node}.
   *
   * @throws NullPointerException if {@code node} is {@code null}.
   */
  private void centerOnNode(Node node) {
    if (node == null) {
      throw new NullPointerException();
    }

    final Bounds b = node.localToScreen(node.getBoundsInLocal());
    final double x = b.getMinX() + (b.getWidth() - getWidth()) / 2.0;
    final double y = b.getMinY() + (b.getHeight() - getHeight()) / 2.0;
    setX(x);
    setY(y);
  }

  /**
   * Sets the input focus to the search text input field.
   */
  private void setInputFocus() {
    controller.inputField.requestFocus();
  }

  /**
   * Actions performed when the dialog window has become visible on the screen.
   */
  private void onShown(WindowEvent event) {
    if (!initialized) {
      initialized = true;
      setMinHeight(getHeight());
      setMinWidth(getWidth());
      // default width within acceptable range
      setWidth(Math.max(getWidth(), 320.0));
      centerOnNode(getTextArea());
    }

    setInputFocus();
    requestFocus();
  }

  /**
   * Called when the "Close" button is pressed.
   */
  private void onClose() {
    hide();
  }

  /**
   * Called when a search is initiated.
   */
  private void onFind() {
    if (getInputText().isEmpty()) {
      return;
    }

    initPosition();

    try {
      updateInputPattern();
    } catch (PatternSyntaxException e) {
      Logger.error(e, "Regular expression syntax error");
      setInputFocus();
      Utils.showErrorDialog(this, R.get("ui.find.message.syntax.error.title"),
          R.get("ui.find.message.syntax.error.header"), e.getMessage());
      return;
    } catch (IllegalArgumentException e) {
      Logger.error(e, "Error parsing the input string");
      setInputFocus();
      Utils.showErrorDialog(this, R.ERROR(), R.get("ui.find.message.generic.error.header"), e.getMessage());
      return;
    }

    final IndexRange range = findNext();
    if (range != null) {
      Logger.debug("Found match (text: {}, range: {})", getInputText(), range);
      getTextArea().selectRange(range.getStart(), range.getEnd());
    } else {
      resetSearch();
      Utils.showMessageDialog(this, R.INFORMATION(),
          String.format("%s\n%s", R.get("ui.find.message.search.header"),
              Utils.getSafeString(getInputText(), 40, true)), null);
      setInputFocus();
    }
  }

  /**
   * Returns an {@link IndexRange} object of the next available matching substring in the associated text area.
   *
   * @return A {@link IndexRange} object if a match is found, {@code null} otherwise.
   */
  private IndexRange findNext() {
    if (position < 0 || position > getTextArea().getLength()) {
      return null;
    }

    IndexRange retVal = null;
    final Matcher matcher = getInputPattern().matcher(getTextArea().getText());
    for (int i = 0; i < 2; i++) {
      if (matcher.find(position)) {
        int start = matcher.start();
        int end = matcher.end();
        retVal = new IndexRange(start, end);
        position = end;
        break;
      } else if (isWrapAround()) {
        position = 0;
      } else {
        break;
      }
    }

    return retVal;
  }

  /**
   * Resets internal data to indicate that a new search is started.
   */
  private void resetSearch() {
    position = -1;
    inputPattern = null;
  }

  /**
   * Sets the initial caret position in the text area for the search.
   */
  private void initPosition() {
    if (position < 0 || position >= getTextArea().getLength()) {
      // initializing position
      position = 0;
    }
  }

  /**
   * Recreates the regular expression pattern for the input string if needed.
   *
   * @throws PatternSyntaxException   if the input string is parsed as regular expression and contains syntax errors.
   * @throws IllegalArgumentException if the input string could not be processed.
   */
  private void updateInputPattern() throws IllegalArgumentException {
    if (inputPattern == null) {
      int flags = Pattern.MULTILINE;
      if (!isMatchCase()) {
        flags |= Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE;
      }
      final String text = getResolvedString(getInputText());
      final String regex = isRegularExpression() ? text : Pattern.quote(text);
      inputPattern = Pattern.compile(regex, flags);
    }
  }

  /**
   * Returns the current regular expression pattern for the input string.
   */
  private Pattern getInputPattern() {
    return inputPattern;
  }

  private void init() throws Exception {
    final FXMLLoader loader = new FXMLLoader(FXML_FILE, R.getBundle());
    final VBox vbox = loader.load();
    controller = loader.getController();

    controller.matchCaseCheckBox.setOnAction(event -> {
      if (!getInputText().equals(getInputText().toLowerCase(Locale.ROOT))) {
        resetSearch();
      }
    });
    controller.regexCheckBox.setOnAction(event -> resetSearch());

    controller.inputField.textProperty().addListener((ob, ov, nv) -> resetSearch());

    controller.findButton.setOnAction(event -> onFind());
    controller.closeButton.setOnAction(event -> onClose());

    final Scene scene = new CustomScene(vbox);
    setScene(scene);
    setTitle(R.get("ui.find.title"));
    getIcons().addAll(Icons.Icon.getImages());
    setResizable(true);

    setOnShown(this::onShown);
  }

  /**
   * Resolves special symbols in the given string (e.g. '\t' or '\n').
   *
   * @param s The string to evaluate.
   * @return A string with resolved symbols.
   */
  private static String getResolvedString(String s) {
    if (s == null) {
      return "";
    }

    final String[][] replace = {
        { "\\t", "\t" },
        { "\\n", "\n" },
        { "\\r", "\r" },
        // must be replaced last
        { "\\\\", "\\" },
    };

    String retVal = s;
    for (String[] entry : replace) {
      retVal = retVal.replace(entry[0], entry[1]);
    }

    return retVal;
  }
}
