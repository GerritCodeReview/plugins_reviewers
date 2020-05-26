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

package com.googlesource.gerrit.plugins.reviewers.config;

import static com.google.common.truth.Truth.assertThat;

import com.google.common.collect.ImmutableList;
import com.google.gerrit.acceptance.NoHttpd;
import com.google.gerrit.acceptance.TestPlugin;
import com.google.gerrit.entities.Project;
import com.googlesource.gerrit.plugins.reviewers.AbstractReviewersPluginTest;
import com.googlesource.gerrit.plugins.reviewers.ForProjectValidator;
import org.eclipse.jgit.junit.TestRepository;
import org.junit.Before;
import org.junit.Test;

@NoHttpd
@TestPlugin(name = "reviewers", sysModule = "com.googlesource.gerrit.plugins.reviewers.Module")
public class ReviewersConfigIT extends AbstractReviewersPluginTest {
  private static final String BRANCH_MAIN = "branch:main";
  private static final String MALFORMED_FILTER = "branches:master,stable2";
  private static final String NO_FILTER = "*";
  private static final String JANE_DOE = "jane.doe@example.com";
  private static final String JOHN_DOE = "john.doe@example.com";

  @Before
  public void setUp() throws Exception {
    checkoutRefsMetaConfig(testRepo);
  }

  @Test
  public void reviewersConfigSingle() throws Exception {
    createFilters(filter(NO_FILTER).reviewer(JOHN_DOE), filter(BRANCH_MAIN).reviewer(JANE_DOE));

    assertProjectHasFilters(
        project, filter(NO_FILTER).reviewer(JOHN_DOE), filter(BRANCH_MAIN).reviewer(JANE_DOE));
  }

  @Test
  public void reviewersConfigSingleWithCc() throws Exception {
    createFilters(filter(NO_FILTER).reviewer(JOHN_DOE).cc(JANE_DOE));

    assertProjectHasFilters(project, filter(NO_FILTER).reviewer(JOHN_DOE).cc(JANE_DOE));
  }

  @Test
  public void reviewersConfigSingleWithCcSeparateFilters() throws Exception {
    createFilters(filter(NO_FILTER).reviewer(JOHN_DOE), filter(BRANCH_MAIN).cc(JANE_DOE));

    assertProjectHasFilters(
        project, filter(NO_FILTER).reviewer(JOHN_DOE), filter(BRANCH_MAIN).cc(JANE_DOE));
  }

  @Test
  public void reviewersConfigWithMergedInheritanceWithCc() throws Exception {
    Project.NameKey childProject = projectOperations.newProject().parent(project).create();
    TestRepository<?> childTestRepo = checkoutRefsMetaConfig(cloneProject(childProject));

    createFiltersFor(
        childTestRepo, filter(NO_FILTER).reviewer(JANE_DOE), filter(BRANCH_MAIN).cc(JANE_DOE));

    createFilters(filter(NO_FILTER).cc(JOHN_DOE), filter(BRANCH_MAIN).reviewer(JOHN_DOE));

    assertProjectHasFilters(
        childProject,
        filter(NO_FILTER).cc(JOHN_DOE).reviewer(JANE_DOE),
        filter(BRANCH_MAIN).reviewer(JOHN_DOE).cc(JANE_DOE));
  }

  @Test
  public void malformedFilterQuery() throws Exception {
    createFiltersWithError(
        ForProjectValidator.MALFORMED_CONFIG, filter(MALFORMED_FILTER).reviewer(JOHN_DOE));
  }

  private void assertProjectHasFilters(Project.NameKey project, TestFilter... filters) {
    assertThat(filters().withInheritance(project))
        .containsExactlyElementsIn(ImmutableList.copyOf(filters));
  }

  private FiltersFactory filters() {
    return plugin.getSysInjector().getInstance(FiltersFactory.class);
  }
}
