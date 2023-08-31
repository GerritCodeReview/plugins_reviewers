# Configuration

## Global configuration

Global configuration of the @PLUGIN@ plugin is done in the
`reviewers.config` file in the site's `etc` directory.

```
  [reviewers]
    enableREST = true
    suggestOnly = false
    ignoreWip = false
    mergeFilters = true
```

**reviewers.enableREST**
: Enable the REST API. When set to `false`, the REST API is not available.
  Defaults to `true`.

**reviewers.suggestOnly**
: Provide the configured reviewers as suggestions in the "Add Reviewer" dialog
  instead of automatically adding them to the change. Only supports accounts;
  groups are not suggested. Defaults to `false`. By default Gerrit will consider
  the suggestions with a weight of 1. To force the suggestions higher in the
  list, set a higher value (like 1000) in `addReviewer.@PLUGIN@-reviewer-suggestion.weight`
  in `gerrit.config`.

**reviewers.ignoreWip**
: Ignore changes in WIP state. When set to `true` changes in WIP state are not
  considered when adding reviewers. Defaults to `true`. To enable adding
  reviewers on changes in WIP state set this value to `false`.

**reviewers.mergeFilters**
: Decides whether to merge the filter-sections from parent projects or not.
  If set to `false`	filter-sections in child project will override
  filter-sections, with identical filters, in the parent project(s).
  Defaults to `true`.

## Per project configuration

Per project configuration of the @PLUGIN@ plugin is done in the
`reviewers.config` file of the `refs/meta/config` branch of the project.
Missing values are inherited from the parent projects. This means a global
default configuration can be done in the `reviewers.config` file of the
`All-Projects` root project. Other projects can then override the configuration
in their own `reviewers.config` file.

Editing per project configuration requires
[Owner](Documentation/access-control.html#category_owner) or
[Administrator](Documentation/access-control.html#administrators) privileges.
Per project configuration can be edited in the Web UI through the "Edit reviewers
config" action on the project's repo command page. You could also choose to
edit the reviewers.config file through the
[inline edit feature](Documentation/user-inline-edit.html).

```
  [filter "*"]
    reviewer = john.doe@example.com
    cc = DevGroup

  [filter "branch:main file:^lib/.*"]
    reviewer = jane.doe@example.com

  [filter "branch:stable-2.10"]
    reviewer = QAGroup

```

**filter.\<filter\>.reviewer**
: An account or a group name. Must be an exact match (case sensitive) with the
  account's email address or username, or the group name.  Multiple `reviewer`
  occurrences are allowed.\
  **NOTE**: *Reviewers are added in the context of the uploader which means
  that if a group is configured it needs to be visible to the uploader for
  reviewers to be added.*

**filter.\<filter\>.cc**
: An account or a group name. Must be an exact match (case sensitive) with the
  account's email address or username, or the group name.  Multiple `cc`
  occurrences are allowed.

## Multiple filter matches

The plugin supports multiple filter matches.
If a reviewer, according to filter matches, should be added as both `reviewer` and `cc`,
`reviewer` takes precedence.

### Example

```
  [filter "file:^build/modules/.*"]
    reviewer = john.doe@example.com

  [filter "file:^build/.*"]
    reviewer = jane.doe@example.com

```

1. Push a change for review involving file "build/modules/GLOBAL.pm".
2. Both john.doe@example.com and jane.doe@example.com get added or suggested as reviewers.
