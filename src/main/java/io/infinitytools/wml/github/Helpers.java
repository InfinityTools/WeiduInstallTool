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
package io.infinitytools.wml.github;

import org.tinylog.Logger;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.*;
import java.util.NoSuchElementException;

/**
 * A collection of constants and static methods to help managing GitHub API data.
 */
public class Helpers {
  /**
   * The default timeout value for connecting to the host, in milliseconds.
   */
  public static final int DEFAULT_CONNECT_TIMEOUT = 5000;
  /**
   * The default timeout value for reading data from the host, in milliseconds.
   */
  public static final int DEFAULT_READ_TIMEOUT = 5000;

  /**
   * Retrieves a JSON structure from the specified {@link URI} and returns it as a String.
   * The method uses timeout values defined by {@link #DEFAULT_CONNECT_TIMEOUT} and {@link #DEFAULT_READ_TIMEOUT}.
   *
   * @param uri {@link URI} to get the JSON structure from.
   * @return JSON data as string.
   * @throws IOException If the method failed to retrieve data from the server.
   */
  public static String requestJSON(URI uri) throws IOException {
    return requestJSON(uri, DEFAULT_CONNECT_TIMEOUT, DEFAULT_READ_TIMEOUT);
  }

  /**
   * Retrieves a JSON structure from the specified {@link URI} and returns it as a String.
   * Only http and https protocols are supported.
   *
   * @param uri            {@link URI} to get the JSON structure from.
   * @param connectTimeOut Timeout value, in milliseconds, for opening the connection.
   * @param readTimeOut    Timeout value, in milliseconds, for reading data from the connection.
   * @return JSON data as string.
   * @throws IllegalArgumentException if the {@code uri} uses an unsupported protocol.
   * @throws IOException              If the method failed to retrieve data from the server.
   */
  public static String requestJSON(URI uri, int connectTimeOut, int readTimeOut)
      throws IllegalArgumentException, IOException {
    final URL url = uri.toURL();
    if (!url.getProtocol().contains("http")) {
      throw new IllegalArgumentException("Unsupported protocol: " + url.getProtocol());
    }

    // detecting system proxy
    Proxy proxy = null;
    try {
      proxy = ProxySelector.getDefault().select(URI.create(url.getProtocol() + "://" + url.getHost())).iterator().next();
    } catch (IllegalArgumentException | NoSuchElementException e) {
      Logger.error(e, "Error getting system proxy settings");
    }

    final HttpURLConnection con;
    if (proxy != null) {
      con = (HttpURLConnection) url.openConnection(proxy);
    } else {
      con = (HttpURLConnection) url.openConnection();
    }

    try {
      con.setRequestMethod("GET");
      con.setRequestProperty("Content-Type", "application/json");
      con.setConnectTimeout(connectTimeOut);
      con.setReadTimeout(readTimeOut);

      int status = con.getResponseCode();

      if (status != HttpURLConnection.HTTP_OK) {
        String msg = readStream((status > 299) ? con.getErrorStream() : con.getInputStream());
        throw new IOException(String.format("Could not connect to host (%s). Status: %d. Message: %s.",
            url.getHost(), status, msg));
      }

      return readStream(con.getInputStream());
    } finally {
      con.disconnect();
    }
  }

  /**
   * Reads all available data from the given input stream and returns it as a string.
   *
   * @param inputStream {@link InputStream} instance to read data from.
   * @return A string converted from the {@code inputStream} data.
   * @throws IOException If an I/O error occurs.
   */
  private static String readStream(InputStream inputStream) throws IOException {
    final StringBuilder sb = new StringBuilder();
    try (final BufferedReader br = new BufferedReader(new InputStreamReader(inputStream))) {
      String line;
      while ((line = br.readLine()) != null) {
        sb.append(line);
      }
    }
    return sb.toString();
  }
}
