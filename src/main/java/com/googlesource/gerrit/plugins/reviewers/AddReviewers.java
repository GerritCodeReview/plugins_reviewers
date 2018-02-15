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

import com.google.gerrit.extensions.api.GerritApi;
import com.google.gerrit.extensions.api.changes.AddReviewerInput;
import com.google.gerrit.extensions.api.changes.ReviewInput;
import com.google.gerrit.extensions.restapi.RestApiException;
import com.google.gerrit.reviewdb.client.Account;
import com.google.gerrit.reviewdb.client.Change;
import com.google.gerrit.reviewdb.server.ReviewDb;
import com.google.gerrit.server.CurrentUser;
import com.google.gerrit.server.IdentifiedUser;
import com.google.gerrit.server.util.RequestContext;
import com.google.gerrit.server.util.ThreadLocalRequestContext;
import com.google.gwtorm.server.OrmException;
import com.google.gwtorm.server.SchemaFactory;
import com.google.inject.Provider;
import com.google.inject.ProvisionException;
import java.util.ArrayList;
import java.util.Set;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

abstract class AddReviewers implements Runnable {
  private static final Logger log = LoggerFactory.getLogger(AddReviewers.class);

  private final ThreadLocalRequestContext tl;
  protected final GerritApi gApi;
  protected final IdentifiedUser.GenericFactory identifiedUserFactory;
  protected final SchemaFactory<ReviewDb> schemaFactory;
  protected final Change change;

  private ReviewDb db = null;

  AddReviewers(
      ThreadLocalRequestContext tl,
      GerritApi gApi,
      IdentifiedUser.GenericFactory identifiedUserFactory,
      SchemaFactory<ReviewDb> schemaFactory,
      Change change) {
    this.tl = tl;
    this.gApi = gApi;
    this.identifiedUserFactory = identifiedUserFactory;
    this.schemaFactory = schemaFactory;
    this.change = change;
  }

  abstract Set<Account.Id> getReviewers();

  @Override
  public void run() {
    RequestContext old =
        tl.setContext(
            new RequestContext() {

              @Override
              public CurrentUser getUser() {
                return identifiedUserFactory.create(change.getOwner());
              }

              @Override
              public Provider<ReviewDb> getReviewDbProvider() {
                return new Provider<ReviewDb>() {
                  @Override
                  public ReviewDb get() {
                    if (db == null) {
                      try {
                        db = schemaFactory.open();
                      } catch (OrmException e) {
                        throw new ProvisionException("Cannot open ReviewDb", e);
                      }
                    }
                    return db;
                  }
                };
              }
            });
    try {
      addReviewers(getReviewers(), change);
    } finally {
      tl.setContext(old);
      if (db != null) {
        db.close();
        db = null;
      }
    }
  }

  private void addReviewers(Set<Account.Id> reviewers, Change change) {
    try {
      // TODO(davido): Switch back to using changes API again,
      // when it supports batch mode for adding reviewers
      ReviewInput in = new ReviewInput();
      in.reviewers = new ArrayList<>(reviewers.size());
      for (Account.Id account : reviewers) {
        AddReviewerInput addReviewerInput = new AddReviewerInput();
        addReviewerInput.reviewer = account.toString();
        in.reviewers.add(addReviewerInput);
      }
      gApi.changes().id(change.getId().get()).current().review(in);
    } catch (RestApiException e) {
      log.error("Couldn't add reviewers to the change", e);
    }
  }
}
