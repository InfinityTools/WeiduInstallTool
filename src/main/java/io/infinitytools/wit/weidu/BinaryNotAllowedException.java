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
package io.infinitytools.wit.weidu;

import java.nio.file.Path;

/**
 * Thrown to indicate that the binary failed to pass validation checks such as whitelists or blacklists.
 */
public class BinaryNotAllowedException extends UnsupportedOperationException {
  private final Path binPath;

  public BinaryNotAllowedException(Path binPath) {
    super();
    this.binPath = binPath;
  }

  public BinaryNotAllowedException(Path binPath, String message) {
    super(message);
    this.binPath = binPath;
  }

  public BinaryNotAllowedException(Path binPath, String message, Throwable cause) {
    super(message, cause);
    this.binPath = binPath;
  }

  public BinaryNotAllowedException(Path binPath, Throwable cause) {
    super(cause);
    this.binPath = binPath;
  }

  /**
   * Returns the file {@link Path} associated with the exception.
   */
  public Path getBinPath() {
    return binPath;
  }
}
