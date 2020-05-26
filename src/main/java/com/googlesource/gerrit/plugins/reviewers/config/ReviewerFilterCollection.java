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

import static com.googlesource.gerrit.plugins.reviewers.config.ForProject.KEY_CC;
import static com.googlesource.gerrit.plugins.reviewers.config.ForProject.KEY_REVIEWER;
import static com.googlesource.gerrit.plugins.reviewers.config.ForProject.SECTION_FILTER;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import com.google.gerrit.index.query.QueryParseException;
import com.google.gerrit.server.git.ValidationError;
import com.google.inject.assistedinject.Assisted;
import com.google.inject.assistedinject.AssistedInject;
import com.googlesource.gerrit.plugins.reviewers.ReviewerFilter;
import java.util.List;
import java.util.Optional;
import org.eclipse.jgit.lib.Config;

/** Representation of the collection of {@link ReviewerFilter}s in a {@link Config}. */
class ReviewerFilterCollection {

  private final ReviewersQueryValidator queryValidator;
  private final Config cfg;
  private final Optional<ValidationError.Sink> validationErrorSink;

  interface Factory {
    ReviewerFilterCollection create(Config cfg);

    ReviewerFilterCollection create(Config cfg, ValidationError.Sink validationErrorSink);
  }

  @AssistedInject
  ReviewerFilterCollection(ReviewersQueryValidator queryValidator, @Assisted Config cfg) {
    this(queryValidator, cfg, null);
  }

  @AssistedInject
  ReviewerFilterCollection(
      ReviewersQueryValidator queryValidator,
      @Assisted Config cfg,
      @Assisted ValidationError.Sink validationErrorSink) {
    this.queryValidator = queryValidator;
    this.cfg = cfg;
    this.validationErrorSink = Optional.ofNullable(validationErrorSink);
    check();
  }

  List<ReviewerFilter> getAll() {
    ImmutableList.Builder<ReviewerFilter> b = ImmutableList.builder();
    for (String f : cfg.getSubsections(SECTION_FILTER)) {
      ReviewerFilterSection section = newReviewerFilter(f);
      b.add(section);
    }
    return b.build();
  }

  /* Validates all the filter in this collection and adds the ValidationErrors
   * to the ValidationError.Sink. */
  private void check() {
    for (String f : cfg.getSubsections(SECTION_FILTER)) {
      checkForErrors(f);
    }
  }

  ReviewerFilterSection get(String filter) {
    return newReviewerFilter(filter);
  }

  private ReviewerFilterSection newReviewerFilter(String filter) {
    ReviewerFilterSection section = new ReviewerFilterSection(filter);
    checkForErrors(section.getFilter()).ifPresent(ve -> section.filterError(ve));
    return section;
  }

  /* Checks if filterQuery is a valid query. If not it adds the corresponding
   * ValidationError to the ValidationError.Sink and returns the error. */
  private Optional<String> checkForErrors(String filterQuery) {
    try {
      queryValidator.validateQuery(filterQuery);
    } catch (QueryParseException qpe) {
      validationErrorSink.ifPresent(ves -> ves.error(ValidationError.create(qpe.getMessage())));
      return Optional.of(qpe.getMessage());
    }
    return Optional.empty();
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

    public void filterError(String error) {
      this.filterError = error;
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
