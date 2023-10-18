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
package io.infinitytools.wit.gui;

import io.infinitytools.wit.Configuration;
import io.infinitytools.wit.utils.Utils;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.SceneAntialiasing;
import javafx.scene.paint.Paint;
import org.tinylog.Logger;

import java.util.HashSet;
import java.util.Objects;

/**
 * This class expands the {@link Scene} class by optional dark mode coloring.
 */
public class CustomScene extends Scene {
  /**
   * Path to the dark mode stylesheet file.
   */
  private static final String DARK_MODE = Utils.getClassPath(CustomScene.class, "darkMode.css");

  private static final HashSet<Scene> SCENE_CACHE = new HashSet<>();

  /**
   * Returns whether the specified {@link Scene} has dark mode enabled.
   */
  public static boolean isDarkMode(Scene scene) {
    return Objects.requireNonNull(scene).getStylesheets().contains(DARK_MODE);
  }

  /**
   * Enables or disables dark mode for the specified {@link Scene} object based on the current {@link Configuration}.
   */
  public static void setDarkMode(Scene scene) {
    try {
      boolean enabled = Configuration.getInstance().getOption(Configuration.Key.DARK_UI_MODE);
      setDarkMode(scene, enabled);
    } catch (Throwable t) {
      Logger.error(t, "Could not set dark UI mode");
    }
  }

  /**
   * Enables or disables dark mode for the specified {@link Scene} object.
   */
  public static void setDarkMode(Scene scene, boolean enabled) {
    if (scene != null) {
      if (enabled && !scene.getStylesheets().contains(DARK_MODE)) {
        scene.getStylesheets().add(DARK_MODE);
      } else if (!enabled) {
        scene.getStylesheets().remove(DARK_MODE);
      }
    }
  }

  /**
   * Adds the specified {@link Scene} to the cache for updating the dark mode state.
   */
  public static void registerScene(Scene scene) {
    if (scene != null) {
      SCENE_CACHE.add(scene);
    }
  }

  /**
   * Removes the specified {@link Scene} from the cache for updating the dark mode state.
   */
  public static void unregisterScene(Scene scene) {
    if (scene != null) {
      SCENE_CACHE.remove(scene);
    }
  }

  /**
   * Updates the current dark mode state for all cached {@link Scene} objects.
   */
  public static void updateSceneCache() {
    for (final Scene scene : SCENE_CACHE) {
      setDarkMode(scene);
    }
  }

  /**
   * Enforces the specified dark mode state to all cached {@link Scene} objects.
   */
  public static void updateSceneCache(boolean enable) {
    for (final Scene scene : SCENE_CACHE) {
      setDarkMode(scene, enable);
    }
  }


  public CustomScene(Parent root) {
    super(root);
    registerScene(this);
    setDarkMode();
  }

  public CustomScene(Parent root, double width, double height) {
    super(root, width, height);
    registerScene(this);
    setDarkMode();
  }

  public CustomScene(Parent root, double width, double height, boolean depthBuffer) {
    super(root, width, height, depthBuffer);
    registerScene(this);
    setDarkMode();
  }

  public CustomScene(Parent root, double width, double height, boolean depthBuffer, SceneAntialiasing antiAliasing) {
    super(root, width, height, depthBuffer, antiAliasing);
    registerScene(this);
    setDarkMode();
  }

  public CustomScene(Parent root, Paint fill) {
    super(root, fill);
    registerScene(this);
    setDarkMode();
  }

  public CustomScene(Parent root, double width, double height, Paint fill) {
    super(root, width, height, fill);
    registerScene(this);
    setDarkMode();
  }

  /**
   * Returns whether dark mode is applied to the scene.
   */
  public boolean isDarkMode() {
    return isDarkMode(this);
  }

  /**
   * Enables or disables dark mode for this scene based on the current {@link Configuration}.
   */
  public void setDarkMode() {
    setDarkMode(this);
  }

  /**
   * Enables or disables dark mode for this scene.
   */
  public void setDarkMode(boolean enabled) {
    setDarkMode(this, enabled);
  }
}
