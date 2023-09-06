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

/**
 * Event class that indicates that some amount of output data from the process is available.
 */
public class SysProcOutputEvent extends SysProcEvent {
  private final String text;

  public SysProcOutputEvent(SysProc source, String text) {
    super(source);
    this.text = text;
  }

  /** Returns the text content that was produced by the process since the last event of this type. */
  public String getText() {
    return text;
  }
}
