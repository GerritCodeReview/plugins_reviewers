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
import com.google.gerrit.server.query.change.ChangeData;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.googlesource.gerrit.plugins.reviewers.config.FiltersFactory;
import com.googlesource.gerrit.plugins.reviewers.config.GlobalConfig;
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
  private final GlobalConfig config;
  private final FiltersFactory filters;
  private final ReviewersFilterUtil filterUtil;
  private final ChangeData.Factory changeDataFactory;

  @Inject
  Reviewers(
      ReviewersResolver resolver,
      AddReviewers.Factory addReviewersFactory,
      ReviewerWorkQueue workQueue,
      GlobalConfig config,
      FiltersFactory filters,
      ReviewersFilterUtil util,
      ChangeData.Factory changeDataFactory) {
    this.resolver = resolver;
    this.addReviewersFactory = addReviewersFactory;
    this.workQueue = workQueue;
    this.config = config;
    this.filters = filters;
    this.filterUtil = util;
    this.changeDataFactory = changeDataFactory;
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
    return filters.withInheritance(projectName);
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
      ChangeData cd = changeDataFactory.create(Project.nameKey(c.project), Change.id(c._number));
      Set<String> reviewers = filterUtil.findReviewers(cd, filters);
      Set<String> ccs = filterUtil.findCcs(cd, filters);
      if (reviewers.isEmpty() && ccs.isEmpty()) {
        return;
      }
      /* Remove all reviewer identifiers (account-ids, group-ids) from ccs that are present in reviewers.
       * Further filtering of individual accounts is done in AddReviewers after the ids have been resolved into Accounts. */
      ccs.removeAll(reviewers);
      // This listener is called after a revision was created to add reviewers
      // according to the configs that project owners provided. Respecting
      // account visibility here by checking if the caller (e.g. the user adding
      // a new revision) can see the reviewer to be added does not make sense.
      final AddReviewers addReviewers =
          addReviewersFactory.create(
              c,
              resolver.resolve(reviewers, projectName, changeNumber, uploader, true),
              resolver.resolve(ccs, projectName, changeNumber, uploader, true));
      workQueue.submit(addReviewers);
    } catch (QueryParseException e) {
      logger.atWarning().log(
          "Could not add default reviewers for change %d of project %s, filter is invalid: %s",
          changeNumber, projectName.get(), e.getMessage());
    } catch (StorageException x) {
      logger.atSevere().withCause(x).log("%s", x.getMessage());
    }
  }
}
