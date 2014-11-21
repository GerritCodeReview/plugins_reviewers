// Copyright (C) 2014 The Android Open Source Project
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

import com.google.gerrit.extensions.restapi.AuthException;
import com.google.gerrit.extensions.restapi.BadRequestException;
import com.google.gerrit.extensions.restapi.ResourceConflictException;
import com.google.gerrit.extensions.restapi.RestReadView;
import com.google.gerrit.reviewdb.client.Account;
import com.google.gerrit.reviewdb.server.ReviewDb;
import com.google.gerrit.server.IdentifiedUser;
import com.google.gerrit.server.project.ProjectControl;
import com.google.gerrit.server.project.ProjectResource;
import com.google.inject.Inject;
import com.google.inject.Provider;
import com.google.inject.Singleton;

import java.util.ArrayList;
import java.util.List;

@Singleton
public class GetSuggestion implements RestReadView<ProjectResource> {

  private final Provider<ReviewDb> db;
  private final IdentifiedUser.GenericFactory userFactory;
  private final ProjectControl.GenericFactory pcFactory;


  @Inject
  GetSuggestion(Provider<ReviewDb> db,
      IdentifiedUser.GenericFactory userFactory,
      ProjectControl.GenericFactory pcFactory) {
    this.db = db;
    this.userFactory = userFactory;
    this.pcFactory = pcFactory;
  }

  @Override
  public List<String> apply(ProjectResource rsrc) throws AuthException,
      BadRequestException, ResourceConflictException, Exception {
    List<String> l = new ArrayList<>();
    for (Account a : db.get().accounts().all()) {
      IdentifiedUser user = userFactory.create(db, a.getId());
      if (pcFactory.controlFor(rsrc.getNameKey(), user).isVisible()) {
        String username = a.getFullName();
        if (username != null) {
          l.add(username);
        }
      }
    }
    return l;
  }
}
