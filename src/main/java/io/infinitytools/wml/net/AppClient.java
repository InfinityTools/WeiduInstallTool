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

import org.tinylog.Logger;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.InetAddress;
import java.net.Socket;

/**
 * This class can be used to communicate with the application server to request specific actions or
 * states.
 */
public class AppClient implements AutoCloseable {
  /**
   * A convenience method that determines whether another app instance is already running.
   *
   * @param bringToFront Indicates whether the app should be made visible, brought to the top of the window stack
   *                     and receive window focus.
   * @return {@code true} if an instance of the application is already running, {@code false} otherwise.
   */
  public static boolean isAppRunning(boolean bringToFront) {
    boolean retVal = false;
    try (final AppClient client = new AppClient()) {
      retVal = client.ping(bringToFront);
    } catch (IllegalArgumentException | IOException e) {
      Logger.debug("Server not available: {}", e.getMessage());
    } catch (Exception e) {
      Logger.debug(e, "Client connecting to server");
    }

    return retVal;
  }

  /**
   * A convenience method that determines whether another app instance is already running and passes the given
   * arguments over.
   *
   * @param args List of arguments that are passed to a WeiDU process.
   * @return {@code true} if an instance of the application is already running and accepted the arguments.
   * {@code false} if there is no instance running or the app is currently running another process.
   */
  public static boolean executeArguments(String... args) {
    boolean retVal = false;

    try (final AppClient client = new AppClient()) {
      retVal = client.execute(args);
    } catch (IllegalArgumentException | IOException e) {
      Logger.debug("Server not available: {}", e.getMessage());
    } catch (Exception e) {
      Logger.debug(e, "Client connecting to server");
    }

    return retVal;
  }

  /**
   * A convenience method that determines whether another app instance is already running as a server and instructs it
   * to terminate the server state.
   *
   * @return {@code true} if an instance was running in server state, {@code false} otherwise.
   */
  public static boolean terminateAppServer() {
    boolean retVal = false;

    try (final AppClient client = new AppClient()) {
      retVal = client.terminate();
    } catch (IllegalArgumentException | IOException e) {
      Logger.debug("Server not available: {}", e.getMessage());
    } catch (Exception e) {
      Logger.debug(e, "Client connecting to server");
    }

    return retVal;
  }

  private final Socket clientSocket;
  private final PrintWriter writer;
  private final BufferedReader reader;

  public AppClient() throws Exception {
    // Determine first available port number
    Socket cs = null;
    Exception lastEx = null;
    for (int i = 0; true; i++) {
      try {
        int port = AppServer.getPort(i);
        cs = new Socket(InetAddress.getLoopbackAddress(), port);
        break;
      } catch (IllegalArgumentException e) {
        // port is out of range
        lastEx = e;
        break;
      } catch (IOException e) {
        lastEx = e;
      }
    }

    if (cs == null) {
      throw lastEx;
    }

    clientSocket = cs;
    writer = new PrintWriter(clientSocket.getOutputStream(), true);
    reader = new BufferedReader(new InputStreamReader((clientSocket.getInputStream())));
    Logger.debug("Client socket initialized: {}", clientSocket);
  }

  /**
   * Determines whether a server application is already running.
   *
   * @param bringToFront Indicates whether the server application should make the main window visible and receive
   *                     window focus.
   * @return {@code true} if the ping was successful, {@code false} otherwise.
   */
  public boolean ping(boolean bringToFront) {
    boolean retVal = false;

    try {
      final NetData ack = sendMessage(new NetData(NetData.Type.REQ_PING, Boolean.toString(bringToFront)));
      retVal = ack.getType() == NetData.Type.ACK_PING;
    } catch (IllegalArgumentException | IOException e) {
      Logger.debug(e, "Error sending message");
    }

    return retVal;
  }

  /**
   * Requests a WeiDU process execution with the specified arguments.
   *
   * @param args List of arguments to be passed to the WeiDU process.
   * @return {@code true} if the arguments were successfully passed to the server application and are ready
   * for execution, {@code false} otherwise.
   */
  public boolean execute(String... args) {
    boolean retVal = false;

    try {
      final NetData ack = sendMessage(new NetData(NetData.Type.REQ_EXEC, args));
      retVal = ack.getType() == NetData.Type.ACK_EXEC;
    } catch (IllegalArgumentException | IOException e) {
      Logger.debug(e, "Error sending message");
    }

    return retVal;
  }

  /**
   * Requests termination of the singleton state of the server application.
   *
   * @return {@code true} if a server application was running has terminated the singleton state,
   * {@code false} otherwise.
   */
  public boolean terminate() {
    boolean retVal = false;

    try {
      final NetData ack = sendMessage(new NetData(NetData.Type.REQ_TERM));
      retVal = ack.getType() == NetData.Type.ACK_TERM;
    } catch (IllegalArgumentException | IOException e) {
      Logger.debug(e, "Error sending message");
    }

    return retVal;
  }

  /**
   * Sends the specified {@link NetData} instance to the server and returns the server response.
   *
   * @param req Request to send to the server.
   * @return Response from the server.
   * @throws IOException              If an I/O error occurs.
   * @throws IllegalArgumentException If the response could not be evaluated.
   */
  private NetData sendMessage(NetData req) throws IOException, IllegalArgumentException {
    writer.println(req.toString());
    String ackMsg = reader.readLine();
    return NetData.decode(ackMsg);
  }

  @Override
  public void close() throws Exception {
    reader.close();
    writer.close();
    clientSocket.close();
  }
}
