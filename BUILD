load("@rules_java//java:defs.bzl", "java_library")
load("//tools/bzl:junit.bzl", "junit_tests")
load(
    "//tools/bzl:plugin.bzl",
    "PLUGIN_DEPS",
    "PLUGIN_TEST_DEPS",
    "gerrit_plugin",
)
load("//tools/bzl:genrule2.bzl", "genrule2")
load("//tools/bzl:js.bzl", "polygerrit_plugin")

gerrit_plugin(
    name = "reviewers",
    srcs = glob(["src/main/java/**/*.java"]),
    manifest_entries = [
        "Gerrit-PluginName: reviewers",
        "Gerrit-Module: com.googlesource.gerrit.plugins.reviewers.Module",
    ],
    resources = glob(["src/main/resources/**/*"]),
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
