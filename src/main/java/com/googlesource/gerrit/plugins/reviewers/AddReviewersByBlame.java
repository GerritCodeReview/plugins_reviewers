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

import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Maps;
import com.google.common.collect.Ordering;
import com.google.gerrit.extensions.api.GerritApi;
import com.google.gerrit.reviewdb.client.Account;
import com.google.gerrit.reviewdb.client.Change;
import com.google.gerrit.reviewdb.client.Patch.ChangeType;
import com.google.gerrit.reviewdb.client.PatchSet;
import com.google.gerrit.reviewdb.server.ReviewDb;
import com.google.gerrit.server.IdentifiedUser.GenericFactory;
import com.google.gerrit.server.account.AccountByEmailCache;
import com.google.gerrit.server.account.AccountCache;
import com.google.gerrit.server.git.GitRepositoryManager;
import com.google.gerrit.server.patch.PatchList;
import com.google.gerrit.server.patch.PatchListCache;
import com.google.gerrit.server.patch.PatchListEntry;
import com.google.gerrit.server.patch.PatchListNotAvailableException;
import com.google.gerrit.server.util.ThreadLocalRequestContext;
import com.google.gwtorm.server.OrmException;
import com.google.gwtorm.server.SchemaFactory;
import com.google.inject.Inject;
import com.google.inject.assistedinject.Assisted;
import java.io.IOException;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import org.eclipse.jgit.api.BlameCommand;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.blame.BlameResult;
import org.eclipse.jgit.diff.Edit;
import org.eclipse.jgit.lib.ObjectId;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.revwalk.RevCommit;
import org.eclipse.jgit.revwalk.RevWalk;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class AddReviewersByBlame extends AddReviewers {
  private static final Logger log = LoggerFactory.getLogger(AddReviewersByBlame.class);

  private final PatchListCache patchListCache;
  private final GitRepositoryManager repoManager;
  private final AccountByEmailCache byEmailCache;
  private final AccountCache accountCache;
  private final int maxReviewers;
  private final String ignoreFileRegEx;

  interface Factory {
    AddReviewersByBlame create(Change change);
  }

  @Inject
  AddReviewersByBlame(
      ThreadLocalRequestContext tl,
      GerritApi gApi,
      GenericFactory identifiedUserFactory,
      SchemaFactory<ReviewDb> schemaFactory,
      PatchListCache patchListCache,
      GitRepositoryManager repoManager,
      AccountByEmailCache byEmailCache,
      AccountCache accountCache,
      @Assisted Change change) {
    super(tl, gApi, identifiedUserFactory, schemaFactory, change);
    this.patchListCache = patchListCache;
    this.repoManager = repoManager;
    this.byEmailCache = byEmailCache;
    this.accountCache = accountCache;

    // TODO set these by config
    this.maxReviewers = 3;
    this.ignoreFileRegEx = "";
  }

  @Override
  Set<Account.Id> getReviewers() {
    try (Repository repo = repoManager.openRepository(change.getProject());
        RevWalk rw = new RevWalk(repo);
        ReviewDb reviewDb = schemaFactory.open()) {
      PatchSet ps = reviewDb.patchSets().get(change.currentPatchSetId());
      String revision = ps.getRevision().get();
      ObjectId objectId = ObjectId.fromString(revision);
      RevCommit commit = rw.parseCommit(objectId);
      if (commit.getParentCount() != 1) {
        // Ignore merges and initial commit
        return ImmutableSet.of();
      }
      PatchList patchList;
      try {
        patchList = patchListCache.get(change, ps);
      } catch (PatchListNotAvailableException ex) {
        log.error("Couldn't load patchlist for change {}", change.getKey(), ex);
        return ImmutableSet.of();
      }
      Map<Account.Id, Integer> reviewers = Maps.newHashMap();
      for (PatchListEntry entry : patchList.getPatches()) {
        BlameResult blameResult;
        if ((entry.getChangeType() == ChangeType.MODIFIED
                || entry.getChangeType() == ChangeType.DELETED)
            && (ignoreFileRegEx.isEmpty() || !entry.getNewName().matches(ignoreFileRegEx))
            && (blameResult = computeBlame(repo, entry, commit.getParent(0))) != null) {
          List<Edit> edits = entry.getEdits();
          reviewers.putAll(getReviewersForPatch(edits, blameResult));
        }
      }
      return findTopReviewers(reviewers);
    } catch (IOException | OrmException e) {
      log.error(
          "Couldn't open project {} for change {}", change.getProject().get(), change.getKey(), e);
    }

    return ImmutableSet.of();
  }

  /**
   * Get a map of all the possible reviewers based on the provided blame data
   *
   * @param edits List of edits that were made for this patch
   * @param blameResult Result of blame computation
   * @return a set of all possible reviewers, empty if none, never <code>null</code>
   */
  private Map<Account.Id, Integer> getReviewersForPatch(List<Edit> edits, BlameResult blameResult) {
    Map<Account.Id, Integer> reviewers = Maps.newHashMap();
    for (Edit edit : edits) {
      for (int i = edit.getBeginA(); i < edit.getEndA(); i++) {
        RevCommit commit = blameResult.getSourceCommit(i);
        Set<Account.Id> ids = byEmailCache.get(commit.getAuthorIdent().getEmailAddress());
        for (Account.Id id : ids) {
          Account account = accountCache.get(id).getAccount();
          if (account.isActive() && !change.getOwner().equals(account.getId())) {
            Integer count = reviewers.get(account.getId());
            reviewers.put(account.getId(), count == null ? 1 : count.intValue() + 1);
          }
        }
      }
    }
    return reviewers;
  }

  /**
   * Create a set of reviewers based on data collected from line annotations, the reviewers are
   * ordered by their weight and n greatest of the entries are chosen, where n is the maximum number
   * of reviewers
   *
   * @param reviewers A set of reviewers with their weight mapped to their {@link Account} Id.
   * @return Reviewers that are best matches for this change, empty if none, never <code>null</code>
   */
  private Set<Account.Id> findTopReviewers(Map<Account.Id, Integer> reviewers) {
    List<Entry<Account.Id, Integer>> entries =
        Ordering.from(
                new Comparator<Entry<Account.Id, Integer>>() {
                  @Override
                  public int compare(
                      Entry<Account.Id, Integer> first, Entry<Account.Id, Integer> second) {
                    return first.getValue() - second.getValue();
                  }
                })
            .greatestOf(reviewers.entrySet(), this.maxReviewers);

    return entries.stream().map(e -> e.getKey()).collect(toSet());
  }

  /**
   * Compute the blame data for the parent, we are not interested in the specific commit but the
   * parent, since we only want to know the last person that edited this specific part of the code.
   *
   * @param repo {@link Repository}
   * @param entry {@link PatchListEntry}
   * @param parent Parent {@link RevCommit}
   * @return Result of blame computation, null if the computation fails
   */
  private BlameResult computeBlame(Repository repo, PatchListEntry entry, RevCommit parent) {
    BlameCommand blameCommand =
        new BlameCommand(repo).setStartCommit(parent).setFilePath(entry.getNewName());
    try {
      BlameResult blameResult = blameCommand.call();
      blameResult.computeAll();
      return blameResult;
    } catch (GitAPIException ex) {
      log.error("Couldn't execute blame for commit {}", parent.getName(), ex);
    } catch (IOException err) {
      log.error("Error while computing blame for commit {}", parent.getName(), err);
    }
    return null;
  }
}
