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

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import java.util.List;
import java.util.Set;
import org.eclipse.jgit.lib.Config;

/**
 * Representation of the collection of filter sections in reviewers.config. Example:
 *
 * <pre>
 * [filter "'"]
 *   reviewer = joe
 *   reviewer = jane
 * </pre>
 */
public class ReviewerFilterCollection {
  static final String SECTION_FILTER = "filter";
  static final String KEY_REVIEWER = "reviewer";

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

  ReviewerFilter get(String filter) {
    return new ReviewerFilterSection(filter);
  }

  private class ReviewerFilterSection extends ReviewerFilter {
    private final String filter;
    private final Set<String> reviewers;

    private ReviewerFilterSection(String filter) {
      this.filter = filter;
      this.reviewers = Sets.newHashSet(cfg.getStringList(SECTION_FILTER, filter, KEY_REVIEWER));
    }

    @Override
    public String getFilter() {
      return filter;
    }

    @Override
    public Set<String> getReviewers() {
      return reviewers;
    }

    @Override
    public void removeReviewer(String reviewer) {
      this.reviewers.remove(reviewer);
      save();
    }

    @Override
    public void addReviewer(String reviewer) {
      this.reviewers.add(reviewer);
      save();
    }

    private void save() {
      if (this.reviewers.isEmpty()) {
        cfg.unsetSection(SECTION_FILTER, filter);
      } else {
        cfg.setStringList(SECTION_FILTER, filter, KEY_REVIEWER, Lists.newArrayList(this.reviewers));
      }
    }
  }
}
