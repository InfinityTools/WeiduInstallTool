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
 * Event class that indicates a change in the process execution.
 */
public class SysProcChangeEvent extends SysProcEvent {
  /** Defines specific change types. */
  public enum Type {
    /** This type indicates that execution of the process has started. */
    Started,
    /** This type indicates that the process has terminated. */
    Terminated,
  }

  private final SysProcChangeEvent.Type type;

  public SysProcChangeEvent(SysProc source, SysProcChangeEvent.Type type) {
    super(source);
    this.type = type;
  }

  /** Returns the change type of the process. */
  public SysProcChangeEvent.Type getType() {
    return type;
  }
}
