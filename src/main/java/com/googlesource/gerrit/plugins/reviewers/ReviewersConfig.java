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

package com.googlesource.gerrit.plugins.reviewers;

import com.google.common.base.Strings;
import com.google.common.flogger.FluentLogger;
import com.google.gerrit.extensions.annotations.PluginName;
import com.google.gerrit.reviewdb.client.Project;
import com.google.gerrit.reviewdb.client.RefNames;
import com.google.gerrit.server.config.PluginConfigFactory;
import com.google.gerrit.server.git.meta.VersionedMetaData;
import com.google.gerrit.server.project.NoSuchProjectException;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import java.io.IOException;
import java.util.List;
import org.eclipse.jgit.errors.ConfigInvalidException;
import org.eclipse.jgit.lib.CommitBuilder;
import org.eclipse.jgit.lib.Config;

@Singleton
public class ReviewersConfig {
  private static final FluentLogger logger = FluentLogger.forEnclosingClass();

  static final String FILENAME = "reviewers.config";
  private static final String KEY_ENABLE_REST = "enableREST";
  private static final String KEY_SUGGEST_ONLY = "suggestOnly";
  private static final String KEY_IGNORE_WIP = "ignoreWip";
  private static final String KEY_IGNORE_PRIVATE = "ignorePrivate";

  private final PluginConfigFactory cfgFactory;
  private final String pluginName;

  private final boolean enableREST;
  private final boolean suggestOnly;
  private final boolean ignoreWip;
  private final boolean ignorePrivate;

  @Inject
  ReviewersConfig(PluginConfigFactory cfgFactory, @PluginName String pluginName) {
    this.cfgFactory = cfgFactory;
    this.pluginName = pluginName;
    Config cfg = cfgFactory.getGlobalPluginConfig(pluginName);
    this.enableREST = cfg.getBoolean(pluginName, null, KEY_ENABLE_REST, true);
    this.suggestOnly = cfg.getBoolean(pluginName, null, KEY_SUGGEST_ONLY, false);
    this.ignoreWip = cfg.getBoolean(pluginName, null, KEY_IGNORE_WIP, true);
    this.ignorePrivate = cfg.getBoolean(pluginName, null, KEY_IGNORE_PRIVATE, true);
  }

  public List<ReviewerFilterSection> filtersWithInheritance(Project.NameKey projectName) {
    Config cfg;
    try {
      cfg = cfgFactory.getProjectPluginConfigWithMergedInheritance(projectName, pluginName);
    } catch (NoSuchProjectException e) {
      logger.atSevere().log("Unable to get config for project %s", projectName.get());
      cfg = new Config();
    }
    return new ReviewerFilterSection.Factory(cfg).getAll();
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

  public boolean ignorePrivate() {
    return ignorePrivate;
  }

  static class ForProject extends VersionedMetaData {
    private Config cfg;
    private ReviewerFilterSection.Factory filterSections;

    ForProject() {}

    List<ReviewerFilterSection> getReviewerFilterSections() {
      return this.filterSections.getAll();
    }

    void addReviewer(String filter, String reviewer) {
      filterSections.forFilter(filter).addReviewer(reviewer);
    }

    void removeReviewer(String filter, String reviewer) {
      filterSections.forFilter(filter).removeReviewer(reviewer);
    }

    @Override
    protected String getRefName() {
      return RefNames.REFS_CONFIG;
    }

    @Override
    protected void onLoad() throws IOException, ConfigInvalidException {
      this.cfg = readConfig(FILENAME);
      this.filterSections = new ReviewerFilterSection.Factory(cfg);
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
