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

import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import com.google.common.collect.ImmutableList;
import com.google.gerrit.reviewdb.client.Project;
import com.google.gerrit.server.cache.CacheModule;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.google.inject.TypeLiteral;
import com.google.inject.name.Named;
import java.util.List;
import java.util.concurrent.ExecutionException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Singleton
public class ReviewersConfigCache {
  private static final Logger log = LoggerFactory.getLogger(ReviewersConfigCache.class);
  private static final String CACHE_NAME = "projectConfigCache";

  private final LoadingCache<Project.NameKey, List<ReviewerFilterSection>> cache;

  @Inject
  ReviewersConfigCache(
      @Named(CACHE_NAME) LoadingCache<Project.NameKey, List<ReviewerFilterSection>> cache) {
    this.cache = cache;
  }

  public List<ReviewerFilterSection> get(Project.NameKey projectName) {
    try {
      return cache.get(projectName);
    } catch (ExecutionException e) {
      log.warn("Cannot get reviewers config for project {}", projectName, e);
      return ImmutableList.of();
    }
  }

  public void evict(Project.NameKey projectName) {
    cache.invalidate(projectName);
  }

  public static class Module extends CacheModule {
    @Override
    protected void configure() {
      cache(CACHE_NAME, Project.NameKey.class, new TypeLiteral<List<ReviewerFilterSection>>() {})
          .loader(Loader.class);
    }
  }

  static class Loader extends CacheLoader<Project.NameKey, List<ReviewerFilterSection>> {
    private final ReviewersConfig.Factory configFactory;

    @Inject
    Loader(ReviewersConfig.Factory configFactory) {
      this.configFactory = configFactory;
    }

    @Override
    public List<ReviewerFilterSection> load(Project.NameKey projectName) throws Exception {
      return configFactory.create(projectName).getReviewerFilterSections();
    }
  }
}
