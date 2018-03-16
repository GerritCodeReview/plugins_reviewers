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

import static java.util.stream.Collectors.toSet;

import com.google.common.base.Preconditions;
import com.google.common.base.Strings;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Sets;
import com.google.gerrit.common.Nullable;
import com.google.gerrit.common.errors.NoSuchGroupException;
import com.google.gerrit.extensions.client.ChangeStatus;
import com.google.gerrit.extensions.common.AccountInfo;
import com.google.gerrit.extensions.common.ChangeInfo;
import com.google.gerrit.extensions.events.DraftPublishedListener;
import com.google.gerrit.extensions.events.RevisionCreatedListener;
import com.google.gerrit.extensions.events.RevisionEvent;
import com.google.gerrit.extensions.restapi.UnprocessableEntityException;
import com.google.gerrit.reviewdb.client.Account;
import com.google.gerrit.reviewdb.client.Change;
import com.google.gerrit.reviewdb.client.Project;
import com.google.gerrit.reviewdb.server.ReviewDb;
import com.google.gerrit.server.CurrentUser;
import com.google.gerrit.server.IdentifiedUser;
import com.google.gerrit.server.account.AccountResolver;
import com.google.gerrit.server.account.GroupMembers;
import com.google.gerrit.server.change.ReviewerSuggestion;
import com.google.gerrit.server.change.SuggestedReviewer;
import com.google.gerrit.server.git.WorkQueue;
import com.google.gerrit.server.group.GroupsCollection;
import com.google.gerrit.server.project.NoSuchProjectException;
import com.google.gerrit.server.query.QueryParseException;
import com.google.gerrit.server.query.change.ChangeQueryBuilder;
import com.google.gerrit.server.query.change.InternalChangeQuery;
import com.google.gwtorm.server.OrmException;
import com.google.gwtorm.server.SchemaFactory;
import com.google.inject.Inject;
import com.google.inject.Provider;
import com.google.inject.Singleton;
import java.io.IOException;
import java.util.List;
import java.util.Set;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Singleton
class Reviewers implements RevisionCreatedListener, DraftPublishedListener, ReviewerSuggestion {
  private static final Logger log = LoggerFactory.getLogger(Reviewers.class);

  private final AccountResolver accountResolver;
  private final Provider<GroupsCollection> groupsCollection;
  private final GroupMembers.Factory groupMembersFactory;
  private final AddReviewersByConfiguration.Factory byConfigFactory;
  private final WorkQueue workQueue;
  private final IdentifiedUser.GenericFactory identifiedUserFactory;
  private final SchemaFactory<ReviewDb> schemaFactory;
  private final ReviewersConfig config;
  private final Provider<CurrentUser> user;
  private final ChangeQueryBuilder queryBuilder;
  private final Provider<InternalChangeQuery> queryProvider;

  @Inject
  Reviewers(
      AccountResolver accountResolver,
      Provider<GroupsCollection> groupsCollection,
      GroupMembers.Factory groupMembersFactory,
      AddReviewersByConfiguration.Factory byConfigFactory,
      WorkQueue workQueue,
      IdentifiedUser.GenericFactory identifiedUserFactory,
      SchemaFactory<ReviewDb> schemaFactory,
      ReviewersConfig config,
      Provider<CurrentUser> user,
      ChangeQueryBuilder queryBuilder,
      Provider<InternalChangeQuery> queryProvider) {
    this.accountResolver = accountResolver;
    this.groupsCollection = groupsCollection;
    this.groupMembersFactory = groupMembersFactory;
    this.byConfigFactory = byConfigFactory;
    this.workQueue = workQueue;
    this.identifiedUserFactory = identifiedUserFactory;
    this.schemaFactory = schemaFactory;
    this.config = config;
    this.user = user;
    this.queryBuilder = queryBuilder;
    this.queryProvider = queryProvider;
  }

  @Override
  public void onRevisionCreated(RevisionCreatedListener.Event event) {
    onEvent(event);
  }

  @Override
  public void onDraftPublished(DraftPublishedListener.Event event) {
    onEvent(event);
  }

  @Override
  public Set<SuggestedReviewer> suggestReviewers(
      Project.NameKey projectName,
      @Nullable Change.Id changeId,
      @Nullable String query,
      Set<Account.Id> candidates) {
    List<ReviewerFilterSection> sections = getSections(projectName);

    if (sections.isEmpty() || changeId == null) {
      return ImmutableSet.of();
    }

    try (ReviewDb reviewDb = schemaFactory.open()) {
      Set<String> reviewers = findReviewers(sections);
      if (!reviewers.isEmpty()) {
        return toAccounts(reviewDb, reviewers, projectName, changeId.get())
            .stream()
            .map(a -> suggestedReviewer(a))
            .collect(toSet());
      }
    } catch (OrmException | QueryParseException x) {
      log.error(x.getMessage(), x);
    }
    return ImmutableSet.of();
  }

  private SuggestedReviewer suggestedReviewer(Account.Id account) {
    SuggestedReviewer reviewer = new SuggestedReviewer();
    reviewer.account = account;
    reviewer.score = 1;
    return reviewer;
  }

  private List<ReviewerFilterSection> getSections(Project.NameKey projectName) {
    // TODO(davido): we have to cache per project configuration
    return config.forProject(projectName).getReviewerFilterSections();
  }

  private void onEvent(RevisionEvent event) {
    ChangeInfo c = event.getChange();
    if (config.ignoreDrafts() && c.status == ChangeStatus.DRAFT) {
      log.debug("Ignoring draft change");
      return;
    }
    Project.NameKey projectName = new Project.NameKey(c.project);

    List<ReviewerFilterSection> sections = getSections(projectName);

    if (sections.isEmpty()) {
      return;
    }

    AccountInfo uploader = event.getWho();
    int changeNumber = c._number;
    try (ReviewDb reviewDb = schemaFactory.open()) {
      Set<String> reviewers = findReviewers(sections);
      if (reviewers.isEmpty()) {
        return;
      }
      final Runnable task =
          byConfigFactory.create(
              c, toAccounts(reviewDb, reviewers, projectName, changeNumber, uploader));

      workQueue.getDefaultQueue().submit(task);
    } catch (QueryParseException e) {
      log.warn(
          "Could not add default reviewers for change {} of project {}, filter is invalid: {}",
          changeNumber,
          projectName.get(),
          e.getMessage());
    } catch (OrmException x) {
      log.error(x.getMessage(), x);
    }
  }

  private Set<String> findReviewers(List<ReviewerFilterSection> sections)
      throws OrmException, QueryParseException {
    ImmutableSet.Builder<String> reviewers = ImmutableSet.builder();
    List<ReviewerFilterSection> found = findReviewerSections(sections);
    for (ReviewerFilterSection s : found) {
      reviewers.addAll(s.getReviewers());
    }
    return reviewers.build();
  }

  private List<ReviewerFilterSection> findReviewerSections(List<ReviewerFilterSection> sections)
      throws OrmException, QueryParseException {
    ImmutableList.Builder<ReviewerFilterSection> found = ImmutableList.builder();
    for (ReviewerFilterSection s : sections) {
      if (Strings.isNullOrEmpty(s.getFilter()) || s.getFilter().equals("*")) {
        found.add(s);
      } else if (filterMatch(s.getFilter())) {
        found.add(s);
      }
    }
    return found.build();
  }

  boolean filterMatch(String filter) throws OrmException, QueryParseException {
    Preconditions.checkNotNull(filter);
    ChangeQueryBuilder qb = queryBuilder.asUser(user.get());
    return !queryProvider.get().noFields().query(qb.parse(filter)).isEmpty();
  }

  private Set<Account.Id> toAccounts(
      ReviewDb reviewDb, Set<String> in, Project.NameKey p, int changeNumber) {
    return toAccounts(reviewDb, in, p, changeNumber, null);
  }

  /**
   * Convert a set of account names to {@link com.google.gerrit.reviewdb.client.Account.Id}s.
   *
   * @param reviewDb DB
   * @param in the set of account names to convert
   * @param p the project name
   * @param changeNumber the change Id
   * @param uploader account to use to look up groups, or null if groups are not needed
   * @return set of {@link com.google.gerrit.reviewdb.client.Account.Id}s.
   */
  private Set<Account.Id> toAccounts(
      ReviewDb reviewDb,
      Set<String> in,
      Project.NameKey p,
      int changeNumber,
      @Nullable AccountInfo uploader) {
    Set<Account.Id> reviewers = Sets.newHashSetWithExpectedSize(in.size());
    GroupMembers groupMembers = null;
    for (String r : in) {
      try {
        Account account = accountResolver.find(reviewDb, r);
        if (account != null && account.isActive()) {
          if (uploader == null || uploader._accountId != account.getId().get()) {
            reviewers.add(account.getId());
          }
          continue;
        }
      } catch (OrmException e) {
        // If the account doesn't exist, find() will return null.  We only
        // get here if something went wrong accessing the database
        log.error(
            "For the change {} of project {}: failed to resolve account {}.",
            changeNumber,
            p,
            r,
            e);
        continue;
      }

      // find uploader's group in order to try to retrieve other groups' members later
      if (groupMembers == null && uploader != null) {
        // email is not unique to one account, try to locate the account using
        // "Full name <email>" to increase chance of finding only one.
        String uploaderNameEmail = String.format("%s <%s>", uploader.name, uploader.email);
        try {
          Account uploaderAccount = accountResolver.findByNameOrEmail(reviewDb, uploaderNameEmail);
          if (uploaderAccount != null) {
            groupMembers =
                groupMembersFactory.create(identifiedUserFactory.create(uploaderAccount.getId()));
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
      }

      // if the reviewer entry is a group, find its members using uploader's account
      try {
        if (groupMembers != null) {
          Set<Account.Id> accounts =
              groupMembers
                  .listAccounts(groupsCollection.get().parse(r).getGroupUUID(), p)
                  .stream()
                  .filter(Account::isActive)
                  .map(Account::getId)
                  .collect(toSet());
          reviewers.addAll(accounts);
        } else {
          log.warn(
              "For the change {} of project {}: failed to list accounts for group {}; cannot retrieve uploader account for {}.",
              changeNumber,
              p,
              r,
              uploader.email);
        }
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
    return reviewers;
  }
}
