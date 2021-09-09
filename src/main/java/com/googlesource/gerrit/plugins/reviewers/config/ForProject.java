package com.googlesource.gerrit.plugins.reviewers.config;

import com.google.common.base.Strings;
import com.google.gerrit.entities.RefNames;
import com.google.gerrit.server.git.meta.VersionedMetaData;
import com.googlesource.gerrit.plugins.reviewers.ReviewerType;
import java.io.IOException;
import org.eclipse.jgit.errors.ConfigInvalidException;
import org.eclipse.jgit.lib.CommitBuilder;
import org.eclipse.jgit.lib.Config;

public class ForProject extends VersionedMetaData {
  private Config cfg;
  private ReviewerFilterCollection filters;

  public void addReviewer(String filter, String reviewer, ReviewerType type) {
    switch (type) {
      case REVIEWER:
        filters.get(filter).addReviewer(reviewer);
        break;
      case CC:
        filters.get(filter).addCc(reviewer);
    }
  }

  public void removeReviewer(String filter, String reviewer, ReviewerType type) {
    switch (type) {
      case REVIEWER:
        filters.get(filter).removeReviewer(reviewer);
        break;
      case CC:
        filters.get(filter).removeCc(reviewer);
    }
  }

  @Override
  protected String getRefName() {
    return RefNames.REFS_CONFIG;
  }

  @Override
  protected void onLoad() throws IOException, ConfigInvalidException {
    this.cfg = readConfig(ReviewersConfig.FILENAME);
    this.filters = new ReviewerFilterCollection(cfg);
  }

  @Override
  protected boolean onSave(CommitBuilder commit) throws IOException, ConfigInvalidException {
    if (Strings.isNullOrEmpty(commit.getMessage())) {
      commit.setMessage("Update reviewers configuration\n");
    }
    saveConfig(ReviewersConfig.FILENAME, cfg);
    return true;
  }
}
