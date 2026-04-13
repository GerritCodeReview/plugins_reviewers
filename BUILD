load("@com_googlesource_gerrit_bazlets//:gerrit_plugin.bzl", "gerrit_plugin", "gerrit_plugin_tests")

gerrit_plugin(
    name = "reviewers",
    srcs = glob(["src/main/java/**/*.java"]),
    manifest_entries = [
        "Gerrit-PluginName: reviewers",
        "Gerrit-Module: com.googlesource.gerrit.plugins.reviewers.Module",
    ],
    resource_jars = ["//plugins/reviewers/web:rv-reviewers"],
    resources = glob(["src/main/resources/**/*"]),
)

gerrit_plugin_tests(
    name = "reviewers_tests",
    size = "small",
    srcs = glob(["src/test/java/**/*.java"]),
    tags = ["reviewers"],
    deps = [
        ":reviewers__plugin",
    ],
)
