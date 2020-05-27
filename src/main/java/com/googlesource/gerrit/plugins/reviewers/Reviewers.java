// Copyright (C) 2013 The Android Open Source Project
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

import com.google.common.base.Preconditions;
import com.google.common.base.Strings;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import com.google.common.flogger.FluentLogger;
import com.google.gerrit.common.Nullable;
import com.google.gerrit.entities.Account;
import com.google.gerrit.entities.Change;
import com.google.gerrit.entities.Project;
import com.google.gerrit.exceptions.StorageException;
import com.google.gerrit.extensions.common.AccountInfo;
import com.google.gerrit.extensions.common.ChangeInfo;
import com.google.gerrit.extensions.events.ChangeEvent;
import com.google.gerrit.extensions.events.PrivateStateChangedListener;
import com.google.gerrit.extensions.events.RevisionCreatedListener;
import com.google.gerrit.extensions.events.WorkInProgressStateChangedListener;
import com.google.gerrit.index.query.QueryParseException;
import com.google.gerrit.server.CurrentUser;
import com.google.gerrit.server.change.ReviewerSuggestion;
import com.google.gerrit.server.change.SuggestedReviewer;
import com.google.gerrit.server.query.change.ChangeData;
import com.google.gerrit.server.query.change.ChangeQueryBuilder;
import com.google.inject.Inject;
import com.google.inject.Provider;
import com.google.inject.Singleton;
import com.googlesource.gerrit.plugins.reviewers.config.ReviewersConfig;
import java.util.List;
import java.util.Set;

/** Handles automatic adding of reviewers and reviewer suggestions. */
@Singleton
class Reviewers
    implements RevisionCreatedListener,
        PrivateStateChangedListener,
        WorkInProgressStateChangedListener,
        ReviewerSuggestion {
  private static final FluentLogger logger = FluentLogger.forEnclosingClass();

  private final ReviewersResolver resolver;
  private final AddReviewers.Factory addReviewersFactory;
  private final ChangeData.Factory changeDataFactory;
  private final ReviewerWorkQueue workQueue;
  private final ReviewersConfig config;
  private final Provider<CurrentUser> user;
  private final ChangeQueryBuilder queryBuilder;

  @Inject
  Reviewers(
      ReviewersResolver resolver,
      AddReviewers.Factory addReviewersFactory,
      ChangeData.Factory changeDataFactory,
      ReviewerWorkQueue workQueue,
      ReviewersConfig config,
      Provider<CurrentUser> user,
      ChangeQueryBuilder queryBuilder) {
    this.resolver = resolver;
    this.addReviewersFactory = addReviewersFactory;
    this.changeDataFactory = changeDataFactory;
    this.workQueue = workQueue;
    this.config = config;
    this.user = user;
    this.queryBuilder = queryBuilder;
  }

  @Override
  public void onRevisionCreated(RevisionCreatedListener.Event event) {
    onEvent(event);
  }

  @Override
  public void onWorkInProgressStateChanged(WorkInProgressStateChangedListener.Event event) {
    onEvent(event);
  }

  @Override
  public void onPrivateStateChanged(PrivateStateChangedListener.Event event) {
    onEvent(event);
  }

  @Override
  public Set<SuggestedReviewer> suggestReviewers(
      Project.NameKey projectName,
      @Nullable Change.Id changeId,
      @Nullable String query,
      Set<Account.Id> candidates) {
    List<ReviewerFilter> filters = getFilters(projectName);

    if (filters.isEmpty() || changeId == null) {
      return ImmutableSet.of();
    }

    try {
      Set<String> reviewers =
          findReviewers(changeDataFactory.create(projectName, changeId), filters);
      if (!reviewers.isEmpty()) {
        return resolver.resolve(reviewers, projectName, changeId.get(), null).stream()
            .map(a -> suggestedReviewer(a))
            .collect(toSet());
      }
    } catch (StorageException | QueryParseException x) {
      logger.atSevere().withCause(x).log(x.getMessage());
    }
    return ImmutableSet.of();
  }

  private SuggestedReviewer suggestedReviewer(Account.Id account) {
    SuggestedReviewer reviewer = new SuggestedReviewer();
    reviewer.account = account;
    reviewer.score = 1;
    return reviewer;
  }

  private List<ReviewerFilter> getFilters(Project.NameKey projectName) {
    // TODO(davido): we have to cache per project configuration
    return config.filtersWithInheritance(projectName);
  }

  private void onEvent(ChangeEvent event) {
    ChangeInfo c = event.getChange();
    /* Never add reviewers automatically to private changes. */
    if (Boolean.TRUE.equals(c.isPrivate)) {
      return;
    }
    if (config.ignoreWip() && Boolean.TRUE.equals(c.workInProgress)) {
      return;
    }
    Project.NameKey projectName = Project.nameKey(c.project);

    List<ReviewerFilter> filters = getFilters(projectName);

    if (filters.isEmpty()) {
      return;
    }

    AccountInfo uploader = event.getWho();
    int changeNumber = c._number;
    try {
      Set<String> reviewers =
          findReviewers(
              changeDataFactory.create(
                  Project.nameKey(event.getChange().project), Change.id(event.getChange()._number)),
              filters);
      if (reviewers.isEmpty()) {
        return;
      }
      final AddReviewers addReviewers =
          addReviewersFactory.create(
              c, resolver.resolve(reviewers, projectName, changeNumber, uploader));
      workQueue.submit(addReviewers);
    } catch (QueryParseException e) {
      logger.atWarning().log(
          "Could not add default reviewers for change %d of project %s, filter is invalid: %s",
          changeNumber, projectName.get(), e.getMessage());
    } catch (StorageException x) {
      logger.atSevere().withCause(x).log(x.getMessage());
    }
  }

  private Set<String> findReviewers(ChangeData cd, List<ReviewerFilter> filters)
      throws StorageException, QueryParseException {
    ImmutableSet.Builder<String> reviewers = ImmutableSet.builder();
    List<ReviewerFilter> found = findReviewerFilters(cd, filters);
    for (ReviewerFilter s : found) {
      reviewers.addAll(s.getReviewers());
    }
    return reviewers.build();
  }

  private List<ReviewerFilter> findReviewerFilters(ChangeData cd, List<ReviewerFilter> filters)
      throws StorageException, QueryParseException {
    ImmutableList.Builder<ReviewerFilter> found = ImmutableList.builder();
    for (ReviewerFilter s : filters) {
      if (Strings.isNullOrEmpty(s.getFilter()) || s.getFilter().equals("*")) {
        found.add(s);
      } else if (filterMatch(cd, s.getFilter())) {
        found.add(s);
      }
    }
    return found.build();
  }

  boolean filterMatch(ChangeData cd, String filter) throws StorageException, QueryParseException {
    Preconditions.checkNotNull(filter);
    return queryBuilder.asUser(user.get()).parse(filter).asMatchable().match(cd);
  }
}
