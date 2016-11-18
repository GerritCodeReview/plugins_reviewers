load("@io_bazlets//:gerrit_plugin.bzl", "gerrit_plugin")
load("@io_bazlets//:gerrit_api_neverlink.bzl", "gerrit_api_neverlink")

MODULE = 'com.googlesource.gerrit.plugins.reviewers.ReviewersForm'

gerrit_api_neverlink()

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
