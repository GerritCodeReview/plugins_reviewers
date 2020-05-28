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

import com.google.common.collect.Lists;
import com.google.gerrit.acceptance.TestAccount;
import java.util.List;

/** Assists tests to define a filter. */
class FilterData {
  public static FilterData filter(String filter) {
    return new FilterData(filter);
  }

  List<String> reviewers;
  String filter;

  FilterData(String filter) {
    this.filter = filter;
    this.reviewers = Lists.newArrayList();
  }

  FilterData reviewer(TestAccount reviewer) {
    return reviewer(reviewer.email());
  }

  FilterData reviewer(String reviewer) {
    reviewers.add(reviewer);
    return this;
  }
}
