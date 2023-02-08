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

import com.google.common.base.Preconditions;
import com.google.common.base.Strings;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Sets;
import com.google.gerrit.exceptions.StorageException;
import com.google.gerrit.index.query.QueryParseException;
import com.google.gerrit.server.CurrentUser;
import com.google.gerrit.server.query.change.ChangeData;
import com.google.gerrit.server.query.change.ChangeQueryBuilder;
import com.google.inject.Inject;
import com.google.inject.Provider;
import java.util.List;
import java.util.Set;

public class ReviewersFilterUtil {
  private final ChangeQueryBuilder queryBuilder;
  private final Provider<CurrentUser> user;

  @Inject
  public ReviewersFilterUtil(ChangeQueryBuilder queryBuilder, Provider<CurrentUser> user) {
    this.queryBuilder = queryBuilder;
    this.user = user;
  }

  public Set<String> findReviewers(ChangeData cd, List<ReviewerFilter> filters)
      throws StorageException, QueryParseException {
    Set<String> reviewers = Sets.newHashSet();
    List<ReviewerFilter> found = findReviewerFilters(cd, filters);
    for (ReviewerFilter s : found) {
      reviewers.addAll(s.getReviewers());
    }
    return reviewers;
  }

  public Set<String> findCcs(ChangeData cd, List<ReviewerFilter> filters)
      throws StorageException, QueryParseException {
    Set<String> ccs = Sets.newHashSet();
    List<ReviewerFilter> found = findReviewerFilters(cd, filters);
    for (ReviewerFilter s : found) {
      ccs.addAll(s.getCcs());
    }
    return ccs;
  }

  private List<ReviewerFilter> findReviewerFilters(ChangeData cd, List<ReviewerFilter> sections)
      throws StorageException, QueryParseException {
    ImmutableList.Builder<ReviewerFilter> found = ImmutableList.builder();
    for (ReviewerFilter s : sections) {
      if (Strings.isNullOrEmpty(s.getFilter()) || s.getFilter().equals("*")) {
        found.add(s);
      } else if (filterMatch(cd, s.getFilter())) {
        found.add(s);
      }
    }
    return found.build();
  }

  boolean filterMatch(ChangeData cd, String filter) throws StorageException, QueryParseException {
    Preconditions.checkNotNull(filter);
    return queryBuilder.asUser(user.get()).parse(filter).asMatchable().match(cd);
  }
}
