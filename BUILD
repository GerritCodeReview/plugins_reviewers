load("//tools/bzl:junit.bzl", "junit_tests")
load(
    "//tools/bzl:plugin.bzl",
    "PLUGIN_DEPS",
    "PLUGIN_TEST_DEPS",
    "gerrit_plugin",
)

SRC = "src/main/java/com/googlesource/gerrit/plugins/reviewers/"

BACKEND_SRCS = glob([
    SRC + "common/*.java",
    SRC + "server/*.java",
])

gerrit_plugin(
    name = "reviewers-backend",
    srcs = BACKEND_SRCS,
    dir_name = "reviewers",
    manifest_entries = [
        # Different jar name, but use same plugin name in manifest so REST API is compatible.
        "Gerrit-PluginName: reviewers",
        "Gerrit-Module: com.googlesource.gerrit.plugins.reviewers.server.BackendModule",
    ],
    resources = glob(["src/main/resources/**/*"]),
)

gerrit_plugin(
    name = "reviewers",
    srcs = BACKEND_SRCS + glob([
        SRC + "*.java",
        SRC + "client/*.java",
    ]),
    gwt_module = "com.googlesource.gerrit.plugins.reviewers.ReviewersForm",
    manifest_entries = [
        "Gerrit-PluginName: reviewers",
        "Gerrit-Module: com.googlesource.gerrit.plugins.reviewers.Module",
    ],
    resources = glob(["src/main/**/*"]),
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
