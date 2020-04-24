// Copyright (C) 2014 The Android Open Source Project
//
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at
//
// http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.

package com.googlesource.gerrit.plugins.reviewers;

import java.util.Objects;
import java.util.Set;

/**
 * Representation of a filter section in reviewers.config. Example:
 *
 * <pre>
 * [filter "*"]
 *   reviewer = joe
 *   reviewer = jane
 * </pre>
 */
public abstract class ReviewerFilter {
  protected String filter;
  protected Set<String> reviewers;
  protected Set<String> ccs;

  String getFilter() {
    return filter;
  }

  Set<String> getReviewers() {
    return reviewers;
  }

  Set<String> getCcs() {
    return ccs;
  }

  @Override
  public boolean equals(Object o) {
    if (o instanceof ReviewerFilter) {
      ReviewerFilter other = ((ReviewerFilter) o);
      return Objects.equals(filter, other.filter)
          && Objects.equals(reviewers, other.reviewers)
          && Objects.equals(ccs, other.ccs);
    }
    return false;
  }

  @Override
  public int hashCode() {
    return Objects.hash(filter, reviewers, ccs);
  }
}
