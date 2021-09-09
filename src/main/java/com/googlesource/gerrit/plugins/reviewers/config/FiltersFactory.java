package com.googlesource.gerrit.plugins.reviewers.config;

import com.google.common.flogger.FluentLogger;
import com.google.gerrit.entities.Project;
import com.google.gerrit.extensions.annotations.PluginName;
import com.google.gerrit.server.config.PluginConfigFactory;
import com.google.gerrit.server.project.NoSuchProjectException;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.googlesource.gerrit.plugins.reviewers.ReviewerFilter;
import java.util.List;
import org.eclipse.jgit.lib.Config;

@Singleton
public class FiltersFactory {
  private static final FluentLogger logger = FluentLogger.forEnclosingClass();

  private final PluginConfigFactory configFactory;
  private final String pluginName;

  @Inject
  public FiltersFactory(PluginConfigFactory configFactory, @PluginName String pluginName) {
    this.configFactory = configFactory;
    this.pluginName = pluginName;
  }

  public List<ReviewerFilter> withInheritance(Project.NameKey projectName) {
    Config cfg;
    try {
      cfg = configFactory.getProjectPluginConfigWithMergedInheritance(projectName, pluginName);
    } catch (NoSuchProjectException e) {
      logger.atSevere().log("Unable to get config for project %s", projectName.get());
      cfg = new Config();
    }
    return new ReviewerFilterCollection(cfg).getAll();
  }
}
