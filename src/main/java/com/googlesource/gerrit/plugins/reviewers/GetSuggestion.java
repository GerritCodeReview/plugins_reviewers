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

import com.google.gerrit.extensions.annotations.PluginName;
import com.google.gerrit.extensions.restapi.AuthException;
import com.google.gerrit.extensions.restapi.BadRequestException;
import com.google.gerrit.extensions.restapi.ResourceConflictException;
import com.google.gerrit.extensions.restapi.RestReadView;
import com.google.gerrit.reviewdb.client.Account;
import com.google.gerrit.reviewdb.server.ReviewDb;
import com.google.gerrit.server.IdentifiedUser;
import com.google.gerrit.server.config.PluginConfigFactory;
import com.google.gerrit.server.project.ProjectControl;
import com.google.gerrit.server.project.ProjectResource;
import com.google.inject.Inject;
import com.google.inject.Provider;
import com.google.inject.Singleton;

import org.kohsuke.args4j.Option;

import java.util.ArrayList;
import java.util.List;

@Singleton
public class GetSuggestion implements RestReadView<ProjectResource> {

  private static final String MAX_SUFFIX = "\u9fa5";
  private static final int DEFAULT_MAX_SUGGESTED = 10;

  private final Provider<ReviewDb> dbProvider;
  private final IdentifiedUser.GenericFactory userFactory;
  private final ProjectControl.GenericFactory pcFactory;
  private final int maxSuggestedReviewers;
  private int limit;
  private String query;

  @Option(name = "--limit", aliases = {"-n"}, metaVar = "CNT",
      usage = "maximum number of reviewers to list")
  public void setLimit(int l) {
    this.limit =
        l <= 0 ? maxSuggestedReviewers : Math.min(l,
            maxSuggestedReviewers);
  }

  @Option(name = "--query", aliases = {"-q"}, metaVar = "QUERY",
      usage = "match reviewers query")
  public void setQuery(String q) {
    this.query = q;
  }


  @Inject
  GetSuggestion(Provider<ReviewDb> dbProvider,
      IdentifiedUser.GenericFactory userFactory,
      ProjectControl.GenericFactory pcFactory,
      PluginConfigFactory cfgFactory,
      @PluginName String pluginName) {
    this.dbProvider = dbProvider;
    this.userFactory = userFactory;
    this.pcFactory = pcFactory;
    this.maxSuggestedReviewers =
        cfgFactory.getFromGerritConfig(pluginName).getInt(
            "maxSuggestedReviewers", DEFAULT_MAX_SUGGESTED);
    this.limit = maxSuggestedReviewers;
  }

  @Override
  public List<String> apply(ProjectResource rsrc) throws AuthException,
      BadRequestException, ResourceConflictException, Exception {
    String a = query;
    String b = a + MAX_SUFFIX;

    List<String> l = new ArrayList<>();
    for (Account p : dbProvider.get().accounts()
        .suggestByFullName(a, b, limit)) {
      IdentifiedUser user = userFactory.create(p.getId());
      if (pcFactory.controlFor(rsrc.getNameKey(), user).isVisible()) {
        String username = p.getFullName();
        l.add(username);
      }
    }
    return l;
  }
}
