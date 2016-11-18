load('//tools/bzl:plugin.bzl', 'gerrit_plugin')

MODULE = 'com.googlesource.gerrit.plugins.reviewers.ReviewersForm'

gerrit_plugin(
  name = 'reviewers',
  srcs = glob(['src/main/java/**/*.java']),
  resources = glob(['src/main/**/*']),
  gwt_module = MODULE,
  manifest_entries = [
    'Gerrit-PluginName: reviewers',
    'Gerrit-Module: com.googlesource.gerrit.plugins.reviewers.Module',
  ]
)
