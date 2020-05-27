// Copyright (C) 2020 The Android Open Source Project
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

import static com.googlesource.gerrit.plugins.reviewers.ReviewersQueryValidator.validateQuery;

import com.google.common.annotations.VisibleForTesting;
import com.google.gerrit.entities.BranchNameKey;
import com.google.gerrit.entities.PatchSet.Id;
import com.google.gerrit.entities.RefNames;
import com.google.gerrit.server.IdentifiedUser;
import com.google.gerrit.server.events.CommitReceivedEvent;
import com.google.gerrit.server.git.CodeReviewCommit;
import com.google.gerrit.server.git.validators.CommitValidationException;
import com.google.gerrit.server.git.validators.CommitValidationListener;
import com.google.gerrit.server.git.validators.CommitValidationMessage;
import com.google.gerrit.server.git.validators.MergeValidationException;
import com.google.gerrit.server.git.validators.MergeValidationListener;
import com.google.gerrit.server.project.ProjectState;
import com.googlesource.gerrit.plugins.reviewers.config.ReviewersConfig.ForProject;
import java.io.IOException;
import java.util.Collections;
import java.util.List;
import org.eclipse.jgit.errors.ConfigInvalidException;
import org.eclipse.jgit.lib.Repository;

/** Validates changes to reviewers.config through push or merge. */
public class ReviewersConfigValidator implements MergeValidationListener, CommitValidationListener {

  @VisibleForTesting
  public static String MALFORMED_FILTER = "Malformed reviewers.config filter \"%s\"";

  private static String MALFORMED_CONFIG = "Malformed reviewers.config";

  @Override
  public void onPreMerge(
      Repository repo,
      CodeReviewCommit commit,
      ProjectState destProject,
      BranchNameKey destBranch,
      Id patchSetId,
      IdentifiedUser caller)
      throws MergeValidationException {
    if (!RefNames.REFS_CONFIG.equals(destBranch.branch())) {
      return;
    }

    ForProject cfg = new ForProject();
    try {
      cfg.load(destProject.getNameKey(), repo, commit);
    } catch (IOException | ConfigInvalidException e) {
      throw new MergeValidationException(MALFORMED_CONFIG);
    }
    for (ReviewerFilter filter : cfg.getFilters()) {
      try {
        validateQuery(filter.getFilter());
      } catch (UnsupportedReviewersQueryException e) {
        throw new MergeValidationException(String.format(MALFORMED_FILTER, filter.getFilter()));
      }
    }
  }

  @Override
  public List<CommitValidationMessage> onCommitReceived(CommitReceivedEvent receiveEvent)
      throws CommitValidationException {
    if (RefNames.REFS_CONFIG.equals(receiveEvent.getBranchNameKey().branch())) {
      ForProject cfg = new ForProject();
      try {
        cfg.load(receiveEvent.getProjectNameKey(), receiveEvent.revWalk, receiveEvent.commit);
      } catch (IOException | ConfigInvalidException e) {
        throw new CommitValidationException(MALFORMED_CONFIG);
      }
      for (ReviewerFilter filter : cfg.getFilters()) {
        try {
          validateQuery(filter.getFilter());
        } catch (UnsupportedReviewersQueryException e) {
          throw new CommitValidationException(String.format(MALFORMED_FILTER, filter.getFilter()));
        }
      }
    }
    return Collections.emptyList();
  }
}
