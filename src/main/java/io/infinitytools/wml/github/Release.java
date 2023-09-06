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

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.tinylog.Logger;

import java.io.IOException;
import java.net.URI;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.*;

/**
 * This class handles data from the GitHub API Release structure.
 */
public class Release implements Comparable<Release> {
  /**
   * Symbolic identifier for the latest available public release.
   */
  public static final long ID_LATEST = -1L;

  /**
   * Hostname for requesting GitHub API data, provided as URI instance.
   */
  public static final URI GITHUB_API = URI.create("https://api.github.com");

  /**
   * Returns a list of all available releases made for the specified repository.
   *
   * @param owner          User or organization name owning the repository.
   * @param repo           Name of the repository.
   * @param connectTimeOut Timeout value, in milliseconds, for opening the connection to the GitHub server.
   * @param readTimeOut    Timeout value, in milliseconds, for reading data from the connection.
   * @return {@link List} of available releases.
   * @throws NullPointerException if {@code owner} or {@code repo} are {@code null}.
   * @throws IOException          if the method failed to retrieve data from the GitHub server.
   */
  public static List<Release> getReleases(String owner, String repo, int connectTimeOut, int readTimeOut)
      throws IOException, JSONException {
    List<Release> retVal = new ArrayList<>();

    try {
      final URI uri = assembleUrl(owner, repo, "releases");
      final String s = Helpers.requestJSON(uri, connectTimeOut, readTimeOut);
      final JSONArray json = new JSONArray(s);
      for (int i = 0, size = json.length(); i < size; i++) {
        retVal.add(new Release(json.getJSONObject(i)));
      }
    } catch (IllegalArgumentException e) {
      Logger.error("Retrieving GitHub Release data", e);
    }

    return retVal;
  }

  /**
   * Assembles a full URI for the GitHub Releases API from the specified parameters.
   *
   * @param owner  The GitHub account owner (user or organization).
   * @param repo   Name of the repository.
   * @param params additional parameters for the GitHub API. Each parameter is added as an additional path element to
   *               the returned URI.
   * @return {@link URI} of the assembled GitHub API URL.
   */
  private static URI assembleUrl(String owner, String repo, String... params) {
    owner = URLEncoder.encode(owner, StandardCharsets.UTF_8);
    repo = URLEncoder.encode(repo, StandardCharsets.UTF_8);
    params = Arrays.stream(params).map(s -> URLEncoder.encode(s, StandardCharsets.UTF_8)).toArray(String[]::new);

    final String path;
    if (params.length > 0) {
      path = String.format("/repos/%s/%s/%s", owner, repo, String.join("/", params));
    } else {
      path = String.format("/repos/%s/%s", owner, repo);
    }
    return GITHUB_API.resolve(path);
  }

  /**
   * Returns a string representation of the specified release id.
   */
  private static String validateId(long id) {
    return (id == ID_LATEST) ? "latest" : Long.toString(id);
  }

  private final List<Asset> assets = new ArrayList<>();

  private long id;
  private String url;
  private String assetsUrl;
  private String uploadUrlTemplate;
  private String htmlUrl;
  private User author;
  private String nodeId;
  private String tagName;
  private String targetCommitish;
  private String name;
  private boolean draft;
  private boolean prerelease;
  private String createdAt;
  private String publishedAt;
  private String tarballUrl;
  private String zipballUrl;
  private String body;

  /**
   * Used internally to create a new {@code Release} instance from the specified JSON data.
   *
   * @param jsonRelease JSON structure containing information about a specific release.
   * @throws JSONException if the JSON structure could not be parsed.
   */
  private Release(JSONObject jsonRelease) throws JSONException {
    init(jsonRelease);
  }

  /**
   * Creates a new {@code Release} instance with the specified parameters and initializes it with data retrieved from
   * the GitHub server, using timeout values defined by {@link Helpers#DEFAULT_CONNECT_TIMEOUT} and
   * {@link Helpers#DEFAULT_READ_TIMEOUT}.
   *
   * @param owner User or organization name owning the repository.
   * @param repo  Name of the repository.
   * @param id    Unique identifier of the release. Specify {@link #ID_LATEST} to get information about the
   *              latest release.
   * @throws IOException   if the method failed to retrieve data from the GitHub server.
   * @throws JSONException if the JSON structure could not be parsed.
   */
  public Release(String owner, String repo, long id) throws IOException, JSONException {
    this(owner, repo, id, Helpers.DEFAULT_CONNECT_TIMEOUT, Helpers.DEFAULT_READ_TIMEOUT);
  }

  /**
   * Creates a new {@code Release} instance with the specified parameters and initializes it with data
   * retrieved from the GitHub server.
   *
   * @param owner          User or organization name owning the repository.
   * @param repo           Name of the repository.
   * @param id             Unique identifier of the release. Specify {@link #ID_LATEST} to get information about the
   *                       latest release.
   * @param connectTimeOut Timeout value, in milliseconds, for opening the connection to the GitHub server.
   * @param readTimeOut    Timeout value, in milliseconds, for reading data from the connection.
   * @throws IOException   if the method failed to retrieve data from the GitHub server.
   * @throws JSONException if the JSON structure could not be parsed.
   */
  public Release(String owner, String repo, long id, int connectTimeOut, int readTimeOut)
      throws IOException, JSONException {
    final URI uri = assembleUrl(owner, repo, "releases", validateId(id));
    final String s = Helpers.requestJSON(uri, connectTimeOut, readTimeOut);
    final JSONObject json = new JSONObject(s);
    init(json);
  }

  /**
   * Returns the release id.
   */
  public long getId() {
    return id;
  }

  /**
   * Returns the GitHub API URL for this release.
   */
  public String getUrl() {
    return url;
  }

  /**
   * Returns the GitHub API URL for the assets list of this release.
   */
  public String getAssetsUrl() {
    return assetsUrl;
  }

  /**
   * Return all available assets for this release.
   */
  public List<Asset> getAssets() {
    return Collections.unmodifiableList(assets);
  }

  /**
   * Returns the GitHub API URL, including optional parameter list, for uploading assets.
   */
  public String getUploadUrlTemplate() {
    return uploadUrlTemplate;
  }

  /**
   * Returns the browser URL for the release.
   */
  public String getHtmlUrl() {
    return htmlUrl;
  }

  /**
   * Returns the {@link User} of this release.
   */
  public User getAuthor() {
    return author;
  }

  /**
   * Returns the node id string.
   */
  public String getNodeId() {
    return nodeId;
  }

  /**
   * Returns the tag name of this release.
   */
  public String getTagName() {
    return tagName;
  }

  /**
   * Returns the commitish value that determines where the Git tag is created from (e.g. "master" branch).
   */
  public String getTargetCommitish() {
    return targetCommitish;
  }

  /**
   * Returns the release name (or title).
   */
  public String getName() {
    return name;
  }

  /**
   * Returns whether this release is marked as "Draft".
   */
  public boolean isDraft() {
    return draft;
  }

  /**
   * Returns whether this release is marked as "Prerelease".
   */
  public boolean isPrerelease() {
    return prerelease;
  }

  /**
   * Returns the creation time as DateTime string.
   */
  public String getCreatedAt() {
    return createdAt;
  }

  /**
   * Returns the publication time as DateTime string.
   */
  public String getPublishedAt() {
    return publishedAt;
  }

  /**
   * Returns the URL of the tarball associated with the release.
   */
  public String getTarballUrl() {
    return tarballUrl;
  }

  /**
   * Returns the URL of the zip archive associated with the release.
   */
  public String getZipballUrl() {
    return zipballUrl;
  }

  /**
   * Returns the content of the release description.
   */
  public String getBody() {
    return body;
  }

  @Override
  public int compareTo(Release o) {
    return (o != null) ? Long.compare(id, o.id) : 1;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;
    Release release = (Release) o;
    return id == release.id;
  }

  @Override
  public int hashCode() {
    return Objects.hash(id);
  }

  @Override
  public String toString() {
    return String.format("Release(%d, %s, %s)", getId(), getTagName(), getName());
  }

  private void init(JSONObject json) throws JSONException {
    for (Iterator<String> iter = json.keys(); iter.hasNext(); ) {
      final String key = iter.next();
      switch (key) {
        case "url" -> url = json.getString(key);
        case "assets_url" -> assetsUrl = json.getString(key);
        case "upload_url" -> uploadUrlTemplate = json.getString(key);
        case "html_url" -> htmlUrl = json.getString(key);
        case "id" -> id = json.getLong(key);
        case "author" -> author = new User(json.getJSONObject(key));
        case "node_id" -> nodeId = json.getString(key);
        case "tag_name" -> tagName = json.getString(key);
        case "target_commitish" -> targetCommitish = json.getString(key);
        case "name" -> name = json.getString(key);
        case "draft" -> draft = json.getBoolean(key);
        case "prerelease" -> prerelease = json.getBoolean(key);
        case "created_at" -> createdAt = json.getString(key);
        case "published_at" -> publishedAt = json.getString(key);
        case "assets" -> {
          final JSONArray ja = json.getJSONArray(key);
          for (int i = 0, size = ja.length(); i < size; i++) {
            assets.add(new Asset(ja.getJSONObject(i)));
          }
        }
        case "tarball_url" -> tarballUrl = json.getString(key);
        case "zipball_url" -> zipballUrl = json.getString(key);
        case "body" -> body = json.getString(key);
        default -> {
        }
      }
    }
  }
}
