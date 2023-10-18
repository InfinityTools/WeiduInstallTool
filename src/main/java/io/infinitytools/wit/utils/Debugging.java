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
package io.infinitytools.wit.utils;

import org.tinylog.Logger;

/**
 * Collection of static methods for debugging and profiling needs.
 */
public class Debugging {
  /**
   * Supported temporal resolutions for timer methods.
   */
  public enum TimeFormat {
    NANOSECONDS, MICROSECONDS, MILLISECONDS, SECONDS
  }

  private static long timeBase = System.nanoTime();

  /**
   * Resets timer to current time.
   */
  public static synchronized void timerReset() {
    timeBase = System.nanoTime();
  }

  /**
   * Shows elapsed time in the desired resolution and resets timer.
   *
   * @param msg Display an optional message
   * @param fmt The temporal resolution of the elapsed time
   */
  public static synchronized void timerShow(String msg, TimeFormat fmt) {
    if (msg != null && !msg.isEmpty()) {
      Logger.debug("[" + msg + "] " + toTimeFormatString(fmt, System.nanoTime() - timeBase));
    } else {
      Logger.debug(toTimeFormatString(fmt, System.nanoTime() - timeBase));
    }
    timerReset();
  }

  /**
   * Returns elapsed time in the desired resolution and resets timer.
   *
   * @param fmt The temporal resolution of the elapsed time
   * @return The elapsed time in the specified resolution
   */
  public static synchronized long timerGet(TimeFormat fmt) {
    long time = toTimeFormat(fmt, System.nanoTime() - timeBase);
    timerReset();
    return time;
  }

  // ------------------------------ PRIVATE METHODS ------------------------------

  private static long toTimeFormat(TimeFormat fmt, long time) {
    return switch (fmt) {
      case MICROSECONDS -> time / 1000L;
      case MILLISECONDS -> time / 1000000L;
      case SECONDS -> time / 1000000000L;
      default -> time;
    };
  }

  private static String toTimeFormatString(TimeFormat fmt, long time) {
    return switch (fmt) {
      case NANOSECONDS -> toTimeFormat(fmt, time) + " ns";
      case MICROSECONDS -> toTimeFormat(fmt, time) + " µs";
      case MILLISECONDS -> toTimeFormat(fmt, time) + " ms";
      case SECONDS -> toTimeFormat(fmt, time) + " s";
    };
  }
}
