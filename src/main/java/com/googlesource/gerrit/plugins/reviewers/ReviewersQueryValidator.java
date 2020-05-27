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

import static com.google.gerrit.index.query.QueryParser.AND;
import static com.google.gerrit.index.query.QueryParser.DEFAULT_FIELD;
import static com.google.gerrit.index.query.QueryParser.FIELD_NAME;
import static com.google.gerrit.index.query.QueryParser.NOT;
import static com.google.gerrit.index.query.QueryParser.OR;
import static java.util.Objects.requireNonNull;

import com.google.common.collect.ImmutableSortedSet;
import com.google.gerrit.index.query.QueryParseException;
import com.google.gerrit.index.query.QueryParser;
import org.antlr.runtime.tree.Tree;

/** Validates that a reviewer filter query is formatted correctly. */
public abstract class ReviewersQueryValidator {

  // Since predicates doesn't currently know if they are supported on non-index queries, this
  // set is a copy-paste from CheckerQuery.java in the checks plugin:
  // https://gerrit.googlesource.com/plugins/checks/+/08d8aad35e27f07f50095037bd99b173d8d7588e \
  // /java/com/google/gerrit/plugins/checks/CheckerQuery.java
  //
  // Note that this list contains *operators*, not predicates. If there are multiple operators
  // aliased together to the same predicate ("f:", "file:"), they all need to be listed explicitly.
  //
  // We chose to list operators instead of predicates because:
  //  * It insulates us from changes in implementation details of the query system, such as
  //    predicate classes being renamed, or additional predicates being ORed into existing
  //    operators.
  //  * It's easier to keep in sync with the documentation.
  //
  // This doesn't rule out switching to predicates in the future, particularly if the predicate
  // classes gain some informative methods like "boolean matchMethodNeedsToQueryIndex()".
  //
  // Predicates that definitely cannot be allowed:
  //  * Anything where match() needs to query the index, i.e. any full-text fields. This includes
  //    the default field.
  //  * Anything where post-filtering is unreasonably expensive.
  //  * Any predicate over projects, since that may conflict with the projects field.
  //
  // Beyond that, this set is mostly based on what we subjectively consider useful for limiting the
  // changes that a checker runs on. It will probably grow, based on user feedback.
  private static final ImmutableSortedSet<String> ALLOWED_OPERATORS =
      ImmutableSortedSet.of(
          "added",
          "after",
          "age",
          "assignee",
          "author",
          "before",
          "branch",
          "committer",
          "deleted",
          "delta",
          "destination",
          "dir",
          "directory",
          "ext",
          "extension",
          "f",
          "file",
          "footer",
          "hashtag",
          "intopic",
          "label",
          "onlyextensions",
          "onlyexts",
          "ownerin",
          "path",
          "r",
          "ref",
          "reviewer",
          "reviewerin",
          "size",
          "status",
          "submittable",
          "topic",
          "unresolved",
          "wip");

  static void validateQuery(String query) throws UnsupportedReviewersQueryException {
    String trimmedQuery = requireNonNull(query, "Query must not be null").trim();
    /* Reviewers supports '*' as a special case query. */
    if (trimmedQuery.equals("*")) {
      return;
    }
    try {
      validateOperators(QueryParser.parse(trimmedQuery));
    } catch (QueryParseException e) {
      throw new UnsupportedReviewersQueryException(
          String.format("Unsupported filter: \"%s\"", query), e);
    }
  }

  private static void validateOperators(Tree node) throws UnsupportedReviewersQueryException {
    switch (node.getType()) {
      case AND:
      case OR:
      case NOT:
        for (int i = 0; i < node.getChildCount(); i++) {
          validateOperators(node.getChild(i));
        }
        break;
      case FIELD_NAME:
        if (!ALLOWED_OPERATORS.contains(node.getText())) {
          throw new UnsupportedReviewersQueryException("Unsupported operator: " + node);
        }
        break;
      case DEFAULT_FIELD:
        throw new UnsupportedReviewersQueryException(
            "Specific search operator required: " + node.getChild(0));
      default:
        throw new UnsupportedReviewersQueryException("Unsupported filter: " + node);
    }
  }
}
