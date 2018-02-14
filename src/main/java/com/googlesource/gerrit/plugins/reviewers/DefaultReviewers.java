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

import com.google.gerrit.extensions.api.GerritApi;
import com.google.gerrit.reviewdb.client.Account;
import com.google.gerrit.reviewdb.client.Change;
import com.google.gerrit.reviewdb.server.ReviewDb;
import com.google.gerrit.server.IdentifiedUser;
import com.google.gerrit.server.util.ThreadLocalRequestContext;
import com.google.gwtorm.server.SchemaFactory;
import com.google.inject.Inject;
import com.google.inject.assistedinject.Assisted;
import java.util.Set;

class DefaultReviewers extends AddReviewers {
  private final Set<Account.Id> reviewers;

  interface Factory {
    DefaultReviewers create(Change change, Set<Account.Id> reviewers);
  }

  @Inject
  DefaultReviewers(
      GerritApi gApi,
      IdentifiedUser.GenericFactory identifiedUserFactory,
      ThreadLocalRequestContext tl,
      SchemaFactory<ReviewDb> schemaFactory,
      @Assisted Change change,
      @Assisted Set<Account.Id> reviewers) {
    super(gApi, identifiedUserFactory, tl, schemaFactory, change);
    this.reviewers = reviewers;
  }

  @Override
  Set<Account.Id> getReviewers() {
    return reviewers;
  }
}
