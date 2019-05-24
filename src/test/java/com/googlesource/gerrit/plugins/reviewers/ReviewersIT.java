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
import static com.google.common.truth.Truth.assert_;
import static com.google.gerrit.acceptance.GitUtil.fetch;
import static com.google.gerrit.extensions.client.ReviewerState.REVIEWER;
import static com.googlesource.gerrit.plugins.reviewers.ReviewersConfig.FILENAME;
import static com.googlesource.gerrit.plugins.reviewers.ReviewersConfig.KEY_REVIEWER;
import static com.googlesource.gerrit.plugins.reviewers.ReviewersConfig.SECTION_FILTER;
import static java.util.stream.Collectors.toSet;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import com.google.gerrit.acceptance.LightweightPluginDaemonTest;
import com.google.gerrit.acceptance.NoHttpd;
import com.google.gerrit.acceptance.TestAccount;
import com.google.gerrit.acceptance.TestPlugin;
import com.google.gerrit.acceptance.testsuite.project.ProjectOperations;
import com.google.gerrit.extensions.common.AccountInfo;
import com.google.gerrit.reviewdb.client.BranchNameKey;
import com.google.gerrit.reviewdb.client.RefNames;
import com.google.inject.Inject;
import java.util.Collection;
import org.eclipse.jgit.lib.Config;
import org.eclipse.jgit.revwalk.RevCommit;
import org.junit.Before;
import org.junit.Test;

@NoHttpd
@TestPlugin(name = "reviewers", sysModule = "com.googlesource.gerrit.plugins.reviewers.Module")
public class ReviewersIT extends LightweightPluginDaemonTest {
  @Inject private ProjectOperations projectOperations;

  @Before
  public void setUp() throws Exception {
    fetch(testRepo, RefNames.REFS_CONFIG + ":refs/heads/config");
    testRepo.reset("refs/heads/config");
  }

  @Test
  public void addReviewers() throws Exception {
    RevCommit oldHead = projectOperations.project(project).getHead("master");
    TestAccount user2 = accountCreator.user2();

    Config cfg = new Config();
    cfg.setStringList(
        SECTION_FILTER, "*", KEY_REVIEWER, ImmutableList.of(user.email(), user2.email()));

    pushFactory
        .create(admin.newIdent(), testRepo, "Add reviewers", FILENAME, cfg.toText())
        .to(RefNames.REFS_CONFIG)
        .assertOkStatus();

    testRepo.reset(oldHead);
    String changeId = createChange().getChangeId();

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
          assert_().fail("Timeout of 100 ms exceeded");
        }
      }
    } while (reviewers == null);

    assertThat(reviewers.stream().map(a -> a._accountId).collect(toSet()))
        .containsExactlyElementsIn(
            ImmutableSet.of(admin.id().get(), user.id().get(), user2.id().get()));
  }

  @Test
  public void addReviewersMatchMultipleSections() throws Exception {
    RevCommit oldHead = projectOperations.project(project).getHead("master");
    TestAccount user2 = accountCreator.user2();

    Config cfg = new Config();
    cfg.setStringList(SECTION_FILTER, "*", KEY_REVIEWER, ImmutableList.of(user.email()));
    cfg.setStringList(SECTION_FILTER, "^a.txt", KEY_REVIEWER, ImmutableList.of(user2.email()));

    pushFactory
        .create(admin.newIdent(), testRepo, "Add reviewers", FILENAME, cfg.toText())
        .to(RefNames.REFS_CONFIG)
        .assertOkStatus();

    testRepo.reset(oldHead);
    String changeId = createChange().getChangeId();

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
          assert_().fail("Timeout of 100 ms exceeded");
        }
      }
    } while (reviewers == null);

    assertThat(reviewers.stream().map(a -> a._accountId).collect(toSet()))
        .containsExactlyElementsIn(
            ImmutableSet.of(admin.id().get(), user.id().get(), user2.id().get()));
  }

  @Test
  public void doNotAddReviewersFromNonMatchingFilters() throws Exception {
    RevCommit oldHead = projectOperations.project(project).getHead("master");

    Config cfg = new Config();
    cfg.setString(SECTION_FILTER, "branch:master", KEY_REVIEWER, user.email());

    pushFactory
        .create(admin.newIdent(), testRepo, "Add reviewers", FILENAME, cfg.toText())
        .to(RefNames.REFS_CONFIG)
        .assertOkStatus();

    testRepo.reset(oldHead);

    createBranch(BranchNameKey.create(project, "other-branch"));

    // Create a change that matches the filter section.
    createChange("refs/for/master");

    // The actual change we want to test
    String changeId = createChange("refs/for/other-branch").getChangeId();

    Collection<AccountInfo> reviewers;
    long wait = 0;
    do {
      Thread.sleep(10);
      wait += 10;
      reviewers = gApi.changes().id(changeId).get().reviewers.get(REVIEWER);
    } while (reviewers == null && wait < 100);

    assertThat(reviewers).isNull();
  }

  @Test
  public void addReviewersFromMatchingFilters() throws Exception {
    RevCommit oldHead = projectOperations.project(project).getHead("master");

    Config cfg = new Config();
    cfg.setString(SECTION_FILTER, "branch:other-branch", KEY_REVIEWER, user.email());

    pushFactory
        .create(admin.newIdent(), testRepo, "Add reviewers", FILENAME, cfg.toText())
        .to(RefNames.REFS_CONFIG)
        .assertOkStatus();

    testRepo.reset(oldHead);

    createBranch(BranchNameKey.create(project, "other-branch"));

    // Create a change that doesn't match the filter section.
    createChange("refs/for/master");

    // The actual change we want to test
    String changeId = createChange("refs/for/other-branch").getChangeId();

    Collection<AccountInfo> reviewers;
    long wait = 0;
    do {
      Thread.sleep(10);
      wait += 10;
      reviewers = gApi.changes().id(changeId).get().reviewers.get(REVIEWER);
    } while (reviewers == null && wait < 100);

    assertThat(reviewers.stream().map(a -> a._accountId).collect(toSet()))
        .containsExactlyElementsIn(ImmutableSet.of(admin.id().get(), user.id().get()));
  }
}
