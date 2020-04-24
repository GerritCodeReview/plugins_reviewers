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
import com.google.gerrit.extensions.api.changes.AddReviewerInput;
import com.google.gerrit.extensions.api.changes.ReviewInput;
import com.google.gerrit.extensions.client.ReviewerState;
import com.google.gerrit.extensions.common.ChangeInfo;
import com.google.gerrit.extensions.restapi.RestApiException;
import com.google.gerrit.server.CurrentUser;
import com.google.gerrit.server.IdentifiedUser;
import com.google.gerrit.server.util.RequestContext;
import com.google.gerrit.server.util.ThreadLocalRequestContext;
import java.util.ArrayList;
import java.util.Set;

abstract class AddReviewers implements Runnable {
  private static final FluentLogger logger = FluentLogger.forEnclosingClass();

  private final ThreadLocalRequestContext tl;
  protected final GerritApi gApi;
  protected final IdentifiedUser.GenericFactory identifiedUserFactory;
  protected final ChangeInfo changeInfo;

  AddReviewers(
      ThreadLocalRequestContext tl,
      GerritApi gApi,
      IdentifiedUser.GenericFactory identifiedUserFactory,
      ChangeInfo changeInfo) {
    this.tl = tl;
    this.gApi = gApi;
    this.identifiedUserFactory = identifiedUserFactory;
    this.changeInfo = changeInfo;
  }

  abstract Set<Account.Id> getReviewers();

  abstract Set<Account.Id> getCcs();

  @Override
  public void run() {
    RequestContext old =
        tl.setContext(
            new RequestContext() {

              @Override
              public CurrentUser getUser() {
                return identifiedUserFactory.create(Account.id(changeInfo.owner._accountId));
              }
            });
    try {
      addReviewers();
    } finally {
      tl.setContext(old);
    }
  }

  private void addReviewers() {
    try {
      // TODO(davido): Switch back to using changes API again,
      // when it supports batch mode for adding reviewers

      Set<Account.Id> reviewers = getReviewers();
      Set<Account.Id> ccs = getCcs();
      ReviewInput in = new ReviewInput();
      in.reviewers = new ArrayList<>(reviewers.size() + ccs.size());
      for (Account.Id account : reviewers) {
        AddReviewerInput addReviewerInput = new AddReviewerInput();
        addReviewerInput.reviewer = account.toString();
        in.reviewers.add(addReviewerInput);
      }
      for (Account.Id account : ccs) {
        AddReviewerInput input = new AddReviewerInput();
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
