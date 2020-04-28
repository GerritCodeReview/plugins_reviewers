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

import static com.google.common.truth.Truth.assertThat;
import static com.google.common.truth.Truth.assertWithMessage;
import static com.google.gerrit.acceptance.GitUtil.fetch;
import static com.google.gerrit.extensions.client.ReviewerState.REVIEWER;
import static com.googlesource.gerrit.plugins.reviewers.ReviewersConfig.FILENAME;
import static com.googlesource.gerrit.plugins.reviewers.ReviewersConfig.KEY_REVIEWER;
import static com.googlesource.gerrit.plugins.reviewers.ReviewersConfig.SECTION_FILTER;
import static java.util.stream.Collectors.toSet;

import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Lists;
import com.google.gerrit.acceptance.LightweightPluginDaemonTest;
import com.google.gerrit.acceptance.NoHttpd;
import com.google.gerrit.acceptance.PushOneCommit;
import com.google.gerrit.acceptance.TestAccount;
import com.google.gerrit.acceptance.TestPlugin;
import com.google.gerrit.acceptance.testsuite.project.ProjectOperations;
import com.google.gerrit.entities.Account;
import com.google.gerrit.entities.BranchNameKey;
import com.google.gerrit.entities.RefNames;
import com.google.gerrit.extensions.common.AccountInfo;
import com.google.gerrit.extensions.restapi.RestApiException;
import com.google.inject.Inject;
import java.util.Arrays;
import java.util.Collection;
import java.util.Set;
import org.eclipse.jgit.lib.Config;
import org.junit.Test;

@NoHttpd
@TestPlugin(name = "reviewers", sysModule = "com.googlesource.gerrit.plugins.reviewers.Module")
public class ReviewersIT extends LightweightPluginDaemonTest {
  @Inject private ProjectOperations projectOperations;

  @Test
  public void addReviewers() throws Exception {
    TestAccount user2 = accountCreator.user2();
    setReviewerFilters(newFilter("*", user, user2));
    String changeId = createChange().getChangeId();
    assertThat(reviewersFor(changeId))
        .containsExactlyElementsIn(ImmutableSet.of(user.id(), user2.id()));
  }

  @Test
  public void addReviewersMatchMultipleSections() throws Exception {
    TestAccount user2 = accountCreator.user2();
    setReviewerFilters(newFilter("*", user), newFilter("\"^a.txt\"", user2));
    String changeId = createChange().getChangeId();
    assertThat(reviewersFor(changeId))
        .containsExactlyElementsIn(ImmutableSet.of(user.id(), user2.id()));
  }

  @Test
  public void doNotAddReviewersFromNonMatchingFilters() throws Exception {
    setReviewerFilters(newFilter("branch:master", user));
    createBranch(BranchNameKey.create(project, "other-branch"));
    // Create a change that matches the filter section.
    createChange("refs/for/master");
    // The actual change we want to test
    String changeId = createChange("refs/for/other-branch").getChangeId();
    assertNoReviewersAddedFor(changeId);
  }

  @Test
  public void addReviewersFromMatchingFilters() throws Exception {
    setReviewerFilters(newFilter("branch:other-branch", user));
    // Create a change that doesn't match the filter section.
    createChange("refs/for/master");
    // The actual change we want to test
    createBranch(BranchNameKey.create(project, "other-branch"));
    String changeId = createChange("refs/for/other-branch").getChangeId();
    assertThat(reviewersFor(changeId)).containsExactlyElementsIn(ImmutableSet.of(user.id()));
  }

  @Test
  public void dontAddReviewersForPrivateChange() throws Exception {
    setReviewerFilters(newFilter("*", user));
    PushOneCommit.Result r = createChange("refs/for/master%private");
    assertThat(r.getChange().change().isPrivate()).isTrue();
    assertNoReviewersAddedFor(r.getChangeId());
  }

  private void setReviewerFilters(ReviewerFilterSection... filters) throws Exception {
    fetch(testRepo, RefNames.REFS_CONFIG + ":refs/heads/config");
    testRepo.reset("refs/heads/config");
    Config cfg = new Config();
    for (ReviewerFilterSection s : filters) {
      cfg.setStringList(
          SECTION_FILTER, s.getFilter(), KEY_REVIEWER, Lists.newArrayList(s.getReviewers()));
    }
    pushFactory
        .create(admin.newIdent(), testRepo, "Add reviewers", FILENAME, cfg.toText())
        .to(RefNames.REFS_CONFIG)
        .assertOkStatus();
    testRepo.reset(projectOperations.project(project).getHead("master"));
  }

  private ReviewerFilterSection newFilter(String filter, TestAccount... reviewers) {
    return new ReviewerFilterSection(
        filter, Arrays.stream(reviewers).map(r -> r.email()).collect(toSet()));
  }

  private Set<Account.Id> reviewersFor(String changeId)
      throws RestApiException, InterruptedException {
    Collection<AccountInfo> reviewers;
    // Wait for 100 ms until the create patch set event
    // is processed by the reviewers plugin
    long wait = 0;
    do {
      reviewers = gApi.changes().id(changeId).get().reviewers.get(REVIEWER);
      if (reviewers == null) {
        Thread.sleep(10);
        wait += 10;
        if (wait > 100) {
          assertWithMessage("Timeout of 100 ms exceeded").fail();
        }
      }
    } while (reviewers == null);
    return reviewers.stream().map(a -> Account.id(a._accountId)).collect(toSet());
  }

  private void assertNoReviewersAddedFor(String changeId)
      throws InterruptedException, RestApiException {
    Collection<AccountInfo> reviewers;
    long wait = 0;
    do {
      Thread.sleep(10);
      wait += 10;
      reviewers = gApi.changes().id(changeId).get().reviewers.get(REVIEWER);
    } while (reviewers == null && wait < 100);

    assertThat(reviewers).isNull();
  }
}
