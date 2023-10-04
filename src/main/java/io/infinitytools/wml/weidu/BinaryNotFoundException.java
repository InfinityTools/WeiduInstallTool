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
package io.infinitytools.wml.weidu;

/**
 * Thrown to indicate that a valid WeiDU binary could not be found.
 */
public class BinaryNotFoundException extends UnsupportedOperationException {
  public BinaryNotFoundException() {
    super();
  }

  public BinaryNotFoundException(String message) {
    super(message);
  }

  public BinaryNotFoundException(String message, Throwable cause) {
    super(message, cause);
  }

  public BinaryNotFoundException(Throwable cause) {
    super(cause);
  }
}
