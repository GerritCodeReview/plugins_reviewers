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

import static com.google.common.collect.ImmutableList.toImmutableList;
import static com.google.common.truth.Truth.assertThat;
import static com.google.gerrit.acceptance.GitUtil.fetch;
import static com.googlesource.gerrit.plugins.reviewers.FilterData.filter;
import static com.googlesource.gerrit.plugins.reviewers.ReviewersConfig.FILENAME;
import static com.googlesource.gerrit.plugins.reviewers.ReviewersConfig.KEY_REVIEWER;
import static com.googlesource.gerrit.plugins.reviewers.ReviewersConfig.SECTION_FILTER;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import com.google.gerrit.acceptance.LightweightPluginDaemonTest;
import com.google.gerrit.acceptance.NoHttpd;
import com.google.gerrit.acceptance.TestPlugin;
import com.google.gerrit.acceptance.testsuite.project.ProjectOperations;
import com.google.gerrit.entities.Project;
import com.google.gerrit.entities.RefNames;
import com.google.inject.Inject;
import java.util.Arrays;
import org.eclipse.jgit.junit.TestRepository;
import org.eclipse.jgit.lib.Config;
import org.junit.Before;
import org.junit.Test;

@NoHttpd
@TestPlugin(name = "reviewers", sysModule = "com.googlesource.gerrit.plugins.reviewers.Module")
public class ReviewersConfigIT extends LightweightPluginDaemonTest {
  private static final String BRANCH_MAIN = "branch:main";
  private static final String NO_FILTER = "*";
  private static final String JANE_DOE = "jane.doe@example.com";
  private static final String JOHN_DOE = "john.doe@example.com";

  @Inject private ProjectOperations projectOperations;

  @Before
  public void setUp() throws Exception {
    prepareTestRepo(testRepo);
  }

  @Test
  public void reviewersConfigSingle() throws Exception {
    createFilters(
        testRepo, filter(NO_FILTER).reviewer(JOHN_DOE), filter(BRANCH_MAIN).reviewer(JANE_DOE));

    assertProjectHasFilters(
        project, filter(NO_FILTER).reviewer(JOHN_DOE), filter(BRANCH_MAIN).reviewer(JANE_DOE));
  }

  @Test
  public void reviewersConfigWithMergedInheritance() throws Exception {
    Project.NameKey childProject = projectOperations.newProject().parent(project).create();
    TestRepository<?> childTestRepo = prepareTestRepo(cloneProject(childProject));

    createFilters(
        childTestRepo,
        filter(NO_FILTER).reviewer(JANE_DOE),
        filter(BRANCH_MAIN).reviewer(JANE_DOE));

    createFilters(
        testRepo, filter(NO_FILTER).reviewer(JOHN_DOE), filter(BRANCH_MAIN).reviewer(JOHN_DOE));

    assertProjectHasFilters(
        childProject,
        filter(NO_FILTER).reviewer(JOHN_DOE).reviewer(JANE_DOE),
        filter(BRANCH_MAIN).reviewer(JOHN_DOE).reviewer(JANE_DOE));
  }

  private TestRepository<?> prepareTestRepo(TestRepository<?> repo) throws Exception {
    fetch(repo, RefNames.REFS_CONFIG + ":refs/heads/config");
    repo.reset("refs/heads/config");
    return repo;
  }

  private void assertProjectHasFilters(Project.NameKey project, FilterData... filters) {
    assertThat(reviewersConfig().forProject(project).getReviewerFilterSections())
        .containsExactlyElementsIn(toSections(filters))
        .inOrder();
  }

  private ImmutableList<ReviewerFilterSection> toSections(FilterData... filters) {
    return Arrays.stream(filters)
        .map(f -> new ReviewerFilterSection(f.filter, ImmutableSet.copyOf(f.reviewers)))
        .collect(toImmutableList());
  }

  private void createFilters(TestRepository<?> repo, FilterData... filters) throws Exception {
    Config cfg = new Config();
    Arrays.stream(filters)
        .forEach(f -> cfg.setStringList(SECTION_FILTER, f.filter, KEY_REVIEWER, f.reviewers));
    pushFactory
        .create(admin.newIdent(), repo, "Add reviewers", FILENAME, cfg.toText())
        .to(RefNames.REFS_CONFIG)
        .assertOkStatus();
  }

  private ReviewersConfig reviewersConfig() {
    return plugin.getSysInjector().getInstance(ReviewersConfig.class);
  }
}
