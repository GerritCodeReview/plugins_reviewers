// Copyright (C) 2020 The Android Open Source Project
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

package com.googlesource.gerrit.plugins.reviewers.config;

import static com.googlesource.gerrit.plugins.reviewers.config.ReviewersConfig.KEY_CC;
import static com.googlesource.gerrit.plugins.reviewers.config.ReviewersConfig.KEY_REVIEWER;
import static com.googlesource.gerrit.plugins.reviewers.config.ReviewersConfig.SECTION_FILTER;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import com.googlesource.gerrit.plugins.reviewers.ReviewerFilter;
import java.util.List;
import org.eclipse.jgit.lib.Config;

/** Representation of the collection of {@link ReviewerFilter}s in a {@link Config}. */
class ReviewerFilterCollection {

  private final Config cfg;

  ReviewerFilterCollection(Config cfg) {
    this.cfg = cfg;
  }

  List<ReviewerFilter> getAll() {
    ImmutableList.Builder<ReviewerFilter> b = ImmutableList.builder();
    for (String f : cfg.getSubsections(SECTION_FILTER)) {
      b.add(new ReviewerFilterSection(f));
    }
    return b.build();
  }

  ReviewerFilterSection get(String filter) {
    return new ReviewerFilterSection(filter);
  }

  class ReviewerFilterSection extends ReviewerFilter {

    private ReviewerFilterSection(String filter) {
      this.filter = filter;
      this.reviewers = Sets.newHashSet(cfg.getStringList(SECTION_FILTER, filter, KEY_REVIEWER));
      this.ccs = Sets.newHashSet(cfg.getStringList(SECTION_FILTER, filter, KEY_CC));
    }

    public void removeReviewer(String reviewer) {
      reviewers.remove(reviewer);
      save();
    }

    public void addReviewer(String reviewer) {
      reviewers.add(reviewer);
      save();
    }

    public void removeCc(String cc) {
      ccs.remove(cc);
      save();
    }

    public void addCc(String cc) {
      ccs.add(cc);
      save();
    }

    private void save() {
      if (this.reviewers.isEmpty() && this.ccs.isEmpty()) {
        cfg.unsetSection(SECTION_FILTER, filter);
      } else {
        cfg.setStringList(SECTION_FILTER, filter, KEY_REVIEWER, Lists.newArrayList(this.reviewers));
        cfg.setStringList(SECTION_FILTER, filter, KEY_CC, Lists.newArrayList(this.ccs));
      }
    }
  }
}
