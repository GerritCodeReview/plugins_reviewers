// Copyright (C) 2014 The Android Open Source Project
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

import static com.googlesource.gerrit.plugins.reviewers.ModifyReviewersConfigCapability.MODIFY_REVIEWERS_CONFIG;

import com.google.common.flogger.FluentLogger;
import com.google.gerrit.entities.Project;
import com.google.gerrit.exceptions.StorageException;
import com.google.gerrit.extensions.annotations.PluginName;
import com.google.gerrit.extensions.api.access.PluginPermission;
import com.google.gerrit.extensions.restapi.AuthException;
import com.google.gerrit.extensions.restapi.ResourceConflictException;
import com.google.gerrit.extensions.restapi.ResourceNotFoundException;
import com.google.gerrit.extensions.restapi.Response;
import com.google.gerrit.extensions.restapi.RestApiException;
import com.google.gerrit.extensions.restapi.RestModifyView;
import com.google.gerrit.extensions.restapi.UnprocessableEntityException;
import com.google.gerrit.server.account.AccountResolver;
import com.google.gerrit.server.account.AccountResolver.UnresolvableAccountException;
import com.google.gerrit.server.git.meta.MetaDataUpdate;
import com.google.gerrit.server.group.GroupResolver;
import com.google.gerrit.server.permissions.PermissionBackend;
import com.google.gerrit.server.permissions.PermissionBackendException;
import com.google.gerrit.server.permissions.ProjectPermission;
import com.google.gerrit.server.project.ProjectCache;
import com.google.gerrit.server.project.ProjectResource;
import com.google.inject.Inject;
import com.google.inject.Provider;
import com.google.inject.Singleton;
import com.googlesource.gerrit.plugins.reviewers.PostReviewers.Input;
import java.io.IOException;
import java.util.List;
import org.eclipse.jgit.errors.ConfigInvalidException;
import org.eclipse.jgit.errors.RepositoryNotFoundException;

/** POST REST end-point that removes or adds a reviewer to a {@link ReviewerFilterSection}. */
@Singleton
class PostReviewers implements RestModifyView<ProjectResource, Input> {
  private static final FluentLogger logger = FluentLogger.forEnclosingClass();

  protected static class Input {
    public Action action;
    public String filter;
    public String reviewer;
  }

  private enum Action {
    ADD,
    REMOVE
  }

  private final String pluginName;
  private final ReviewersConfig config;
  private final Provider<MetaDataUpdate.User> metaDataUpdateFactory;
  private final ProjectCache projectCache;
  private final AccountResolver accountResolver;
  private final Provider<GroupResolver> groupResolver;
  private final PermissionBackend permissionBackend;

  @Inject
  PostReviewers(
      @PluginName String pluginName,
      ReviewersConfig config,
      Provider<MetaDataUpdate.User> metaDataUpdateFactory,
      ProjectCache projectCache,
      AccountResolver accountResolver,
      Provider<GroupResolver> groupResolver,
      PermissionBackend permissionBackend) {
    this.pluginName = pluginName;
    this.config = config;
    this.metaDataUpdateFactory = metaDataUpdateFactory;
    this.projectCache = projectCache;
    this.accountResolver = accountResolver;
    this.groupResolver = groupResolver;
    this.permissionBackend = permissionBackend;
  }

  @Override
  public Response<List<ReviewerFilterSection>> apply(ProjectResource rsrc, Input input)
      throws RestApiException, PermissionBackendException {
    Project.NameKey projectName = rsrc.getNameKey();
    ReviewersConfig.ForProject cfg = config.forProject(projectName);
    if (cfg == null) {
      throw new ResourceNotFoundException("Project" + projectName.get() + " not found");
    }

    PermissionBackend.WithUser userPermission = permissionBackend.user(rsrc.getUser());
    if (!userPermission.project(rsrc.getNameKey()).testOrFalse(ProjectPermission.WRITE_CONFIG)
        && !userPermission.testOrFalse(new PluginPermission(pluginName, MODIFY_REVIEWERS_CONFIG))) {
      throw new AuthException("not allowed to modify reviewers config");
    }

    try (MetaDataUpdate md = metaDataUpdateFactory.get().create(projectName)) {
      if (input.action == Action.ADD) {
        validateReviewer(input.reviewer);
      }
      try {
        StringBuilder message = new StringBuilder(pluginName).append(" plugin: ");
        cfg.load(md);
        if (input.action == Action.ADD) {
          message
              .append("Add reviewer ")
              .append(input.reviewer)
              .append(" to filter ")
              .append(input.filter);
          cfg.addReviewer(input.filter, input.reviewer);
        } else {
          message
              .append("Remove reviewer ")
              .append(input.reviewer)
              .append(" from filter ")
              .append(input.filter);
          cfg.removeReviewer(input.filter, input.reviewer);
        }
        message.append("\n");
        md.setMessage(message.toString());
        try {
          cfg.commit(md);
          projectCache.evict(projectName);
        } catch (IOException e) {
          if (e.getCause() instanceof ConfigInvalidException) {
            throw new ResourceConflictException(
                "Cannot update " + projectName + ": " + e.getCause().getMessage());
          }
          throw new ResourceConflictException("Cannot update " + projectName);
        }
      } catch (ConfigInvalidException err) {
        throw new ResourceConflictException(
            "Cannot read " + pluginName + " configurations for project " + projectName, err);
      } catch (IOException err) {
        throw new ResourceConflictException(
            "Cannot update " + pluginName + " configurations for project " + projectName, err);
      }
    } catch (RepositoryNotFoundException err) {
      throw new ResourceNotFoundException(projectName.get());
    } catch (IOException err) {
      throw new ResourceNotFoundException(projectName.get(), err);
    }
    return Response.ok(cfg.getReviewerFilterSections());
  }

  private void validateReviewer(String reviewer) throws RestApiException {
    try {
      UnresolvableAccountException accountException;
      try {
        accountResolver.resolve(reviewer).asUnique();
        return;
      } catch (UnresolvableAccountException e) {
        accountException = e;
      }
      try {
        groupResolver.get().parse(reviewer);
      } catch (UnprocessableEntityException e) {
        throw new ResourceNotFoundException(
            "Account or group '" + reviewer + "' not found\n" + accountException.getMessage());
      }
    } catch (StorageException | IOException | ConfigInvalidException e) {
      logger.atSevere().log("Failed to resolve account %s", reviewer);
    }
  }
}
