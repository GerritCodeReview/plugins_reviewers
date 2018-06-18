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

package com.googlesource.gerrit.plugins.reviewers.server;

import com.google.gerrit.extensions.api.GerritApi;
import com.google.gerrit.extensions.common.ChangeInfo;
import com.google.gerrit.reviewdb.client.Account;
import com.google.gerrit.reviewdb.server.ReviewDb;
import com.google.gerrit.server.IdentifiedUser;
import com.google.gerrit.server.util.ThreadLocalRequestContext;
import com.google.gwtorm.server.SchemaFactory;
import com.google.inject.Inject;
import com.google.inject.assistedinject.Assisted;
import java.util.Set;

class AddReviewersByConfiguration extends AddReviewers {
  private final Set<Account.Id> reviewers;

  interface Factory {
    AddReviewersByConfiguration create(ChangeInfo changeInfo, Set<Account.Id> reviewers);
  }

  @Inject
  AddReviewersByConfiguration(
      ThreadLocalRequestContext tl,
      GerritApi gApi,
      IdentifiedUser.GenericFactory identifiedUserFactory,
      SchemaFactory<ReviewDb> schemaFactory,
      @Assisted ChangeInfo changeInfo,
      @Assisted Set<Account.Id> reviewers) {
    super(tl, gApi, identifiedUserFactory, schemaFactory, changeInfo);
    this.reviewers = reviewers;
  }

  @Override
  Set<Account.Id> getReviewers() {
    return reviewers;
  }
}
