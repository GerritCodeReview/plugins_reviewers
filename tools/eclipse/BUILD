load(
    "//tools/bzl:plugin.bzl",
    "GWT_PLUGIN_DEPS",
)
load("//tools/bzl:classpath.bzl", "classpath_collector")

classpath_collector(
    name = "main_classpath_collect",
    testonly = 1,
    deps = GWT_PLUGIN_DEPS + [
        "//:reviewers__plugin_test_deps",
    ],
)
