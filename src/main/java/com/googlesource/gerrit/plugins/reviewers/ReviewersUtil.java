package com.googlesource.gerrit.plugins.reviewers;

import com.google.common.base.Preconditions;
import com.google.common.base.Strings;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import com.google.gerrit.exceptions.StorageException;
import com.google.gerrit.index.query.QueryParseException;
import com.google.gerrit.server.CurrentUser;
import com.google.gerrit.server.query.change.ChangeQueryBuilder;
import com.google.gerrit.server.query.change.InternalChangeQuery;
import com.google.inject.Inject;
import com.google.inject.Provider;
import java.util.List;
import java.util.Set;

public class ReviewersUtil {
  private final ChangeQueryBuilder queryBuilder;
  private final Provider<InternalChangeQuery> queryProvider;
  private final Provider<CurrentUser> user;

  @Inject
  public ReviewersUtil(
      ChangeQueryBuilder queryBuilder,
      Provider<InternalChangeQuery> queryProvider,
      Provider<CurrentUser> user) {
    this.queryBuilder = queryBuilder;
    this.queryProvider = queryProvider;
    this.user = user;
  }

  public Set<String> findReviewers(int change, List<ReviewerFilterSection> sections)
      throws StorageException, QueryParseException {
    ImmutableSet.Builder<String> reviewers = ImmutableSet.builder();
    List<ReviewerFilterSection> found = findReviewerSections(change, sections);
    for (ReviewerFilterSection s : found) {
      reviewers.addAll(s.getReviewers());
    }
    return reviewers.build();
  }

  private List<ReviewerFilterSection> findReviewerSections(
      int change, List<ReviewerFilterSection> sections)
      throws StorageException, QueryParseException {
    ImmutableList.Builder<ReviewerFilterSection> found = ImmutableList.builder();
    for (ReviewerFilterSection s : sections) {
      if (Strings.isNullOrEmpty(s.getFilter()) || s.getFilter().equals("*")) {
        found.add(s);
      } else if (filterMatch(change, s.getFilter())) {
        found.add(s);
      }
    }
    return found.build();
  }

  boolean filterMatch(int change, String filter) throws StorageException, QueryParseException {
    Preconditions.checkNotNull(filter);
    ChangeQueryBuilder qb = queryBuilder.asUser(user.get());
    return !queryProvider
        .get()
        .noFields()
        .query(qb.parse(String.format("change:%s %s", change, filter)))
        .isEmpty();
  }
}
