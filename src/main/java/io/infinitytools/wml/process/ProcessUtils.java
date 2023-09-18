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
package io.infinitytools.wml.process;

import org.tinylog.Logger;

import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/**
 * Collection of convenience methods for executing processes.
 */
public class ProcessUtils {
  /**
   * Returns the output as string from the specified command.
   * <p>
   * Program output is assumed to be encoded in {@code UTF-8}. The method waits 1000 ms for the program to complete
   * before it is forcefully terminated.
   * </p>
   *
   * @param command A string array containing the program and arguments.
   * @return A string containing the whole content of the output and error stream of the program. Returns {@code null}
   * if the program did not execute successfully.
   */
  public static ProcessOutput getProcessOutput(String... command) {
    return getProcessOutput(null, true, StandardCharsets.UTF_8, 1000L, command);
  }

  /**
   * Returns the output as string from the specified command.
   * <p>
   * Program output is assumed to be encoded in {@code UTF-8}. The method waits 1000 ms for the program to complete
   * before it is forcefully terminated.
   * </p>
   *
   * @param workingDir The working directory where the process should be invoked. Specify {@code null} to ignore.
   * @param command    A string array containing the program and arguments.
   * @return A string containing the whole content of the output and error stream of the program. Returns {@code null}
   * if the program did not execute successfully.
   */
  public static ProcessOutput getProcessOutput(Path workingDir, String... command) {
    return getProcessOutput(workingDir, true, StandardCharsets.UTF_8, 1000L, command);
  }

  /**
   * Returns the output as string from the specified command.
   *
   * @param workingDir   The working directory where the process should be invoked. Specify {@code null} to ignore.
   * @param includeError Whether to include the error stream in the output.
   * @param cs           Character set to convert process output data.
   * @param maxWaitMs    Max. number of milliseconds to wait for the program to complete execution before it is
   *                     forcefully terminated. Specify {@code -1L} to wait indefinitely.
   * @param command      A string array containing the program and arguments.
   * @return A string containing the whole output of the program. Returns {@code null} if the program does not execute
   * successfully.
   */
  public static ProcessOutput getProcessOutput(Path workingDir, boolean includeError, Charset cs, long maxWaitMs,
                                               String... command) {
    ProcessOutput retVal = null;

    if (cs == null) {
      cs = StandardCharsets.UTF_8;
    }

    if (workingDir != null) {
      if (!Files.isDirectory(workingDir)) {
        workingDir = workingDir.getParent();
      }
    }

    final SysProc sp = new SysProc(workingDir, includeError, cs, command);
    try {
      final Future<Integer> result = sp.execute();

      int exitCode = result.get(maxWaitMs, TimeUnit.MILLISECONDS);
      if (exitCode == 0) {
        retVal = sp.getOutput();
      }
    } catch (IOException | InterruptedException | ExecutionException | TimeoutException e) {
      Logger.error(e, "Error while getting process output");
    }

    return retVal;
  }
}
