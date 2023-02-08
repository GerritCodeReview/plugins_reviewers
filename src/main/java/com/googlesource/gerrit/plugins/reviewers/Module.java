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

import static com.google.gerrit.server.project.ProjectResource.PROJECT_KIND;
import static com.googlesource.gerrit.plugins.reviewers.ModifyReviewersConfigCapability.MODIFY_REVIEWERS_CONFIG;

import com.google.gerrit.common.UsedAt;
import com.google.gerrit.common.UsedAt.Project;
import com.google.gerrit.extensions.annotations.Exports;
import com.google.gerrit.extensions.config.CapabilityDefinition;
import com.google.gerrit.extensions.config.FactoryModule;
import com.google.gerrit.extensions.events.PrivateStateChangedListener;
import com.google.gerrit.extensions.events.RevisionCreatedListener;
import com.google.gerrit.extensions.events.WorkInProgressStateChangedListener;
import com.google.gerrit.extensions.registration.DynamicSet;
import com.google.gerrit.extensions.restapi.RestApiModule;
import com.google.gerrit.extensions.webui.JavaScriptPlugin;
import com.google.gerrit.extensions.webui.WebUiPlugin;
import com.google.gerrit.server.change.ReviewerSuggestion;
import com.google.gerrit.server.git.validators.CommitValidationListener;
import com.google.gerrit.server.git.validators.MergeValidationListener;
import com.google.inject.AbstractModule;
import com.google.inject.Inject;
import com.googlesource.gerrit.plugins.reviewers.config.ConfigModule;
import com.googlesource.gerrit.plugins.reviewers.config.GlobalConfig;

public class Module extends FactoryModule {
  enum ThreadPool {
    DIRECT,
    WORK_QUEUE,
    FAN_OUT
  }

  static class ForTest extends Module {
    ForTest() {
      super(true, false, ThreadPool.DIRECT);
    }
  }

  private final boolean enableREST;
  private final boolean suggestOnly;
  private final ThreadPool threadPool;

  @Inject
  public Module(GlobalConfig cfg) {
    this(cfg.enableREST(), cfg.suggestOnly(), ThreadPool.WORK_QUEUE);
  }

  @UsedAt(Project.GOOGLE)
  public Module(boolean enableREST, boolean suggestOnly, ThreadPool threadPool) {
    this.enableREST = enableREST;
    this.suggestOnly = suggestOnly;
    this.threadPool = threadPool;
  }

  @Override
  protected void configure() {
    switch (threadPool) {
      case DIRECT:
        bind(ReviewerWorkQueue.class).to(ReviewerWorkQueue.Direct.class);
        break;
      case WORK_QUEUE:
        bind(ReviewerWorkQueue.class).to(ReviewerWorkQueue.Scheduled.class);
        break;
      case FAN_OUT:
        bind(ReviewerWorkQueue.class).to(ReviewerWorkQueue.ScheduledFanOut.class);
      default:
        break;
    }
    bind(CapabilityDefinition.class)
        .annotatedWith(Exports.named(MODIFY_REVIEWERS_CONFIG))
        .to(ModifyReviewersConfigCapability.class);
    DynamicSet.bind(binder(), MergeValidationListener.class).to(ForProjectValidator.class);
    DynamicSet.bind(binder(), CommitValidationListener.class).to(ForProjectValidator.class);
    if (suggestOnly) {
      install(
          new AbstractModule() {
            @Override
            protected void configure() {
              bind(ReviewerSuggestion.class)
                  .annotatedWith(Exports.named("reviewer-suggest"))
                  .to(ReviewerSuggest.class);
            }
          });
    } else {
      DynamicSet.bind(binder(), RevisionCreatedListener.class).to(Reviewers.class);
      DynamicSet.bind(binder(), WorkInProgressStateChangedListener.class).to(Reviewers.class);
      DynamicSet.bind(binder(), PrivateStateChangedListener.class).to(Reviewers.class);
    }

    factory(AddReviewers.Factory.class);

    if (enableREST) {
      install(
          new RestApiModule() {
            @Override
            protected void configure() {
              get(PROJECT_KIND, "reviewers").to(GetReviewers.class);
              post(PROJECT_KIND, "reviewers").to(PostReviewers.class);
            }
          });
    }
    install(
        new AbstractModule() {
          @Override
          protected void configure() {
            DynamicSet.bind(binder(), WebUiPlugin.class)
                .toInstance(new JavaScriptPlugin("rv-reviewers.js"));
          }
        });
    install(new ConfigModule());
  }
}
