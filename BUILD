load("//tools/bzl:junit.bzl", "junit_tests")
load(
    "//tools/bzl:plugin.bzl",
    "PLUGIN_DEPS",
    "PLUGIN_TEST_DEPS",
    "gerrit_plugin",
)
load("//tools/bzl:genrule2.bzl", "genrule2")
load("//tools/bzl:js.bzl", "polygerrit_plugin")

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
    resource_jars = [":rv-reviewers-static"],
)

genrule2(
    name = "rv-reviewers-static",
    srcs = [":rv-reviewers"],
    outs = ["rv-reviewers-static.jar"],
    cmd = " && ".join([
        "mkdir $$TMP/static",
        "cp -r $(locations :rv-reviewers) $$TMP/static",
        "cd $$TMP",
        "zip -Drq $$ROOT/$@ -g .",
    ]),
)

polygerrit_plugin(
    name = "rv-reviewers",
    srcs = glob([
        "rv-reviewers/*.html",
        "rv-reviewers/*.js",
    ]),
    app = "plugin.html",
)

junit_tests(
    name = "reviewers_tests",
    size = "small",
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
