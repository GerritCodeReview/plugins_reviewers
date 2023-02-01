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
import com.google.common.flogger.FluentLogger;
import com.google.gerrit.common.Nullable;
import com.google.gerrit.entities.Account;
import com.google.gerrit.entities.Project;
import com.google.gerrit.exceptions.StorageException;
import com.google.gerrit.extensions.common.AccountInfo;
import com.google.gerrit.extensions.restapi.UnprocessableEntityException;
import com.google.gerrit.server.account.AccountResolver;
import com.google.gerrit.server.account.GroupMembers;
import com.google.gerrit.server.group.GroupResolver;
import com.google.gerrit.server.project.NoSuchProjectException;
import com.google.inject.Inject;
import com.google.inject.Provider;
import com.google.inject.Singleton;
import java.io.IOException;
import java.util.Set;
import org.eclipse.jgit.errors.ConfigInvalidException;

/**
 * Attempts to resolve string identifiers in reviewers.config into valid {@link
 * com.google.gerrit.entities.Account.Id}s when string identifies an account and groups that are
 * expanded into {@link com.google.gerrit.entities.Account.Id}s if it identifies a group.
 */
@Singleton
class ReviewersResolver {
  private static final FluentLogger logger = FluentLogger.forEnclosingClass();

  private final AccountResolver accountResolver;
  private final Provider<GroupResolver> groupResolver;
  private final GroupMembers groupMembers;

  @Inject
  ReviewersResolver(
      AccountResolver accountResolver,
      Provider<GroupResolver> groupResolver,
      GroupMembers groupMembers) {
    this.accountResolver = accountResolver;
    this.groupResolver = groupResolver;
    this.groupMembers = groupMembers;
  }

  /**
   * Resolve a set of account names to {@link com.google.gerrit.entities.Account.Id}s. Group names
   * are resolved to their account members.
   *
   * @param names the set of account names to convert
   * @param project the project name
   * @param changeNumber the change Id
   * @param uploader account to use to look up groups, or null if groups are not needed
   * @param ignoreAccountVisibility if account visibiltiy should be ignored
   * @return set of {@link com.google.gerrit.entities.Account.Id}s.
   */
  @VisibleForTesting
  Set<Account.Id> resolve(
      Set<String> names,
      Project.NameKey project,
      int changeNumber,
      @Nullable AccountInfo uploader,
      boolean ignoreAccountVisibility) {
    Set<Account.Id> reviewers = Sets.newHashSetWithExpectedSize(names.size());
    for (String name : names) {
      if (resolveAccount(
          project, changeNumber, uploader, reviewers, name, ignoreAccountVisibility)) {
        continue;
      }

      resolveGroup(project, changeNumber, reviewers, groupMembers, name);
    }
    return reviewers;
  }

  private boolean resolveAccount(
      Project.NameKey project,
      int changeNumber,
      @Nullable AccountInfo uploader,
      Set<Account.Id> reviewers,
      String accountName,
      boolean ignoreAccountVisibility) {
    try {
      AccountResolver.Result result =
          ignoreAccountVisibility
              ? accountResolver.resolveIgnoreVisibility(accountName)
              : accountResolver.resolve(accountName);
      if (result.asList().size() == 1) {
        Account.Id id = result.asList().get(0).account().id();
        if (uploader == null || id.get() != uploader._accountId) {
          reviewers.add(id);
        }
        return true;
      }
      return false;
    } catch (StorageException | IOException | ConfigInvalidException e) {
      logger.atSevere().withCause(e).log(
          "For the change %d of project %s: failed to resolve account %s.",
          changeNumber, project, accountName);
      return true;
    }
  }

  private void resolveGroup(
      Project.NameKey project,
      int changeNumber,
      Set<Account.Id> reviewers,
      GroupMembers groupMembers,
      String group) {
    try {
      Set<Account.Id> accounts =
          groupMembers.listAccounts(groupResolver.get().parse(group).getGroupUUID(), project)
              .stream()
              .filter(Account::isActive)
              .map(Account::id)
              .collect(toSet());
      reviewers.addAll(accounts);
    } catch (UnprocessableEntityException e) {
      logger.atWarning().log(
          "For the change %d of project %s: reviewer %s is neither an account nor a group.",
          changeNumber, project, group);
    } catch (NoSuchProjectException | IOException e) {
      logger.atSevere().withCause(e).log(
          "For the change %d of project %s: failed to list accounts for group %s.",
          changeNumber, project, group);
    }
  }
}
