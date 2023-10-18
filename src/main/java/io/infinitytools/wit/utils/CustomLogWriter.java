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
package io.infinitytools.wit.utils;

import javafx.application.Platform;
import org.tinylog.Level;
import org.tinylog.core.LogEntry;
import org.tinylog.core.LogEntryValue;
import org.tinylog.pattern.FormatPatternParser;
import org.tinylog.pattern.Token;
import org.tinylog.writers.AbstractWriter;

import java.io.PrintStream;
import java.util.*;
import java.util.List;
import java.util.concurrent.ConcurrentLinkedDeque;

/**
 * Writer for handling log entries.
 * <p>
 * Entries are written to a system stream and buffered internally for on-demand output.
 * </p>
 */
public class CustomLogWriter extends AbstractWriter {
  private static final String NEW_LINE = System.getProperty("line.separator", "\n");
  private static final String DEFAULT_FORMAT_PATTERN =
      "{date} [{thread}] {class}.{method}()" + NEW_LINE + "{level}: {message}";

  private static final Comparator<LogEntry> LOG_ENTRY_COMPARATOR =
      (a, b) -> {
        final long diff = a.getTimestamp().calcDifferenceInNanoseconds(b.getTimestamp());
        if (diff < 0) {
          return -1;
        } else if (diff > 0) {
          return 1;
        } else {
          return 0;
        }
      };

  /**
   * Storage for all created writer instances.
   */
  private static CustomLogWriter INSTANCE;

  /**
   * Returns the current log writer instance.
   *
   * @return {@link CustomLogWriter} instance used by the logger. Returns {@code null} if the logger has not yet
   * been initialized.
   */
  public static CustomLogWriter getInstance() {
    return INSTANCE;
  }

  private final ConcurrentLinkedDeque<LogEntry> entries = new ConcurrentLinkedDeque<>();

  private final ConcurrentLinkedDeque<LogEventHandler> eventHandlers = new ConcurrentLinkedDeque<>();
  private final Level errorLevel;
  private final PrintStream printer;
  private final Token token;

  public CustomLogWriter(Map<String, String> properties) {
    super(properties);

    // Consider log entries BELOW the specified level
    errorLevel = switch (properties.getOrDefault("level", "trace")) {
      case "off" -> Level.values()[0];
      case "error" -> Level.values()[Level.ERROR.ordinal() + 1];
      case "warn" -> Level.values()[Level.WARN.ordinal() + 1];
      case "info" -> Level.values()[Level.INFO.ordinal() + 1];
      case "debug" -> Level.values()[Level.DEBUG.ordinal() + 1];
      default -> Level.values()[Level.TRACE.ordinal() + 1];
    };

    printer = switch (properties.getOrDefault("stream", "auto")) {
      case "err" -> System.err;
      case "out" -> System.out;
      default -> null;
    };

    token = new FormatPatternParser(getStringValue("exception")).parse(DEFAULT_FORMAT_PATTERN + NEW_LINE);

    INSTANCE = this;
  }

  /**
   * Returns the tracked log levels as {@link Set}.
   *
   * @return {@link Set} of tracked log levels.
   */
  public Set<Level> getLevels() {
    final Set<Level> retVal = new HashSet<>();

    if (errorLevel.ordinal() > 0) {
      retVal.addAll(Arrays.asList(Level.values()).subList(errorLevel.ordinal() - 1, Level.values().length));
    }

    return retVal;
  }

  /**
   * Clears all log entries collected by this instance so far.
   */
  public void reset() {
    entries.clear();

    try {
      Platform.runLater(this::fireClearLogEvent);
    } catch (IllegalStateException ignored) {
      // JavaFX not yet initialized
    }
  }

  /**
   * Returns all log entries of the specified levels.
   *
   * @param levels Log levels to consider. Specify {@code null} or empty set to return everything.
   * @return A list of log entries filtered by the specified level(s), sorted by date/time.
   */
  public List<LogEntry> getFiltered(Set<Level> levels) {
    final Set<Level> levels2 = new HashSet<>();
    if (levels != null) {
      levels2.addAll(levels);
    }
    if (levels2.isEmpty()) {
      for (int i = 0; i < errorLevel.ordinal(); i++) {
        if (i < Level.values().length) {
          levels2.add(Level.values()[i]);
        }
      }
    }

    final List<LogEntry> retVal = new ArrayList<>(entries.stream().filter(e -> levels2.contains(e.getLevel())).toList());
    retVal.sort(LOG_ENTRY_COMPARATOR);

    return retVal;
  }

  @Override
  public Collection<LogEntryValue> getRequiredLogEntryValues() {
    Collection<LogEntryValue> logEntryValues = token.getRequiredLogEntryValues();
    logEntryValues.add(LogEntryValue.LEVEL);
    return logEntryValues;
  }

  @Override
  public void write(LogEntry logEntry) {
    entries.add(logEntry);

    try {
      Platform.runLater(() -> fireAddLogEvent(logEntry));
    } catch (IllegalStateException ignored) {
      // JavaFX not yet initialized
    }

    if (logEntry.getLevel().ordinal() < errorLevel.ordinal()) {
      getPrinter(logEntry.getLevel()).print(render(logEntry));
    }
  }

  @Override
  public void flush() {
    System.err.flush();
  }

  @Override
  public void close() throws Exception {
    // nothing to do...
  }

  /**
   * Renders a log entry as string.
   *
   * @param logEntry Log entry to render.
   * @return Rendered log entry.
   */
  public String render(LogEntry logEntry) {
    final StringBuilder builder = new StringBuilder(1024);
    builder.setLength(0);
    token.render(logEntry, builder);
    return builder.toString();
  }

  /**
   * Registers the specified event handler for {@link LogEvent}s.
   */
  public void addEventHandler(LogEventHandler handler) {
    if (handler != null) {
      eventHandlers.add(handler);
    }
  }

  /**
   * Unregisters the specified event handler.
   */
  public boolean removeEventHandler(LogEventHandler handler) {
    if (handler != null) {
      return eventHandlers.remove(handler);
    }
    return false;
  }

  /**
   * Returns an unmodifiable collection of all registered event handlers for {@link LogEvent}s.
   */
  public Collection<LogEventHandler> getEventHandlers() {
    return Collections.unmodifiableCollection(eventHandlers);
  }

  /**
   * Fires a {@link LogEvent} for the specified {@link LogEntry} of type 'ADD'.
   */
  private void fireAddLogEvent(LogEntry logEntry) {
    if (!eventHandlers.isEmpty()) {
      final LogEvent event = new LogEvent(LogEvent.Type.ADD, this, logEntry);
      eventHandlers.forEach(handler -> handler.handle(event));
    }
  }

  /**
   * Fires a {@link LogEvent} of type 'CLEAR'.
   */
  private void fireClearLogEvent() {
    if (!eventHandlers.isEmpty()) {
      final LogEvent event = new LogEvent(LogEvent.Type.CLEAR, this, null);
      eventHandlers.forEach(handler -> handler.handle(event));
    }
  }

  /**
   * Returns the {@link PrintStream} instance to write the log entry to.
   *
   * @param level Level of the log entry.
   * @return A {@link PrintStream} instance.
   */
  private PrintStream getPrinter(Level level) {
    if (printer != null) {
      return printer;
    } else if (level != null) {
      return (level == Level.ERROR) ? System.err : System.out;
    } else {
      return System.out;
    }
  }
}
