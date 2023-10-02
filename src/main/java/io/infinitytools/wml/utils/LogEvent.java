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
package io.infinitytools.wml.utils;

import org.tinylog.core.LogEntry;

/**
 * Event that informs about a new log entry emitted by the logger.
 *
 * @param type     The log event {@link Type}.
 * @param writer   {@link CustomLogWriter} instance that emitted the log entry.
 * @param logEntry {@link LogEntry} emitted by the logger.
 */
public record LogEvent(Type type, CustomLogWriter writer, LogEntry logEntry) {
  public enum Type {
    /**
     * Indicates that a new log entry has been added that can be retrieved by {@link #logEntry()}.
     */
    ADD,
    /**
     * Indicates that all recorded entries have been removed from the log buffer.
     * <p>
     * {@link #logEntry() will always return {@code null} for this event type.
     * </p>
     */
    CLEAR,
  }
}
