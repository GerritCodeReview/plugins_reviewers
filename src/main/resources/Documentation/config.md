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
	groups are not suggested. Defaults to false. The weighting of the suggestions
	can be configured in the `[addreviewer]` section of `gerrit.config`. If this
	is not set, it defaults to 1.

Per project configuration of the @PLUGIN@ plugin is done in the
`reviewers.config` file of the project. Missing values are inherited
from the parent projects. This means a global default configuration can
be done in the `reviewers.config` file of the `All-Projects` root project.
Other projects can then override the configuration in their own
`reviewers.config` file.

```
  [filter "*"]
    reviewer = john.doe@example.com

  [filter "branch:main file:^lib/.*"]
    reviewer = jane.doe@example.com

  [filter "branch:stable-2.10"]
    reviewer = QAGroup

  [filter "-status:draft"]
    reviewer = DevGroup
```

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
