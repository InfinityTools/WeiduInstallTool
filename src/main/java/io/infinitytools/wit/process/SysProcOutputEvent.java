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
package io.infinitytools.wit.process;

/**
 * Event class that indicates that some amount of output data from the process is available.
 */
public class SysProcOutputEvent extends SysProcEvent {
  private final byte[] output;

  public SysProcOutputEvent(SysProc source, byte[] output) {
    super(source);
    this.output = output;
  }

  /**
   * Returns the raw byte data that was produced by the process since the last event of this type.
   */
  public byte[] getData() {
    return output;
  }
}
