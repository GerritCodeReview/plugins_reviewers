// Copyright (C) 2014 The Android Open Source Project
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

import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Strings;
import com.google.common.flogger.FluentLogger;
import com.google.gerrit.entities.Project;
import com.google.gerrit.entities.RefNames;
import com.google.gerrit.extensions.annotations.PluginName;
import com.google.gerrit.server.config.PluginConfigFactory;
import com.google.gerrit.server.git.meta.VersionedMetaData;
import com.google.gerrit.server.project.NoSuchProjectException;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.googlesource.gerrit.plugins.reviewers.ReviewerFilter;
import com.googlesource.gerrit.plugins.reviewers.ReviewerType;
import java.io.IOException;
import java.util.List;
import org.eclipse.jgit.errors.ConfigInvalidException;
import org.eclipse.jgit.lib.CommitBuilder;
import org.eclipse.jgit.lib.Config;

/** Global and project local configurations. */
@Singleton
public class ReviewersConfig {
  private static final FluentLogger logger = FluentLogger.forEnclosingClass();

  @VisibleForTesting public static final String FILENAME = "reviewers.config";
  @VisibleForTesting public static final String SECTION_FILTER = "filter";
  @VisibleForTesting public static final String KEY_CC = "cc";
  @VisibleForTesting public static final String KEY_REVIEWER = "reviewer";
  private static final String KEY_ENABLE_REST = "enableREST";
  private static final String KEY_SUGGEST_ONLY = "suggestOnly";
  private static final String KEY_IGNORE_WIP = "ignoreWip";

  private final PluginConfigFactory cfgFactory;
  private final String pluginName;

  private final boolean enableREST;
  private final boolean suggestOnly;
  private final boolean ignoreWip;

  @Inject
  ReviewersConfig(PluginConfigFactory cfgFactory, @PluginName String pluginName) {
    this.cfgFactory = cfgFactory;
    this.pluginName = pluginName;
    Config cfg = cfgFactory.getGlobalPluginConfig(pluginName);
    this.enableREST = cfg.getBoolean(pluginName, null, KEY_ENABLE_REST, true);
    this.suggestOnly = cfg.getBoolean(pluginName, null, KEY_SUGGEST_ONLY, false);
    this.ignoreWip = cfg.getBoolean(pluginName, null, KEY_IGNORE_WIP, true);
  }

  public List<ReviewerFilter> filtersWithInheritance(Project.NameKey projectName) {
    Config cfg;
    try {
      cfg = cfgFactory.getProjectPluginConfigWithMergedInheritance(projectName, pluginName);
    } catch (NoSuchProjectException e) {
      logger.atSevere().log("Unable to get config for project %s", projectName.get());
      cfg = new Config();
    }
    return new ReviewerFilterCollection(cfg).getAll();
  }

  public boolean enableREST() {
    return enableREST;
  }

  public boolean suggestOnly() {
    return suggestOnly;
  }

  public boolean ignoreWip() {
    return ignoreWip;
  }

  public static class ForProject extends VersionedMetaData {
    private Config cfg;
    private ReviewerFilterCollection filters;

    public void addReviewer(String filter, String reviewer, ReviewerType type) {
      switch (type) {
        case REVIEWER:
          filters.get(filter).addReviewer(reviewer);
          break;
        case CC:
          filters.get(filter).addCc(reviewer);
      }
    }

    public void removeReviewer(String filter, String reviewer, ReviewerType type) {
      switch (type) {
        case REVIEWER:
          filters.get(filter).removeReviewer(reviewer);
          break;
        case CC:
          filters.get(filter).removeCc(reviewer);
      }
    }

    public List<ReviewerFilter> getFilters() {
      return filters.getAll();
    }

    @Override
    protected String getRefName() {
      return RefNames.REFS_CONFIG;
    }

    @Override
    protected void onLoad() throws IOException, ConfigInvalidException {
      this.cfg = readConfig(FILENAME);
      this.filters = new ReviewerFilterCollection(cfg);
    }

    @Override
    protected boolean onSave(CommitBuilder commit) throws IOException, ConfigInvalidException {
      if (Strings.isNullOrEmpty(commit.getMessage())) {
        commit.setMessage("Update reviewers configuration\n");
      }
      saveConfig(FILENAME, cfg);
      return true;
    }
  }
}
