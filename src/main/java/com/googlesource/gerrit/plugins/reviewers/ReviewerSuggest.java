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

package com.googlesource.gerrit.plugins.reviewers;

import static java.util.stream.Collectors.toSet;

import com.google.common.collect.ImmutableSet;
import com.google.common.flogger.FluentLogger;
import com.google.gerrit.entities.Account;
import com.google.gerrit.entities.Change.Id;
import com.google.gerrit.entities.Project.NameKey;
import com.google.gerrit.exceptions.StorageException;
import com.google.gerrit.index.query.QueryParseException;
import com.google.gerrit.server.change.ReviewerSuggestion;
import com.google.gerrit.server.change.SuggestedReviewer;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.googlesource.gerrit.plugins.reviewers.config.FiltersFactory;
import java.util.List;
import java.util.Set;

@Singleton
public class ReviewerSuggest implements ReviewerSuggestion {
  private static final FluentLogger logger = FluentLogger.forEnclosingClass();

  private final FiltersFactory filters;
  private final ReviewersFilterUtil util;
  private final ReviewersResolver resolver;

  @Inject
  public ReviewerSuggest(
      FiltersFactory filters, ReviewersFilterUtil filterUtil, ReviewersResolver resolver) {
    this.filters = filters;
    this.util = filterUtil;
    this.resolver = resolver;
  }

  @Override
  public Set<SuggestedReviewer> suggestReviewers(
      NameKey project,
      Id changeId,
      String query,
      Set<com.google.gerrit.entities.Account.Id> candidates) {
    List<ReviewerFilter> sections = filters.withInheritance(project);

    if (sections.isEmpty() || changeId == null) {
      return ImmutableSet.of();
    }

    try {
      Set<String> reviewers = util.findReviewers(changeId.get(), sections);
      if (!reviewers.isEmpty()) {
        return resolver.resolve(reviewers, project, changeId.get(), null, false).stream()
            .map(a -> suggestedReviewer(a))
            .collect(toSet());
      }
    } catch (StorageException | QueryParseException x) {
      logger.atSevere().withCause(x).log("%s", x.getMessage());
    }
    return ImmutableSet.of();
  }

  private SuggestedReviewer suggestedReviewer(Account.Id account) {
    SuggestedReviewer reviewer = new SuggestedReviewer();
    reviewer.account = account;
    reviewer.score = 1;
    return reviewer;
  }
}
