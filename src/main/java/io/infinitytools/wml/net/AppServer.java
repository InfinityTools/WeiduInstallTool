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
package io.infinitytools.wml.net;

import io.infinitytools.wml.gui.MainWindow;
import io.infinitytools.wml.utils.R;
import io.infinitytools.wml.utils.Utils;
import javafx.application.Platform;
import org.tinylog.Logger;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;

/**
 * This class can be used to set up a global server socket for the app.
 */
public class AppServer implements Runnable {
  /**
   * Initial default port for the server socket.
   */
  private static final int PORT = 50505;

  /**
   * Max. port number for the server socket.
   */
  private static final int PORT_MAX = PORT + 10;

  private static final AppServer instance = new AppServer();

  /**
   * Helper method to determine the right port.
   *
   * @param attempt Number of the current attempt to receive a port.
   * @return Port number.
   * @throws IllegalArgumentException on underflow or if the number of attempts exceeds a certain threshold.
   */
  public static int getPort(int attempt) throws IllegalArgumentException {
    int port = PORT + attempt;
    if (port < PORT || port > PORT_MAX) {
      throw new IllegalArgumentException(String.format("Port outside of supported range: [%d, %d]", PORT, PORT_MAX));
    }
    return port;
  }

  /**
   * Provides access to the singleton instance of the {@link AppServer}.
   */
  public static AppServer getInstance() {
    return instance;
  }

  private ServerSocket serverSocket;

  private AppServer() {
  }

  /**
   * Returns whether a server is currently running.
   *
   * @param global Indicates whether to also check that a global server is running.
   * @return {@code true} if a server is running, {@code false} otherwise.
   */
  public boolean isRunning(boolean global) {
    boolean retVal = (serverSocket != null);

    if (global && !retVal) {
      retVal = AppClient.isAppRunning(false);
    }

    return retVal;
  }

  /**
   * Starts the server task. Does nothing if a server is already running.
   *
   * @throws IOException if an I/O error occurs when opening the socket.
   */
  public void start() throws Exception {
    if (isRunning(true)) {
      return;
    }

    // Determine first available port number
    ServerSocket ss = null;
    Exception lastEx = null;
    for (int i = 0; true; i++) {
      try {
        final int port = getPort(i);
        // limit server to loopback connections only
        ss = new ServerSocket(port, 20, InetAddress.getLoopbackAddress());
        break;
      } catch (IllegalArgumentException e) {
        // port is out of range
        lastEx = e;
        break;
      } catch (IOException e) {
        lastEx = e;
      }
    }

    if (ss == null) {
      throw lastEx;
    }

    serverSocket = ss;
    Logger.debug("Server socket initialized: {}", serverSocket);

    Thread.ofVirtual().start(this);
  }

  /**
   * Signals the current server to stop running. Does nothing if the server has already stopped.
   *
   * @return {@code true} if the server has been shut down successfully or was not running at all. Returns {@code false}
   * otherwise
   */
  public boolean stop() {
    if (!isRunning(false)) {
      return true;
    }

    // Signal the server to shut down
    return AppClient.terminateAppServer();
  }

  @Override
  public void run() {
    boolean terminate = false;
    while (!terminate) {
      try (final Socket clientSocket = serverSocket.accept()) {
        Logger.debug("Client connected: {}", clientSocket);
        final PrintWriter out = new PrintWriter(clientSocket.getOutputStream(), true);
        final BufferedReader in = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
        String inData = in.readLine();

        NetData nd = null;
        try {
          nd = NetData.decode(inData);
        } catch (Exception e) {
          Logger.debug(e, "Invalid data received (ignored)");
        }

        if (nd != null) {
          try {
            switch (nd.getType()) {
              case REQ_PING -> {
                final boolean bringToFront = Boolean.parseBoolean(nd.getContent().getFirst());
                Logger.debug("REQ_PING received: bringToFront={}", bringToFront);
                out.println(NetData.encode(NetData.Type.ACK_PING));
                if (bringToFront) {
                  Platform.runLater(() -> MainWindow.getInstance().restoreWindow());
                }
              }
              case REQ_EXEC -> {
                final boolean isRunning = MainWindow.getInstance().isProcessRunning();
                out.println(NetData.encode(NetData.Type.ACK_EXEC, Boolean.toString(isRunning)));
                Logger.debug("REQ_EXEC received: isRunning={}, commands={}", isRunning, nd.getContent());
                final NetData nd2 = nd;
                Platform.runLater(() -> {
                  MainWindow.getInstance().restoreWindow();
                  if (!nd2.getContent().isEmpty()) {
                    if (isRunning) {
                      Utils.showErrorDialog(MainWindow.getInstance().getStage(), R.ERROR(),
                          R.get("ui.main.dragdrop.openFile.header"),
                          R.get("ui.main.dragdrop.openFile.processRunning.content"));
                    } else {
                      try {
                        MainWindow.getInstance().restart(nd2.getContent().toArray(new String[0]));
                      } catch (UnsupportedOperationException e) {
                        Logger.debug(e, "Main window restart");
                      }
                    }
                  }
                });
              }
              case REQ_TERM -> {
                Logger.debug("REQ_TERM received");
                terminate = true;
                out.println(NetData.encode(NetData.Type.ACK_TERM));
              }
            }
          } catch (IllegalArgumentException e) {
            Logger.warn(e, "Illegal data structure assembled");
          }
        }
      } catch (SocketException e) {
        Logger.debug(e, "Termination requested");
        break;
      } catch (IOException e) {
        Logger.warn(e, "Connecting with client socket");
        break;
      }
    }

    Logger.debug("Shutting down server socket");
    try {
      serverSocket.close();
    } catch (IOException e) {
      Logger.debug(e, "Closing server socket");
    }

    serverSocket = null;
  }
}
