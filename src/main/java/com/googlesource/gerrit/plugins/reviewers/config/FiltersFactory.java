// Copyright (C) 2021 The Android Open Source Project
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
  private final ReviewerFilterCollection.Factory filterCollectionFactory;
  private final GlobalConfig globalConfig;
  private final String pluginName;

  @Inject
  public FiltersFactory(
      PluginConfigFactory configFactory,
      ReviewerFilterCollection.Factory filterCollectionFactory,
      GlobalConfig globalConfig,
      @PluginName String pluginName) {
    this.configFactory = configFactory;
    this.filterCollectionFactory = filterCollectionFactory;
    this.globalConfig = globalConfig;
    this.pluginName = pluginName;
  }

  public List<ReviewerFilter> withInheritance(Project.NameKey projectName) {
    Config cfg;
    try {
      if (globalConfig.mergeFilters()) {
        cfg = configFactory.getProjectPluginConfigWithMergedInheritance(projectName, pluginName);
      } else {
        cfg = configFactory.getProjectPluginConfigWithInheritance(projectName, pluginName);
      }
    } catch (NoSuchProjectException e) {
      logger.atSevere().log("Unable to get config for project %s", projectName.get());
      cfg = new Config();
    }
    return filterCollectionFactory.create(cfg).getAll();
  }
}
