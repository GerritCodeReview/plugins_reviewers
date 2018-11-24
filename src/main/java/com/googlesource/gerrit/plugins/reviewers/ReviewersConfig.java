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
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
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
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import org.eclipse.jgit.errors.ConfigInvalidException;
import org.eclipse.jgit.lib.CommitBuilder;
import org.eclipse.jgit.lib.Config;

@Singleton
public class ReviewersConfig {
  private static final FluentLogger logger = FluentLogger.forEnclosingClass();

  static final String FILENAME = "reviewers.config";
  static final String SECTION_FILTER = "filter";
  static final String KEY_REVIEWER = "reviewer";
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

  public ForProject forProject(Project.NameKey projectName) {
    Config cfg;
    try {
      cfg = cfgFactory.getProjectPluginConfigWithMergedInheritance(projectName, pluginName);
    } catch (NoSuchProjectException e) {
      logger.atSevere().log("Unable to get config for project %s", projectName.get());
      cfg = new Config();
    }
    return new ForProject(cfg);
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

    ForProject(Config cfg) {
      this.cfg = cfg;
    }

    List<ReviewerFilterSection> getReviewerFilterSections() {
      ImmutableList.Builder<ReviewerFilterSection> b = ImmutableList.builder();
      for (String f : cfg.getSubsections(SECTION_FILTER)) {
        b.add(newReviewerFilterSection(f));
      }
      return b.build();
    }

    void addReviewer(String filter, String reviewer) {
      if (!newReviewerFilterSection(filter).getReviewers().contains(reviewer)) {
        List<String> values =
            new ArrayList<>(Arrays.asList(cfg.getStringList(SECTION_FILTER, filter, KEY_REVIEWER)));
        values.add(reviewer);
        cfg.setStringList(SECTION_FILTER, filter, KEY_REVIEWER, values);
      }
    }

    void removeReviewer(String filter, String reviewer) {
      if (newReviewerFilterSection(filter).getReviewers().contains(reviewer)) {
        List<String> values =
            new ArrayList<>(Arrays.asList(cfg.getStringList(SECTION_FILTER, filter, KEY_REVIEWER)));
        values.remove(reviewer);
        if (values.isEmpty()) {
          cfg.unsetSection(SECTION_FILTER, filter);
        } else {
          cfg.setStringList(SECTION_FILTER, filter, KEY_REVIEWER, values);
        }
      }
    }

    private ReviewerFilterSection newReviewerFilterSection(String filter) {
      ImmutableSet.Builder<String> b = ImmutableSet.builder();
      for (String reviewer : cfg.getStringList(SECTION_FILTER, filter, KEY_REVIEWER)) {
        b.add(reviewer);
      }
      return new ReviewerFilterSection(filter, b.build());
    }

    @Override
    protected String getRefName() {
      return RefNames.REFS_CONFIG;
    }

    @Override
    protected void onLoad() throws IOException, ConfigInvalidException {
      cfg = readConfig(FILENAME);
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
