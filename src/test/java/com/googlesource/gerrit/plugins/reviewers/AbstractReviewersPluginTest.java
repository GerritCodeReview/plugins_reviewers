// Copyright (C) 2020 The Android Open Source Project
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

import static com.google.gerrit.acceptance.GitUtil.fetch;
import static com.googlesource.gerrit.plugins.reviewers.config.ReviewersConfig.FILENAME;
import static com.googlesource.gerrit.plugins.reviewers.config.ReviewersConfig.KEY_REVIEWER;
import static com.googlesource.gerrit.plugins.reviewers.config.ReviewersConfig.SECTION_FILTER;

import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import com.google.gerrit.acceptance.LightweightPluginDaemonTest;
import com.google.gerrit.acceptance.PushOneCommit;
import com.google.gerrit.acceptance.TestAccount;
import com.google.gerrit.acceptance.testsuite.project.ProjectOperations;
import com.google.gerrit.entities.RefNames;
import com.google.inject.Inject;
import java.util.Arrays;
import java.util.Optional;
import java.util.Set;
import org.eclipse.jgit.junit.TestRepository;
import org.eclipse.jgit.lib.Config;
import org.junit.Ignore;

/** Base class for reviewer plugin tests. */
@Ignore
public class AbstractReviewersPluginTest extends LightweightPluginDaemonTest {
  @Inject protected ProjectOperations projectOperations;

  protected void createFilters(TestFilter... filters) throws Exception {
    createFiltersFor(testRepo, Optional.empty(), filters);
  }

  protected void createFiltersFor(TestRepository<?> repo, TestFilter... filters) throws Exception {
    createFiltersFor(repo, Optional.empty(), filters);
  }

  protected void createFiltersWithError(String errorMessage, TestFilter... filters)
      throws Exception {
    createFiltersFor(testRepo, Optional.of(errorMessage), filters);
  }

  protected void createFiltersFor(
      TestRepository<?> repo, Optional<String> errorMessage, TestFilter... filters)
      throws Exception {
    String previousHead = repo.getRepository().getBranch();
    checkoutRefsMetaConfig(repo);
    Config cfg = new Config();
    Arrays.stream(filters)
        .forEach(
            f ->
                cfg.setStringList(
                    SECTION_FILTER, f.filter, KEY_REVIEWER, Lists.newArrayList(f.reviewers)));
    PushOneCommit.Result result =
        pushFactory
            .create(admin.newIdent(), repo, "Add reviewers", FILENAME, cfg.toText())
            .to(RefNames.REFS_CONFIG);
    if (errorMessage.isPresent()) {
      result.assertErrorStatus(errorMessage.get());
    } else {
      result.assertOkStatus();
    }
    repo.reset(previousHead);
  }

  protected TestRepository<?> checkoutRefsMetaConfig(TestRepository<?> repo) throws Exception {
    fetch(repo, RefNames.REFS_CONFIG + ":refs/heads/config");
    repo.reset("refs/heads/config");
    return repo;
  }

  protected TestFilter filter(String filter, Set<String> reviewers) {
    return new TestFilter(filter, reviewers);
  }

  protected TestFilter filter(String filter) {
    return new TestFilter(filter);
  }

  protected static class TestFilter extends ReviewerFilter {

    public TestFilter(String filter, Set<String> reviewers) {
      this.filter = filter;
      this.reviewers = reviewers;
    }

    public TestFilter(String filter) {
      this(filter, Sets.newHashSet());
    }

    public TestFilter reviewer(String reviewerId) {
      reviewers.add(reviewerId);
      return this;
    }

    public TestFilter reviewer(TestAccount reviewer) {
      return reviewer(reviewer.email());
    }
  }
}
