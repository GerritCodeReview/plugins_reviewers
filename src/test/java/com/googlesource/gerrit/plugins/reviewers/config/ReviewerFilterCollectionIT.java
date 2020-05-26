package com.googlesource.gerrit.plugins.reviewers.config;

import static com.google.common.truth.Truth.assertThat;
import static com.googlesource.gerrit.plugins.reviewers.config.ForProject.KEY_REVIEWER;
import static com.googlesource.gerrit.plugins.reviewers.config.ForProject.SECTION_FILTER;

import com.google.gerrit.acceptance.NoHttpd;
import com.google.gerrit.acceptance.TestPlugin;
import com.google.gerrit.extensions.config.FactoryModule;
import com.googlesource.gerrit.plugins.reviewers.AbstractReviewersPluginTest;
import org.eclipse.jgit.lib.Config;
import org.junit.Test;

@NoHttpd
@TestPlugin(
    name = "reviewers",
    sysModule =
        "com.googlesource.gerrit.plugins.reviewers.config.ReviwerFilterCollectionIT.Testmodule")
public class ReviewerFilterCollectionIT extends AbstractReviewersPluginTest {

  @Test
  public void malformedFilterErrorPropagated() throws Exception {
    String malformedQuery = "malformed:query";
    Config malformed = new Config();
    malformed.setString(SECTION_FILTER, malformedQuery, KEY_REVIEWER, "User");
    assertThat(filters().create(malformed).get(malformedQuery).getFilterError())
        .isEqualTo(String.format("Unsupported operator %s", malformedQuery));
  }

  private ReviewerFilterCollection.Factory filters() {
    return plugin.getSysInjector().getInstance(ReviewerFilterCollection.Factory.class);
  }

  public static class TestModule extends FactoryModule {

    @Override
    public void configure() {
      factory(ReviewerFilterCollection.Factory.class);
    }
  }
}
