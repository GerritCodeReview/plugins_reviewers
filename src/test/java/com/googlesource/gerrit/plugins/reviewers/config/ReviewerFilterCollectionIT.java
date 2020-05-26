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

import static com.google.common.truth.Truth.assertThat;
import static com.googlesource.gerrit.plugins.reviewers.config.ForProject.KEY_REVIEWER;
import static com.googlesource.gerrit.plugins.reviewers.config.ForProject.SECTION_FILTER;

import com.google.gerrit.acceptance.LightweightPluginDaemonTest;
import com.google.gerrit.acceptance.NoHttpd;
import com.google.gerrit.acceptance.TestPlugin;
import com.google.gerrit.extensions.config.FactoryModule;
import org.eclipse.jgit.lib.Config;
import org.junit.Test;

@NoHttpd
@TestPlugin(
    name = "reviewers",
    sysModule =
        "com.googlesource.gerrit.plugins.reviewers.config.ReviewerFilterCollectionIT$TestModule")
public class ReviewerFilterCollectionIT extends LightweightPluginDaemonTest {

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
