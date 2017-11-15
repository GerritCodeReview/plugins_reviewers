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

import com.google.common.base.Joiner;
import com.google.gerrit.extensions.annotations.PluginName;
import com.google.gerrit.extensions.api.GerritApi;
import com.google.gerrit.extensions.api.changes.AddReviewerInput;
import com.google.gerrit.extensions.api.changes.NotifyHandling;
import com.google.gerrit.extensions.api.changes.ReviewInput;
import com.google.gerrit.extensions.restapi.RestApiException;
import com.google.gerrit.reviewdb.client.Account;
import com.google.gerrit.reviewdb.client.Change;
import com.google.gerrit.server.config.PluginConfigFactory;
import com.google.inject.Inject;
import com.google.inject.assistedinject.Assisted;

import org.eclipse.jgit.lib.Config;

import java.util.ArrayList;
import java.util.Set;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

class DefaultReviewers implements Runnable {
  private static final Logger log = LoggerFactory.getLogger(DefaultReviewers.class);

  private final GerritApi gApi;
  private final Change change;
  private final Set<Account> reviewers;
  private final Set<ReviewerFilterSection> filters;
  private final String plugin;

  @Inject private PluginConfigFactory cfg;

  interface Factory {
    DefaultReviewers create(
        Change change, Set<Account> reviewers, Set<ReviewerFilterSection> filters);
  }

  @Inject
  DefaultReviewers(
      GerritApi gApi,
      @Assisted Change change,
      @Assisted Set<Account> reviewers,
      @Assisted Set<ReviewerFilterSection> filters,
      @PluginName String plugin) {
    this.gApi = gApi;
    this.change = change;
    this.reviewers = reviewers;
    this.filters = filters;
    this.plugin = plugin;
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
        if (account.isActive()) {
          addReviewerInput.reviewer = account.getId().toString();
          in.reviewers.add(addReviewerInput);
        }
      }

      Config config = cfg.getGlobalPluginConfig(plugin);
      if (config.getBoolean("reviewers", "comment", false)) {
        ReviewInput review = new ReviewInput().message(buildMessage());
        review.notify = NotifyHandling.NONE;
        gApi.changes().id(change.getId().get()).current().review(review);
      }

      gApi.changes().id(change.getId().get()).current().review(in);
    } catch (RestApiException e) {
      log.error("Couldn't add reviewers to the change", e);
    }
  }

  private String buildMessage() {
    StringBuilder builder = new StringBuilder();
    builder.append("Matched automatic invitation rules: \n\n");
    for (ReviewerFilterSection filter : filters) {
      builder.append(filter.getFilter()).append('\n');
      builder.append('\t').append(Joiner.on(", ").join(filter.getReviewers())).append('\n');
    }

    return builder.toString();
  }
}
