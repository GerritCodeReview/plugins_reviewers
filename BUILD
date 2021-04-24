load("@rules_java//java:defs.bzl", "java_library")
load("//tools/bzl:junit.bzl", "junit_tests")
load("//tools/js:eslint.bzl", "eslint")
load(
    "//tools/bzl:plugin.bzl",
    "PLUGIN_DEPS",
    "PLUGIN_TEST_DEPS",
    "gerrit_plugin",
)
load("//tools/bzl:js.bzl", "polygerrit_bundle")

gerrit_plugin(
    name = "reviewers",
    srcs = glob(["src/main/java/**/*.java"]),
    manifest_entries = [
        "Gerrit-PluginName: reviewers",
        "Gerrit-Module: com.googlesource.gerrit.plugins.reviewers.Module",
    ],
    resource_jars = [":rv-reviewers"],
    resources = glob(["src/main/resources/**/*"]),
)

polygerrit_bundle(
    name = "rv-reviewers",
    srcs = glob(["rv-reviewers/*.js"]),
    entry_point = "rv-reviewers/plugin.js",
)

junit_tests(
    name = "reviewers_tests",
    size = "small",
    srcs = glob(["src/test/java/**/*.java"]),
    tags = ["reviewers"],
    deps = PLUGIN_DEPS + PLUGIN_TEST_DEPS + [
        ":reviewers__plugin",
    ],
)

# Define the eslinter for the plugin
# The eslint macro creates 2 rules: lint_test and lint_bin
eslint(
    name = "lint",
    srcs = glob([
        "rv-reviewers/*.js",
    ]),
    config = ".eslintrc.json",
    data = [],
    extensions = [
        ".js",
    ],
    ignore = ".eslintignore",
    plugins = [
        "@npm//eslint-config-google",
        "@npm//eslint-plugin-html",
        "@npm//eslint-plugin-import",
        "@npm//eslint-plugin-jsdoc",
    ],
)
