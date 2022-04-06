load("@rules_java//java:defs.bzl", "java_library")
load("@npm//@bazel/rollup:index.bzl", "rollup_bundle")
load("//tools/bzl:junit.bzl", "junit_tests")
load("//tools/js:eslint.bzl", "eslint")
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
    resource_jars = [":rv-reviewers-static"],
    resources = glob(["src/main/resources/**/*"]),
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
    app = "reviewers-bundle.js",
)

rollup_bundle(
    name = "reviewers-bundle",
    srcs = glob(["rv-reviewers/*.js"]),
    entry_point = "rv-reviewers/plugin.js",
    format = "iife",
    rollup_bin = "//tools/node_tools:rollup-bin",
    sourcemap = "hidden",
    deps = [
        "@tools_npm//rollup-plugin-node-resolve",
    ],
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
