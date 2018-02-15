Configuration
=============

Global configuration of the @PLUGIN@ plugin is done in the
`reviewers.config` file in the site's `etc` directory.

```
  [reviewers]
    enableREST = true
    enableUI = false
    ignoreDrafts = true
    suggestOnly = false
```

reviewers.enableREST
:	Enable the REST API. When set to false, the REST API is not available.
	Defaults to true.

reviewers.enableUI
:	Enable the UI.  When set to false, the 'Reviewers' menu is not displayed
	on the project screen. Defaults to true, or false when `enableREST` is false.

reviewers.ignoreDrafts
:	Ignore draft changes. When set to true draft changes are not considered when
	adding reviewers. Defaults to false. To ignore drafts on a per-project basis
	set this value to false and add "-status:draft" to filter in relevant projects.

reviewers.suggestOnly
:	Provide the configured reviewers as suggestions in the "Add Reviewer" dialog
	instead of automatically adding them to the change. Only supports accounts;
	groups are not suggested. Defaults to false. By default Gerrit will consider
	the suggestions with a weight of 1. To force the suggestions higher in the
	list, set a higher value (like 1000) in `addReviewer.@PLUGIN@-reviewer-suggestion.weight`
	in `gerrit.config`. Reviewers are only suggested if they are configured in
	the `reviewers.config` of the project.

reviewers.enableBlame
:	Enable adding reviewers by blame. When enabled, reviewers are added if they
	are the author of a line that is modifed by the change under review.

Per project configuration of the @PLUGIN@ plugin is done in the
`reviewers.config` file of the project. Missing values are inherited
from the parent projects. This means a global default configuration can
be done in the `reviewers.config` file of the `All-Projects` root project.
Other projects can then override the configuration in their own
`reviewers.config` file.

```
  [reviewers]
    enableBlame = true
    maxReviewers = 5
    ignoreFiles = "^foo*"
  [filter "*"]
    reviewer = john.doe@example.com

  [filter "branch:main file:^lib/.*"]
    reviewer = jane.doe@example.com

  [filter "branch:stable-2.10"]
    reviewer = QAGroup

  [filter "-status:draft"]
    reviewer = DevGroup
```
reviewers.enableBlame
:	Enable addition of reviewers by blame for this project. Defaults to false.
	Only effective when `reviewers.enableBlame` is also set in the global
	`reviewers.config` file.

reviewers.maxReviewers
:	Maximum number of reviewers to add by blame. Defaults to 3.

reviewers.ignoreFiles
:	Ignore files where the filename matches the given regular expression when
	computing the reviewers by blame. If empty or not set, no files are ignored.
	By default, not set.

filter.\<filter\>.reviewer
:	An account or a group name. Must be an exact match (case sensitive) with the
	account's email address or username, or the group name.  Multiple `reviewer`
	occurrences are allowed.

##Multiple filter matches

The plugin supports multiple filter matches.

###Example

```
  [filter "file:^build/modules/.*"]
    reviewer = john.doe@example.com

  [filter "file:^build/.*"]
    reviewer = jane.doe@example.com

```

1. Push a change for review involving file "build/modules/GLOBAL.pm".
2. Both john.doe@example.com and jane.doe@example.com get added or suggested as reviewers.
