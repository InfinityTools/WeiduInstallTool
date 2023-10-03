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
 * This class handles data from the GitHub API User structure.
 */
public class User implements Comparable<User> {
  /**
   * Assembles a full URI for the GitHub User API from the specified parameters.
   *
   * @param user   The GitHub user name.
   * @param params additional parameters for the GitHub API. Each parameter is added as an additional path element to
   *               the returned URI.
   * @return {@link URI} of the assembled GitHub API URL.
   */
  private static URI assembleUrl(String user, String... params) {
    user = URLEncoder.encode(user, StandardCharsets.UTF_8);
    params = Arrays.stream(params).map(s -> URLEncoder.encode(s, StandardCharsets.UTF_8)).toArray(String[]::new);

    final String path;
    if (params.length > 0) {
      path = String.format("/users/%s/%s", user, String.join("/", params));
    } else {
      path = String.format("/users/%s", user);
    }
    Logger.debug("GitHub API path: {}", path);
    return Release.GITHUB_API.resolve(path);
  }

  private long id;
  private String login;
  private String nodeId;
  private String avatarUrl;
  private String gravatarId;
  private String url;
  private String htmlUrl;
  private String followersUrl;
  private String followingUrlTemplate;
  private String gistsUrlTemplate;
  private String starredUrlTemplate;
  private String subscriptionsUrl;
  private String organizationsUrl;
  private String reposUrl;
  private String eventsUrlTemplate;
  private String receivedEventsUrl;
  private String type;
  private boolean siteAdmin;

  /**
   * Used internally to create a new {@code Author} instance from the specified JSON data.
   *
   * @param jsonAuthor JSON structure containing information about a specific author.
   * @throws JSONException if the JSON structure could not be parsed.
   */
  User(JSONObject jsonAuthor) throws JSONException {
    init(jsonAuthor);
  }

  /**
   * Creates a new {@code Author} instance with the specified parameters and initializes it with data
   * retrieved from the GitHub server, using timeout values defined by {@link Helpers#DEFAULT_CONNECT_TIMEOUT} and
   * {@link Helpers#DEFAULT_READ_TIMEOUT}.
   *
   * @param user User name of the author.
   * @throws IOException   if the method failed to retrieve data from the GitHub server.
   * @throws JSONException if the JSON structure could not be parsed.
   */
  public User(String user) throws IOException, JSONException {
    this(user, Helpers.DEFAULT_CONNECT_TIMEOUT, Helpers.DEFAULT_READ_TIMEOUT);
  }

  /**
   * Creates a new {@code Author} instance with the specified parameters and initializes it with data
   * retrieved from the GitHub server.
   *
   * @param user           User name of the author.
   * @param connectTimeOut Timeout value, in milliseconds, for opening the connection to the GitHub server.
   * @param readTimeOut    Timeout value, in milliseconds, for reading data from the connection.
   * @throws IOException   if the method failed to retrieve data from the GitHub server.
   * @throws JSONException if the JSON structure could not be parsed.
   */
  public User(String user, int connectTimeOut, int readTimeOut) throws IOException, JSONException {
    final URI uri = assembleUrl(user);
    final String s = Helpers.requestJSON(uri, connectTimeOut, readTimeOut);
    final JSONObject json = new JSONObject(s);
    init(json);
  }

  /**
   * Returns the user id.
   */
  public long getId() {
    return id;
  }

  /**
   * Returns the login name.
   */
  public String getLogin() {
    return login;
  }

  /**
   * Returns the node id string.
   */
  public String getNodeId() {
    return nodeId;
  }

  /**
   * Returns the avatar (icon) url.
   */
  public String getAvatarUrl() {
    return avatarUrl;
  }

  /**
   * Returns the gravatar id.
   */
  public String getGravatarId() {
    return gravatarId;
  }

  /**
   * Returns the GitHub API URL for this user.
   */
  public String getUrl() {
    return url;
  }

  /**
   * Returns the browser URL for this user.
   */
  public String getHtmlUrl() {
    return htmlUrl;
  }

  /**
   * Returns the GitHub API URL for the list of followers.
   */
  public String getFollowersUrl() {
    return followersUrl;
  }

  /**
   * Returns the GitHub API URL, including optional parameter list, for a followed user.
   */
  public String getFollowingUrlTemplate() {
    return followingUrlTemplate;
  }

  /**
   * Returns the GitHub API URL, including optional parameter list, for gists.
   */
  public String getGistsUrlTemplate() {
    return gistsUrlTemplate;
  }

  /**
   * Returns the GitHub API URL, including optional parameter list, for starred repos.
   */
  public String getStarredUrlTemplate() {
    return starredUrlTemplate;
  }

  /**
   * Returns the GitHub API URL for subscribed repos.
   */
  public String getSubscriptionsUrl() {
    return subscriptionsUrl;
  }

  /**
   * Returns the GitHub API URL for associated organizations.
   */
  public String getOrganizationsUrl() {
    return organizationsUrl;
  }

  /**
   * Returns the GitHub API URL for associated repositories.
   */
  public String getReposUrl() {
    return reposUrl;
  }

  /**
   * Returns the GitHub API URL, including optional parameter list, for associated events.
   */
  public String getEventsUrlTemplate() {
    return eventsUrlTemplate;
  }

  /**
   * Returns the GitHub API URL for received events.
   */
  public String getReceivedEventsUrl() {
    return receivedEventsUrl;
  }

  /**
   * Returns the user type.
   */
  public String getType() {
    return type;
  }

  /**
   * Returns whether the user is a site admin.
   */
  public boolean isSiteAdmin() {
    return siteAdmin;
  }

  @Override
  public int compareTo(User o) {
    return (o != null) ? Long.compare(id, o.id) : 1;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;
    User user = (User) o;
    return id == user.id;
  }

  @Override
  public int hashCode() {
    return Objects.hash(id);
  }

  @Override
  public String toString() {
    return String.format("User(%d, %s)", getId(), getLogin());
  }

  private void init(JSONObject json) throws JSONException {
    for (Iterator<String> iter = json.keys(); iter.hasNext(); ) {
      final String key = iter.next();
      switch (key) {
        case "login" -> login = json.getString(key);
        case "id" -> id = json.getLong(key);
        case "node_id" -> nodeId = json.getString(key);
        case "avatar_url" -> avatarUrl = json.getString(key);
        case "gravatar_id" -> gravatarId = json.getString(key);
        case "url" -> url = json.getString(key);
        case "html_url" -> htmlUrl = json.getString(key);
        case "followers_url" -> followersUrl = json.getString(key);
        case "following_url" -> followingUrlTemplate = json.getString(key);
        case "gists_url" -> gistsUrlTemplate = json.getString(key);
        case "starred_url" -> starredUrlTemplate = json.getString(key);
        case "subscriptions_url" -> subscriptionsUrl = json.getString(key);
        case "organizations_url" -> organizationsUrl = json.getString(key);
        case "repos_url" -> reposUrl = json.getString(key);
        case "events_url" -> eventsUrlTemplate = json.getString(key);
        case "received_events_url" -> receivedEventsUrl = json.getString(key);
        case "type" -> type = json.getString(key);
        case "site_admin" -> siteAdmin = json.getBoolean(key);
        default -> {
        }
      }
      Logger.debug("User initialized: {}", toString());
    }
  }
}
