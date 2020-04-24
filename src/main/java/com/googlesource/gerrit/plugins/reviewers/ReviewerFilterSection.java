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

import com.google.common.annotations.VisibleForTesting;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import org.eclipse.jgit.lib.Config;

class ReviewerFilterSection {
  static final String KEY_REVIEWER = "reviewer";
  static final String KEY_CC = "cc";
  static final String SECTION_FILTER = "filter";

  public static class Factory {
    private final Config cfg;

    Factory(Config cfg) {
      this.cfg = cfg;
    }

    List<ReviewerFilterSection> getAll() {
      ImmutableList.Builder<ReviewerFilterSection> b = ImmutableList.builder();
      for (String f : cfg.getSubsections(SECTION_FILTER)) {
        b.add(new ReviewerFilterSection(f, this.cfg));
      }
      return b.build();
    }

    ReviewerFilterSection forFilter(String filter) {
      return new ReviewerFilterSection(filter, this.cfg);
    }
  }

  private final transient Config cfg;
  private final String filter;
  private final Set<String> reviewers;
  private final Set<String> ccs;

  ReviewerFilterSection(String filter, Config cfg) {
    this.cfg = cfg;
    this.filter = filter;
    this.reviewers = Sets.newHashSet(this.cfg.getStringList(SECTION_FILTER, filter, KEY_REVIEWER));
    this.ccs = Sets.newHashSet(this.cfg.getStringList(SECTION_FILTER, filter, KEY_CC));
  }

  @VisibleForTesting
  ReviewerFilterSection(String filter, Set<String> reviewers, Set<String> ccs) {
    this.cfg = null;
    this.reviewers = reviewers;
    this.ccs = ccs;
    this.filter = filter;
  }

  String getFilter() {
    return filter;
  }

  Set<String> getReviewers() {
    return reviewers;
  }

  Set<String> getCcs() {
    return ccs;
  }

  void removeReviewer(String reviewer) {
    this.reviewers.remove(reviewer);
    save();
  }

  void addReviewer(String reviewer) {
    this.reviewers.add(reviewer);
    save();
  }

  void addCc(String reviewer) {
    this.ccs.add(reviewer);
    save();
  }

  void removeCc(String reviewer) {
    this.ccs.remove(reviewer);
    save();
  }

  void save() {
    if (this.reviewers.isEmpty() && this.ccs.isEmpty()) {
      cfg.unsetSection(SECTION_FILTER, filter);
    } else {
      cfg.setStringList(SECTION_FILTER, filter, KEY_REVIEWER, Lists.newArrayList(this.reviewers));
      cfg.setStringList(SECTION_FILTER, filter, KEY_CC, Lists.newArrayList(this.ccs));
    }
  }

  @Override
  public boolean equals(Object o) {
    if (o instanceof ReviewerFilterSection) {
      ReviewerFilterSection other = ((ReviewerFilterSection) o);
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
