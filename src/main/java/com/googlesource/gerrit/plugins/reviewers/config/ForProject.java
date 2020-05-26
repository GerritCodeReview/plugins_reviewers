package com.googlesource.gerrit.plugins.reviewers.config;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Strings;
import com.google.gerrit.entities.RefNames;
import com.google.gerrit.server.git.ValidationError;
import com.google.gerrit.server.git.meta.VersionedMetaData;
import com.google.inject.Inject;
import com.googlesource.gerrit.plugins.reviewers.ReviewerType;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import org.eclipse.jgit.errors.ConfigInvalidException;
import org.eclipse.jgit.lib.CommitBuilder;
import org.eclipse.jgit.lib.Config;

public class ForProject extends VersionedMetaData implements ValidationError.Sink {
  @VisibleForTesting public static final String FILENAME = "reviewers.config";
  @VisibleForTesting public static final String SECTION_FILTER = "filter";
  @VisibleForTesting public static final String KEY_CC = "cc";
  @VisibleForTesting public static final String KEY_REVIEWER = "reviewer";

  public interface Factory {
    public ForProject create();
  }

  private final ReviewerFilterCollection.Factory filterCollectionFactory;
  private Config cfg;
  private ReviewerFilterCollection filters;
  private List<ValidationError> validationErrors;

  @Inject
  public ForProject(ReviewerFilterCollection.Factory filterCollectionFactory) {
    this.filterCollectionFactory = filterCollectionFactory;
  }

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

  /**
   * Get the validation errors, if any were discovered.
   *
   * @return list of errors; empty list if there are no errors.
   */
  public List<ValidationError> getValidationErrors() {
    if (validationErrors != null) {
      return Collections.unmodifiableList(validationErrors);
    }
    return Collections.emptyList();
  }

  @Override
  public void error(ValidationError error) {
    if (validationErrors == null) {
      validationErrors = new ArrayList<>(4);
    }
    validationErrors.add(error);
  }

  @Override
  protected String getRefName() {
    return RefNames.REFS_CONFIG;
  }

  @Override
  protected void onLoad() throws IOException, ConfigInvalidException {
    this.cfg = readConfig(FILENAME);
    this.filters = filterCollectionFactory.create(cfg, this);
    /* Load all filters to trigger validation. */
    filters.getAll();
  }

  @Override
  protected boolean onSave(CommitBuilder commit) throws IOException, ConfigInvalidException {
    if (Strings.isNullOrEmpty(commit.getMessage())) {
      commit.setMessage("Update reviewers configuration\n");
    }
    saveConfig(FILENAME, cfg);
    return true;
  }
}
