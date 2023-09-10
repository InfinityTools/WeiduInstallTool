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
package io.infinitytools.wml.ini;

import java.util.Objects;

/**
 * Represents a single comment line.
 */
public class IniComment extends IniBaseNode {
  private String comment;

  /**
   * Creates a new INI comment. This constructor is invoked internally by {@link IniMapSection#addComment(String)}.
   *
   * @param section INI section this comment is associated with.
   * @param comment Comment string.
   */
  IniComment(IniMapSection section, String comment) {
    super(section);
    this.comment = "";
    setComment(comment);
  }

  /**
   * Returns the current comment.
   */
  public String getComment() {
    return comment;
  }

  /**
   * Assigns a new comment. Line breaks are automatically converted to spaces.
   */
  public void setComment(String newComment) {
    if (newComment == null) {
      newComment = "";
    }
    newComment = newComment.replaceAll("[\r\n]", " ").strip();
    this.comment = newComment;
  }

  @Override
  public int compareTo(IniBaseNode o) {
    if (o instanceof IniComment cmt) {
      return comment.compareToIgnoreCase(cmt.comment);
    } else {
      return -1;
    }
  }

  @Override
  public int hashCode() {
    return Objects.hash(comment.toLowerCase());
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj) {
      return true;
    }
    if (obj == null) {
      return false;
    }
    if (getClass() != obj.getClass()) {
      return false;
    }
    IniComment other = (IniComment) obj;
    return comment.equalsIgnoreCase(other.comment);
  }

  @Override
  public String toString() {
    return String.format("%s %s", getCommentStyle(), getComment());
  }

  /**
   * Returns the comment style used by this INI map.
   */
  private IniMap.Style getCommentStyle() {
    return getSection().getMap().getStyle();
  }
}
