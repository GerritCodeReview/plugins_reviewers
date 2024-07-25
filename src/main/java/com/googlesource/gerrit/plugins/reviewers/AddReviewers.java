// Copyright (C) 2018 The Android Open Source Project
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
import com.google.gerrit.entities.Account;
import com.google.gerrit.extensions.api.GerritApi;
import com.google.gerrit.extensions.api.changes.ReviewInput;
import com.google.gerrit.extensions.api.changes.ReviewerInput;
import com.google.gerrit.extensions.client.ReviewerState;
import com.google.gerrit.extensions.common.ChangeInfo;
import com.google.gerrit.extensions.restapi.RestApiException;
import com.google.gerrit.server.util.ManualRequestContext;
import com.google.gerrit.server.util.OneOffRequestContext;
import com.google.inject.Inject;
import com.google.inject.assistedinject.Assisted;
import java.util.ArrayList;
import java.util.Set;
import java.util.stream.Collectors;

/** Adds reviewers to a change. */
class AddReviewers implements Runnable {
  private static final FluentLogger logger = FluentLogger.forEnclosingClass();

  private final GerritApi gApi;
  private final OneOffRequestContext requestContext;
  private final ChangeInfo changeInfo;
  private final Set<Account.Id> reviewers;
  private final Set<Account.Id> ccs;

  interface Factory {
    AddReviewers create(
        ChangeInfo changeInfo,
        @Assisted("reviewers") Set<Account.Id> reviewers,
        @Assisted("ccs") Set<Account.Id> ccs);
  }

  @Inject
  AddReviewers(
      GerritApi gApi,
      OneOffRequestContext requestContext,
      @Assisted ChangeInfo changeInfo,
      @Assisted("reviewers") Set<Account.Id> reviewers,
      @Assisted("ccs") Set<Account.Id> ccs) {
    this.gApi = gApi;
    this.requestContext = requestContext;
    this.changeInfo = changeInfo;
    this.reviewers = reviewers;
    this.ccs = ccs;
  }

  @Override
  public void run() {
    try (ManualRequestContext ctx =
        requestContext.openAs(Account.id(changeInfo.owner._accountId))) {
      //addReviewers();
    }
  }

  private void addReviewers() {
    try {
      // TODO(davido): Switch back to using changes API again,
      // when it supports batch mode for adding reviewers
      Set<Account.Id> existingReviewers =
          gApi.changes().id(changeInfo._number).reviewers().stream()
              .map(r -> Account.id(r._accountId))
              .collect(Collectors.toSet());
      /* Don't add, or change state of, already existing reviewers. */
      Set<Account.Id> reviewersToAdd =
          reviewers.stream()
              .filter(r -> !existingReviewers.contains(r))
              .collect(Collectors.toSet());
      /* If account is already configured to be added as reviewer, don't attempt to add as cc. */
      Set<Account.Id> ccsToAdd =
          ccs.stream()
              .filter(c -> !existingReviewers.contains(c))
              .filter(r -> !reviewersToAdd.contains(r))
              .collect(Collectors.toSet());
      if (reviewersToAdd.isEmpty() && ccsToAdd.isEmpty()) {
        return;
      }

      ReviewInput in = new ReviewInput();
      in.reviewers = new ArrayList<>(reviewers.size() + ccs.size());
      for (Account.Id account : reviewersToAdd) {
        ReviewerInput addReviewerInput = new ReviewerInput();
        addReviewerInput.reviewer = account.toString();
        in.reviewers.add(addReviewerInput);
      }
      for (Account.Id account : ccsToAdd) {
        ReviewerInput input = new ReviewerInput();
        input.state = ReviewerState.CC;
        input.reviewer = account.toString();
        in.reviewers.add(input);
      }
      gApi.changes().id(changeInfo._number).current().review(in);
    } catch (RestApiException e) {
      logger.atSevere().withCause(e).log("Couldn't add reviewers to the change");
    }
  }
}
