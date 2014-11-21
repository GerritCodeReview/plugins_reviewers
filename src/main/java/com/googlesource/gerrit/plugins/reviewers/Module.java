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

import com.google.gerrit.common.ChangeListener;
import com.google.gerrit.extensions.registration.DynamicSet;
import com.google.gerrit.extensions.restapi.RestApiModule;
import com.google.gerrit.extensions.webui.TopMenu;
import com.google.gerrit.server.config.FactoryModule;

class Module extends FactoryModule {
  @Override
  protected void configure() {
    DynamicSet.bind(binder(), TopMenu.class).to(
        ReviewersTopMenu.class);
    DynamicSet.bind(binder(), ChangeListener.class).to(
        ChangeEventListener.class);
    factory(DefaultReviewers.Factory.class);
    factory(ReviewersConfig.Factory.class);
    install(new RestApiModule() {
      @Override
      protected void configure() {
        get(PROJECT_KIND, "reviewers").to(GetReviewers.class);
        put(PROJECT_KIND, "reviewers").to(PutReviewers.class);
        get(PROJECT_KIND, "suggest_reviewers").to(GetSuggestion.class);
      }
    });
  }
}
