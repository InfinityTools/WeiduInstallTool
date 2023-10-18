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
package io.infinitytools.wit;

import io.infinitytools.wit.gui.MainWindow;
import io.infinitytools.wit.net.AppClient;
import io.infinitytools.wit.net.AppServer;
import org.tinylog.Logger;

/**
 * Container class for the {@code main(String[]} method. Launches the main window of the application.
 */
public class WeiduInstallTool {
  public static void main(String[] args) {
    // checking single instance mode
    if (AppClient.executeArguments(args)) {
      Logger.debug("Application is already running.");
      Runtime.getRuntime().exit(0);
    }

    MainWindow.launch(MainWindow.class, args);

    // shutting down single instance server
    if (!AppServer.getInstance().stop()) {
      Logger.warn("Could not shut down application server");
    }

    // Terminate JVM without delay: workaround for macOS which keeps the app available for reuse, otherwise.
    Runtime.getRuntime().exit(0);
  }
}
