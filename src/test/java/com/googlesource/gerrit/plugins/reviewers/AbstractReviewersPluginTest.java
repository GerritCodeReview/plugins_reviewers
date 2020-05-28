package com.googlesource.gerrit.plugins.reviewers;

import static com.google.gerrit.acceptance.GitUtil.fetch;
import static com.googlesource.gerrit.plugins.reviewers.ReviewersConfig.FILENAME;
import static com.googlesource.gerrit.plugins.reviewers.ReviewersConfig.KEY_REVIEWER;
import static com.googlesource.gerrit.plugins.reviewers.ReviewersConfig.SECTION_FILTER;

import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Lists;
import com.google.gerrit.acceptance.LightweightPluginDaemonTest;
import com.google.gerrit.acceptance.TestAccount;
import com.google.gerrit.acceptance.testsuite.project.ProjectOperations;
import com.google.gerrit.entities.RefNames;
import com.google.inject.Inject;
import java.util.Arrays;
import java.util.List;
import org.eclipse.jgit.junit.TestRepository;
import org.eclipse.jgit.lib.Config;

public class AbstractReviewersPluginTest extends LightweightPluginDaemonTest {
  @Inject protected ProjectOperations projectOperations;

  protected void createFilters(FilterData... filters) throws Exception {
    createFiltersFor(testRepo, filters);
  }

  protected void createFiltersFor(TestRepository<?> repo, FilterData... filters) throws Exception {
    String previousHead = repo.getRepository().getBranch();
    checkoutRefsMetaConfig(repo);
    Config cfg = new Config();
    Arrays.stream(filters)
        .forEach(f -> cfg.setStringList(SECTION_FILTER, f.filter, KEY_REVIEWER, f.reviewers));
    pushFactory
        .create(admin.newIdent(), repo, "Add reviewers", FILENAME, cfg.toText())
        .to(RefNames.REFS_CONFIG)
        .assertOkStatus();
    repo.reset(previousHead);
  }

  protected TestRepository<?> checkoutRefsMetaConfig(TestRepository<?> repo) throws Exception {
    fetch(repo, RefNames.REFS_CONFIG + ":refs/heads/config");
    repo.reset("refs/heads/config");
    return repo;
  }

  protected FilterData filter(String filter) {
    return new FilterData(filter);
  }

  /** Assists tests to define a filter. */
  protected static class FilterData {
    List<String> reviewers;
    String filter;

    FilterData(String filter) {
      this.filter = filter;
      this.reviewers = Lists.newArrayList();
    }

    FilterData reviewer(TestAccount reviewer) {
      return reviewer(reviewer.email());
    }

    FilterData reviewer(String reviewer) {
      reviewers.add(reviewer);
      return this;
    }

    public ReviewerFilterSection asSection() {
      return new ReviewerFilterSection(filter, ImmutableSet.copyOf(reviewers));
    }
  }
}
