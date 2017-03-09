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

import com.google.gerrit.extensions.api.GerritApi;
import com.google.gerrit.extensions.api.changes.AddReviewerInput;
import com.google.gerrit.extensions.api.changes.ReviewInput;
import com.google.gerrit.extensions.restapi.RestApiException;
import com.google.gerrit.reviewdb.client.Account;
import com.google.gerrit.reviewdb.client.Change;
import com.google.inject.Inject;
import com.google.inject.assistedinject.Assisted;

import java.util.ArrayList;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

class DefaultReviewers implements Runnable {
  private static final Logger log = LoggerFactory
      .getLogger(DefaultReviewers.class);

  private final GerritApi gApi;
  private final Change change;
  private final Set<Account> reviewers;

  interface Factory {
    DefaultReviewers create(Change change, Set<Account> reviewers);
  }

  @Inject
  DefaultReviewers(
      GerritApi gApi,
      @Assisted Change change,
      @Assisted Set<Account> reviewers) {
    this.gApi = gApi;
    this.change = change;
    this.reviewers = reviewers;
  }

  @Override
  public void run() {
    addReviewers(reviewers, change);
  }

  /**
   * Append the reviewers to change#{@link Change}
   *
   * @param reviewers Set of reviewers to add
   * @param change {@link Change} to add the reviewers to
   */
  private void addReviewers(Set<Account> reviewers, Change change) {
    try {
      // TODO(davido): Switch back to using changes API again,
      // when it supports batch mode for adding reviewers
      ReviewInput in = new ReviewInput();
      in.reviewers = new ArrayList<>(reviewers.size());
      for (Account account : reviewers) {
        AddReviewerInput addReviewerInput = new AddReviewerInput();
        addReviewerInput.reviewer = account.getId().toString();
        in.reviewers.add(addReviewerInput);
      }
      gApi.changes()
          .id(change.getId().get())
          .current()
          .review(in);
    } catch (RestApiException e) {
      log.error("Couldn't add reviewers to the change", e);
    }
  }
}
