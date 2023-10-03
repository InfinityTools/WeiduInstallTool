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
import io.infinitytools.wml.utils.R;
import io.infinitytools.wml.utils.ThreadUtils;
import javafx.fxml.FXMLLoader;
import javafx.scene.Cursor;
import javafx.scene.Scene;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.Window;
import javafx.stage.WindowEvent;
import org.tinylog.Logger;

import java.io.IOException;
import java.io.PrintStream;
import java.io.PrintWriter;
import java.net.URL;
import java.util.Objects;
import java.util.concurrent.*;

/**
 * A modal dialog that displays progress feedback for a user-defined operation.
 * It behaves similar to the {@code ProgressMonitor} class of the Java Swing Framework.
 */
public class ProgressDialog extends Stage implements AutoCloseable {
  /**
   * Path to the FXML definition file for this window.
   */
  private static final URL FXML_FILE = ProgressDialog.class.getResource("progress.fxml");

  /**
   * Executes a given task and shows a modal dialog that informs about the progress.
   * <p>
   * Closing the progress dialog signals the task to terminate. However, actual signal handling requires cooperation
   * if the task itself. This method blocks until the task is completed.
   * </p>
   *
   * @param owner   The dialog owner.
   * @param title   The dialog title.
   * @param message A descriptive message that is shown while the task is executed.
   * @param task    The task to be perform. The task is performed in their own thread.
   * @return {@code true} if the task completed successfully. {@code false} if the task was cancelled by the user
   * or prematurely terminated.
   * @throws NullPointerException if {@code task} is null.
   * @throws IOException          if the progress dialog could not be initialized.
   * @throws TaskException        if the task was prematurely terminated by an error. The actual exception thrown by
   *                              the task is encapsulated in the {@code TaskException}.
   */
  public static boolean performTask(Window owner, String title, String message, UpdateTask task)
      throws NullPointerException, IOException, TaskException {
    try (final ProgressDialog dialog = new ProgressDialog(owner, title, message, task)) {
      dialog.showAndWait();

      final Cursor defCursor = (owner != null) ? owner.getScene().getCursor() : null;
      try {
        if (owner != null) {
          ThreadUtils.runUiAction(() -> owner.getScene().setCursor(Cursor.WAIT));
        }
        dialog.getScene().setCursor(Cursor.WAIT);
        final Exception ex = dialog.getResult().get();
        if (ex != null) {
          throw new TaskException(ex);
        }
      } catch (ExecutionException e) {
        Logger.error(e, "Performed task was aborted");
        throw new TaskException(e.getCause());
      } catch (CancellationException | InterruptedException e) {
        Logger.info(e, "Performed task cancelled by user");
        return false;
      } finally {
        if (owner != null) {
          ThreadUtils.runUiAction(() -> owner.getScene().setCursor(defCursor));
        }
      }

      return true;
    }
  }

  private final UpdateTask task;

  private ProgressWindowController controller;
  private ExecutorService taskExecutor;
  private Future<Exception> taskResult;
  private boolean closingAllowed = true;
  private int terminationState;

  private ProgressDialog(Window owner, String title, String message, UpdateTask task)
      throws NullPointerException, IOException {
    super();
    this.task = Objects.requireNonNull(task, "Task is null");

    if (owner != null) {
      initOwner(owner);
    }
    setTitle(title != null ? title : R.get("ui.progress.title"));
    initModality(Objects.isNull(owner) ? Modality.APPLICATION_MODAL : Modality.WINDOW_MODAL);

    init(message);
  }

  /**
   * Returns the dialog's description message.
   *
   * @return Message as string.
   */
  public String getMessage() {
    return controller.messageLabel.getText();
  }

  /**
   * Returns the current progression note.
   *
   * @return Note as string.
   */
  public String getNote() {
    return controller.noteLabel.getText();
  }

  /**
   * Sets a new progression note.
   *
   * @param note Note as string.
   */
  public void setNote(String note) {
    final String s = (note != null) ? note : "";
    ThreadUtils.runUiAction(() -> controller.noteLabel.setText(s));
  }

  /**
   * Returns the current progress as a normalized value between 0.0 and 1.0.
   *
   * @return progress as double value.
   */
  public double getProgress() {
    return controller.updateProgress.getProgress();
  }

  /**
   * Sets the current progress as a normalized value between 0.0 and 1.0.
   *
   * @param value new progress as double value in the range [0.0, 1.0] where 0.0 is 0% and 1.0 is 100%.
   */
  public void setProgress(double value) {
    ThreadUtils.runUiAction(() -> controller.updateProgress.setProgress(value));
  }

  /**
   * Returns whether closing the dialog (via button or window controls) can close the dialog.
   */
  public boolean isClosingAllowed() {
    return closingAllowed;
  }

  /**
   * Specifies whether closing the dialog (via button or window controls) can close the dialog.
   */
  public void setClosingAllowed(boolean allowed) {
    if (allowed != closingAllowed) {
      ThreadUtils.runUiAction(() -> {
        closingAllowed = allowed;
        controller.closeButton.setDisable(!closingAllowed);
      });
    }
  }

  /**
   * Returns whether the dialog was closed by the user before completion.
   */
  public boolean isCancelled() {
    return taskResult.isCancelled();
  }

  /**
   * This method has been repurposed to close the resources used to execute the user-defined task.
   * Call {@link Window#hide()} to perform the original behavior.
   */
  @Override
  public void close() {
    if (taskExecutor != null) {
      taskExecutor.close();
      taskExecutor = null;
    }
  }

  private void setCancelled() {
    if (!taskResult.isDone()) {
      taskResult.cancel(true);
    }
  }

  private void forceClose(boolean cooperative) {
    if (taskExecutor != null) {
      if (cooperative) {
        taskExecutor.shutdown();
      } else {
        taskExecutor.shutdownNow();
        hide();
      }
    }
  }

  /**
   * Returns the completion state of the user task as an {@link Exception} encapsulated in a {@link Future} object.
   */
  private Future<Exception> getResult() {
    return taskResult;
  }

  private void executeTask() {
    if (taskExecutor == null) {
      taskExecutor = Executors.newVirtualThreadPerTaskExecutor();
      taskResult = taskExecutor.submit(() -> {
        Exception exception = null;
        try {
          this.task.perform(ProgressDialog.this);
        } catch (Exception e) {
          exception = e;
        }
        ThreadUtils.runUiAction(ProgressDialog.this::hide);
        return exception;
      });
    }
  }

  /**
   * Positions the dialog at the center of the parent window.
   */
  private void onWindowShown(WindowEvent event) {
    if (getOwner() != null) {
      final double x = getOwner().getX() + (getOwner().getWidth() - this.getWidth()) / 2.0;
      final double y = getOwner().getY() + (getOwner().getHeight() - this.getHeight()) / 2.0;
      setX(x);
      setY(y);
    } else {
      centerOnScreen();
    }
    if (getScene().getRoot() instanceof Region r) {
      if (r.getWidth() > 0) {
        setMinWidth(r.getWidth());
      }
    }
    setMinHeight(getHeight());

    executeTask();
  }

  private void onWindowHidden(WindowEvent event) {
    if (!taskResult.isDone()) {
      taskResult.cancel(true);
    }
  }

  private void onCloseRequest(WindowEvent event) {
    if (isClosingAllowed()) {
      switch (terminationState) {
        case 0 -> setCancelled();
        case 1 -> forceClose(true);
        default -> forceClose(false);
      }
      terminationState++;
    } else {
      event.consume();
    }
  }

  private void init(String message) throws NullPointerException, IOException {
    final FXMLLoader loader = new FXMLLoader(FXML_FILE, R.getBundle());
    final VBox vbox = loader.load();
    controller = loader.getController();

    if (message != null) {
      controller.messageLabel.setText(message);
    }

    if (getOwner() instanceof Stage stage) {
      // setting dialog to an appropriate dimension
      final double width = stage.getMinWidth() / 2.0;
      vbox.setMinWidth(width);
      vbox.setPrefWidth(width);
    }

    final Scene scene = new CustomScene(vbox);
    setScene(scene);

    // assigning application icon
    getIcons().addAll(Icons.Icon.getImages());

    controller.closeButton.setOnAction(event -> setCancelled());

    setOnCloseRequest(this::onCloseRequest);
    setOnShown(this::onWindowShown);
    setOnHidden(this::onWindowHidden);
  }

  /**
   * A functional interface that is used to perform a user-defined task.
   */
  @FunctionalInterface
  public interface UpdateTask {
    /**
     * @param dialog the {@link ProgressDialog} instance associated with this task.
     * @throws Exception thrown to signal that the task terminated unsuccessfully.
     *                   Exception will be encapsulated in a {@link TaskException} and forwarded to the
     *                   {@link ProgressDialog#performTask(Window, String, String, UpdateTask)} method.
     */
    void perform(ProgressDialog dialog) throws Exception;
  }

  /**
   * Thrown to indicate that the task terminated with an error.
   * <p>
   * {@link TaskException#getCause()} returns the actual exception thrown by the task.
   */
  public static class TaskException extends Exception {
    public TaskException(Throwable cause) {
      super(Objects.requireNonNull(cause, "Cause is null"));
    }

    @Override
    public String getLocalizedMessage() {
      return getCause().getLocalizedMessage();
    }

    @Override
    public String getMessage() {
      return getCause().getMessage();
    }

    @Override
    public StackTraceElement[] getStackTrace() {
      return getCause().getStackTrace();
    }

    @Override
    public void printStackTrace() {
      getCause().printStackTrace();
    }

    @Override
    public void printStackTrace(PrintStream s) {
      getCause().printStackTrace(s);
    }

    @Override
    public void printStackTrace(PrintWriter s) {
      getCause().printStackTrace(s);
    }

    @Override
    public String toString() {
      return getCause().toString();
    }
  }
}
