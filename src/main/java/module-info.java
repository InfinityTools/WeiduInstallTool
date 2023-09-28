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
module io.infinitytools.wml {
  requires jdk.charsets;
  requires transitive javafx.controls;
  requires transitive javafx.graphics;
  requires javafx.fxml;
  requires org.apache.commons.text;
  requires org.json;
  requires org.tinylog.impl;
  requires org.tinylog.api;

  opens io.infinitytools.wml.gui to javafx.fxml;
  exports io.infinitytools.wml.gui;
  exports io.infinitytools.wml.mod;
  exports io.infinitytools.wml.mod.info;
  exports io.infinitytools.wml.mod.ini;
}
