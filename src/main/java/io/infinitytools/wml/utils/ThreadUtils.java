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
package io.infinitytools.wml.utils;

import javafx.application.Platform;
import org.tinylog.Logger;

import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

/**
 * A utility class that provides quick and easy access to thread pools.
 */
public class ThreadUtils {
  /**
   * Ensures that the specified action is executed on the JavaFX UI thread. Unlike {@link Platform#runLater(Runnable)}
   * this action may be executed immediately if the current thread is the JavaFX UI thread.
   *
   * @param action Action to perform.
   * @throws NullPointerException if {@code action} is {@code null}.
   */
  public static void runUiAction(Runnable action) {
    if (action == null) {
      throw new NullPointerException("Action argument is null");
    }

    if (Platform.isFxApplicationThread()) {
      action.run();
    } else {
      Platform.runLater(action);
    }
  }

  /**
   * Creates a thread pool with a pool size depending on the number of available CPU cores.<br>
   * <br>
   * <b>numThreads:</b> Number of available CPU cores.<br>
   * <b>maxQueueSize:</b> 2 x {@code numThreads}.<br>
   *
   * @return A ThreadPoolExecutor instance.
   */
  public static ThreadPoolExecutor createThreadPool() {
    int numThreads = Runtime.getRuntime().availableProcessors();
    return createThreadPool(numThreads, numThreads * 2);
  }

  /**
   * Creates a thread pool with the specified parameters.
   *
   * @param numThreads   Max. number of parallel threads to execute. Must be >= 1.
   * @param maxQueueSize Max. size of the working queue. Must be >= {@code numThreads}.
   * @return A ThreadPoolExecutor instance.
   */
  public static ThreadPoolExecutor createThreadPool(int numThreads, int maxQueueSize) {
    numThreads = Math.max(1, numThreads);
    maxQueueSize = Math.max(numThreads, maxQueueSize);
    return new ThreadPoolExecutor(numThreads, maxQueueSize, 0L, TimeUnit.MILLISECONDS,
        new ArrayBlockingQueue<>(maxQueueSize));
  }

  /**
   * Helper routine which can be used to check or block execution of new threads while the blocking queue is full.
   *
   * @param executor  The executor to query.
   * @param block     Specify {@code true} to block execution as long as the queue is full.
   * @param maxWaitMs Specify max. time to block queue, in milliseconds. Specify -1 to block indefinitely.
   * @return {@code true} if queue is ready for new elements, {@code false} otherwise.
   */
  public static boolean isQueueReady(ThreadPoolExecutor executor, boolean block, int maxWaitMs) {
    if (executor != null) {
      if (block) {
        if (maxWaitMs < 0) {
          maxWaitMs = Integer.MAX_VALUE;
        }
        int curWaitMs = 0;
        while (curWaitMs < maxWaitMs && executor.getQueue().size() > executor.getCorePoolSize()) {
          try {
            Thread.sleep(1);
          } catch (InterruptedException e) {
            Logger.trace("Waiting loop interrupted", e);
          }
          curWaitMs++;
        }
      }
      return executor.getQueue().size() <= executor.getCorePoolSize();
    }
    return false;
  }
}
