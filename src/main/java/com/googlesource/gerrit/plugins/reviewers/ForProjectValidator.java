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

import com.google.common.annotations.VisibleForTesting;
import com.google.common.collect.Lists;
import com.google.gerrit.entities.BranchNameKey;
import com.google.gerrit.entities.PatchSet.Id;
import com.google.gerrit.entities.RefNames;
import com.google.gerrit.server.IdentifiedUser;
import com.google.gerrit.server.events.CommitReceivedEvent;
import com.google.gerrit.server.git.CodeReviewCommit;
import com.google.gerrit.server.git.ValidationError;
import com.google.gerrit.server.git.validators.CommitValidationException;
import com.google.gerrit.server.git.validators.CommitValidationListener;
import com.google.gerrit.server.git.validators.CommitValidationMessage;
import com.google.gerrit.server.git.validators.MergeValidationException;
import com.google.gerrit.server.git.validators.MergeValidationListener;
import com.google.gerrit.server.project.ProjectState;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.googlesource.gerrit.plugins.reviewers.config.ForProject;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import org.eclipse.jgit.errors.ConfigInvalidException;
import org.eclipse.jgit.lib.Repository;

/** Validates changes to reviewers.config through push or merge. */
@Singleton
public class ForProjectValidator implements MergeValidationListener, CommitValidationListener {
  @VisibleForTesting public static String MALFORMED_CONFIG = "Malformed reviewers.config";

  private final ForProject.Factory forProjectFactory;

  @Inject
  public ForProjectValidator(ForProject.Factory forProjectFactory) {
    this.forProjectFactory = forProjectFactory;
  }

  @Override
  public void onPreMerge(
      Repository repo,
      CodeReviewCommit.CodeReviewRevWalk crrw,
      CodeReviewCommit commit,
      ProjectState destProject,
      BranchNameKey destBranch,
      Id patchSetId,
      IdentifiedUser caller)
      throws MergeValidationException {
    if (!RefNames.REFS_CONFIG.equals(destBranch.branch())) {
      return;
    }

    ForProject cfg = forProjectFactory.create();
    try {
      cfg.load(destProject.getNameKey(), repo, commit);
    } catch (IOException ioe) {
      throw new MergeValidationException("Unable to read config.", ioe);
    } catch (ConfigInvalidException cie) {
      throw new MergeValidationException(MALFORMED_CONFIG, cie);
    }

    if (!cfg.getValidationErrors().isEmpty()) {
      throw new MergeValidationException(formatValidationErrors(cfg.getValidationErrors()));
    }
  }

  @Override
  public List<CommitValidationMessage> onCommitReceived(CommitReceivedEvent receiveEvent)
      throws CommitValidationException {
    if (RefNames.REFS_CONFIG.equals(receiveEvent.getBranchNameKey().branch())) {
      ForProject cfg = forProjectFactory.create();
      try {
        cfg.load(receiveEvent.getProjectNameKey(), receiveEvent.revWalk, receiveEvent.commit);
      } catch (IOException ioe) {
        throw new CommitValidationException("Unable to read config.", ioe);
      } catch (ConfigInvalidException cie) {
        throw new CommitValidationException(MALFORMED_CONFIG);
      }
      if (!cfg.getValidationErrors().isEmpty()) {
        ArrayList<CommitValidationMessage> messages = Lists.newArrayList();
        messages.add(new CommitValidationMessage(MALFORMED_CONFIG, true));
        cfg.getValidationErrors()
            .forEach(ve -> messages.add(new CommitValidationMessage("  " + ve.getMessage(), true)));
        throw new CommitValidationException(MALFORMED_CONFIG, messages);
      }
    }
    return Collections.emptyList();
  }

  private static String formatValidationErrors(List<ValidationError> errors) {
    StringBuilder errorMessage = new StringBuilder();
    errorMessage.append("[\"");
    errors.forEach(ve -> errorMessage.append(ve.getMessage() + "\", \""));
    errorMessage.append("]");
    return "Malformed reviewers.config filters: " + errorMessage.toString();
  }
}
