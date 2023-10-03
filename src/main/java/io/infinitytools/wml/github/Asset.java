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

import org.json.JSONException;
import org.json.JSONObject;
import org.tinylog.Logger;

import java.io.IOException;
import java.net.URI;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Iterator;
import java.util.Objects;

/**
 * This class handles data from the GitHub API Asset structure.
 */
public class Asset implements Comparable<Asset> {
  /**
   * Assembles a full URI for the GitHub Asset API from the specified parameters.
   *
   * @param owner  The GitHub account owner (user or organization).
   * @param repo   Name of the repository.
   * @param id     Unique asset identifier.
   * @param params additional parameters for the GitHub API. Each parameter is added as an additional path element to
   *               the returned URI.
   * @return {@link URI} of the assembled GitHub API URL.
   */
  private static URI assembleUrl(String owner, String repo, long id, String... params) {
    owner = URLEncoder.encode(owner, StandardCharsets.UTF_8);
    repo = URLEncoder.encode(repo, StandardCharsets.UTF_8);
    params = Arrays.stream(params).map(s -> URLEncoder.encode(s, StandardCharsets.UTF_8)).toArray(String[]::new);

    final String path;
    if (params.length > 0) {
      path = String.format("/repos/%s/%s/releases/assets/%d/%s", owner, repo, id, String.join("/", params));
    } else {
      path = String.format("/repos/%s/%s/releases/assets/%d", owner, repo, id);
    }
    return Release.GITHUB_API.resolve(path);
  }

  private long id;
  private String url;
  private String nodeId;
  private String name;
  private String label;
  private User uploader;
  private String contentType;
  private String state;
  private long size;
  private long downloadCount;
  private String createdAt;
  private String updatedAt;
  private String browserDownloadUrl;

  /**
   * Used internally to create a new {@code Asset} instance from the specified JSON data.
   *
   * @param jsonAsset JSON structure containing information about a specific asset.
   * @throws JSONException if the JSON structure could not be parsed.
   */
  Asset(JSONObject jsonAsset) throws JSONException {
    init(jsonAsset);
  }

  /**
   * Creates a new {@code Asset} instance with the specified parameters and initializes it with data
   * retrieved from the GitHub server, using timeout values defined by {@link Helpers#DEFAULT_CONNECT_TIMEOUT} and
   * {@link Helpers#DEFAULT_READ_TIMEOUT}.
   *
   * @param owner User or organization name owning the repository.
   * @param repo  Name of the repository.
   * @param id    Unique identifier of the asset.
   * @throws IOException   if the method failed to retrieve data from the GitHub server.
   * @throws JSONException if the JSON structure could not be parsed.
   */
  public Asset(String owner, String repo, long id) throws IOException, JSONException {
    this(owner, repo, id, Helpers.DEFAULT_CONNECT_TIMEOUT, Helpers.DEFAULT_READ_TIMEOUT);
  }

  /**
   * Creates a new {@code Asset} instance with the specified parameters and initializes it with data
   * retrieved from the GitHub server.
   *
   * @param owner          User or organization name owning the repository.
   * @param repo           Name of the repository.
   * @param id             Unique identifier of the asset.
   * @param connectTimeOut Timeout value, in milliseconds, for opening the connection to the GitHub server.
   * @param readTimeOut    Timeout value, in milliseconds, for reading data from the connection.
   * @throws IOException   if the method failed to retrieve data from the GitHub server.
   * @throws JSONException if the JSON structure could not be parsed.
   */
  public Asset(String owner, String repo, long id, int connectTimeOut, int readTimeOut)
      throws IOException, JSONException {
    final URI uri = assembleUrl(owner, repo, id);
    final String s = Helpers.requestJSON(uri, connectTimeOut, readTimeOut);
    final JSONObject json = new JSONObject(s);
    init(json);
  }

  /**
   * Returns the asset id.
   */
  public long getId() {
    return id;
  }

  /**
   * Returns the GitHub API URL for this asset.
   */
  public String getUrl() {
    return url;
  }

  /**
   * Returns the node id string.
   */
  public String getNodeId() {
    return nodeId;
  }

  /**
   * Returns the asset filename.
   */
  public String getName() {
    return name;
  }

  /**
   * Returns an optional label (may be {@code null}).
   */
  public String getLabel() {
    return label;
  }

  /**
   * Returns a {@link User} object of the uploader.
   */
  public User getUploader() {
    return uploader;
  }

  /**
   * Returns the content type string of the asset.
   */
  public String getContentType() {
    return contentType;
  }

  /**
   * Returns the asset state.
   */
  public String getState() {
    return state;
  }

  /**
   * Returns the asset size, in bytes.
   */
  public long getSize() {
    return size;
  }

  /**
   * Returns the current download count.
   */
  public long getDownloadCount() {
    return downloadCount;
  }

  /**
   * Returns the creation time as DateTime string.
   */
  public String getCreatedAt() {
    return createdAt;
  }

  /**
   * Returns the last update time as DateTime string.
   */
  public String getUpdatedAt() {
    return updatedAt;
  }

  /**
   * Returns the direct download URL of this asset.
   */
  public String getBrowserDownloadUrl() {
    return browserDownloadUrl;
  }

  @Override
  public int compareTo(Asset o) {
    return (o != null) ? Long.compare(id, o.id) : 1;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;
    Asset asset = (Asset) o;
    return id == asset.id;
  }

  @Override
  public int hashCode() {
    return Objects.hash(id);
  }

  @Override
  public String toString() {
    return String.format("Asset(%d, %s, %d bytes)", getId(), getName(), getSize());
  }

  private void init(JSONObject json) throws JSONException {
    for (Iterator<String> iter = json.keys(); iter.hasNext(); ) {
      final String key = iter.next();
      switch (key) {
        case "url" -> url = json.getString(key);
        case "id" -> id = json.getLong(key);
        case "node_id" -> nodeId = json.getString(key);
        case "name" -> name = json.getString(key);
        case "label" -> label = json.isNull(key) ? null : json.getString(key);
        case "uploader" -> uploader = new User(json.getJSONObject(key));
        case "content_type" -> contentType = json.getString(key);
        case "state" -> state = json.getString(key);
        case "size" -> size = json.getLong(key);
        case "download_count" -> downloadCount = json.getLong(key);
        case "created_at" -> createdAt = json.getString(key);
        case "uploaded_at" -> updatedAt = json.getString(key);
        case "browser_download_url" -> browserDownloadUrl = json.getString(key);
        default -> {
        }
      }
    }
    Logger.debug("Asset initialized: {}", toString());
  }
}
