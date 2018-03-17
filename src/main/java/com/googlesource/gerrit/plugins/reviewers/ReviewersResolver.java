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

import static java.util.stream.Collectors.toSet;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.collect.Sets;
import com.google.gerrit.common.Nullable;
import com.google.gerrit.common.errors.NoSuchGroupException;
import com.google.gerrit.extensions.common.AccountInfo;
import com.google.gerrit.extensions.restapi.UnprocessableEntityException;
import com.google.gerrit.reviewdb.client.Account;
import com.google.gerrit.reviewdb.client.Project;
import com.google.gerrit.reviewdb.server.ReviewDb;
import com.google.gerrit.server.IdentifiedUser;
import com.google.gerrit.server.account.AccountResolver;
import com.google.gerrit.server.account.GroupMembers;
import com.google.gerrit.server.group.GroupsCollection;
import com.google.gerrit.server.project.NoSuchProjectException;
import com.google.gwtorm.server.OrmException;
import com.google.inject.Inject;
import com.google.inject.Provider;
import com.google.inject.Singleton;
import java.io.IOException;
import java.util.Set;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/* Resolve account and group names to account ids */
@Singleton
class ReviewersResolver {
  private static final Logger log = LoggerFactory.getLogger(ReviewersResolver.class);

  private final AccountResolver accountResolver;
  private final Provider<GroupsCollection> groupsCollection;
  private final GroupMembers.Factory groupMembersFactory;
  private final IdentifiedUser.GenericFactory identifiedUserFactory;

  @Inject
  ReviewersResolver(
      AccountResolver accountResolver,
      Provider<GroupsCollection> groupsCollection,
      GroupMembers.Factory groupMembersFactory,
      IdentifiedUser.GenericFactory identifiedUserFactory) {
    this.accountResolver = accountResolver;
    this.groupsCollection = groupsCollection;
    this.groupMembersFactory = groupMembersFactory;
    this.identifiedUserFactory = identifiedUserFactory;
  }

  /**
   * Resolve a set of account names to {@link com.google.gerrit.reviewdb.client.Account.Id}s. Group
   * names are resolved to their account members.
   *
   * @param reviewDb DB
   * @param in the set of account names to convert
   * @param p the project name
   * @param changeNumber the change Id
   * @param uploader account to use to look up groups, or null if groups are not needed
   * @return set of {@link com.google.gerrit.reviewdb.client.Account.Id}s.
   */
  @VisibleForTesting
  Set<Account.Id> resolve(
      ReviewDb reviewDb,
      Set<String> in,
      Project.NameKey p,
      int changeNumber,
      @Nullable AccountInfo uploader) {
    Set<Account.Id> reviewers = Sets.newHashSetWithExpectedSize(in.size());
    GroupMembers groupMembers = null;
    for (String r : in) {
      if (resolveAccount(reviewDb, p, changeNumber, uploader, reviewers, r)) {
        continue;
      }

      if (groupMembers == null && uploader != null) {
        groupMembers = createGroupMembers(reviewDb, p, changeNumber, uploader, r);
      }

      if (groupMembers != null) {
        resolveGroup(p, changeNumber, reviewers, groupMembers, r);
      } else {
        log.warn(
            "For the change {} of project {}: failed to list accounts for group {}; cannot retrieve uploader account for {}.",
            changeNumber,
            p,
            r,
            uploader.email);
      }
    }
    return reviewers;
  }

  private boolean resolveAccount(
      ReviewDb reviewDb,
      Project.NameKey p,
      int changeNumber,
      AccountInfo uploader,
      Set<Account.Id> reviewers,
      String r) {
    try {
      Account account = accountResolver.find(reviewDb, r);
      if (account != null && account.isActive()) {
        if (uploader == null || uploader._accountId != account.getId().get()) {
          reviewers.add(account.getId());
        }
        return true;
      }
    } catch (OrmException e) {
      // If the account doesn't exist, find() will return null.  We only
      // get here if something went wrong accessing the database
      log.error(
          "For the change {} of project {}: failed to resolve account {}.", changeNumber, p, r, e);
      return true;
    }
    return false;
  }

  private void resolveGroup(
      Project.NameKey p,
      int changeNumber,
      Set<Account.Id> reviewers,
      GroupMembers groupMembers,
      String r) {
    try {
      Set<Account.Id> accounts =
          groupMembers
              .listAccounts(groupsCollection.get().parse(r).getGroupUUID(), p)
              .stream()
              .filter(Account::isActive)
              .map(Account::getId)
              .collect(toSet());
      reviewers.addAll(accounts);
    } catch (UnprocessableEntityException | NoSuchGroupException e) {
      log.warn(
          "For the change {} of project {}: reviewer {} is neither an account nor a group.",
          changeNumber,
          p,
          r);
    } catch (NoSuchProjectException e) {
      log.warn(
          "For the change {} of project {}: failed to list accounts for group {}.",
          changeNumber,
          p,
          r);
    } catch (IOException | OrmException e) {
      log.warn(
          "For the change {} of project {}: failed to list accounts for group {}.",
          changeNumber,
          p,
          r,
          e);
    }
  }

  private GroupMembers createGroupMembers(
      ReviewDb reviewDb, Project.NameKey p, int changeNumber, AccountInfo uploader, String r) {
    // email is not unique to one account, try to locate the account using
    // "Full name <email>" to increase chance of finding only one.
    String uploaderNameEmail = String.format("%s <%s>", uploader.name, uploader.email);
    try {
      Account uploaderAccount = accountResolver.findByNameOrEmail(reviewDb, uploaderNameEmail);
      if (uploaderAccount != null) {
        return groupMembersFactory.create(identifiedUserFactory.create(uploaderAccount.getId()));
      }
    } catch (OrmException e) {
      log.warn(
          "For the change {} of project {}: failed to list accounts for group {}, cannot retrieve uploader account {}.",
          changeNumber,
          p,
          r,
          uploaderNameEmail,
          e);
    }
    return null;
  }
}
