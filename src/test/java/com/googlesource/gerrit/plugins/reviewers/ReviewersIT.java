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
import static com.google.gerrit.acceptance.GitUtil.fetch;
import static com.google.gerrit.extensions.client.ReviewerState.REVIEWER;
import static com.googlesource.gerrit.plugins.reviewers.config.ReviewersConfig.KEY_REVIEWER;
import static com.googlesource.gerrit.plugins.reviewers.config.ReviewersConfig.SECTION_FILTER;
import static com.googlesource.gerrit.plugins.reviewers.config.ReviewersConfig.FILENAME;
import static java.util.stream.Collectors.toSet;

import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Lists;
import com.google.gerrit.acceptance.LightweightPluginDaemonTest;
import com.google.gerrit.acceptance.NoHttpd;
import com.google.gerrit.acceptance.TestAccount;
import com.google.gerrit.acceptance.TestPlugin;
import com.google.gerrit.acceptance.testsuite.project.ProjectOperations;
import com.google.gerrit.entities.Account;
import com.google.gerrit.entities.BranchNameKey;
import com.google.gerrit.entities.RefNames;
import com.google.inject.Inject;
import java.util.List;
import java.util.Set;
import org.eclipse.jgit.lib.Config;
import org.junit.Test;

@NoHttpd
@TestPlugin(name = "reviewers", sysModule = "com.googlesource.gerrit.plugins.reviewers.TestModule")
public class ReviewersIT extends LightweightPluginDaemonTest {
  @Inject private ProjectOperations projectOperations;

  @Test
  public void addReviewers() throws Exception {
    TestAccount user2 = accountCreator.user2();
    setReviewerFilters(filter("*").reviewer(user).reviewer(user2));
    String changeId = createChange().getChangeId();
    assertThat(reviewersFor(changeId))
        .containsExactlyElementsIn(ImmutableSet.of(user.id(), user2.id()));
  }

  @Test
  public void addReviewersMatchMultipleSections() throws Exception {
    TestAccount user2 = accountCreator.user2();
    setReviewerFilters(filter("*").reviewer(user), filter("\"^a.txt\"").reviewer(user2));
    String changeId = createChange().getChangeId();
    assertThat(reviewersFor(changeId))
        .containsExactlyElementsIn(ImmutableSet.of(user.id(), user2.id()));
  }

  @Test
  public void doNotAddReviewersFromNonMatchingFilters() throws Exception {
    setReviewerFilters(filter("branch:master").reviewer(user));
    createBranch(BranchNameKey.create(project, "other-branch"));
    // Create a change that matches the filter section.
    createChange("refs/for/master");
    // The actual change we want to test
    String changeId = createChange("refs/for/other-branch").getChangeId();
    assertNoReviewersAddedFor(changeId);
  }

  @Test
  public void addReviewersFromMatchingFilters() throws Exception {
    setReviewerFilters(filter("branch:other-branch").reviewer(user));
    // Create a change that doesn't match the filter section.
    createChange("refs/for/master");
    // The actual change we want to test
    createBranch(BranchNameKey.create(project, "other-branch"));
    String changeId = createChange("refs/for/other-branch").getChangeId();
    assertThat(reviewersFor(changeId)).containsExactlyElementsIn(ImmutableSet.of(user.id()));
  }

  private void setReviewerFilters(Filter... filters) throws Exception {
    fetch(testRepo, RefNames.REFS_CONFIG + ":refs/heads/config");
    testRepo.reset("refs/heads/config");
    Config cfg = new Config();
    for (Filter f : filters) {
      cfg.setStringList(SECTION_FILTER, f.filter, KEY_REVIEWER, f.reviewers);
    }
    pushFactory
        .create(admin.newIdent(), testRepo, "Add reviewers", FILENAME, cfg.toText())
        .to(RefNames.REFS_CONFIG)
        .assertOkStatus();
    testRepo.reset(projectOperations.project(project).getHead("master"));
  }

  private Set<Account.Id> reviewersFor(String changeId) throws Exception {
    return gApi.changes().id(changeId).get().reviewers.get(REVIEWER).stream()
        .map(a -> Account.id(a._accountId))
        .collect(toSet());
  }

  private void assertNoReviewersAddedFor(String changeId) throws Exception {
    assertThat(gApi.changes().id(changeId).get().reviewers.get(REVIEWER)).isNull();
  }

  private Filter filter(String filter) {
    return new Filter(filter);
  }

  private class Filter {
    List<String> reviewers;
    String filter;

    Filter(String filter) {
      this.filter = filter;
      this.reviewers = Lists.newArrayList();
    }

    Filter reviewer(TestAccount reviewer) {
      reviewers.add(reviewer.email());
      return this;
    }
  }
}
