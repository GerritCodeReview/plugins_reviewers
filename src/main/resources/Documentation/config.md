Configuration
=============

The configuration of the @PLUGIN@ plugin is done on project level in
the `reviewers.config` file of the project. Missing values are inherited
from the parent projects. This means a global default configuration can
be done in the `reviewers.config` file of the `All-Projects` root project.
Other projects can then override the configuration in their own
`reviewers.config` file.

WARNING: Due to a bug in Gerrit core in all released versions, the config
file must be called `reviewers` and not `reviewers.config`.  That bug was
fixed in upcoming 2.10 release (not released yet). Note that after upgrade,
the file must be renamed to `reviewers.config`.

```
  [filter "*"]
    reviewer = john.doe@example.com

  [filter "branch:main file:^lib/.*"]
    reviewer = jane.doe@example.com

  [filter "branch:stable-2.10"]
    reviewer = QAGroup

```

filter.<filter>.reviewer
:	An account (email or full user name) or a group name. Multiple
	`reviewer` occurrences are allowed.

###Multiple filter matches

The plugin supports multiple filter matches.

####For example:

```
  [filter "file:^build/modules/.*"]
    reviewer = reviewer1@abc.com

  [filter "file:^build/.*"]
    reviewer = reviewer2@abc.com

```

1. Submit a change on file "build/modules/GLOBAL.pm".
2. Both reviewer1@abc.com and reviewers2@abc.com are added as reviewers.