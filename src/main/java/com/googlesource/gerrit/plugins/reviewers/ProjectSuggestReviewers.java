package com.googlesource.gerrit.plugins.reviewers;

import com.google.gerrit.extensions.common.SuggestedReviewerInfo;
import com.google.gerrit.extensions.restapi.BadRequestException;
import com.google.gerrit.extensions.restapi.RestReadView;
import com.google.gerrit.reviewdb.client.Account;
import com.google.gerrit.reviewdb.server.ReviewDb;
import com.google.gerrit.server.IdentifiedUser;
import com.google.gerrit.server.IdentifiedUser.GenericFactory;
import com.google.gerrit.server.account.AccountVisibility;
import com.google.gerrit.server.change.ReviewersUtil;
import com.google.gerrit.server.change.ReviewersUtil.VisibilityControl;
import com.google.gerrit.server.config.GerritServerConfig;
import com.google.gerrit.server.change.SuggestReviewers;
import com.google.gerrit.server.project.ProjectResource;
import com.google.gwtorm.server.OrmException;
import com.google.inject.Inject;
import com.google.inject.Provider;

import org.eclipse.jgit.lib.Config;

import java.io.IOException;
import java.util.List;

public class ProjectSuggestReviewers extends SuggestReviewers
      implements RestReadView<ProjectResource> {
  @Inject
  ProjectSuggestReviewers(AccountVisibility av,
      GenericFactory identifiedUserFactory,
      Provider<ReviewDb> dbProvider,
      @GerritServerConfig Config cfg,
      ReviewersUtil reviewersUtil) {
    super(av, identifiedUserFactory, dbProvider, cfg, reviewersUtil);
  }

  @Override
  public List<SuggestedReviewerInfo> apply(ProjectResource rsrc)
      throws BadRequestException, OrmException, IOException {
    return reviewersUtil.suggestReviewers(this, rsrc.getControl().getProject(),
        rsrc.getControl(), getVisibility(rsrc));
  }

  private VisibilityControl getVisibility(final ProjectResource rsrc) {
    return new VisibilityControl() {
      @Override
      public boolean isVisibleTo(Account.Id account) throws OrmException {
        IdentifiedUser who =
            identifiedUserFactory.create(dbProvider, account);
        // we can't use projectControl directly as it won't suggest reviewers
        // to drafts
        return rsrc.getControl().forUser(who).isVisible();
      }
    };
  }
}