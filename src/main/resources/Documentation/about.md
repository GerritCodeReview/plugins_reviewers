A plugin that allows adding default reviewers to a change.

The configuration for adding reviewers to submitted changes can be
[configured per project](config.md).

__NOTE__:
Private changes are ignored, which means that this plugin will never add reviewers
to a change that is in `private` state. Reviewers will only be added to such a
change if it transitions out of `private` state.

__NOTE__:
This plugin automatically naively filters out groups that follow the style from
the singleusergroup plugin.
That is; groups that start with `user/` will not be suggested, regardless of source.

SEE ALSO
--------

* [reviewers-by-blame plugin](https://gerrit-review.googlesource.com/#/admin/projects/plugins/reviewers-by-blame)
