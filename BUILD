load("//tools/bzl:junit.bzl", "junit_tests")
load(
    "//tools/bzl:plugin.bzl",
    "PLUGIN_DEPS",
    "PLUGIN_TEST_DEPS",
    "gerrit_plugin",
)

gerrit_plugin(
    name = "reviewers",
    srcs = glob(["src/main/java/**/*.java"]),
    manifest_entries = [
        "Gerrit-PluginName: reviewers",
        "Gerrit-Module: com.googlesource.gerrit.plugins.reviewers.Module",
    ],
    resources = glob(["src/main/resources/**/*"]),
)

junit_tests(
    name = "reviewers_tests",
    srcs = glob(["src/test/java/**/*.java"]),
    tags = ["reviewers"],
    deps = [
        ":reviewers__plugin_test_deps",
    ],
)

java_library(
    name = "reviewers__plugin_test_deps",
    testonly = 1,
    visibility = ["//visibility:public"],
    exports = PLUGIN_DEPS + PLUGIN_TEST_DEPS + [
        ":reviewers__plugin",
    ],
)
