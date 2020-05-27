Reviewer Filter Query
=====================

The filter query decides whether the reviewers specified for a section should
be added as reviewers or not to a specific change.
The filter can be the special match-all query "*", or a complex query with
operators and operands.
Since the reviewer filter query is not backed by the index the filter query
supports a sub-set of the operators supported by a standard change query.

Supported Operators
------------------

The operators that are currently supported for a reviewer filter query are:

`added, after, age, assignee, author, before, branch, committer, deleted, delta,
destination, dir, directory, ext, extension, f, file, footer, hashtag, intopic,
label, onlyextensions, onlyexts, ownerin, path, r, ref, reviewer, reviewerin,
size, status, submittable, topic, unresolved, wip`
