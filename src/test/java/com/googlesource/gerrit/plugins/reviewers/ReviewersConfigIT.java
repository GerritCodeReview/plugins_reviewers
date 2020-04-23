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
import static com.googlesource.gerrit.plugins.reviewers.ReviewerFilterSection.KEY_REVIEWER;
import static com.googlesource.gerrit.plugins.reviewers.ReviewerFilterSection.SECTION_FILTER;
import static com.googlesource.gerrit.plugins.reviewers.ReviewersConfig.FILENAME;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import com.google.gerrit.acceptance.LightweightPluginDaemonTest;
import com.google.gerrit.acceptance.NoHttpd;
import com.google.gerrit.acceptance.TestPlugin;
import com.google.gerrit.acceptance.testsuite.project.ProjectOperations;
import com.google.gerrit.entities.Project;
import com.google.gerrit.entities.RefNames;
import com.google.inject.Inject;
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
    fetch(testRepo, RefNames.REFS_CONFIG + ":refs/heads/config");
    testRepo.reset("refs/heads/config");
  }

  @Test
  public void reviewersConfigSingle() throws Exception {
    Config cfg = new Config();
    cfg.setString(SECTION_FILTER, NO_FILTER, KEY_REVIEWER, JOHN_DOE);
    cfg.setString(SECTION_FILTER, BRANCH_MAIN, KEY_REVIEWER, JANE_DOE);

    pushFactory
        .create(admin.newIdent(), testRepo, "Add reviewers", FILENAME, cfg.toText())
        .to(RefNames.REFS_CONFIG)
        .assertOkStatus();

    assertThat(reviewersConfig().filtersWithInheritance(project))
        .containsExactlyElementsIn(
            ImmutableList.of(
                new ReviewerFilterSection(NO_FILTER, ImmutableSet.of(JOHN_DOE)),
                new ReviewerFilterSection(BRANCH_MAIN, ImmutableSet.of(JANE_DOE))))
        .inOrder();
  }

  @Test
  public void reviewersConfigWithMergedInheritance() throws Exception {
    Config parentCfg = new Config();
    parentCfg.setString(SECTION_FILTER, NO_FILTER, KEY_REVIEWER, JOHN_DOE);
    parentCfg.setString(SECTION_FILTER, BRANCH_MAIN, KEY_REVIEWER, JOHN_DOE);

    pushFactory
        .create(
            admin.newIdent(),
            testRepo,
            "Add reviewers parent project",
            FILENAME,
            parentCfg.toText())
        .to(RefNames.REFS_CONFIG)
        .assertOkStatus();

    Project.NameKey childProject = projectOperations.newProject().parent(project).create();
    TestRepository<?> childTestRepo = cloneProject(childProject);
    fetch(childTestRepo, RefNames.REFS_CONFIG + ":refs/heads/config");
    childTestRepo.reset("refs/heads/config");

    Config cfg = new Config();
    cfg.setString(SECTION_FILTER, NO_FILTER, KEY_REVIEWER, JANE_DOE);
    cfg.setString(SECTION_FILTER, BRANCH_MAIN, KEY_REVIEWER, JANE_DOE);

    pushFactory
        .create(
            admin.newIdent(), childTestRepo, "Add reviewers child project", FILENAME, cfg.toText())
        .to(RefNames.REFS_CONFIG)
        .assertOkStatus();

    assertThat(reviewersConfig().filtersWithInheritance(childProject))
        .containsExactlyElementsIn(
            ImmutableList.of(
                new ReviewerFilterSection(NO_FILTER, ImmutableSet.of(JOHN_DOE, JANE_DOE)),
                new ReviewerFilterSection(BRANCH_MAIN, ImmutableSet.of(JOHN_DOE, JANE_DOE))))
        .inOrder();
  }

  private ReviewersConfig reviewersConfig() {
    return plugin.getSysInjector().getInstance(ReviewersConfig.class);
  }
}
