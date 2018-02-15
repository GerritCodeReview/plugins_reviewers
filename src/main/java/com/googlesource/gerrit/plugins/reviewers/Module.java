// Copyright (C) 2013 The Android Open Source Project
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

import static com.google.gerrit.server.project.ProjectResource.PROJECT_KIND;

import com.google.gerrit.extensions.annotations.Exports;
import com.google.gerrit.extensions.config.FactoryModule;
import com.google.gerrit.extensions.events.DraftPublishedListener;
import com.google.gerrit.extensions.events.RevisionCreatedListener;
import com.google.gerrit.extensions.registration.DynamicSet;
import com.google.gerrit.extensions.restapi.RestApiModule;
import com.google.gerrit.extensions.webui.GwtPlugin;
import com.google.gerrit.extensions.webui.TopMenu;
import com.google.gerrit.extensions.webui.WebUiPlugin;
import com.google.gerrit.server.change.ReviewerSuggestion;
import com.google.inject.AbstractModule;
import com.google.inject.Inject;

public class Module extends FactoryModule {
  private final boolean enableUI;
  private final boolean enableREST;
  private final boolean suggestOnly;

  @Inject
  public Module(ReviewersConfig cfg) {
    this.enableREST = cfg.enableREST();
    this.enableUI = cfg.enableUI();
    this.suggestOnly = cfg.suggestOnly();
  }

  public Module(boolean enableUI, boolean enableREST, boolean suggestOnly) {
    this.enableUI = enableUI;
    this.enableREST = enableREST;
    this.suggestOnly = suggestOnly;
  }

  @Override
  protected void configure() {
    if (enableUI) {
      DynamicSet.bind(binder(), TopMenu.class).to(ReviewersTopMenu.class);
      DynamicSet.bind(binder(), WebUiPlugin.class).toInstance(new GwtPlugin("reviewers"));
    }

    if (suggestOnly) {
      install(
          new AbstractModule() {
            @Override
            protected void configure() {
              bind(ReviewerSuggestion.class)
                  .annotatedWith(Exports.named("reviewer-suggest"))
                  .to(Reviewers.class);
            }
          });
    } else {
      DynamicSet.bind(binder(), RevisionCreatedListener.class).to(Reviewers.class);
      DynamicSet.bind(binder(), DraftPublishedListener.class).to(Reviewers.class);
    }

    factory(AddReviewersByConfiguration.Factory.class);

    if (enableREST) {
      install(
          new RestApiModule() {
            @Override
            protected void configure() {
              get(PROJECT_KIND, "reviewers").to(GetReviewers.class);
              put(PROJECT_KIND, "reviewers").to(PutReviewers.class);
              get(PROJECT_KIND, "suggest_reviewers").to(SuggestProjectReviewers.class);
            }
          });
    }
  }
}
