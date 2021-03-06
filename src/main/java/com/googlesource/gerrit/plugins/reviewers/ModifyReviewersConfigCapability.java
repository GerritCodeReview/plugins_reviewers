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

import com.google.gerrit.extensions.config.CapabilityDefinition;

/** Capability that allows for editing reviewers.config. */
public class ModifyReviewersConfigCapability extends CapabilityDefinition {
  static final String MODIFY_REVIEWERS_CONFIG = "modifyReviewersConfig";

  @Override
  public String getDescription() {
    return "Modify Reviewers Config";
  }
}
