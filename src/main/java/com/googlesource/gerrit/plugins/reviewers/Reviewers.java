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

import com.google.common.flogger.FluentLogger;
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
import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.googlesource.gerrit.plugins.reviewers.config.ReviewersConfig;
import java.util.List;
import java.util.Set;

/** Handles automatic adding of reviewers and reviewer suggestions. */
@Singleton
class Reviewers
    implements RevisionCreatedListener,
        PrivateStateChangedListener,
        WorkInProgressStateChangedListener {
  private static final FluentLogger logger = FluentLogger.forEnclosingClass();

  private final ReviewersResolver resolver;
  private final AddReviewers.Factory addReviewersFactory;
  private final ReviewerWorkQueue workQueue;
  private final ReviewersConfig config;
  private final ReviewersFilterUtil filterUtil;

  @Inject
  Reviewers(
      ReviewersResolver resolver,
      AddReviewers.Factory addReviewersFactory,
      ReviewerWorkQueue workQueue,
      ReviewersConfig config,
      ReviewersFilterUtil util) {
    this.resolver = resolver;
    this.addReviewersFactory = addReviewersFactory;
    this.workQueue = workQueue;
    this.config = config;
    this.filterUtil = util;
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
          filterUtil.findReviewers(
              Project.nameKey(event.getChange().project), Change.id(changeNumber), filters);
      Set<String> ccs =
          filterUtil.findCcs(
              Project.nameKey(event.getChange().project), Change.id(changeNumber), filters);
      if (reviewers.isEmpty() && ccs.isEmpty()) {
        return;
      }
      /* Remove all reviewer identifiers (account-ids, group-ids) from ccs that are present in reviewers.
       * Further filtering of individual accounts is done in AddReviewers after the ids have been resolved into Accounts. */
      ccs.removeAll(reviewers);
      final AddReviewers addReviewers =
          addReviewersFactory.create(
              c,
              resolver.resolve(reviewers, projectName, changeNumber, uploader),
              resolver.resolve(ccs, projectName, changeNumber, uploader));
      workQueue.submit(addReviewers);
    } catch (QueryParseException e) {
      logger.atWarning().log(
          "Could not add default reviewers for change %d of project %s, filter is invalid: %s",
          changeNumber, projectName.get(), e.getMessage());
    } catch (StorageException x) {
      logger.atSevere().withCause(x).log(x.getMessage());
    }
  }
}
