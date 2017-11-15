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

import java.util.Set;

class ReviewerFilterSection {
  private final String filter;
  private final String originFilter;
  private final Set<String> reviewers;
  private final Set<String> excluded;

  ReviewerFilterSection(String filter, Set<String> reviewers,
      Set<String> excluded, String originFilter) {
    this.filter = filter;
    this.reviewers = reviewers;
    this.excluded = excluded;
    this.originFilter = originFilter;
  }

  ReviewerFilterSection(String filter, Set<String> reviewers,
      Set<String> excluded) {
    this(filter, reviewers, excluded, "");
  }

  String getFilter() {
    return filter;
  }

  String getOriginFilter() {
    return originFilter;
  }

  Set<String> getReviewers() {
    return reviewers;
  }

  Set<String> getExcluded() {
    return excluded;
  }

}
