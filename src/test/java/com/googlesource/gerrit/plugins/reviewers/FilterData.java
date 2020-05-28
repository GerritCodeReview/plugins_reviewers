package com.googlesource.gerrit.plugins.reviewers;

import com.google.common.collect.Lists;
import com.google.gerrit.acceptance.TestAccount;
import java.util.List;

/** Assists tests to define a filter. */
class FilterData {
  public static FilterData filter(String filter) {
    return new FilterData(filter);
  }

  List<String> reviewers;
  String filter;

  FilterData(String filter) {
    this.filter = filter;
    this.reviewers = Lists.newArrayList();
  }

  FilterData reviewer(TestAccount reviewer) {
    return reviewer(reviewer.email());
  }

  FilterData reviewer(String reviewer) {
    reviewers.add(reviewer);
    return this;
  }
}
