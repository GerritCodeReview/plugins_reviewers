package com.googlesource.gerrit.plugins.reviewers;

import com.google.gerrit.extensions.restapi.AuthException;
import com.google.gerrit.extensions.restapi.BadRequestException;
import com.google.gerrit.extensions.restapi.ResourceConflictException;
import com.google.gerrit.extensions.restapi.RestReadView;
import com.google.gerrit.server.project.ProjectResource;
import com.google.inject.Inject;

import java.util.List;

public class GetReviewers implements RestReadView<ProjectResource> {

  private final ReviewersConfig.Factory configFactory;

  @Inject
  GetReviewers(ReviewersConfig.Factory configFactory) {
    this.configFactory = configFactory;
  }

  @Override
  public List<ReviewerFilterSection> apply(ProjectResource resource) throws AuthException,
      BadRequestException, ResourceConflictException, Exception {
    return configFactory.create(resource.getNameKey()).getReviewerFilterSections();
  }
}
