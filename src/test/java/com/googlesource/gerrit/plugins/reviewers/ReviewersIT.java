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
import com.google.gerrit.extensions.common.AccountInfo;
import com.google.gerrit.reviewdb.client.RefNames;
import java.util.Collection;
import org.eclipse.jgit.lib.Config;
import org.eclipse.jgit.revwalk.RevCommit;
import org.junit.Before;
import org.junit.Test;

@NoHttpd
@TestPlugin(name = "reviewers", sysModule = "com.googlesource.gerrit.plugins.reviewers.Module")
public class ReviewersIT extends LightweightPluginDaemonTest {
  private static final String NO_FILTER = "*";

  @Before
  @Override
  public void setUp() throws Exception {
    super.setUp();
    fetch(testRepo, RefNames.REFS_CONFIG + ":refs/heads/config");
    testRepo.reset("refs/heads/config");
  }

  @Test
  public void addReviewers() throws Exception {
    RevCommit oldHead = getRemoteHead();
    TestAccount user2 = accounts.user2();

    Config cfg = new Config();
    cfg.setStringList(
        SECTION_FILTER, NO_FILTER, KEY_REVIEWER, ImmutableList.of(user.email, user2.email));

    pushFactory
        .create(db, admin.getIdent(), testRepo, "Add reviewers", FILENAME, cfg.toText())
        .to(RefNames.REFS_CONFIG)
        .assertOkStatus();

    testRepo.reset(oldHead);
    String changeId = createChange().getChangeId();

    Collection<AccountInfo> reviewers;
    // Repeat until the create patch set event is processed by the reviewers plugin
    do {
      reviewers = gApi.changes().id(changeId).get().reviewers.get(REVIEWER);
    } while (reviewers == null);

    assertThat(reviewers.stream().map(a -> a._accountId).collect(toSet()))
        .containsExactlyElementsIn(ImmutableSet.of(admin.id.get(), user.id.get(), user2.id.get()));
  }
}
