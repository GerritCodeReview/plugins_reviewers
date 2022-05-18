load("@rules_java//java:defs.bzl", "java_library")
load("//tools/bzl:junit.bzl", "junit_tests")
load("//tools/js:eslint.bzl", "plugin_eslint")
load(
    "//tools/bzl:plugin.bzl",
    "PLUGIN_DEPS",
    "PLUGIN_TEST_DEPS",
    "gerrit_plugin",
)
load("//tools/bzl:js.bzl", "gerrit_js_bundle")
load("@npm//@bazel/typescript:index.bzl", "ts_config", "ts_project")

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

ts_config(
    name = "tsconfig",
    src = "tsconfig.json",
    deps = [
        "//plugins:tsconfig-plugins-base.json",
    ],
)

ts_project(
    name = "rv-reviewers-ts",
    srcs = glob([
        "web/**/*.ts",
    ]),
    incremental = True,
    supports_workers = True,
    tsc = "//tools/node_tools:tsc-bin",
    tsconfig = ":tsconfig",
)

gerrit_js_bundle(
    name = "rv-reviewers",
    srcs = [":rv-reviewers-ts"],
    entry_point = "web/plugin.js",
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

plugin_eslint()
