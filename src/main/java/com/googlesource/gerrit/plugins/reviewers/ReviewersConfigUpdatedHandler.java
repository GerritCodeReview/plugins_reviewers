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

import static com.google.gerrit.reviewdb.client.RefNames.REFS_CONFIG;

import com.google.gerrit.extensions.events.GitReferenceUpdatedListener;
import com.google.gerrit.reviewdb.client.Project;
import com.google.gerrit.server.git.GitRepositoryManager;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.lib.ObjectReader;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.revwalk.RevWalk;
import org.eclipse.jgit.treewalk.AbstractTreeIterator;
import org.eclipse.jgit.treewalk.CanonicalTreeParser;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Singleton
public class ReviewersConfigUpdatedHandler implements GitReferenceUpdatedListener {
  private static final Logger log = LoggerFactory.getLogger(ReviewersConfigUpdatedHandler.class);

  private final GitRepositoryManager repoManager;
  private final ReviewersConfigCache configCache;

  @Inject
  ReviewersConfigUpdatedHandler(
      GitRepositoryManager repoManager, ReviewersConfigCache configCache) {
    this.repoManager = repoManager;
    this.configCache = configCache;
  }

  @Override
  public void onGitReferenceUpdated(Event event) {
    if (event.getRefName().equals(REFS_CONFIG)) {
      Project.NameKey projectName = new Project.NameKey(event.getProjectName());
      try (Repository r = repoManager.openRepository(projectName);
          Git git = new Git(r)) {
        OutputStream out = new ByteArrayOutputStream();
        git.diff()
            .setShowNameAndStatusOnly(true)
            .setOutputStream(out)
            .setOldTree(getTree(r, event.getOldObjectId()))
            .setNewTree(getTree(r, event.getNewObjectId()))
            .call();
        String result = out.toString();
        if (result.contains("reviewers.config")) {
          configCache.evict(projectName);
        }
      } catch (IOException | GitAPIException e) {
        log.warn("Unable to check diff", e);
      }
    }
  }

  private AbstractTreeIterator getTree(Repository r, String objectId) throws IOException {
    CanonicalTreeParser p = new CanonicalTreeParser();
    try (ObjectReader reader = r.newObjectReader();
        RevWalk rw = new RevWalk(r)) {
      p.reset(reader, rw.parseTree(r.resolve(objectId)));
      return p;
    }
  }
}
